#include "LLMInference.h"
#include <jni.h>
#include <string>

// JNI bridge between com.smallllm.llamacpp.SmolLM (Kotlin) and LLMInference (C++).
// A LLMInference* is passed back to Kotlin as a jlong handle.

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_smallllm_llamacpp_SmolLM_nativeLoad(
        JNIEnv* env, jobject /*thiz*/, jstring modelPath, jfloat minP, jfloat temperature,
        jint topK, jfloat topP, jlong contextSize, jint numThreads) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    auto* engine = new LLMInference();
    try {
        engine->loadModel(path, minP, temperature, topK, topP, contextSize, numThreads);
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(modelPath, path);
        delete engine;
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, e.what());
        return 0;
    }
    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_smallllm_llamacpp_SmolLM_nativeAddChatMessage(
        JNIEnv* env, jobject /*thiz*/, jlong handle, jstring message, jstring role) {
    auto* engine = reinterpret_cast<LLMInference*>(handle);
    const char* msg = env->GetStringUTFChars(message, nullptr);
    const char* r = env->GetStringUTFChars(role, nullptr);
    engine->addChatMessage(msg, r);
    env->ReleaseStringUTFChars(message, msg);
    env->ReleaseStringUTFChars(role, r);
}

JNIEXPORT void JNICALL
Java_com_smallllm_llamacpp_SmolLM_nativeStartCompletion(
        JNIEnv* env, jobject /*thiz*/, jlong handle, jstring query) {
    auto* engine = reinterpret_cast<LLMInference*>(handle);
    const char* q = env->GetStringUTFChars(query, nullptr);
    engine->startCompletion(q);
    env->ReleaseStringUTFChars(query, q);
}

JNIEXPORT jstring JNICALL
Java_com_smallllm_llamacpp_SmolLM_nativeCompletionLoop(
        JNIEnv* env, jobject /*thiz*/, jlong handle) {
    auto* engine = reinterpret_cast<LLMInference*>(handle);
    bool done = false;
    std::string piece = engine->completionLoop(done);
    if (done) {
        return nullptr; // signals end-of-generation to Kotlin
    }
    return env->NewStringUTF(piece.c_str());
}

JNIEXPORT void JNICALL
Java_com_smallllm_llamacpp_SmolLM_nativeStop(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    reinterpret_cast<LLMInference*>(handle)->stop();
}

JNIEXPORT void JNICALL
Java_com_smallllm_llamacpp_SmolLM_nativeFree(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    delete reinterpret_cast<LLMInference*>(handle);
}

} // extern "C"
