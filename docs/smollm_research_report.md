# SmolLM-135M & the Sub-200MB LLM Landscape
**Research Report — June 2026**

---

## Overview

SmolLM-135M is a 135M-parameter language model released by HuggingFace in July 2024. With an int4-quantized footprint of approximately 94MB, it is one of the smallest production-ready LLMs available. This report profiles SmolLM-135M, its successor SmolLM2-135M, and the broader landscape of competing models at and near this size class, with attention to tool calling capability, deployment format support, and real-world suitability.

---

## SmolLM-135M (July 2024)

**Developer:** HuggingFace (HuggingFaceTB)  
**Parameters:** 135M  
**Size on disk:** ~270MB BF16 / ~94MB int4 quantized  
**Context window:** 2,048 tokens
**Architecture:** Decoder-only transformer, Grouped Query Attention (GQA)  
**License:** Apache 2.0  
**Tool calling:** ❌ Not supported

### Training data

SmolLM-135M was trained on the SmolLM-Corpus, a curated collection built from:

- **Cosmopedia v2** — synthetic educational textbooks and stories
- **FineWeb-Edu** — high-quality educational web samples
- **Python-Edu** — educational Python code from The Stack

### Benchmark performance

| Benchmark | SmolLM-135M | Notes |
|---|---|---|
| ARC-Easy | ~52% | Common sense reasoning |
| HellaSwag | ~46% | Sentence completion reasoning |
| PIQA | ~61% | Physical intuition Q&A |
| OpenBookQA | ~36% | Open book question answering |

### Strengths

- Smallest production-ready LLM you can pip-install
- Fast inference on CPU — viable on Raspberry Pi-class hardware
- Runs entirely in a browser via WebGPU (demo available)
- Well-suited for text classification, short Q&A, keyword extraction, and routing in larger pipelines
- Fully transparent training data and methodology

### Limitations

- 2K context ceiling — cannot handle longer documents or conversations
- No function/tool calling support (intentionally excluded from post-training)
- Multi-step reasoning, math beyond arithmetic, and coding are unreliable
- Not a viable general chat assistant at this size

---

## SmolLM2-135M (November 2024)

**Developer:** HuggingFace  
**Parameters:** 135M  
**Size on disk:** ~94MB int4 (pip-installable as a Python package)  
**Context window:** 2,048 tokens  
**License:** Apache 2.0  
**Tool calling:** ❌ Not supported

SmolLM2 is the direct successor to SmolLM, trained on a significantly richer dataset with 2 trillion tokens (compared to ~600B in v1). Key additions include FineMath, InfiMM-WebMath, and a filtered version of DCLM with a FineWeb-Edu quality classifier. A multi-stage training strategy was used for the 1.7B variant; the 135M and 360M models benefited from a single-stage approach with consistently high-quality data.

Post-training used SFT on a filtered subset of SmolTalk, followed by DPO with UltraFeedback. Complex tasks like function calling were deliberately removed from the finetuning mix to match the model's capacity.

### Why it matters

The int4-quantized SmolLM2-135M-Instruct fits inside a Python package at almost exactly 100MB — just under PyPI's upload limit. This makes it the first LLM you can literally `pip install`, with no separate model download step.

| Benchmark | SmolLM-135M | SmolLM2-135M | Delta |
|---|---|---|---|
| ARC avg | ~52% | ~57% | +5pp |
| HellaSwag | ~46% | ~52% | +6pp |
| PIQA | ~61% | ~65% | +4pp |

**Verdict:** Strictly better than SmolLM v1 at the same footprint. Still shares all architectural limitations (2K context, no tool calling).

---

## Direct Competitors at Sub-300MB

### FunctionGemma 270M (Google, December 2025)

**Parameters:** 270M  
**Size:** 288MB int8 / ~271MB int8 optimized  
**Context window:** 32K tokens  
**Architecture:** Gemma 3 270M base, function-calling SFT  
**Tool calling:** ✅ Specialized for function calling  
**License:** Gemma license (free for commercial use after acceptance)

FunctionGemma is built on the Gemma 3 270M base model and trained specifically for function calling and tool use. It is not intended as a general chat model — it is a purpose-built routing engine that translates natural language into structured API calls.

Key characteristics:
- Accuracy out-of-the-box: **58%** on the Mobile Actions benchmark
- Accuracy after fine-tuning on your API surface: **~85%**
- Decode speed on Samsung S25 Ultra CPU: **~126 tok/s**
- Uses a custom non-JSON output format (not standard OpenAI tool format)
- Officially supported on Android via LiteRT .litertlm and .task formats
- Demonstrated in Google AI Edge Gallery for mobile actions and game logic

**When to choose FunctionGemma:** When tool calling is your only requirement and you have defined API surface + capacity to fine-tune. Not a drop-in for general assistants.

### Gemma 3 270M (Google, 2025)

**Parameters:** 270M  
**Size:** ~500MB FP16  
**Context window:** 32K tokens  
**Tool calling:** ❌ Not natively supported  

The general-purpose base model from which FunctionGemma was derived. Slightly better at open-ended text generation than FunctionGemma (which traded general capability for function-calling specialization). Benchmark scores: HellaSwag ~37.7%, PIQA ~66.2% zero-shot.

### MobileLLM-125M / 350M (Meta, ICML 2024)

**Parameters:** 125M or 350M  
**Size:** ~240MB / ~670MB  
**Tool calling:** ❌ Not supported  

