#ifndef SMALLLLM_LLMINFERENCE_H
#define SMALLLLM_LLMINFERENCE_H

#include "llama.h"
#include <string>
#include <vector>

/**
 * Minimal C++ wrapper around llama.cpp for chat-style generation. One instance owns one loaded GGUF
 * model + context. Generation is driven incrementally: startCompletion() once per user turn, then
 * completionLoop() repeatedly until it reports completion — this maps cleanly onto a Kotlin Flow.
 *
 * Targets the modern llama.cpp C API (llama_model_load_from_file / llama_sampler chain / etc.).
 * Pin the submodule to a compatible tag (see llamacpp/README.md).
 */
class LLMInference {
public:
    void loadModel(const char* modelPath, float minP, float temperature,
                   int topK, float topP, long contextSize, int numThreads);

    void addChatMessage(const char* message, const char* role);

    // Begins a generation turn for `query`. Returns the number of prompt tokens.
    void startCompletion(const char* query);

    // Produces the next token piece. Sets `done` to true when generation is finished.
    std::string completionLoop(bool& done);

    void stop();

    ~LLMInference();

private:
    llama_model*   model   = nullptr;
    llama_context* ctx     = nullptr;
    llama_sampler* sampler = nullptr;
    const llama_vocab* vocab = nullptr;

    std::vector<llama_chat_message> messages;       // role/content pairs (strings owned below)
    std::vector<char>               formattedPrompt; // scratch buffer for the chat template
    int                             prevFormattedLen = 0;

    std::vector<llama_token> promptTokens;
    llama_batch              batch;
    llama_token              currToken = 0;
    int                      nDecoded = 0;
    int                      nCtxMax  = 0;
    std::string              cachedResponse;
    bool                     stopRequested = false;
};

#endif // SMALLLLM_LLMINFERENCE_H
