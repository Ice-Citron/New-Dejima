/**
 * New Dejima — GCP Compute Engine GPU provisioner
 *
 * Flow:
 *   1. Create n1-standard-4 + T4 GPU instance (Deep Learning VM image, CUDA pre-installed)
 *   2. Poll until RUNNING + external IP assigned
 *   3. Wait for SSH to be ready
 *   4. Run nvidia-smi via gcloud compute ssh → prove real GPU
 *   5. Open SSH tunnel localhost:8001 → instance:8000 for vLLM
 *   6. Poll vLLM /v1/models until model is loaded
 *   7. Run test inference call
 *   8. Return instance details + endpoint
 */
import { execSync, spawn } from "child_process";
import dotenv from "dotenv";
dotenv.config();

const PROJECT = process.env.GCP_PROJECT ?? "project-2c418787-8ea4-496d-a91";

// GPU candidates tried in order — first available wins
// L4 first: T4s are chronically exhausted in us-central1
const GPU_CANDIDATES = [
  { zone: "us-central1-a", machineType: "g2-standard-4", gpuType: "nvidia-l4",        pricePerHour: 0.92 },
  { zone: "us-central1-b", machineType: "g2-standard-4", gpuType: "nvidia-l4",        pricePerHour: 0.92 },
  { zone: "us-central1-b", machineType: "n1-standard-4", gpuType: "nvidia-tesla-t4", pricePerHour: 0.54 },
  { zone: "us-central1-c", machineType: "n1-standard-4", gpuType: "nvidia-tesla-t4", pricePerHour: 0.54 },
  { zone: "us-central1-f", machineType: "n1-standard-4", gpuType: "nvidia-tesla-t4", pricePerHour: 0.54 },
  { zone: "us-east1-c",    machineType: "n1-standard-4", gpuType: "nvidia-tesla-t4", pricePerHour: 0.54 },
];
// Deep Learning VM: CUDA 12.8 + NVIDIA 570 driver pre-installed (Ubuntu 22.04)
const IMAGE_FAMILY  = "common-cu128-ubuntu-2204-nvidia-570";
const IMAGE_PROJECT = "deeplearning-platform-release";

// ─────────────────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────────────────

