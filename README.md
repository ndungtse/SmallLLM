# SmallLLM

> Testing how small and efficient an on-device LLM can get — and turning that into real, offline-first mobile AI.

**SmallLLM** is a learning-driven Android side project for running small language models
(≤ ~1B parameters) fully **on-device**, with no cloud calls. It is a sandbox to test different
models across real use cases — **chatting, function/tool calling, and resource search/RAG** — and
to find the smallest, most efficient model that still does a useful job.

It is inspired by [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) but
deliberately leaner, and is structured around a personal growth path: **Kotlin, Jetpack Compose,
on-device inference (LiteRT / MediaPipe), function calling, and eventually fine-tuning.**

> 🔬 **A study/research project, not a product.** There is no deadline and no "MVP" — just phases to
> learn through. Nothing is cut; later capabilities simply come in later phases.
> Current focus: **Phase 0 — standing up the runtimes.** See the [Roadmap](docs/ROADMAP.md).

## Why

Most LLM apps phone home to a server. SmallLLM explores the opposite: as models shrink and
quantization improves, more can run on the phone itself — private, offline, and free to run. This
project is where that idea gets studied model by model, use case by use case — for learning and
research, not to ship on a schedule.

The companion study that motivates it: [Sub-200MB LLM Landscape](docs/smollm_research_report.md).

## Architecture at a glance

```
┌─────────────────────────────────────────────┐
│  UI (Jetpack Compose)                         │
│  Gallery  →  Model Detail  →  Chat            │
│  ViewModel + StateFlow                        │
└───────────────────────┬─────────────────────┘
                        │
┌───────────────────────▼─────────────────────┐
│  runtime/  — LlmRuntime interface            │
│  ┌──────────────┐  ┌───────────┐  ┌────────┐ │
│  │ LiteRT-LM    │  │ MediaPipe │  │ GGUF   │ │
│  │              │  │           │  │llama.cpp│ │
│  └──────────────┘  └───────────┘  └────────┘ │
└───────────────────────┬─────────────────────┘
                        │
┌───────────────────────▼─────────────────────┐
│  data/  — model registry + download + storage│
└─────────────────────────────────────────────┘
```

A single `LlmRuntime` abstraction lets the app swap inference backends without touching the UI. The
first phase stands up **all** the runtimes behind it — LiteRT-LM (FunctionGemma, Qwen3 0.6B),
MediaPipe, and llama.cpp/GGUF (SmolLM2-135M) — so that any model in the registry can be loaded and
studied, regardless of its format. See [Architecture](docs/ARCHITECTURE.md).

## Build & run

**Requirements**
- Android Studio (latest stable), JDK 17+
- An Android device or emulator, **minSdk 24** (Android 7.0)
- A physical device recommended once inference lands (GPU/NPU acceleration + real memory)

**Steps**
1. Clone and open the project in Android Studio.
2. Let Gradle sync (Kotlin 2.2.10, AGP 9.2.1, Compose BOM 2026.02).
3. Run the `app` configuration on your device/emulator.
4. Models are **not bundled** — once the gallery is in place, you browse it in-app and download a
   model (e.g. Qwen3 0.6B or FunctionGemma 270M) to device storage before chatting.

## Documentation

| Doc | What's in it |
|---|---|
| [docs/SPECS.md](docs/SPECS.md) | Product scope, curated model registry, functional & non-functional requirements |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layering, the `LlmRuntime` abstraction, model lifecycle, dependency plan |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Phases 0–5, from runtimes + gallery/chat to fine-tuning |
| [docs/smollm_research_report.md](docs/smollm_research_report.md) | The sub-200MB LLM landscape study that motivates the project |

## Credits

Architecture and patterns are distilled from
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0) — in particular its
model-allowlist data shape and its LiteRT-LM inference flow.
