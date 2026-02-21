import OpenAI from "openai";
import dotenv from "dotenv";
dotenv.config();

// Crusoe's inference API is OpenAI-compatible
const crusoe = new OpenAI({
  apiKey: process.env.CRUSOE_API_KEY!,
  baseURL: "https://hackeurope.crusoecloud.com/v1/",
});

export async function crusoeInference(
  prompt: string,
  model: string = "NVFP4/Qwen3-235B-A22B-Instruct-2507-FP4"
) {
  const completion = await crusoe.chat.completions.create({
    model,
    messages: [
      { role: "system", content: "You are a helpful assistant." },
      { role: "user", content: prompt },
    ],
    max_tokens: 500,
  });

  return {
    response: completion.choices[0].message.content,
    model: completion.model,
    usage: completion.usage,
  };
}