export interface GcpInstance {
  instanceName: string;
  externalIp: string;
  zone: string;
  gpuType: string;
  pricePerHour: number;
  nvidiaSmiOutput: string;
  apiEndpoint: string;         // tunnelled local endpoint
  externalApiEndpoint: string; // direct external (instance:8000)
  model: string;
  status: "ready" | "provisioning" | "model_loading";
  provisionedAt: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

function gcloud(args: string, timeoutMs: number = 60_000, zone?: string): string {
  const zonePart = zone ? `--zone=${zone}` : "";
  return execSync(`gcloud ${args} ${zonePart} --project=${PROJECT} --quiet`, {
    encoding: "utf-8",
    timeout: timeoutMs,
  });
}

function gcloudJson(args: string, timeoutMs: number = 60_000, zone?: string): any {
  const out = gcloud(`${args} --format=json`, timeoutMs, zone);
  return JSON.parse(out);
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Create instance
// ─────────────────────────────────────────────────────────────────────────────

async function createInstance(
  instanceName: string,
  model: string,
  zone: string,
  machineType: string,
  gpuType: string
): Promise<void> {
  // DL VM image: deeplearning-platform-release/common-cu128-ubuntu-2204-nvidia-570
  // Has: CUDA 12.8, NVIDIA 570 driver, Python 3.10
  // Needs: Docker + nvidia-container-toolkit installed by us
  const startupScript = [
    "#!/bin/bash",
    "exec > /tmp/startup.log 2>&1",
    "set -ex",
    "export PATH=$PATH:/usr/bin:/usr/local/bin:/usr/local/cuda/bin",
    "",
    "echo '[dejima] Waiting for GPU drivers...'",
    "sleep 30",
    "nvidia-smi | tee /tmp/nvidia-smi.txt",
    "",
    "# Install Docker if not present",
    "if ! command -v docker &>/dev/null; then",
    "  echo '[dejima] Installing Docker...'",
    "  curl -fsSL https://get.docker.com | sh",
    "fi",
    "",
    "# Install nvidia-container-toolkit",
    "if ! command -v nvidia-ctk &>/dev/null; then",
    "  echo '[dejima] Installing nvidia-container-toolkit...'",
    "  curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg",
    "  curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list | sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | tee /etc/apt/sources.list.d/nvidia-container-toolkit.list",
    "  apt-get update -qq",
    "  apt-get install -y -qq nvidia-container-toolkit",
    "fi",
    "",
    "# Configure Docker for GPU and restart",
    "nvidia-ctk runtime configure --runtime=docker",
    "systemctl restart docker",
    "sleep 5",
    "",
    "echo '[dejima] Starting vLLM Docker container...'",
    "docker run -d \\",
    "  --gpus all \\",
    "  -p 8000:8000 \\",
    "  --name vllm-server \\",
    "  --restart unless-stopped \\",
    `  vllm/vllm-openai:latest \\`,
    `  --model ${model} \\`,
    "  --quantization awq \\",
    "  --dtype half \\",
    "  --max-model-len 4096 \\",
    "  --port 8000 \\",
    "  --host 0.0.0.0 \\",
    "  --enforce-eager 2>&1 | tee /tmp/vllm.log",
    "",
    "echo '[dejima] Startup script complete.'",
  ].join("\n");

  const { writeFileSync } = await import("fs");
  const { tmpdir } = await import("os");
  const scriptPath = `${tmpdir()}/dejima-startup-${Date.now()}.sh`;
  writeFileSync(scriptPath, startupScript);

  gcloud([
    `compute instances create ${instanceName}`,
    `--machine-type=${machineType}`,
    `--accelerator=type=${gpuType},count=1`,
    `--image-family=${IMAGE_FAMILY}`,
    `--image-project=${IMAGE_PROJECT}`,
    `--boot-disk-size=80GB`,
    `--boot-disk-type=pd-ssd`,
    `--maintenance-policy=TERMINATE`,
    `--metadata-from-file=startup-script=${scriptPath}`,
    `--scopes=default`,
  ].join(" "), 180_000, zone);
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — Poll until instance is RUNNING and has an external IP
// ─────────────────────────────────────────────────────────────────────────────

async function pollUntilRunning(
  instanceName: string,
  zone: string,
  timeoutMs: number = 300_000
): Promise<string> {
  const deadline = Date.now() + timeoutMs;
  process.stdout.write("  [gcp] Waiting for instance");

  while (Date.now() < deadline) {
    try {
      const data = gcloudJson(`compute instances describe ${instanceName}`, 30_000, zone);
      if (data.status === "RUNNING") {
        const ip = data.networkInterfaces?.[0]?.accessConfigs?.[0]?.natIP;
        if (ip) {
          console.log(` ready! (${ip})`);
          return ip;
        }
      }
    } catch {
      // Still starting
    }
    process.stdout.write(".");
    await new Promise(r => setTimeout(r, 5_000));
  }
  throw new Error(`GCP instance ${instanceName} did not become RUNNING within ${timeoutMs / 1000}s`);
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Wait for SSH to accept connections
// ─────────────────────────────────────────────────────────────────────────────

async function waitForSSH(instanceName: string, zone: string, timeoutMs: number = 180_000): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  process.stdout.write("  [gcp] Waiting for SSH");

  while (Date.now() < deadline) {
    try {
      gcloud(
        `compute ssh ${instanceName} --command="echo ok" --ssh-flag="-o ConnectTimeout=10"`,
        20_000, zone
      );
      console.log(" ready!");
      return;
    } catch {
      process.stdout.write(".");
      await new Promise(r => setTimeout(r, 8_000));
    }
  }
  throw new Error("SSH never became ready");
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4 — Run command via SSH
// ─────────────────────────────────────────────────────────────────────────────

function sshRun(instanceName: string, zone: string, cmd: string): string {
  return gcloud(
    `compute ssh ${instanceName} --command=${JSON.stringify(cmd)} --ssh-flag="-o ConnectTimeout=15"`,
    30_000, zone
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 5 — SSH tunnel to vLLM port 8000
// ─────────────────────────────────────────────────────────────────────────────

let tunnelProcess: ReturnType<typeof spawn> | null = null;

function openSSHTunnel(instanceName: string, zone: string, localPort: number = 8001): void {
  tunnelProcess = spawn("gcloud", [
    "compute", "ssh", instanceName,
    `--zone=${zone}`,
    `--project=${PROJECT}`,
    "--",
    "-N",
    "-L", `${localPort}:localhost:8000`,
    "-o", "StrictHostKeyChecking=no",
    "-o", "ConnectTimeout=15",
  ], { stdio: "ignore", detached: true });
  tunnelProcess.unref();
}

export function closeGcpSSHTunnel(): void {
  if (tunnelProcess) {
    tunnelProcess.kill();
    tunnelProcess = null;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 6 — Poll vLLM until model is loaded
// ─────────────────────────────────────────────────────────────────────────────

async function pollVllmReady(
  localPort: number = 8001,
  timeoutMs: number = 900_000  // 15 min: Docker pull + model download
): Promise<boolean> {
  const deadline = Date.now() + timeoutMs;
  process.stdout.write("  [gcp] Waiting for vLLM model to load");

  while (Date.now() < deadline) {
    try {
      const r = await fetch(`http://localhost:${localPort}/v1/models`, {
        signal: AbortSignal.timeout(5_000),
      });
      if (r.ok) {
        const data = await r.json() as any;
        if (data.data?.length > 0) {
          console.log(` loaded! (${data.data[0].id})`);
          return true;
        }
      }
    } catch {
      // Not ready yet
    }
    process.stdout.write(".");
    await new Promise(r => setTimeout(r, 10_000));
  }
  console.log(" timeout — model may still be loading");
  return false;
}

// ─────────────────────────────────────────────────────────────────────────────
// Master function
// ─────────────────────────────────────────────────────────────────────────────

export async function provisionGcpInstance(
  model: string = "Qwen/Qwen2.5-7B-Instruct-AWQ",
  localTunnelPort: number = 8001
): Promise<GcpInstance> {
  const instanceName = `dejima-gpu-${Date.now()}`;

  let chosenZone = "";
  let chosenGpuType = "";
  let chosenPrice = 0;

  for (const candidate of GPU_CANDIDATES) {
    console.log(`  [gcp] Trying ${candidate.gpuType} in ${candidate.zone}...`);
    try {
      await createInstance(instanceName, model, candidate.zone, candidate.machineType, candidate.gpuType);
      chosenZone    = candidate.zone;
      chosenGpuType = candidate.gpuType;
      chosenPrice   = candidate.pricePerHour;
      console.log(`  [gcp] Instance created in ${chosenZone} (${chosenGpuType})`);
      break;
    } catch (err: any) {
      if (err.message.includes("ZONE_RESOURCE_POOL_EXHAUSTED") || err.message.includes("does not have enough resources")) {
        console.warn(`  [gcp] ${candidate.zone} exhausted, trying next...`);
        continue;
      }
      throw err;
    }
  }

  if (!chosenZone) {
    throw new Error("All GPU candidates exhausted — no GPU instances available right now");
  }

  const externalIp = await pollUntilRunning(instanceName, chosenZone);

  await new Promise(r => setTimeout(r, 10_000));
  await waitForSSH(instanceName, chosenZone);

  console.log("  [gcp] Waiting 60s for GPU drivers to initialise...");
  await new Promise(r => setTimeout(r, 60_000));
  console.log("  [gcp] Running nvidia-smi...");
  let nvidiaSmiOutput = "";
  for (let attempt = 1; attempt <= 6; attempt++) {
    try {
      nvidiaSmiOutput = sshRun(
        instanceName, chosenZone,
        "nvidia-smi || /usr/bin/nvidia-smi || /usr/local/cuda/bin/nvidia-smi || sudo nvidia-smi"
      );
      console.log("\n" + nvidiaSmiOutput);
      break;
    } catch (err: any) {
      if (attempt < 6) {
        process.stdout.write(`  [gcp] nvidia-smi attempt ${attempt} failed, retrying in 15s...`);
        await new Promise(r => setTimeout(r, 15_000));
      } else {
        nvidiaSmiOutput = `[nvidia-smi failed after ${attempt} attempts: ${err.message}]`;
        console.warn(`\n  [gcp] nvidia-smi failed: ${err.message}`);
      }
    }
  }

  console.log(`  [gcp] Opening SSH tunnel localhost:${localTunnelPort} → instance:8000...`);
  openSSHTunnel(instanceName, chosenZone, localTunnelPort);
  await new Promise(r => setTimeout(r, 5_000));

  const modelReady = await pollVllmReady(localTunnelPort);

  if (modelReady) {
    console.log("  [gcp] Running test inference call...");
    try {
      const resp = await fetch(`http://localhost:${localTunnelPort}/v1/chat/completions`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          model,
          messages: [{ role: "user", content: "Reply with exactly: DEJIMA_ONLINE" }],
          max_tokens: 10,
        }),
        signal: AbortSignal.timeout(30_000),
      });
      const data = await resp.json() as any;
      const reply = data.choices?.[0]?.message?.content ?? "(no response)";
      console.log(`  [gcp] Model response: "${reply.trim()}"`);
    } catch (err: any) {
      console.warn(`  [gcp] Test inference failed: ${err.message}`);
    }
  }

  return {
    instanceName,
    externalIp,
    zone: chosenZone,
    gpuType: chosenGpuType,
    pricePerHour: chosenPrice,
    nvidiaSmiOutput,
    apiEndpoint: `http://localhost:${localTunnelPort}/v1`,
    externalApiEndpoint: `http://${externalIp}:8000/v1`,
    model,
    status: modelReady ? "ready" : "model_loading",
    provisionedAt: new Date().toISOString(),
  };
}

export async function destroyGcpInstance(instanceName: string, zone?: string): Promise<void> {
  closeGcpSSHTunnel();
  const zones = zone ? [zone] : GPU_CANDIDATES.map(c => c.zone);
  for (const z of [...new Set(zones)]) {
    try {
      gcloud(`compute instances delete ${instanceName}`, 60_000, z);
      console.log(`  [gcp] Instance ${instanceName} deleted from ${z}`);
      return;
    } catch {
      // Try next zone
    }
  }
  console.warn(`  [gcp] Could not delete ${instanceName} — may need manual cleanup`);
}
