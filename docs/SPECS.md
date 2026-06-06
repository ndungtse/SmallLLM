# SmallLLM — Product & Feature Specification

> Companion docs: [Architecture](ARCHITECTURE.md) · [Roadmap](ROADMAP.md) · [Research report](smollm_research_report.md)

## 1. Problem statement & target user

On-device LLMs are now small enough (≤ ~1B params, often < 600MB quantized) to run useful tasks
without a server. But which model fits which job — and how small you can go before quality breaks —
is still mostly trial and error.

**Target user:** the developer building this — someone learning native Android (Kotlin + Compose)
and on-device ML, who wants a reusable testbed for offline-first AI features (chat, tool calling,
local search) that can later seed real products.

**Success for the project = ** being able to pull a small model into a phone, exercise it across the
three core use cases, and have an opinion — backed by measurements — on the smallest model that
does each job well.

> This is a **study/research project**, not a product. There is no deadline and no minimum shippable
> cut. Everything below is in scope; the [Roadmap](ROADMAP.md) only sequences *when* each piece is
> studied, in phases. Nothing here is dropped or postponed indefinitely — later just means a later
> phase.

## 2. Scope

### What the project covers (all phases)
- **All inference runtimes behind one abstraction** — LiteRT-LM, MediaPipe, and llama.cpp/GGUF —
  stood up early so any model format can be loaded and tested.
- Browse a **curated registry** of small models and download them to the device.
- **Chat** with a selected model (streaming tokens, stop, reset, switch model).
- **Function / tool calling** — let a model invoke on-device tools.
- **Resource search / RAG** over local documents.
- **Benchmarking** model speed/memory/quality.
- **Fine-tuning** workflow for a small model, then loading the result on-device.

### Boundaries of the study (intentionally not part of it)
These contradict the on-device, research-focused thesis — they are not "later phases," just not what
this project is about:
- Cloud / server-side inference of any kind.
- User accounts, sync, or any backend service.
- Training large models or anything needing a GPU cluster (fine-tuning happens off-device; only the
  artifact is loaded on-device).
- Distribution / Play Store polish.

## 3. Curated model registry (initial seed)

Distilled from the [research report](smollm_research_report.md) and modeled on the Gallery's
`ModelAllowlist.kt` data shape (simplified). The registry is the source of truth for what the app
can download and which runtime loads it.

| Model | Params | Size (quantized) | Context | Tool calling | Format | Runtime | Source |
|---|---|---|---|---|---|---|---|
| SmolLM2-135M-Instruct | 135M | ~94MB int4 | 2K | ❌ | GGUF | llama.cpp | HF `HuggingFaceTB/SmolLM2-135M-Instruct` |
| FunctionGemma 270M | 270M | ~288MB int8 | 32K | ✅ specialist | `.litertlm` / `.task` | LiteRT-LM | HF (Google, LiteRT community) |
| Qwen3 0.6B | 600M | ~400–600MB Q4 | 32K | ✅ native | `.litertlm` | LiteRT-LM | HF `litert-community/Qwen3-0.6B` |

Notes:
- **SmolLM2-135M** is the smallest/baseline (chat + classification/routing). It has **no LiteRT build
  and no tool calling**, so it is loaded through the **GGUF/llama.cpp runtime** — one of the reasons
  all runtimes are stood up early.
- **FunctionGemma 270M** is the tool-calling specialist; needs fine-tuning to reach production
  accuracy (~85% vs ~58% out of the box). Not a general chat model.
- **Qwen3 0.6B** is the best all-rounder under ~600MB: native tool calling + 32K context + LiteRT
  community build. **A good default model to start studying.**
- Registry can grow (Gemma 3 270M, SmolLM2-360M, Qwen3.5 0.8B) — see report's landscape table.

Each registry entry carries (see [Architecture §Model spec](ARCHITECTURE.md)): `name`, `modelId`,
`fileName`, `version/commitHash`, `sizeInBytes`, `contextLength`, `runtimeType`, `supportsTools`,
`downloadUrl`, `minDeviceMemoryInGb`, and default sampling config (topK/topP/temperature/maxTokens).

## 4. Functional requirements — gallery + chat

**Gallery list**
- Show all registry models with name, size, params, context, tool-calling badge, runtime.
- Indicate download state: not downloaded / downloading (%) / downloaded / failed.

**Download + progress**
- Download a model to device storage with visible progress and a cancel option.
- Survive process death gracefully (start with a simple coroutine download; move to
  background/resume in a later phase — see Architecture).
- Support HuggingFace `resolve` URLs; optional bearer token for gated models.

**Model detail**
- Show full spec, source link, storage path, and a delete/clear action.
- Entry point to start a chat with that model.

**Chat**
- Initialize the selected model into its runtime; show load state and errors.
- Send a prompt; **stream** tokens into the UI as they arrive.
- **Stop** an in-flight response; **reset** the conversation.
- **Switch model** / unload to free memory.

## 5. Non-functional requirements

- **Offline-first:** after download, no network needed for inference.
- **Memory ceilings:** respect a per-model `minDeviceMemoryInGb`; warn/disable models the device
  can't hold (Gallery pattern).
- **Cold-load time:** surface model init time to the user (it can be seconds); never block the UI
  thread — inference and load run off the main dispatcher.
- **Storage layout:** `getExternalFilesDir/models/{modelName}/{version}/{fileName}`; deletable per
  model.
- **Resilience:** a failed/corrupt model load must not crash the app; show a recoverable error.
- **Accelerators:** allow CPU / GPU / NPU selection where the runtime supports it (default per model).

## 6. Use-case → model → runtime matrix

| Use case (from research) | Recommended model | Runtime | Phase |
|---|---|---|---|
| General chat, smallest footprint | SmolLM2-135M | llama.cpp/GGUF | Phase 1 (baseline) |
| General chat + light reasoning | Qwen3 0.6B | LiteRT-LM | Phase 1 |
| Function / tool calling | FunctionGemma 270M (+ fine-tune) / Qwen3 0.6B | LiteRT-LM | Phase 2 |
| Resource search / RAG | Qwen3 0.6B + embeddings | LiteRT-LM | Phase 3 |
| Speed/memory/quality comparison | all of the above | all runtimes | Phase 4 |
