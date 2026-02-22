/**
 * Vast.ai GPU VM provisioner for New Dejima agent infrastructure.
 *
 * Full real flow:
 *   1. Register SSH key with Vast.ai
 *   2. Search cheapest available T4 (< $0.30/hr)
 *   3. Create instance with vLLM + model onstart script
 *   4. Poll until instance is running + SSH is up
 *   5. Run nvidia-smi via SSH → proves real GPU
 *   6. Start SSH tunnel to port 8000 for vLLM access
 *   7. Poll vLLM /v1/models until model is loaded
 *   8. Return instance details + endpoint to caller
 */
import { execSync, spawn } from "child_process";
import { readFileSync, existsSync } from "fs";
import { homedir } from "os";
import dotenv from "dotenv";
dotenv.config();

const VAST_API_BASE = "https://console.vast.ai/api/v0";

// ─────────────────────────────────────────────────────────────────────────────
// SSH key helpers
// ─────────────────────────────────────────────────────────────────────────────

function getSSHPublicKey(): string {
  const candidates = [
    `${homedir()}/.ssh/id_ed25519.pub`,
    `${homedir()}/.ssh/id_rsa.pub`,
    `${homedir()}/.ssh/id_ecdsa.pub`,
  ];
  for (const path of candidates) {
    if (existsSync(path)) return readFileSync(path, "utf-8").trim();
  }
  throw new Error("No SSH public key found. Run: ssh-keygen -t ed25519");
}

function getSSHPrivateKeyPath(): string {
  const candidates = [
    `${homedir()}/.ssh/id_ed25519`,
    `${homedir()}/.ssh/id_rsa`,
    `${homedir()}/.ssh/id_ecdsa`,
  ];
  for (const path of candidates) {
    if (existsSync(path)) return path;
  }
  throw new Error("No SSH private key found. Run: ssh-keygen -t ed25519");
}

// ─────────────────────────────────────────────────────────────────────────────
// Vast.ai REST API wrapper
// ─────────────────────────────────────────────────────────────────────────────

