# SmallLLM — Roadmap

> Companion docs: [Specs](SPECS.md) · [Architecture](ARCHITECTURE.md) · [Research report](smollm_research_report.md)

This is a **study/research project** — there is no deadline and nothing is cut. The phases below are
just an order to learn through: each builds on the last and adds a capability or a new thing to study.
"Later phase" never means "dropped" — it means we get there once the groundwork is in place.

The first real work is **Phase 0: stand up all the runtimes**, so that from then on we can load and
test *any* small model regardless of its format. After that, chat, then tool calling (the next focus),
then search/RAG, benchmarking, and fine-tuning.

Each phase lists: **Goal**, **Build**, **What you study/learn**, **Done when** (a checkpoint, not a
deadline).

---

## Phase 0 — Foundations & all runtimes

- **Goal:** Be able to load and run any model in the registry, on any of the supported formats.
- **Build:**
  - Package renamed off `com.example.smallllm` (see Architecture §6).
  - Layering scaffolded (`data/`, `runtime/`, `ui/`, `di/`).
  - `LlmRuntime` interface + `RuntimeType` enum + `RuntimeFactory`.
  - **All three runtimes wired behind the interface:** `LiteRtLmRuntime`, `MediaPipeRuntime`,
    `LlamaCppRuntime` (a runtime may begin as a thin implementation and deepen as needed).
  - Core deps added (LiteRT-LM, MediaPipe GenAI, llama.cpp/JNI, Navigation Compose,
    lifecycle-viewmodel, coroutines).
  - Empty `Gallery → Detail → Chat` navigation that compiles and runs.
- **What you study/learn:** Kotlin project structure, Compose Navigation, JNI/native integration for
  llama.cpp, and designing one interface that hides three very different inference engines.
- **Done when:** the app can take a model file of each format (`.litertlm`, `.task`, GGUF) and produce
  tokens through the common `LlmRuntime` interface.

## Phase 1 — Gallery + Chat

- **Goal:** Browse/download real small models and hold a streaming conversation, offline.
- **Build:**
  - `ModelRegistry` seeded from SPECS §3 (Qwen3 0.6B and SmolLM2-135M as first study subjects).
  - Gallery list with download state; `ModelDownloader` with progress + cancel.
  - Chat screen: streaming tokens, stop, reset, model switch/unload.
- **What you study/learn:** real on-device inference across runtimes, streaming into Compose via
  `Flow`, background download, memory/lifecycle management, and the felt difference between a 135M and
  a 600M model in your hand.
- **Done when:** you can download a model on a device and have a multi-turn streamed conversation
  fully offline; switching models frees memory cleanly.

## Phase 2 — Function / tool calling *(next focus after chat)*

- **Goal:** Let a model invoke on-device tools and act on the result.
- **Build:**
  - A `DeviceTools` set using `@Tool` / `@ToolParam` (start with 2–3 safe tools, e.g. toggle a
    setting, look up a local note/list).
  - Runtime extended to accept a `ToolSet` at initialize; tool-call execution loop wired up.
  - A demo flow showing the model calling a tool and using its output.
  - (Connects to Phase 5) compare FunctionGemma out-of-the-box vs fine-tuned tool accuracy.
- **What you study/learn:** structured/function-calling prompting, the tool-call execution loop, and
  the real gap between general models and tool specialists at this size.
- **Done when:** a natural-language request reliably triggers the correct on-device tool and the model
  incorporates the tool's result in its reply.

## Phase 3 — Resource search / RAG

- **Goal:** Answer questions grounded in local documents — "searching in the resource."
- **Build:** on-device embeddings + simple vector/keyword retrieval over local docs, fed as context
  into the chat model; SmolLM2-135M serves as a tiny baseline here.
- **What you study/learn:** embeddings on-device, chunking/retrieval, prompt-context assembly, and how
  small a model can be while still answering grounded questions well.
- **Done when:** ask a question about an indexed local document and get a grounded, citation-able
  answer offline.

## Phase 4 — Benchmarking

- **Goal:** Measure, don't guess — compare models on the use cases above.
- **Build:** a benchmark mode capturing tok/s (prefill + decode), peak memory, model size, and a small
  quality rubric per use case; results exportable to feed the research report.
- **What you study/learn:** profiling on-device inference and fair cross-model/cross-runtime
  measurement.
- **Done when:** a results table comparing the seed models across chat / tools / search updates the
  [research report](smollm_research_report.md) with first-party numbers.

## Phase 5 — Fine-tuning workflow

- **Goal:** Close the loop — fine-tune a small model off-device, then load the artifact on-device.
- **Build:** documented (and where feasible, scripted) fine-tuning of a small model (e.g. FunctionGemma
  on your tool API surface), export/quantize to the target format (`.litertlm` / GGUF), and load it
  through the existing registry + runtime.
- **What you study/learn:** the full fine-tune → quantize → deploy pipeline for edge models, and how
  much a task-specific fine-tune beats a general model at this size.
- **Done when:** a self-fine-tuned model appears in the gallery and measurably outperforms its base
  model on the Phase 2 tool-calling task (validated via Phase 4 benchmarks).

---

### Capability coverage across the phases

| Capability | Phase |
|---|---|
| Kotlin + Compose + navigation + all runtimes | Phase 0 |
| On-device inference + streaming chat | Phase 1 |
| Function / tool calling | Phase 2 |
| Resource search / RAG | Phase 3 |
| Benchmarking & measurement | Phase 4 |
| Fine-tuning & deployment pipeline | Phase 5 |

By the end, the project has deliberately walked through every capability — that's the point of it.
