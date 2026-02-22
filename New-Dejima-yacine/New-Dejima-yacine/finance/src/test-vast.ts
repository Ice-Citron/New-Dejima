/**
 * Spin up a Vast.ai GPU instance, print SSH credentials, keep alive for 5 min.
 * Ctrl+C or auto-destroy after timeout.
 */
import { config } from "dotenv";
config();

const VAST_API_BASE = "https://console.vast.ai/api/v0";
const KEEP_ALIVE_SECONDS = 300; // 5 minutes

async function vastApi(method: string, path: string, body?: any): Promise<any> {
  const apiKey = process.env.VAST_API_KEY;
  if (!apiKey) throw new Error("VAST_API_KEY not set");
  const response = await fetch(`${VAST_API_BASE}${path}`, {
    method,
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  });
  const text = await response.text();
  if (!response.ok) throw new Error(`${method} ${path} → ${response.status}: ${text}`);
  return JSON.parse(text);
}

async function main() {
  let instanceId: number | null = null;

  // Cleanup on exit
  const cleanup = async () => {
    if (instanceId) {
      console.log(`\n\nDestroying instance ${instanceId}...`);
      try {
        await vastApi("DELETE", `/instances/${instanceId}/`);
        console.log("Instance destroyed. Goodbye.");
      } catch (e: any) {
        console.error("Destroy failed:", e.message);
      }
    }
    process.exit(0);
  };
  process.on("SIGINT", cleanup);
  process.on("SIGTERM", cleanup);

  // ── 1. Register SSH key ──────────────────────────────────────────────────
  console.log("Registering SSH key...");
  const { readFileSync, existsSync } = await import("fs");
  const { homedir } = await import("os");
  const keyPaths = [`${homedir()}/.ssh/id_ed25519.pub`, `${homedir()}/.ssh/id_rsa.pub`];
  const pubKeyPath = keyPaths.find(p => existsSync(p));
  if (!pubKeyPath) throw new Error("No SSH public key — run: ssh-keygen -t ed25519");
  const pubKey = readFileSync(pubKeyPath, "utf-8").trim();

  const user = await vastApi("GET", "/users/current/");
  await vastApi("PUT", `/users/${user.id}/`, { ssh_key: pubKey });
  console.log(`SSH key registered (user: ${user.email})\n`);

  // ── 2. Find cheapest GPU with ≥14GB VRAM ────────────────────────────────
  console.log("Searching for cheapest GPU with ≥14GB VRAM...");
  const query = {
    rented: { eq: false },
    rentable: { eq: true },
    disk_space: { gte: 40 },
    dph_total: { lte: 1.00 },
    gpu_ram: { gte: 14000 },
  };
  const data = await vastApi(
    "GET",
    `/bundles/?q=${encodeURIComponent(JSON.stringify(query))}&order_by=dph_total&type=ask&limit=10`
  );
  const offers: any[] = data.offers ?? (Array.isArray(data) ? data : []);
  if (!offers.length) throw new Error("No GPUs available right now");
  offers.sort((a, b) => a.dph_total - b.dph_total);
  const offer = offers[0];
  console.log(`Found: ${offer.gpu_name} (${Math.round(offer.gpu_ram / 1024)}GB VRAM) @ $${offer.dph_total.toFixed(3)}/hr — ${offer.geolocation}`);

  // ── 3. Create instance ───────────────────────────────────────────────────
  console.log("\nCreating instance (Ubuntu + CUDA, no model load — just SSH)...");
  const body = {
    client_id: "me",
    image: "nvidia/cuda:12.1.0-runtime-ubuntu22.04",
    disk: 40,
    label: `dejima-test-${Date.now()}`,
    onstart: "#!/bin/bash\nnvidia-smi > /tmp/nvidia-smi.txt\necho 'GPU ready' >> /tmp/nvidia-smi.txt\ntail -f /dev/null",
    runtype: "ssh",
    bid_price: offer.dph_total * 1.1,
    ports: "22/tcp",
  };
  const created = await vastApi("PUT", `/asks/${offer.id}/`, body);
  instanceId = created.new_contract ?? created.id ?? created.instance_id;
  if (!instanceId) throw new Error(`Unexpected response: ${JSON.stringify(created)}`);
  console.log(`Instance ID: ${instanceId}\n`);

  // ── 4. Poll until SSH ready ──────────────────────────────────────────────
  process.stdout.write("Waiting for instance to start");
  const deadline = Date.now() + 5 * 60 * 1000;
  let instance: any = null;
  while (Date.now() < deadline) {
    try {
      const resp = await vastApi("GET", `/instances/?owner=me`);
      const inst = (resp.instances ?? []).find((i: any) => i.id === instanceId);
      if (inst?.actual_status === "running" && inst.ssh_host && inst.ssh_port) {
        instance = inst;
        break;
      }
      if (inst?.actual_status === "error") throw new Error("Instance entered error state");
    } catch (e: any) {
      if (e.message.includes("error state")) throw e;
    }
    process.stdout.write(".");
    await new Promise(r => setTimeout(r, 5000));
  }
  if (!instance) throw new Error("Instance did not start within 5 minutes");
  console.log(" ready!\n");

  // Wait for SSH daemon + attach key to running instance
  await new Promise(r => setTimeout(r, 5000));
  console.log("Attaching SSH key to instance...");
  const attachResp = await vastApi("POST", `/instances/${instanceId}/ssh/`, { ssh_key: pubKey });
  console.log("Key attached:", JSON.parse(attachResp.key ?? "{}").id ?? "ok");
  console.log("Waiting 15s for key to propagate...");
  await new Promise(r => setTimeout(r, 15_000));

  // ── 5. Print SSH credentials ─────────────────────────────────────────────
  const privKeyPath = pubKeyPath.replace(".pub", "");
  console.log("╔═══════════════════════════════════════════════════════════╗");
  console.log("║              SSH CREDENTIALS — COPY THIS                  ║");
  console.log("╠═══════════════════════════════════════════════════════════╣");
  console.log(`║  GPU:    ${instance.gpu_name?.padEnd(49) ?? ""}║`);
  console.log(`║  Host:   ${instance.ssh_host?.padEnd(49) ?? ""}║`);
  console.log(`║  Port:   ${String(instance.ssh_port).padEnd(49)}║`);
  console.log("╠═══════════════════════════════════════════════════════════╣");
  console.log("║  RUN THIS IN ANOTHER TERMINAL:                            ║");
  console.log(`║  ssh -p ${instance.ssh_port} -i ${privKeyPath}`.padEnd(62) + "║");
  console.log(`║      root@${instance.ssh_host}`.padEnd(62) + "║");
  console.log("╠═══════════════════════════════════════════════════════════╣");
  console.log("║  THEN RUN:  nvidia-smi                                    ║");
  console.log("╚═══════════════════════════════════════════════════════════╝\n");

  // Also print one-liner for easy copy-paste
  console.log(`ONE-LINER: ssh -o StrictHostKeyChecking=no -p ${instance.ssh_port} -i ${privKeyPath} root@${instance.ssh_host}\n`);
  console.log(`Cost so far: ~$${(offer.dph_total / 3600 * 10).toFixed(5)} (10 seconds)\n`);

  // ── 6. Countdown ─────────────────────────────────────────────────────────
  console.log(`Keeping instance alive for ${KEEP_ALIVE_SECONDS}s — press Ctrl+C to destroy early\n`);
  for (let i = KEEP_ALIVE_SECONDS; i > 0; i--) {
    process.stdout.write(`\r  Destroying in ${i}s... (Ctrl+C to stop early)   `);
    await new Promise(r => setTimeout(r, 1000));
  }

  await cleanup();
}

main().catch(async (err) => {
  console.error("\nError:", err.message);
  process.exit(1);
});
