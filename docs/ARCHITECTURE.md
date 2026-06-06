# SmallLLM — Architecture

> Companion docs: [Specs](SPECS.md) · [Roadmap](ROADMAP.md) · [Research report](smollm_research_report.md)

Design philosophy: **lean, then evolve.** The smallest structure that lets us load and study models —
ViewModel + StateFlow + manual DI. Hilt / DataStore come in only when the work needs them. This is a
research codebase: clarity and the freedom to swap models/runtimes matter more than production
hardening.

> Implementation note: the project uses **AGP 9.2's built-in Kotlin** (no `kotlin-android` plugin),
> and **Gson** for JSON (reflection-based, no compiler plugin) instead of kotlinx-serialization — this
> keeps the build free of extra Kotlin compiler plugins on the foundation.

## 1. Modules & layered structure

**Hybrid Gradle layout:** one `:app` module for all app/UI/runtime code, plus one native `:llamacpp`
module that isolates the llama.cpp JNI/CMake layer.

```
:app  (namespace com.smallllm)
├── SmallLlmApplication.kt     owns AppContainer
├── MainActivity.kt            hosts the nav graph
├── di/AppContainer.kt         manual DI: registry, storage, downloadRepo, runtimeFactory
├── data/
│   ├── model/                 ModelSpec, SamplingConfig, RuntimeType, ModelAllowlist, ModelRegistry
│   ├── download/              DownloadStatus, DownloadWorker (WorkManager), DownloadRepository
│   └── storage/ModelStorage   path resolution, isDownloaded, delete
├── runtime/
│   ├── LlmRuntime.kt          the abstraction (interface)  ← centerpiece
│   ├── RuntimeOptions.kt      Backend enum + generation options
│   ├── RuntimeFactory.kt      RuntimeType → LlmRuntime
│   ├── litertlm/LiteRtLmRuntime.kt
│   └── llamacpp/LlamaCppRuntime.kt   adapts :llamacpp's SmolLM
└── ui/
    ├── gallery/   GalleryScreen + GalleryViewModel
    ├── detail/    ModelDetailScreen + ModelDetailViewModel
    ├── chat/      ChatScreen + ChatViewModel
    ├── navigation/  Destinations + SmallLlmNavGraph
    └── theme/

:llamacpp  (namespace com.smallllm.llamacpp)   — see llamacpp/README.md
├── src/main/cpp/   CMakeLists.txt, LLMInference.{h,cpp}, smollm.cpp (JNI), llama.cpp/ (submodule)
└── src/main/java/com/smallllm/llamacpp/SmolLM.kt   Kotlin JNI surface, Flow streaming
```

**DI:** a hand-written `AppContainer` held by `SmallLlmApplication`, read by ViewModels via
`viewModelFactory { initializer { (this[APPLICATION_KEY] as SmallLlmApplication).container } }`.
Migrate to **Hilt** when the wiring count grows enough to justify it.

## 2. The runtime abstraction (centerpiece)

One interface decouples the UI from any specific inference engine. `RuntimeFactory` picks the impl
from a model's `RuntimeType`, so the UI never branches on engine.

```kotlin
interface LlmRuntime {
    suspend fun initialize(modelPath: String, options: RuntimeOptions) // heavy; off main thread
    fun generate(prompt: String): Flow<String>                          // streamed token chunks
    suspend fun stop()                                                  // cancel in-flight generate
    suspend fun resetConversation()                                     // fresh chat, model stays loaded
    fun close()                                                         // free native resources
}
```

- `generate` returns a `Flow<String>` (collected in the ViewModel) — clean for Compose, and maps onto
  both LiteRT-LM's `MessageCallback` (bridged via `callbackFlow`) and llama.cpp's token loop.
- `RuntimeOptions` carries `Backend` (CPU/GPU/NPU), `maxTokens`, and sampling (topK/topP/temperature).

### Two runtimes behind the interface

The point of the project is to test *any* small model, and models ship in different formats — so the
foundation stands up the runtimes needed to cover the registry:

**`LiteRtLmRuntime`** — wraps `com.google.ai.edge.litertlm:litertlm-android` (Qwen3 0.6B, FunctionGemma;
`.litertlm`/`.task`):
```
initialize → EngineConfig(modelPath, backend, maxNumTokens) → Engine.initialize() → createConversation
generate   → conversation.sendMessageAsync(Contents.of(text), MessageCallback) → callbackFlow emits tokens
stop       → conversation.cancelProcess()
close      → conversation.close() + engine.close()
```
Modeled on the Gallery's `LlmChatModelHelper`, minus AICore routing and image/audio inputs.

