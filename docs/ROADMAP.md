# SmallLLM — Roadmap

> Companion docs: [Specs](SPECS.md) · [Architecture](ARCHITECTURE.md) · [Research report](smollm_research_report.md)

This is a **study/research project** — there is no deadline and nothing is cut. What follows is a
rough order to build things in, **driven by need rather than a fixed schedule**. We don't lock each
piece to a numbered phase; we tackle the next thing when it makes sense. "Later" never means
"dropped."

The one firm starting point: **stand up the runtimes first**, so that from then on we can load and
test *any* small model regardless of its format. Everything after that — chat, tool calling, search,
benchmarking, fine-tuning — builds on that foundation in roughly the order below.

Each step lists what to **build**, what it lets us **explore/learn**, and a **good checkpoint** for
when it's working.

---

## Runtimes & foundation

- **Build:**
  - Package renamed off `com.example.smallllm` (see Architecture §6).
  - Layering scaffolded (`data/`, `runtime/`, `ui/`, `di/`).
  - `LlmRuntime` interface + `RuntimeType` enum + `RuntimeFactory`.
  - **Runtimes wired behind the interface:** `LiteRtLmRuntime` and `LlamaCppRuntime` (the native
    GGUF path may begin thin and deepen as needed). MediaPipe was dropped — deprecated in favour of
    LiteRT-LM, which also covers `.task`.
  - Core deps added (LiteRT-LM, llama.cpp via the `:llamacpp` JNI module, Navigation Compose,
    lifecycle-viewmodel, coroutines, WorkManager) and a `Gallery → Detail → Chat` navigation that runs.
- **Explore/learn:** Kotlin project structure, Compose Navigation, JNI/native integration for
  llama.cpp, and designing one interface that hides three very different inference engines.
- **Checkpoint:** the app can take a model file of each format (`.litertlm`, `.task`, GGUF) and
  produce tokens through the common `LlmRuntime` interface.

## Model gallery & chat

- **Build:** `ModelRegistry` seeded from SPECS §3 (Qwen3 0.6B and SmolLM2-135M as first subjects);
  gallery list with download state; `ModelDownloader` with progress + cancel; chat screen with
  streaming tokens, stop, reset, and model switch/unload.
- **Explore/learn:** real on-device inference across runtimes, streaming into Compose via `Flow`,
  download + storage, memory/lifecycle, and the felt difference between a 135M and a 600M model.
- **Checkpoint:** download a model on a device and hold a multi-turn streamed conversation fully
  offline; switching models frees memory cleanly.

## Function / tool calling

- **Build:** a `DeviceTools` set using `@Tool` / `@ToolParam` (start with 2–3 safe tools, e.g. toggle
  a setting, look up a local note/list); runtime extended to accept a `ToolSet` at initialize; the
  tool-call execution loop; a demo showing the model call a tool and use its output.
- **Explore/learn:** structured/function-calling prompting, the tool-call execution loop, and the gap
  between general models and tool specialists at this size (ties into fine-tuning below).
- **Checkpoint:** a natural-language request reliably triggers the correct on-device tool and the
  model incorporates the result in its reply.

## Resource search / RAG

- **Build:** on-device embeddings + simple vector/keyword retrieval over local docs, fed as context
  into the chat model; SmolLM2-135M as a tiny baseline.
- **Explore/learn:** embeddings on-device, chunking/retrieval, prompt-context assembly, and how small
  a model can be while still answering grounded questions well.
- **Checkpoint:** ask a question about an indexed local document and get a grounded, citation-able
  answer offline.

## Benchmarking

- **Build:** a benchmark mode capturing tok/s (prefill + decode), peak memory, model size, and a small
  quality rubric per use case; results exportable to feed the research report.
- **Explore/learn:** profiling on-device inference and fair cross-model/cross-runtime measurement.
- **Checkpoint:** a results table comparing the seed models across chat / tools / search updates the
  [research report](smollm_research_report.md) with first-party numbers.

## Fine-tuning workflow

- **Build:** documented (and where feasible, scripted) fine-tuning of a small model (e.g. FunctionGemma
  on your tool API surface), export/quantize to the target format (`.litertlm` / GGUF), and load it
  through the existing registry + runtime.
- **Explore/learn:** the full fine-tune → quantize → deploy pipeline for edge models, and how much a
  task-specific fine-tune beats a general model at this size.
- **Checkpoint:** a self-fine-tuned model appears in the gallery and measurably outperforms its base
  model on the tool-calling task (validated via the benchmarks above).

---

### Capability coverage

By working through these steps the project touches every capability it set out to study:

- Kotlin + Compose + navigation + all runtimes — *runtimes & foundation*
- On-device inference + streaming chat — *model gallery & chat*
- Function / tool calling — *function / tool calling*
- Resource search / RAG — *resource search / RAG*
- Benchmarking & measurement — *benchmarking*
- Fine-tuning & deployment pipeline — *fine-tuning workflow*
