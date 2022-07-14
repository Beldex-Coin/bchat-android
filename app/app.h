//
// Created by ms on 3/28/2022.
//

#ifndef SESSION_ANDROID_APP_H
#define SESSION_ANDROID_APP_H

#include <jni.h>

/*
#include <android/log.h>

#define  LOG_TAG    "[NDK]"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
*/

jfieldID getHandleField(JNIEnv *env, jobject obj, const char *fieldName = "handle") {
    jclass c = env->GetObjectClass(obj);
    return env->GetFieldID(c, fieldName, "J"); // of type long
}

template<typename T>
T *getHandle(JNIEnv *env, jobject obj, const char *fieldName = "handle") {
    jlong handle = env->GetLongField(obj, getHandleField(env, obj, fieldName));
    return reinterpret_cast<T *>(handle);
}

void setHandleFromLong(JNIEnv *env, jobject obj, jlong handle) {
    env->SetLongField(obj, getHandleField(env, obj), handle);
}

template<typename T>
void setHandle(JNIEnv *env, jobject obj, T *t) {
    jlong handle = reinterpret_cast<jlong>(t);
    setHandleFromLong(env, obj, handle);
}

#ifdef __cplusplus
extern "C"
{
#endif

//extern const char* const BELDEX_VERSION; // the actual monero core version

// from monero-core crypto/hash-ops.h - avoid #including monero code here
enum {
    HASH_SIZE = 32,
    HASH_DATA_AREA = 136
};

void cn_monero_hash(const void *data, size_t length, char *hash, int variant, int prehashed, uint64_t height);

inline void slow_hash(const void *data, const size_t length, char *hash) {
    cn_monero_hash(data, length, hash, 0 /*variant*/, 0 /*prehashed*/, 0 /*height*/);
}

inline void slow_hash_broken(const void *data, char *hash, int variant) {
    cn_monero_hash(data, 200 /*sizeof(union hash_state)*/, hash, variant, 1 /*prehashed*/, 0 /*height*/);
}

#ifdef __cplusplus
}
#endif

#endif
