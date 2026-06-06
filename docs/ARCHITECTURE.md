# SmallLLM — Architecture

> Companion docs: [Specs](SPECS.md) · [Roadmap](ROADMAP.md) · [Research report](smollm_research_report.md)

Design philosophy: **lean, then evolve.** Start with the smallest structure that lets us load and
study models (ViewModel + StateFlow + manual DI, single module). Add Hilt, WorkManager, and DataStore
only when a phase genuinely needs them — each flagged below. This is a research codebase: clarity and
the ability to swap models/runtimes freely matter more than production hardening.

## 1. Layered structure

Single Gradle module (`app`), organized by responsibility:

```
com.smallllm                  (← rename from com.example.smallllm; see §6)
├── data/
│   ├── ModelSpec.kt          model registry entry (data class)
│   ├── ModelRegistry.kt      curated list (from SPECS §3), seeded from JSON or code
│   ├── ModelDownloader.kt    download to storage + progress
│   └── ModelStorage.kt       path resolution, delete, "is downloaded?"
├── runtime/
│   ├── LlmRuntime.kt         the abstraction (interface)  ← centerpiece
│   ├── RuntimeType.kt        enum: LITERT_LM, MEDIAPIPE, LLAMA_CPP
│   ├── LiteRtLmRuntime.kt    ┐
│   ├── MediaPipeRuntime.kt   ├ all three stood up in Phase 0
│   ├── LlamaCppRuntime.kt    ┘
│   └── RuntimeFactory.kt     ModelSpec.runtimeType → LlmRuntime
├── ui/
│   ├── gallery/              GalleryScreen + GalleryViewModel
│   ├── detail/               ModelDetailScreen + ViewModel
│   ├── chat/                 ChatScreen + ChatViewModel
│   ├── navigation/           NavGraph (Gallery → Detail → Chat)
│   └── theme/                existing Color/Theme/Type
└── di/
    └── AppContainer.kt       manual DI: builds registry, downloader, runtime factory
```

**DI:** a hand-written `AppContainer` held by the `Application` and passed to ViewModels (via a
factory) keeps the early phases dependency-free. Migrate to **Hilt** when wiring count grows
(target: Phase 2).

## 2. The runtime abstraction (centerpiece)

A single interface decouples the UI from any specific inference engine. It is a trimmed version of
the Gallery's `LlmChatModelHelper`
(`/Users/ndungutse/Coding/open-source/ai-edge-gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt`).

```kotlin
interface LlmRuntime {
    /** Load a downloaded model into memory. Suspends; runs off main thread. */
    suspend fun initialize(model: ModelSpec, options: RuntimeOptions)

    /** Send a prompt; emit tokens as they stream back. */
    fun generate(prompt: String): Flow<String>

    /** Cancel the in-flight generation. */
    fun stop()

    /** Clear conversation state but keep the model loaded. */
    fun resetConversation()

    /** Free all native resources. */
    fun cleanUp()
}
```

- `generate` returns a `Flow<String>` of token chunks (collected in the ViewModel) — cleaner for
  Compose than the Gallery's callback style, but maps 1:1 onto LiteRT-LM's `MessageCallback`.
- `RuntimeOptions` carries accelerator (CPU/GPU/NPU), maxTokens, and sampling (topK/topP/temperature).

### Stand up all three runtimes early (Phase 0)

The whole point of the project is to test *any* small model, so the foundation phase implements every
runtime behind the interface — not one now and the rest "later." Different models ship in different
formats; without all three, large parts of the registry can't even be loaded.

**`LiteRtLmRuntime`** — wraps **`com.google.ai.edge.litertlm:litertlm-android:0.11.0`** (FunctionGemma,
Qwen3 0.6B; `.litertlm`/`.task`):
```
initialize → EngineConfig(modelPath, backend, sampler) → Engine.initialize() → Conversation
generate   → conversation.sendMessageAsync(Contents.of(text)) → MessageCallback.onMessage(token)
             → emit into Flow → onDone() → close Flow
stop       → cancel in-flight send
cleanUp    → release Engine + Conversation
```
This mirrors the Gallery's flow, minus its AICore routing and image/audio inputs.

**`MediaPipeRuntime`** — wraps MediaPipe's LLM Inference API (`.task`/`.bin`). The simplest chat path;
useful as a comparison point against LiteRT-LM for the same/similar models.

**`LlamaCppRuntime`** — wraps llama.cpp via JNI to load **GGUF** models (e.g. SmolLM2-135M). The most
native work of the three, but essential — it's the only way to run the headline sub-100MB model.