A research model from Meta demonstrating that for sub-1B language models, depth beats width. Key architectural innovations: deep-and-thin layout, embedding sharing, GQA, and block-wise weight sharing. At 125M scale, achieved 4.3% improvement over prior art on TQA; at 350M, approximately 10 points better on RACE reading comprehension.

MobileLLM is an **influence model, not a deployment choice**. Its architectural lessons are now baked into SmolLM2, Qwen3 edge models, and others. Not widely distributed as an end-user model.

---

## Adjacent Models: 300MB–700MB

These models exceed the sub-200MB class but are directly relevant because the capability gap between 135M and 600M is significant, and quantization is continuously shrinking that gap.

### Qwen3 0.6B (Alibaba, April 2025)

**Parameters:** 0.6B  
**Size:** ~400–600MB Q4 GGUF  
**Context window:** 32K tokens  
**Tool calling:** ✅ Native (built-in, no fine-tuning required)  
**License:** Apache 2.0

The smallest model in the Qwen3 family with native tool calling. Features seamless switching between thinking mode (for complex reasoning) and non-thinking mode (for fast chat). Trained via Strong-to-Weak Distillation from larger Qwen3 models, giving it outsized knowledge for its parameter count.

Key benchmarks show it outperforming Gemma 3 1B IT in non-thinking mode despite being significantly smaller. An official LiteRT community build is available at `litert-community/Qwen3-0.6B`, making it directly deployable on Android with GPU/NPU acceleration.

**Verdict: Best option under ~600MB for general chat + tool calling combined.**

### Qwen3.5 0.8B (Alibaba, February 2026)

**Parameters:** 0.8B  
**Size:** ~500–700MB Q4  
**Context window:** 262K tokens  
**Tool calling:** ✅ Native  
**License:** Apache 2.0

The newest and most capable model in this size class. Uses the Gated Delta Network (GDN) hybrid architecture from the flagship 397B model. Natively multimodal (text, images, video) at sub-1B scale — unprecedented at time of writing. 262K context is the longest in this size class by a significant margin.

Caution: the new GDN architecture may require updated inference frameworks (ensure llama.cpp, Transformers, or your target runtime supports it).

---

## Benchmark Landscape Summary

| Model | Params | Size (quantized) | Context | Tool Calling | Released |
|---|---|---|---|---|---|
| SmolLM-135M | 135M | ~94MB int4 | 2K | ❌ | Jul 2024 |
| SmolLM2-135M | 135M | ~94MB int4 | 2K | ❌ | Nov 2024 |
| MobileLLM-125M | 125M | ~240MB | 2K | ❌ | 2024 |
| Gemma 3 270M | 270M | ~500MB FP16 | 32K | ❌ | 2025 |
| FunctionGemma 270M | 270M | 288MB int8 | 32K | ✅ (specialized) | Dec 2025 |
| Qwen3 0.6B | 600M | ~400–600MB Q4 | 32K | ✅ native | Apr 2025 |
| Qwen3.5 0.8B | 800M | ~500–700MB Q4 | 262K | ✅ native | Feb 2026 |

---

## Deployment Format Support

| Model | LiteRT (.litertlm) | GGUF (llama.cpp) | ExecuTorch | WebGPU / Transformers.js | MNN |
|---|---|---|---|---|---|
| SmolLM2-135M | — | ✅ | — | ✅ | — |
| FunctionGemma 270M | ✅ official | ✅ | — | ✅ | — |
| Gemma 3 270M | ✅ | ✅ | — | ✅ | — |
| Qwen3 0.6B | ✅ community | ✅ | ✅ | — | ✅ |
| Qwen3.5 0.8B | — | ✅ | ✅ | — | ✅ |

---

## Key Findings

1. **SmolLM2-135M is the best 135M model available** — meaningfully better than v1 on every benchmark, same footprint, pip-installable. Use it.

2. **No 135M model supports tool calling** — this is not an oversight. Complex instruction-following tasks were deliberately excluded because the capacity simply isn't there at 135M parameters.

3. **FunctionGemma 270M fills the tool-calling gap but is a specialist**, not a general model. It requires fine-tuning for your specific API surface before reaching production-worthy accuracy.

4. **The real capability jump is at 0.6B** — Qwen3 0.6B offers native tool calling, 32K context, thinking modes, and LiteRT support at ~400–600MB. If your size constraint is 600MB rather than 200MB, this is the clear recommendation.

5. **Quantization is still moving fast** — ParetoQ (Meta, NeurIPS 2025) established that a larger model at 2 bits outperforms a half-size model at 4 bits for equivalent size budgets. Future 1B models at 2-bit quantization may reach the 200MB class with meaningfully better capability than today's 135M options.

6. **Architecture depth vs. width matters most at this scale** — MobileLLM's ICML 2024 finding that depth beats width for sub-billion models is now a consensus design principle. SmolLM2's improvements and Qwen3's distillation gains both reflect this.

---

## Recommendation by Use Case

| Use case | Recommended model |
|---|---|
| Absolute smallest footprint, no tool calling needed | SmolLM2-135M (~94MB int4) |
| Function/tool calling, can fine-tune, mobile deployment | FunctionGemma 270M + fine-tune |
| General chat + tool calling, ~600MB budget | Qwen3 0.6B (LiteRT build) |
| Best overall under 700MB, multimodal, very long context | Qwen3.5 0.8B |
| Pipeline routing / text classification only | SmolLM2-135M or SmolLM2-360M |

---

*Benchmarks are vendor-reported where noted. File sizes are approximate and vary by quantization method and framework. Research compiled June 2026.*
