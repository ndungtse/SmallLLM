#include "LLMInference.h"
#include <android/log.h>
#include <cstring>
#include <stdexcept>

#define LOG_TAG "smollm-native"
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

void LLMInference::loadModel(const char* modelPath, float minP, float temperature,
                             int topK, float topP, long contextSize, int numThreads) {
    llama_backend_init();

    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0; // CPU-only on device
    model = llama_model_load_from_file(modelPath, modelParams);
    if (model == nullptr) {
        throw std::runtime_error("Failed to load model");
    }
    vocab = llama_model_get_vocab(model);

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = static_cast<uint32_t>(contextSize);
    ctxParams.n_batch = static_cast<uint32_t>(contextSize);
    ctxParams.n_threads = numThreads;
    ctxParams.n_threads_batch = numThreads;
    ctx = llama_init_from_model(model, ctxParams);
    if (ctx == nullptr) {
        throw std::runtime_error("Failed to create context");
    }
    nCtxMax = static_cast<int>(llama_n_ctx(ctx));

    llama_sampler_chain_params samplerParams = llama_sampler_chain_default_params();
    sampler = llama_sampler_chain_init(samplerParams);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(topK));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_min_p(minP, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    formattedPrompt.resize(nCtxMax);
    prevFormattedLen = 0;
}

void LLMInference::addChatMessage(const char* message, const char* role) {
    messages.push_back({ strdup(role), strdup(message) });
}

void LLMInference::startCompletion(const char* query) {
    stopRequested = false;
    cachedResponse.clear();
    nDecoded = 0;

    addChatMessage(query, "user");

    const char* tmpl = llama_model_chat_template(model, nullptr);
    int newLen = llama_chat_apply_template(
            tmpl, messages.data(), messages.size(), true,
            formattedPrompt.data(), static_cast<int32_t>(formattedPrompt.size()));
    if (newLen > static_cast<int>(formattedPrompt.size())) {
        formattedPrompt.resize(newLen);
        newLen = llama_chat_apply_template(
                tmpl, messages.data(), messages.size(), true,
                formattedPrompt.data(), static_cast<int32_t>(formattedPrompt.size()));
    }
    if (newLen < 0) {
        throw std::runtime_error("Failed to apply chat template");
    }
    std::string prompt(formattedPrompt.begin() + prevFormattedLen, formattedPrompt.begin() + newLen);

    const bool isFirst = llama_kv_self_used_cells(ctx) == 0;
    int nPromptTokens = -llama_tokenize(vocab, prompt.c_str(), prompt.size(), nullptr, 0, isFirst, true);
    promptTokens.resize(nPromptTokens);
    if (llama_tokenize(vocab, prompt.c_str(), prompt.size(), promptTokens.data(),
                       promptTokens.size(), isFirst, true) < 0) {
        throw std::runtime_error("Failed to tokenize prompt");
    }
    batch = llama_batch_get_one(promptTokens.data(), promptTokens.size());
}

std::string LLMInference::completionLoop(bool& done) {
    const int nCtxUsed = llama_kv_self_used_cells(ctx);
    if (stopRequested || nCtxUsed + batch.n_tokens > nCtxMax) {
        done = true;
        addChatMessage(cachedResponse.c_str(), "assistant");
        prevFormattedLen = llama_chat_apply_template(
                llama_model_chat_template(model, nullptr),
                messages.data(), messages.size(), false, nullptr, 0);
        return "";
    }

    if (llama_decode(ctx, batch) != 0) {
        done = true;
        return "";
    }

    llama_token newTokenId = llama_sampler_sample(sampler, ctx, -1);
    if (llama_vocab_is_eog(vocab, newTokenId)) {
        done = true;
        addChatMessage(cachedResponse.c_str(), "assistant");
        prevFormattedLen = llama_chat_apply_template(
                llama_model_chat_template(model, nullptr),
                messages.data(), messages.size(), false, nullptr, 0);
        return "";
    }

    char buf[256];
    int n = llama_token_to_piece(vocab, newTokenId, buf, sizeof(buf), 0, true);
    std::string piece = (n > 0) ? std::string(buf, n) : std::string();
    cachedResponse += piece;

    currToken = newTokenId;
    batch = llama_batch_get_one(&currToken, 1);
    nDecoded++;
    done = false;
    return piece;
}

void LLMInference::stop() {
    stopRequested = true;
}

LLMInference::~LLMInference() {
    for (auto& m : messages) {
        free(const_cast<char*>(m.role));
        free(const_cast<char*>(m.content));
    }
    if (sampler) llama_sampler_free(sampler);
    if (ctx) llama_free(ctx);
    if (model) llama_model_free(model);
}
