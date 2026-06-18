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

## Current setup (already wired)

- **Toolchain:** NDK `30.0.14904198`, CMake `4.1.2` (pinned in `build.gradle.kts`).
- **llama.cpp submodule:** pinned at tag **b9547** (recorded in `.gitmodules`).
- **ABIs:** `arm64-v8a`, `x86_64`. llama.cpp + ggml are statically linked into a single
  `libsmollm.so`; `common`, CURL, OpenMP, examples/tests/tools are disabled.

### Fresh clone

The submodule isn't pulled automatically — after cloning the repo:
```bash
git submodule update --init --recursive
./gradlew :llamacpp:assembleDebug
```

### Updating llama.cpp

```bash
cd llamacpp/src/main/cpp/llama.cpp && git fetch && git checkout <tag> && cd -
```

> ⚠️ llama.cpp's C API moves quickly. If a newer tag fails to compile, the cause is almost always API
> drift in `LLMInference.cpp` — e.g. `llama_kv_self_used_cells` was removed and replaced here with
> `llama_memory_seq_pos_max(llama_get_memory(ctx), 0) + 1`. Adjust the wrapper or pin a known tag.
