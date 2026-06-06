# `:llamacpp` — GGUF inference via llama.cpp (JNI)

This module wraps [llama.cpp](https://github.com/ggml-org/llama.cpp) behind a small JNI layer so the
app can run **GGUF** models (e.g. SmolLM2-135M) on-device. It exposes one Kotlin class,
[`SmolLM`](src/main/java/com/smallllm/llamacpp/SmolLM.kt), which `LlamaCppRuntime` in `:app` adapts to
the shared `LlmRuntime` interface.

## Layout

```
src/main/cpp/
├── CMakeLists.txt      builds libsmollm.so, links llama.cpp (static)
├── LLMInference.h/.cpp thin C++ wrapper: load model, chat template, incremental token loop
├── smollm.cpp          JNI bindings (com.smallllm.llamacpp.SmolLM ⇄ LLMInference)
└── llama.cpp/          git submodule (NOT checked in here — add it, see below)
src/main/java/com/smallllm/llamacpp/SmolLM.kt   Kotlin surface (Flow streaming)
```

The native build is **opt-in**: `build.gradle.kts` only configures `externalNativeBuild` when
`src/main/cpp/llama.cpp/CMakeLists.txt` exists. Without the submodule the module compiles
Kotlin-only, so the rest of the app (including the LiteRT-LM path) builds with no NDK installed.

## One-time setup to enable GGUF

1. **Install NDK + CMake** via Android Studio → SDK Manager → SDK Tools (NDK side-by-side + CMake),
   or `sdkmanager "ndk;27.0.12077973" "cmake;3.22.1"`.

2. **Add the llama.cpp submodule** (pin a tag known to match the C API used in `LLMInference.cpp` —
   `llama_model_load_from_file`, `llama_kv_self_used_cells`, the `llama_sampler` chain, etc.):
   ```bash
   git submodule add https://github.com/ggml-org/llama.cpp.git llamacpp/src/main/cpp/llama.cpp
   cd llamacpp/src/main/cpp/llama.cpp && git checkout <compatible-tag> && cd -
   ```

3. **Sync & build** — `./gradlew :llamacpp:assembleDebug`. Gradle now picks up the submodule and
   compiles `libsmollm.so` for the configured ABIs.

> ⚠️ llama.cpp's C API moves quickly. If the native build fails to compile, the most likely cause is
> API drift between the pinned tag and `LLMInference.cpp` — adjust the wrapper or pin a different tag.