async function vastApi(method: string, path: string, body?: any): Promise<any> {
  const apiKey = process.env.VAST_API_KEY;
  if (!apiKey) throw new Error("VAST_API_KEY not set in .env — add it from https://cloud.vast.ai/cli/");

  const response = await fetch(`${VAST_API_BASE}${path}`, {
    method,
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
      "Accept": "application/json",
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`Vast.ai ${method} ${path} → ${response.status}: ${text}`);
  }
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Register SSH public key with Vast.ai account
// ─────────────────────────────────────────────────────────────────────────────

async function registerSSHKey(): Promise<string> {
  const publicKey = getSSHPublicKey();
  try {
    // Get user ID first, then update via /users/<id>/
    const user = await vastApi("GET", "/users/current/");
    const userId = user.id;
    await vastApi("PUT", `/users/${userId}/`, { ssh_key: publicKey });
    console.log(`  [vast] SSH key registered for user ${userId}`);
  } catch (err: any) {
    console.warn(`  [vast] SSH key register warning: ${err.message}`);
  }
  return publicKey;
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — Search cheapest available T4
// ─────────────────────────────────────────────────────────────────────────────

async function searchCheapestGpu(minVramMb: number = 14000): Promise<any> {
  // Search for cheapest GPU with enough VRAM for a 7B fp16 model (~14GB)
  // No compute_cap filter — V100 (cap 700), T4 (cap 750), Ampere (cap 800+) all work with vLLM fp16
  const query = {
    "rented": { "eq": false },
    "rentable": { "eq": true },
    "disk_space": { "gte": 40 },
    "dph_total": { "lte": 1.00 },
    "gpu_ram": { "gte": minVramMb },
  };

  const data = await vastApi(
    "GET",
    `/bundles/?q=${encodeURIComponent(JSON.stringify(query))}&order_by=dph_total&type=ask&limit=20`
  );

  const offers: any[] = data.offers ?? (Array.isArray(data) ? data : []);
  if (!offers.length) {
    throw new Error("No GPU instances available right now with enough VRAM");
  }

  // Pick cheapest
  offers.sort((a, b) => (a.dph_total ?? 99) - (b.dph_total ?? 99));
  return offers[0];
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Create instance with vLLM onstart
// ─────────────────────────────────────────────────────────────────────────────

async function createInstance(
  offerId: number,
  bidPrice: number,
  model: string
): Promise<number> {
  // Startup script: capture nvidia-smi, then launch vLLM server
  // Using fp16 on Ampere+ GPUs. --enforce-eager skips torch.compile/CUDA graph
  // compilation so the server starts in ~2 min instead of 30+ min.
  const onstart = [
    "#!/bin/bash",
    "nvidia-smi | tee /tmp/nvidia-smi.txt",
    "echo '[dejima] nvidia-smi done, starting vLLM...'",
    `python3 -m vllm.entrypoints.openai.api_server \\`,
    `  --model ${model} \\`,
    `  --dtype half \\`,
    `  --max-model-len 4096 \\`,
    `  --port 8000 \\`,
    `  --host 0.0.0.0 \\`,
    `  --enforce-eager 2>&1 | tee /tmp/vllm.log`,
  ].join("\n");

  const body = {
    client_id: "me",
    image: "vllm/vllm-openai:latest",
    disk: 40,
    label: `dejima-${Date.now()}`,
    onstart,
    runtype: "ssh",
    bid_price: bidPrice * 1.1, // bid 10% above floor to get it quickly
    ports: "8000/tcp",        // expose vLLM port
  };

  const data = await vastApi("PUT", `/asks/${offerId}/`, body);

  // Response may contain new_contract (instance id) or id
  const instanceId = data.new_contract ?? data.id ?? data.instance_id;
  if (!instanceId) {
    throw new Error(`Unexpected response from /asks/${offerId}/: ${JSON.stringify(data)}`);
  }
  return instanceId;
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4 — Poll until SSH is available
// ─────────────────────────────────────────────────────────────────────────────

async function pollUntilSSHReady(
  instanceId: number,
  timeoutMs: number = 300_000
): Promise<any> {
  const deadline = Date.now() + timeoutMs;
  process.stdout.write("  [vast] Waiting for instance");

  while (Date.now() < deadline) {
    try {
      // Must use ?owner=me — individual instance endpoint requires session auth
      const data = await vastApi("GET", `/instances/?owner=me`);
      const instances: any[] = data.instances ?? [];
      const inst = instances.find((i: any) => i.id === instanceId);

      if (inst && inst.actual_status === "running" && inst.ssh_host && inst.ssh_port) {
        console.log(" ready!");
        return inst;
      }
      if (inst && (inst.actual_status === "error" || inst.actual_status === "failed")) {
        throw new Error(`Instance ${instanceId} entered error state: ${inst.status_msg}`);
      }
    } catch (err: any) {
      if (err.message.includes("error state")) throw err;
    }
    process.stdout.write(".");
    await new Promise(r => setTimeout(r, 5_000));
  }
  throw new Error(`Instance ${instanceId} did not become ready within ${timeoutMs / 1000}s`);
}

async function attachSSHKeyToInstance(instanceId: number, publicKey: string): Promise<void> {
  try {
    await vastApi("POST", `/instances/${instanceId}/ssh/`, { ssh_key: publicKey });
    // Wait for key to propagate into the instance's authorized_keys
    await new Promise(r => setTimeout(r, 15_000));
  } catch (err: any) {
    console.warn(`  [vast] SSH key attach warning: ${err.message}`);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 5 — Run command via SSH
// ─────────────────────────────────────────────────────────────────────────────

function sshRun(host: string, port: number, cmd: string): string {
  const key = getSSHPrivateKeyPath();
  return execSync(
    `ssh -o StrictHostKeyChecking=no -o ConnectTimeout=15 -o ServerAliveInterval=10 ` +
    `-i "${key}" -p ${port} root@${host} ${JSON.stringify(cmd)}`,
    { encoding: "utf-8", timeout: 30_000 }
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 6 — SSH tunnel to vLLM port 8000
// ─────────────────────────────────────────────────────────────────────────────

let tunnelProcess: ReturnType<typeof spawn> | null = null;

function openSSHTunnel(host: string, port: number, localPort: number = 8001): void {
  const key = getSSHPrivateKeyPath();
  tunnelProcess = spawn("ssh", [
    "-N",
    "-o", "StrictHostKeyChecking=no",
    "-o", "ConnectTimeout=15",
    "-i", key,
    "-p", String(port),
    `-L`, `${localPort}:localhost:8000`,
    `root@${host}`,
  ], { stdio: "ignore", detached: true });
  tunnelProcess.unref();
}

export function closeSSHTunnel(): void {
  if (tunnelProcess) {
    tunnelProcess.kill();
    tunnelProcess = null;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 7 — Poll vLLM until model is loaded
// ─────────────────────────────────────────────────────────────────────────────

async function pollVllmReady(
  localPort: number = 8001,
  timeoutMs: number = 600_000  // 10 min: model download ~2min + load ~2min on Ampere GPU
): Promise<boolean> {
  const deadline = Date.now() + timeoutMs;
  process.stdout.write("  [vast] Waiting for vLLM model to load");

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
  console.log(" timeout — model may still be downloading");
  return false;
}

// ─────────────────────────────────────────────────────────────────────────────
// Public types + master function
// ─────────────────────────────────────────────────────────────────────────────

export interface VastInstance {
  instanceId: number;
  sshHost: string;
  sshPort: number;
  gpuName: string;
  pricePerHour: number;
  nvidiaSmiOutput: string;
  apiEndpoint: string;        // tunnelled local endpoint
  externalApiEndpoint: string; // direct (may need port forward)
  model: string;
  status: "ready" | "provisioning" | "model_loading";
  provisionedAt: string;
}

export async function provisionVastInstance(
  model: string = "Qwen/Qwen2.5-7B-Instruct",
  localTunnelPort: number = 8001
): Promise<VastInstance> {
  console.log("  [vast] Registering SSH key with Vast.ai...");
  await registerSSHKey();

  console.log("  [vast] Searching for cheapest GPU with ≥14GB VRAM...");
  const offer = await searchCheapestGpu();
  console.log(`  [vast] Found: ${offer.gpu_name} @ $${Number(offer.dph_total).toFixed(3)}/hr — host: ${offer.geolocation ?? "unknown"}`);

  console.log("  [vast] Creating instance...");
  const instanceId = await createInstance(offer.id, offer.dph_total, model);
  console.log(`  [vast] Instance ID: ${instanceId}`);

  // Poll until SSH is ready
  const instance = await pollUntilSSHReady(instanceId);
  const { ssh_host: sshHost, ssh_port: sshPort } = instance;

  // Wait a few seconds for SSH daemon to fully start
  await new Promise(r => setTimeout(r, 5_000));

  // Attach SSH key to the running instance (key must be pushed post-launch)
  const publicKey = getSSHPublicKey();
  console.log("  [vast] Attaching SSH key to instance...");
  await attachSSHKeyToInstance(instanceId, publicKey);

  // Run nvidia-smi to prove real GPU — retry up to 4 times
  console.log("  [vast] Running nvidia-smi on the instance...");
  let nvidiaSmiOutput = "";
  for (let attempt = 1; attempt <= 4; attempt++) {
    try {
      nvidiaSmiOutput = sshRun(sshHost, sshPort, "nvidia-smi");
      console.log("\n" + nvidiaSmiOutput);
      break;
    } catch (err: any) {
      if (attempt < 4) {
        process.stdout.write(`  [vast] SSH attempt ${attempt} failed, retrying in 5s...`);
        await new Promise(r => setTimeout(r, 5_000));
      } else {
        nvidiaSmiOutput = `[SSH failed after ${attempt} attempts: ${err.message}]`;
        console.warn(`\n  [vast] nvidia-smi failed: ${err.message}`);
      }
    }
  }

  // Open SSH tunnel to vLLM port
  console.log(`  [vast] Opening SSH tunnel localhost:${localTunnelPort} → instance:8000...`);
  openSSHTunnel(sshHost, sshPort, localTunnelPort);
  await new Promise(r => setTimeout(r, 3_000));

  // Wait for vLLM model to finish loading
  const modelReady = await pollVllmReady(localTunnelPort);

  // Make a test inference call to prove the model is actually serving
  if (modelReady) {
    console.log("  [vast] Running test inference call...");
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
      console.log(`  [vast] Model response: "${reply.trim()}"`);
    } catch (err: any) {
      console.warn(`  [vast] Test inference failed: ${err.message}`);
    }
  }

  return {
    instanceId,
    sshHost,
    sshPort,
    gpuName: instance.gpu_name ?? "Tesla T4",
    pricePerHour: instance.dph_total ?? offer.dph_total,
    nvidiaSmiOutput,
    apiEndpoint: `http://localhost:${localTunnelPort}/v1`,
    externalApiEndpoint: `http://${sshHost}:8000/v1`,
    model,
    status: modelReady ? "ready" : "model_loading",
    provisionedAt: new Date().toISOString(),
  };
}

export async function destroyVastInstance(instanceId: number): Promise<void> {
  closeSSHTunnel();
  // Vast.ai DELETE requires an empty JSON body to pass auth
  await vastApi("DELETE", `/instances/${instanceId}/`, {});
  console.log(`  [vast] Instance ${instanceId} destroyed`);
}
