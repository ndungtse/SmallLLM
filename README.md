# SmallLLM

> Testing how small and efficient an on-device LLM can get — and turning that into real, offline-first mobile AI.

**SmallLLM** is a learning-driven Android side project for running small language models
(≤ ~1B parameters) fully **on-device**, with no cloud calls. It is a sandbox to test different
models across real use cases — **chatting, function/tool calling, and resource search/RAG** — and
to find the smallest, most efficient model that still does a useful job.

It is inspired by [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) but
deliberately leaner, and is structured around a personal growth path: **Kotlin, Jetpack Compose,
on-device inference (LiteRT-LM / llama.cpp), function calling, and eventually fine-tuning.**

> 🔬 **A study/research project, not a product.** There is no deadline and no "MVP" — just steps to
> learn through, built as needed rather than on a fixed schedule. Nothing is cut; some parts simply
> come later. Current focus: **standing up the runtimes** (the foundation). See the
> [Roadmap](docs/ROADMAP.md).

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
│  ┌──────────────────┐   ┌──────────────────┐ │
│  │ LiteRT-LM        │   │ llama.cpp (GGUF) │ │
│  │ .litertlm/.task  │   │ :llamacpp / JNI  │ │
│  └──────────────────┘   └──────────────────┘ │
└───────────────────────┬─────────────────────┘
                        │
┌───────────────────────▼─────────────────────┐
│  data/  — model registry + download + storage│
└─────────────────────────────────────────────┘
```

A single `LlmRuntime` abstraction lets the app swap inference backends without touching the UI. The
foundation stands up two runtimes behind it — **LiteRT-LM** (`.litertlm`/`.task`; Qwen3 0.6B,
FunctionGemma) and **llama.cpp/GGUF** (the native `:llamacpp` module; SmolLM2-135M) — so any model in
the registry can be loaded and studied, regardless of its format. (MediaPipe was dropped: its LLM
Inference API is deprecated in favour of LiteRT-LM.) See [Architecture](docs/ARCHITECTURE.md).

## Build & run

**Requirements**
- Android Studio (latest stable), JDK 17+
- An Android device or emulator, **minSdk 24** (Android 7.0)
- A physical device recommended once inference lands (GPU/NPU acceleration + real memory)

**Steps**
1. Clone and open the project in Android Studio.
2. Let Gradle sync (Kotlin 2.2.10, AGP 9.2.1, Compose BOM 2026.02).
3. Run the `app` configuration on your device/emulator.
4. Models are **not bundled** — browse the in-app gallery and download a model (e.g. Qwen3 0.6B) to
   device storage before chatting.

**GGUF (llama.cpp) support** needs a one-time native setup — see [llamacpp/README.md](llamacpp/README.md)
(`git submodule update --init`, NDK + CMake).

**Gated models (Hugging Face access token).** Some models (e.g. Gemma) are gated and need an HF token
to download. Add it to `local.properties` (gitignored — never commit it):
```properties
HF_TOKEN=hf_xxxxxxxxxxxxxxxxxxxxx
```
It's exposed via `BuildConfig.HF_TOKEN` and sent as a bearer token only for models flagged
`requiresAccessToken` in the registry. Alternatively set an `HF_TOKEN` environment variable. Without
it, gated downloads fail with a clear "access denied" message; ungated models (Qwen3, SmolLM2) work
as-is.

## Documentation

| Doc | What's in it |
|---|---|
| [docs/SPECS.md](docs/SPECS.md) | Product scope, curated model registry, functional & non-functional requirements |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layering, the `LlmRuntime` abstraction, model lifecycle, dependency plan |
| [docs/ROADMAP.md](docs/ROADMAP.md) | The build steps, from runtimes + gallery/chat to fine-tuning |
| [docs/smollm_research_report.md](docs/smollm_research_report.md) | The sub-200MB LLM landscape study that motivates the project |

## Credits

Architecture and patterns are distilled from
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0) — in particular its
model-allowlist data shape and its LiteRT-LM inference flow.
