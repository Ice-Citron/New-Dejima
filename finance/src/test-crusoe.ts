import { crusoeInference } from "./crusoe-inference.js";

async function main() {
  console.log("=== Crusoe Inference Test ===\n");
  const result = await crusoeInference("What is 2 + 2? Answer in one word.");
  console.log("Model:    ", result.model);
  console.log("Response: ", result.response);
  console.log("Tokens:   ", result.usage);
}

main().catch(console.error);