**`LlamaCppRuntime`** — adapts the `:llamacpp` module's `SmolLM` (JNI → llama.cpp) for **GGUF** models
(e.g. SmolLM2-135M) — the only way to run the headline sub-100MB model. The native layer is opt-in
(builds once the llama.cpp submodule + NDK are present); until then `SmolLM` compiles but throws at
load, so the LiteRT-LM path is unaffected. See [llamacpp/README.md](../llamacpp/README.md).

> **MediaPipe was dropped.** Its `tasks-genai` LLM Inference API is officially deprecated in favour of
> LiteRT-LM, which also handles the `.task` format — a separate MediaPipe runtime would be redundant.

## 3. Model spec & lifecycle

`ModelSpec` (distilled from the Gallery's `AllowedModel`/`DefaultConfig`, parsed by `ModelRegistry`
from `app/src/main/assets/model_registry.json` via Gson):

```kotlin
data class ModelSpec(
    val name: String, val modelId: String, val description: String,
    val fileName: String, val version: String, val downloadUrl: String,
    val sizeInBytes: Long, val params: String, val contextLength: Int,
    val runtime: RuntimeType, val supportsTools: Boolean,
    val requiresAccessToken: Boolean, val minDeviceMemoryInGb: Int?,
    val sampling: SamplingConfig,                  // topK, topP, temperature, maxTokens
)
```

**Lifecycle:**
```
registry entry (assets JSON)
  → download   → {externalFilesDir}/models/{normalizedName}/{version}/{fileName}
  → initialize(runtime, modelPath, options)
  → generate / chat (streamed)
  → resetConversation (new chat)  /  close (switch model, free memory)
  → delete (ModelStorage)
```

**Downloader (implemented):** `DownloadWorker` is a WorkManager `CoroutineWorker` that streams to a
`.tmp` file, **resumes via an HTTP `Range` header**, reports progress through `setProgress`
(throttled ~200ms) and a **foreground progress notification**, then renames to the final file.
`DownloadRepository` enqueues unique work (so re-enqueue resumes rather than duplicates) and exposes
each model's state as a `Flow<DownloadStatus>` derived from `WorkInfo`.

## 4. UI & state

- **MVVM:** each screen has a ViewModel exposing one `StateFlow<UiState>`; Compose collects with
  `collectAsStateWithLifecycle()`.
- **Navigation:** Compose Navigation, `Gallery → ModelDetail → Chat`, passing the (URL-encoded) model
  name as the route arg.
- **Chat:** `ChatUiState(modelName, loadState, messages, isGenerating)`. The ViewModel initializes the
  runtime on load, then collects `runtime.generate(prompt)`, appending chunks to the last assistant
  message; Stop cancels the job, Reset clears history.
- Long-running work (download, init, generate) always runs off the main dispatcher.

## 5. Dependencies (added so far)

| Dependency | Coordinate | Role |
|---|---|---|
| LiteRT-LM | `com.google.ai.edge.litertlm:litertlm-android:0.11.0` | `.litertlm`/`.task` inference |
| llama.cpp (GGUF) | `:llamacpp` module (CMake + NDK, llama.cpp submodule) | GGUF inference |
| Navigation Compose | `androidx.navigation:navigation-compose` | screen routing |
| Lifecycle ViewModel/Runtime Compose | `androidx.lifecycle:lifecycle-viewmodel-compose`, `…-runtime-compose` | VM + state collection |
| Coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | async + Flow |
| WorkManager | `androidx.work:work-runtime-ktx` | background downloads |
| Gson | `com.google.code.gson:gson` | registry JSON parsing |

Not yet added (per the roadmap): **Hilt** (when manual DI strains), **DataStore** (when settings
persist), Coil (optional model art).

## 6. Package & module setup (done)

Package renamed from the template `com.example.smallllm` → `com.smallllm`. `:llamacpp` added to
`settings.gradle.kts`; `:app` depends on it. AndroidManifest declares `INTERNET`,
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, and `POST_NOTIFICATIONS` for the download
service.

## 7. Tool-calling design preview (next)

LiteRT-LM exposes function calling via annotations (Gallery's `mobileactions` package):

```kotlin
class DeviceTools {
    @Tool(description = "Turns the flashlight on")
    fun turnOnFlashlight(): Map<String, String> { ... }

    @Tool(description = "Looks up a saved note by title")
    fun findNote(@ToolParam(description = "note title") title: String): Map<String, String> { ... }
}
```

The abstraction extends minimally — e.g. `initialize(modelPath, options, tools: ToolSet? = null)` — so
tools are passed into the `Conversation` at load time. Only `LiteRtLmRuntime` implements tools first;
other runtimes can no-op until supported.