`RuntimeFactory` picks the impl from `ModelSpec.runtimeType`, so the UI never branches on engine. A
runtime may start as a thin stub and get fleshed out as models that need it come into focus — but the
interface and wiring for all three exist from Phase 0, so adding a model never means re-architecting.

## 3. Model spec & lifecycle

`ModelSpec` (distilled from `ModelAllowlist.kt`'s `AllowedModel` / `DefaultConfig`):

```kotlin
data class ModelSpec(
    val name: String,
    val modelId: String,          // HF repo, e.g. "litert-community/Qwen3-0.6B"
    val fileName: String,
    val version: String,          // commit hash → storage versioning
    val sizeInBytes: Long,
    val contextLength: Int,
    val runtimeType: RuntimeType,
    val supportsTools: Boolean,
    val minDeviceMemoryInGb: Int?,
    val downloadUrl: String,      // HF resolve URL
    val defaultConfig: SamplingConfig,  // topK, topP, temperature, maxTokens, accelerators
)
```

**Lifecycle:**
```
registry entry
  → download  → getExternalFilesDir/models/{name}/{version}/{fileName}
  → initialize(runtime)
  → generate / chat
  → resetConversation (new chat)  /  cleanUp (switch model, free memory)
  → delete (ModelStorage)
```

**Downloader:** start with a simple `ModelDownloader` (coroutine + `HttpURLConnection`/OkHttp,
progress via `Flow<Int>`). Upgrade path: a **WorkManager `DownloadWorker`** (Gallery pattern at
`.../worker/DownloadWorker.kt`) for background downloads, resume via Range header, and foreground
notifications — adopt when downloads need to survive app death (target: Phase 1/2).

## 4. UI & state

- **MVVM:** each screen has a ViewModel exposing a single `StateFlow<UiState>`; Compose collects it
  with `collectAsStateWithLifecycle()`.
- **Navigation:** Compose Navigation, three destinations — `Gallery → ModelDetail → Chat` — passing
  the model name/id as the route arg.
- **Chat state:** `ChatUiState(messages, isGenerating, modelLoadState, error)`. The ViewModel
  collects `runtime.generate(prompt)` and appends streamed chunks to the last assistant message.
- Long-running work (download, init, generate) always off the main dispatcher.

## 5. Dependencies (staged)

Add to the version catalog as phases require — don't front-load.

| Dependency | Coordinate | When |
|---|---|---|
| LiteRT-LM | `com.google.ai.edge.litertlm:litertlm-android:0.11.0` | Phase 0 |
| MediaPipe Tasks GenAI | `com.google.mediapipe:tasks-genai` | Phase 0 |
| llama.cpp (GGUF, via JNI) | native build / Android wrapper (TBD) | Phase 0 |
| Navigation Compose | `androidx.navigation:navigation-compose` | Phase 0 |
| Lifecycle ViewModel Compose | `androidx.lifecycle:lifecycle-viewmodel-compose` | Phase 0 |
| Coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Phase 0 |
| Serialization | `org.jetbrains.kotlinx:kotlinx-serialization-json` | Phase 1 (registry JSON) |
| Coil (optional, model art) | `io.coil-kt:coil-compose` | optional |
| **Hilt** | `com.google.dagger:hilt-android` | Phase 2 (when manual DI strains) |
| **WorkManager** | `androidx.work:work-runtime-ktx` | Phase 1/2 (background download) |
| **DataStore** | `androidx.datastore:datastore-preferences` | when settings persist |

Exact versions pinned when Phase 0 code begins (align with current Compose BOM 2026.02 /
Kotlin 2.2.10). The llama.cpp coordinate is TBD — it likely needs a native build or a community
Android wrapper rather than a single Maven artifact.

## 6. Package rename

The scaffold uses `com.example.smallllm` (the default template namespace). Rename to a real
namespace before building features — suggested `com.smallllm` or `rw.ivas.smallllm`. Update
`namespace`/`applicationId` in `app/build.gradle.kts`, the package dirs, and `AndroidManifest`.

## 7. Tool-calling design preview (Phase 2)

LiteRT-LM exposes function calling via annotations (Gallery's `mobileactions` package):

```kotlin
class DeviceTools {
    @Tool(description = "Turns the flashlight on")
    fun turnOnFlashlight(): Map<String, String> { ... }

    @Tool(description = "Looks up a saved note by title")
    fun findNote(@ToolParam(description = "note title") title: String): Map<String, String> { ... }
}
```

The abstraction extends minimally — e.g. `initialize(model, options, tools: ToolSet? = null)` — so
tools are passed into the `Conversation` at load time. The runtime surfaces tool-call requests, the
app executes them, and the result map is returned to the model. Only `LiteRtLmRuntime` implements
tools first; other runtimes can no-op until supported.
