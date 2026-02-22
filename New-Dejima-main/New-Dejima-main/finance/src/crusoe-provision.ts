/**
 * Crusoe VM provisioning for New Dejima agent infrastructure.
 *
 * Tries the real Crusoe cloud API first. Falls back to mock if unavailable.
 * In mock mode, notes that the Qwen model is already running on Crusoe's
 * hackathon inference endpoint.
 */
import dotenv from "dotenv";
dotenv.config();

export interface CrusoeInstance {
  instanceId: string;
  ip: string | null;
  apiEndpoint: string;   // vLLM endpoint once model is loaded
  model: string;
  status: "provisioning" | "ready" | "mock";
  provisionedAt: string;
}

const TIER_SPECS: Record<string, { vcpu: number; ram: string; gpu: string }> = {
  small:  { vcpu: 8,  ram: "32GB",  gpu: "A40" },
  medium: { vcpu: 16, ram: "80GB",  gpu: "A100 40GB" },
  large:  { vcpu: 32, ram: "160GB", gpu: "A100 80GB" },
};

const CRUSOE_API_BASE = "https://api.crusoe.ai/v1alpha5";
// Hackathon inference endpoint — Qwen model already loaded here
const HACKATHON_INFERENCE_ENDPOINT = "https://hackeurope.crusoecloud.com/v1";

export async function provisionInstance(
  tier: "small" | "medium" | "large",
  model: "qwen-14b" | "qwen-70b"
): Promise<CrusoeInstance> {
  const apiKey = process.env.CRUSOE_API_KEY;

  if (apiKey) {
    try {
      const spec = TIER_SPECS[tier];
      const response = await fetch(`${CRUSOE_API_BASE}/instances`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name: `dejima-${model}-${Date.now()}`,
          type: tier,
          location: "us-east1",
          gpu_count: 1,
          startup_script: `#!/bin/bash\npip install vllm && python -m vllm.entrypoints.openai.api_server --model Qwen/${model === "qwen-14b" ? "Qwen2.5-14B-Instruct" : "Qwen2.5-72B-Instruct"} --port 8000`,
          metadata: { managed_by: "new-dejima", model },
        }),
      });

      if (response.ok) {
        const data = await response.json() as any;
        return {
          instanceId: data.id ?? `ci-${Math.random().toString(36).slice(2, 9)}`,
          ip: data.network_interfaces?.[0]?.public_ipv4 ?? null,
          apiEndpoint: data.network_interfaces?.[0]?.public_ipv4
            ? `http://${data.network_interfaces[0].public_ipv4}:8000/v1`
            : HACKATHON_INFERENCE_ENDPOINT,
          model,
          status: "provisioning",
          provisionedAt: new Date().toISOString(),
        };
      }
      // Non-2xx → fall through to mock
      console.warn(`  [crusoe-provision] API returned ${response.status} — using mock`);
    } catch (err: any) {
      console.warn(`  [crusoe-provision] API unreachable (${err.message}) — using mock`);
    }
  } else {
    console.warn("  [crusoe-provision] No CRUSOE_API_KEY — using mock");
  }

  // Mock fallback: hackathon endpoint already has Qwen running
  return {
    instanceId: `ci-${Math.random().toString(36).slice(2, 9)}`,
    ip: null,
    apiEndpoint: HACKATHON_INFERENCE_ENDPOINT,
    model,
    status: "mock",
    provisionedAt: new Date().toISOString(),
  };
}
