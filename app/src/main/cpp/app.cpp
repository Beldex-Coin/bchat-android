#include <jni.h>

#include <string.h>
#include <jni.h>
#include "app.h"
//#include "../classes/beldex_api.cpp"
#include <string_view>
#include <string>
#include "wallet2_api.h"

#ifdef __cplusplus
extern "C"
{
#endif

#include <android/log.h>

#define LOG_TAG "WalletNDK"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , LOG_TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG,__VA_ARGS__)

static JavaVM *cachedJVM;
static jclass class_ArrayList;
static jclass class_WalletListener;
static jclass class_TransactionInfo;
static jclass class_Transfer;
/*static jclass class_Ledger;*/
static jclass class_WalletStatus;

std::mutex _listenerMutex;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    cachedJVM = jvm;
    LOGI("JNI_OnLoad");
    JNIEnv *jenv;
    if (jvm->GetEnv(reinterpret_cast<void **>(&jenv), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    LOGI("JNI_OnLoad ok");

    class_ArrayList = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("java/util/ArrayList")));
    class_TransactionInfo = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("com/thoughtcrimes/securesms/model/TransactionInfo")));
    class_Transfer = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("com/thoughtcrimes/securesms/model/Transfer")));
    class_WalletListener = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("com/thoughtcrimes/securesms/model/WalletListener")));
    /*class_Ledger = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("com.thoughtcrime.securesms/ledger/Ledger")));*/
    class_WalletStatus = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("com/thoughtcrimes/securesms/model/Wallet$Status")));
    return JNI_VERSION_1_6;
}
#ifdef __cplusplus
}
#endif

int attachJVM(JNIEnv **jenv) {
    int envStat = cachedJVM->GetEnv((void **) jenv, JNI_VERSION_1_6);
    if (envStat == JNI_EDETACHED) {
        if (cachedJVM->AttachCurrentThread(jenv, nullptr) != 0) {
            LOGE("Failed to attach");
            return JNI_ERR;
        }
    } else if (envStat == JNI_EVERSION) {
        LOGE("GetEnv: version not supported");
        return JNI_ERR;
    }
    //LOGI("envStat=%i", envStat);
    return envStat;
}

void detachJVM(JNIEnv *jenv, int envStat) {
    //LOGI("envStat=%i", envStat);
    if (jenv->ExceptionCheck()) {
        jenv->ExceptionDescribe();
    }

    if (envStat == JNI_EDETACHED) {
        cachedJVM->DetachCurrentThread();
    }
}

struct MyWalletListener : Wallet::WalletListener {
    jobject jlistener;

    MyWalletListener();
    MyWalletListener(JNIEnv *env, jobject aListener) {
        LOGD("Created MyListener");
        jlistener = env->NewGlobalRef(aListener);;
    }

    virtual ~MyWalletListener() {
        LOGD("Destroyed MyListener");
    };

    void deleteGlobalJavaRef(JNIEnv *env) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        env->DeleteGlobalRef(jlistener);
        jlistener = nullptr;
    }

    /**
 * @brief updated  - generic callback, called when any event (sent/received/block reveived/etc) happened with the wallet;
 */
    void updated() {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("updated");
        JNIEnv *jenv;
        int envStat = attachJVM(&jenv);
        if (envStat == JNI_ERR) return;

        jmethodID listenerClass_updated = jenv->GetMethodID(class_WalletListener, "updated", "()V");
        jenv->CallVoidMethod(jlistener, listenerClass_updated);

        detachJVM(jenv, envStat);
    }


    /**
     * @brief moneySpent - called when money spent
     * @param txId       - transaction id
     * @param amount     - amount
     */
    void moneySpent(const std::string &txId, uint64_t amount) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("moneySpent %", amount);
    }

    /**
     * @brief moneyReceived - called when money received
     * @param txId          - transaction id
     * @param amount        - amount
     */
    void moneyReceived(const std::string &txId, uint64_t amount) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("moneyReceived %",amount);
    }

    /**
     * @brief unconfirmedMoneyReceived - called when payment arrived in tx pool
     * @param txId          - transaction id
     * @param amount        - amount
     */
    void unconfirmedMoneyReceived(const std::string &txId, uint64_t amount) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("unconfirmedMoneyReceived %", amount);
    }

    /**
     * @brief newBlock      - called when new block received
     * @param height        - block height
     */
    void newBlock(uint64_t height) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        //LOGD("newBlock");
        JNIEnv *jenv;
        int envStat = attachJVM(&jenv);
        if (envStat == JNI_ERR) return;

        jlong h = static_cast<jlong>(height);
        jmethodID listenerClass_newBlock = jenv->GetMethodID(class_WalletListener, "newBlock",
                                                             "(J)V");
        jenv->CallVoidMethod(jlistener, listenerClass_newBlock, h);

        detachJVM(jenv, envStat);
    }

/**
 * @brief refreshed - called when wallet refreshed by background thread or explicitly refreshed by calling "refresh" synchronously
 */
    void refreshed() {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("refreshed");
        JNIEnv *jenv;

        int envStat = attachJVM(&jenv);
        if (envStat == JNI_ERR) return;

        jmethodID listenerClass_refreshed = jenv->GetMethodID(class_WalletListener, "refreshed","()V");
        jenv->CallVoidMethod(jlistener, listenerClass_refreshed);
        detachJVM(jenv, envStat);
    }
};


extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getSeed(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(wallet->seed().c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getSeedLanguage(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(wallet->getSeedLanguage().c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_setSeedLanguage(JNIEnv *env, jobject instance,
                                                              jstring language) {
    const char *_language = env->GetStringUTFChars(language, nullptr);
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->setSeedLanguage(std::string(_language));
    env->ReleaseStringUTFChars(language, _language);
}

jobject newWalletStatusInstance(JNIEnv *env, int status, const std::string &errorString) {
    jmethodID init = env->GetMethodID(class_WalletStatus, "<init>",
                                      "(ILjava/lang/String;)V");
    jstring _errorString = env->NewStringUTF(errorString.c_str());
    jobject instance = env->NewObject(class_WalletStatus, init, status, _errorString);
    env->DeleteLocalRef(_errorString);
    return instance;
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_statusWithErrorString(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);

    /*  int status;
      std::string errorString;
      wallet->status(status, errorString);*/

    auto stat = wallet->status();


    auto& [status, errorString] = stat;

    if (status != Wallet::Wallet::Status_Ok)
    {
        //error = strdup(errorString.c_str());
        return newWalletStatusInstance(env, status, errorString);
    }

    return newWalletStatusInstance(env, status, errorString);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_setPassword(JNIEnv *env, jobject instance,
                                                          jstring password) {
    const char *_password = env->GetStringUTFChars(password, nullptr);
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    bool success = wallet->setPassword(std::string(_password));
    env->ReleaseStringUTFChars(password, _password);
    return static_cast<jboolean>(success);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getAddressJ(JNIEnv *env, jobject instance,
                                                          jint account_index, jint address_index) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(
            wallet->address((uint32_t) account_index, (uint32_t) address_index).c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_setLogLevel(JNIEnv *env, jclass clazz,
                                                                 jint level) {
    Wallet::WalletManagerFactory::setLogLevel(level);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_logDebug(JNIEnv *env, jclass clazz,
                                                              jstring category, jstring message) {
    const char *_category = env->GetStringUTFChars(category, nullptr);
    const char *_message = env->GetStringUTFChars(message, nullptr);

    Wallet::Wallet::debug(_category, _message);

    env->ReleaseStringUTFChars(category, _category);
    env->ReleaseStringUTFChars(message, _message);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_logInfo(JNIEnv *env, jclass clazz,
                                                             jstring category, jstring message) {
    const char *_category = env->GetStringUTFChars(category, nullptr);
    const char *_message = env->GetStringUTFChars(message, nullptr);

    Wallet::Wallet::info(_category, _message);

    env->ReleaseStringUTFChars(category, _category);
    env->ReleaseStringUTFChars(message, _message);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_logWarning(JNIEnv *env, jclass clazz,
                                                                jstring category, jstring message) {
    const char *_category = env->GetStringUTFChars(category, nullptr);
    const char *_message = env->GetStringUTFChars(message, nullptr);

    Wallet::Wallet::warning(_category, _message);

    env->ReleaseStringUTFChars(category, _category);
    env->ReleaseStringUTFChars(message, _message);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_logError(JNIEnv *env, jclass clazz,
                                                              jstring category, jstring message) {
    const char *_category = env->GetStringUTFChars(category, nullptr);
    const char *_message = env->GetStringUTFChars(message, nullptr);

    Wallet::Wallet::error(_category, _message);

    env->ReleaseStringUTFChars(category, _category);
    env->ReleaseStringUTFChars(message, _message);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_initLogger(JNIEnv *env, jclass clazz,
                                                                jstring argv0,
                                                                jstring default_log_base_name) {
    const char *_argv0 = env->GetStringUTFChars(argv0, nullptr);
    const char *_default_log_base_name = env->GetStringUTFChars(default_log_base_name, nullptr);

    Wallet::Wallet::init(_argv0, _default_log_base_name);

    env->ReleaseStringUTFChars(argv0, _argv0);
    env->ReleaseStringUTFChars(default_log_base_name, _default_log_base_name);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_resolveOpenAlias(JNIEnv *env, jobject thiz,
                                                                      jstring address,
                                                                      jboolean dnssec_valid) {
    const char *_address = env->GetStringUTFChars(address, nullptr);
    bool _dnssec_valid = (bool) dnssec_valid;
    std::string resolvedAlias =
            Wallet::WalletManagerFactory::getWalletManager()->resolveOpenAlias(
                    std::string(_address),
                    _dnssec_valid);
    env->ReleaseStringUTFChars(address, _address);
    return env->NewStringUTF(resolvedAlias.c_str());
}
/*extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_stopMining(JNIEnv *env, jobject thiz) {
    return static_cast<jboolean>(Wallet::WalletManagerFactory::getWalletManager()->stopMining());
}*/
/*extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_startMining(JNIEnv *env, jobject thiz,
                                                                jstring address,
                                                                jboolean background_mining,
                                                                jboolean ignore_battery) {
    const char *_address = env->GetStringUTFChars(address, nullptr);
    bool success =
            Wallet::WalletManagerFactory::getWalletManager()->startMining(std::string(_address),
                                                                             background_mining,
                                                                             ignore_battery);
    env->ReleaseStringUTFChars(address, _address);
    return static_cast<jboolean>(success);
}*/
/*extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_isMining(JNIEnv *env, jobject thiz) {
    return static_cast<jboolean>(Wallet::WalletManagerFactory::getWalletManager()->isMining());
}*/
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_getBlockTarget(JNIEnv *env, jobject thiz) {
    return Wallet::WalletManagerFactory::getWalletManager()->blockTarget();
}
/*extern "C"
JNIEXPORT jdouble JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_getMiningHashRate(JNIEnv *env, jobject thiz) {
    return Wallet::WalletManagerFactory::getWalletManager()->miningHashRate();
}*/
/*extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_getNetworkDifficulty(JNIEnv *env,
                                                                         jobject thiz) {
    return Wallet::WalletManagerFactory::getWalletManager()->networkDifficulty();
}*/
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_getBlockchainTargetHeight(JNIEnv *env,
                                                                               jobject thiz) {
    return Wallet::WalletManagerFactory::getWalletManager()->blockchainTargetHeight();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_getBlockchainHeight(JNIEnv *env, jobject thiz) {
    return Wallet::WalletManagerFactory::getWalletManager()->blockchainHeight();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_getDaemonVersion(JNIEnv *env, jobject thiz) {
    uint32_t version;
    bool isConnected =
            Wallet::WalletManagerFactory::getWalletManager()->connected(&version);
    if (!isConnected) version = 0;
    return version;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_setDaemonAddressJ(JNIEnv *env, jobject thiz,
                                                                       jstring address) {
    const char *_address = env->GetStringUTFChars(address, nullptr);
    Wallet::WalletManagerFactory::getWalletManager()->setDaemonAddress(std::string(_address));
    env->ReleaseStringUTFChars(address, _address);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_queryWalletDeviceJ(JNIEnv *env, jobject thiz,
                                                                        jstring keys_file_name,
                                                                        jstring password) {
    const char *_keys_file_name = env->GetStringUTFChars(keys_file_name, nullptr);
    const char *_password = env->GetStringUTFChars(password, nullptr);
    Wallet::Wallet::Device device_type;
    bool ok = Wallet::WalletManagerFactory::getWalletManager()->
            queryWalletDevice(device_type, std::string(_keys_file_name), std::string(_password));
    env->ReleaseStringUTFChars(keys_file_name, _keys_file_name);
    env->ReleaseStringUTFChars(password, _password);
    if (ok)
        return static_cast<jint>(device_type);
    else
        return -1;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_verifyWalletPassword(JNIEnv *env, jobject thiz,
                                                                          jstring keys_file_name,
                                                                          jstring password,
                                                                          jboolean watch_only) {
    const char *_keys_file_name = env->GetStringUTFChars(keys_file_name, nullptr);
    const char *_password = env->GetStringUTFChars(password, nullptr);
    bool passwordOk =
            Wallet::WalletManagerFactory::getWalletManager()->verifyWalletPassword(
                    std::string(_keys_file_name), std::string(_password), watch_only);
    env->ReleaseStringUTFChars(keys_file_name, _keys_file_name);
    env->ReleaseStringUTFChars(password, _password);
    return static_cast<jboolean>(passwordOk);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_walletExists(JNIEnv *env, jobject thiz,
                                                                  jstring path) {
    const char *_path = env->GetStringUTFChars(path, nullptr);
    bool exists =
            Wallet::WalletManagerFactory::getWalletManager()->walletExists(std::string(_path));
    env->ReleaseStringUTFChars(path, _path);
    return static_cast<jboolean>(exists);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_closeJ(JNIEnv *env, jobject instance,
                                                            jobject walletInstance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, walletInstance);
    bool closeSuccess = Wallet::WalletManagerFactory::getWalletManager()->closeWallet(wallet,
                                                                                      false);
    if (closeSuccess) {
        MyWalletListener *walletListener = getHandle<MyWalletListener>(env, walletInstance,
                                                                       "listenerHandle");
        if (walletListener != nullptr) {
            walletListener->deleteGlobalJavaRef(env);
            delete walletListener;
        }
    }
    LOGD("wallet closed");
    return static_cast<jboolean>(closeSuccess);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_createWalletFromDeviceJ(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jstring path,
                                                                             jstring password,
                                                                             jint network_type,
                                                                             jstring device_name,
                                                                             jlong restore_height,
                                                                             jstring subaddress_lookahead) {
    const char *_path = env->GetStringUTFChars(path, nullptr);
    const char *_password = env->GetStringUTFChars(password, nullptr);
    Wallet::NetworkType _networkType = static_cast<Wallet::NetworkType>(network_type);
    const char *_deviceName = env->GetStringUTFChars(device_name, nullptr);
    const char *_subaddressLookahead = env->GetStringUTFChars(subaddress_lookahead, nullptr);

    Wallet::Wallet *wallet =
            Wallet::WalletManagerFactory::getWalletManager()->createWalletFromDevice(
                    std::string(_path),
                    std::string(_password),
                    _networkType,
                    std::string(_deviceName),
                    (uint64_t) restore_height,
                    std::string(_subaddressLookahead));

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(password, _password);
    env->ReleaseStringUTFChars(device_name, _deviceName);
    env->ReleaseStringUTFChars(subaddress_lookahead, _subaddressLookahead);
    return reinterpret_cast<jlong>(wallet);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_createWalletFromKeysJ(JNIEnv *env, jobject thiz,
                                                                           jstring path,
                                                                           jstring password,
                                                                           jstring language,
                                                                           jint network_type,
                                                                           jlong restore_height,
                                                                           jstring address_string,
                                                                           jstring view_key_string,
                                                                           jstring spend_key_string) {
    const char *_path = env->GetStringUTFChars(path, nullptr);
    const char *_password = env->GetStringUTFChars(password, nullptr);
    const char *_language = env->GetStringUTFChars(language, nullptr);
    Wallet::NetworkType _networkType = static_cast<Wallet::NetworkType>(network_type);
    const char *_addressString = env->GetStringUTFChars(address_string, nullptr);
    const char *_viewKeyString = env->GetStringUTFChars(view_key_string, nullptr);
    const char *_spendKeyString = env->GetStringUTFChars(spend_key_string, nullptr);

    Wallet::Wallet *wallet =
            Wallet::WalletManagerFactory::getWalletManager()->createWalletFromKeys(
                    std::string(_path),
                    std::string(_password),
                    std::string(_language),
                    _networkType,
                    (uint64_t) restore_height,
                    std::string(_addressString),
                    std::string(_viewKeyString),
                    std::string(_spendKeyString));

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(password, _password);
    env->ReleaseStringUTFChars(language, _language);
    env->ReleaseStringUTFChars(address_string, _addressString);
    env->ReleaseStringUTFChars(view_key_string, _viewKeyString);
    env->ReleaseStringUTFChars(spend_key_string, _spendKeyString);
    return reinterpret_cast<jlong>(wallet);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_recoveryWalletJ(JNIEnv *env, jobject thiz,
                                                                     jstring path, jstring password,
                                                                     jstring mnemonic,
                                                                     jint network_type,
                                                                     jlong restore_height) {
    const char *_path = env->GetStringUTFChars(path, nullptr);
    const char *_password = env->GetStringUTFChars(password, nullptr);
    const char *_mnemonic = env->GetStringUTFChars(mnemonic, nullptr);
    Wallet::NetworkType _networkType = static_cast<Wallet::NetworkType>(network_type);

    Wallet::Wallet *wallet =
            Wallet::WalletManagerFactory::getWalletManager()->recoveryWallet(
                    std::string(_path),
                    std::string(_password),
                    std::string(_mnemonic),
                    _networkType,
                    (uint64_t) restore_height);

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(password, _password);
    env->ReleaseStringUTFChars(mnemonic, _mnemonic);
    return reinterpret_cast<jlong>(wallet);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_openWalletJ(JNIEnv *env, jobject thiz,
                                                                 jstring path, jstring password,
                                                                 jint network_type) {
    const char *_path = env->GetStringUTFChars(path, nullptr);
    const char *_password = env->GetStringUTFChars(password, nullptr);
    Wallet::NetworkType _networkType = static_cast<Wallet::NetworkType>(network_type);

    Wallet::Wallet *wallet =
            Wallet::WalletManagerFactory::getWalletManager()->openWallet(
                    std::string(_path),
                    std::string(_password),
                    _networkType);

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(password, _password);
    return reinterpret_cast<jlong>(wallet);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_createWalletJ(JNIEnv *env, jobject thiz,
                                                                   jstring path, jstring password,
                                                                   jstring language,
                                                                   jint network_type) {
    const char *_path = env->GetStringUTFChars(path, nullptr);
    const char *_password = env->GetStringUTFChars(password, nullptr);
    const char *_language = env->GetStringUTFChars(language, nullptr);
    Wallet::NetworkType _networkType = static_cast<Wallet::NetworkType>(network_type);

    Wallet::Wallet *wallet =
            Wallet::WalletManagerFactory::getWalletManager()->createWallet(
                    std::string(_path),
                    std::string(_password),
                    std::string(_language),
                    _networkType);

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(password, _password);
    env->ReleaseStringUTFChars(language, _language);
    return reinterpret_cast<jlong>(wallet);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_nettype(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->nettype();
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getIntegratedAddress(JNIEnv *env, jobject instance,
                                                                   jstring payment_id) {
    const char *_payment_id = env->GetStringUTFChars(payment_id, nullptr);
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    std::string address = wallet->integratedAddress(_payment_id);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return env->NewStringUTF(address.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getPublicViewKey(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(wallet->publicViewKey().c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getSecretViewKey(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(wallet->secretViewKey().c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getPublicSpendKey(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(wallet->publicSpendKey().c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getSecretSpendKey(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(wallet->secretSpendKey().c_str());
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_store(JNIEnv *env, jobject instance, jstring path) {
    const char *_path = env->GetStringUTFChars(path, nullptr);
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    bool success = wallet->store(std::string(_path));
    if (!success) {
        LOGE("store() %s", "wallet->errorString().c_str()");
    }
    env->ReleaseStringUTFChars(path, _path);
    return static_cast<jboolean>(success);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getFilename(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(wallet->filename().c_str());
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_initJ(JNIEnv *env, jobject instance,
                                                    jstring daemon_address,
                                                    jlong upper_transaction_size_limit,
                                                    jstring daemon_username,
                                                    jstring daemon_password) {
    const char *_daemon_address = env->GetStringUTFChars(daemon_address, nullptr);
    const char *_daemon_username = env->GetStringUTFChars(daemon_username, nullptr);
    const char *_daemon_password = env->GetStringUTFChars(daemon_password, nullptr);
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    bool status = wallet->init(_daemon_address, (uint64_t) upper_transaction_size_limit,
                               _daemon_username,
                               _daemon_password);
    env->ReleaseStringUTFChars(daemon_address, _daemon_address);
    env->ReleaseStringUTFChars(daemon_username, _daemon_username);
    env->ReleaseStringUTFChars(daemon_password, _daemon_password);
    return static_cast<jboolean>(status);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_setRestoreHeight(JNIEnv *env, jobject instance,
                                                               jlong height) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->setRefreshFromBlockHeight((uint64_t) height);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getRestoreHeight(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->getRefreshFromBlockHeight();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getConnectionStatusJ(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->connected();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getBalance(JNIEnv *env, jobject instance,
                                                         jint account_index) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->balance((uint32_t) account_index);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getBalanceAll(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->balanceAll();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getUnlockedBalanceAll(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->unlockedBalanceAll();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getUnlockedBalance(JNIEnv *env, jobject instance,
                                                                 jint account_index) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->unlockedBalance((uint32_t) account_index);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_isWatchOnly(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return static_cast<jboolean>(wallet->watchOnly());
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getBlockChainHeight(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->blockChainHeight();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getApproximateBlockChainHeight(JNIEnv *env,
                                                                             jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->approximateBlockChainHeight();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getDaemonBlockChainHeight(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->daemonBlockChainHeight();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getDaemonBlockChainTargetHeight(JNIEnv *env,
                                                                              jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return wallet->daemonBlockChainTargetHeight();
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getDisplayAmount(JNIEnv *env, jclass clazz,
                                                               jlong amount) {
    return env->NewStringUTF(Wallet::Wallet::displayAmount(amount).c_str());
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getAmountFromString(JNIEnv *env, jclass clazz,
                                                                  jstring amount) {
    const char *_amount = env->GetStringUTFChars(amount, nullptr);
    uint64_t x = Wallet::Wallet::amountFromString(_amount);
    env->ReleaseStringUTFChars(amount, _amount);
    return x;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getAmountFromDouble(JNIEnv *env, jclass clazz,
                                                                  jdouble amount) {
    return Wallet::Wallet::amountFromDouble(amount);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_generatePaymentId(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(Wallet::Wallet::genPaymentId().c_str());
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_isPaymentIdValid(JNIEnv *env, jclass clazz,
                                                               jstring payment_id) {
    const char *_payment_id = env->GetStringUTFChars(payment_id, nullptr);
    bool isValid = Wallet::Wallet::paymentIdValid(_payment_id);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return static_cast<jboolean>(isValid);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_isAddressValid(JNIEnv *env, jclass clazz,
                                                             jstring address, jint network_type) {
    const char *_address = env->GetStringUTFChars(address, nullptr);
    Wallet::NetworkType _networkType = static_cast<Wallet::NetworkType>(network_type);
    bool isValid = Wallet::Wallet::addressValid(_address, _networkType);
    env->ReleaseStringUTFChars(address, _address);
    return static_cast<jboolean>(isValid);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getPaymentIdFromAddress(JNIEnv *env, jclass clazz,
                                                                      jstring address,
                                                                      jint network_type) {
    Wallet::NetworkType _networkType = static_cast<Wallet::NetworkType>(network_type);
    const char *_address = env->GetStringUTFChars(address, nullptr);
    std::string payment_id = Wallet::Wallet::paymentIdFromAddress(_address, _networkType);
    env->ReleaseStringUTFChars(address, _address);
    return env->NewStringUTF(payment_id.c_str());
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getMaximumAllowedAmount(JNIEnv *env, jclass clazz) {
    return Wallet::Wallet::maximumAllowedAmount();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_startRefresh(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->startRefresh();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_pauseRefresh(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->pauseRefresh();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_refresh(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return static_cast<jboolean>(wallet->refresh());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_refreshAsync(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->refreshAsync();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_rescanBlockchainAsyncJ(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->rescanBlockchainAsync();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_createTransactionJ(JNIEnv *env, jobject instance,
                                                                 jstring dst_addr,
                                                                 jstring payment_id, jlong amount,
                                                                 jint mixin_count, jint priority,
                                                                 jint account_index) {
    /*const char *_dst_addr = env->GetStringUTFChars(dst_addr, nullptr);
    const char *_payment_id = env->GetStringUTFChars(payment_id, nullptr);
    *//* Bitmonero::PendingTransaction::Priority _priority =
             static_cast<Bitmonero::PendingTransaction::Priority>(priority);*//*
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);

    Wallet::PendingTransaction *tx = wallet->createTransaction(_dst_addr, _payment_id,
                                                                  amount, (uint32_t) mixin_count,
                                                                  priority,
                                                                  (uint32_t) account_index);

    env->ReleaseStringUTFChars(dst_addr, _dst_addr);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return reinterpret_cast<jlong>(tx);*/

    const char *_dst_addr = env->GetStringUTFChars(dst_addr, nullptr);
    const char *_payment_id = env->GetStringUTFChars(payment_id, nullptr);
    /* Wallet::PendingTransaction::Priority _priority =
             static_cast<Wallet::PendingTransaction::Priority>(priority);*/
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    uint32_t subaddr_account = account_index;
    std::set<uint32_t> subaddr_indices = {};
    LOGD("Java_com_thoughtcrimes_securesms_model_Wallet_createTransactionJ before createTransaction");
    LOGD("Java_com_thoughtcrimes_securesms_model_Wallet_createTransactionJ amount %ld subaddr_account %d",amount,subaddr_account);

    Wallet::PendingTransaction *tx = wallet->createTransaction(_dst_addr,
                                                               amount,
                                                               priority,
                                                               subaddr_account,
                                                               subaddr_indices);
    LOGD("Java_com_thoughtcrimes_securesms_model_Wallet_createTransactionJ after createTransaction pointer %ld", reinterpret_cast<jlong>(tx));
    if (!tx){
        LOGD("No TX pointer found");
    }
    env->ReleaseStringUTFChars(dst_addr, _dst_addr);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return reinterpret_cast<jlong>(tx);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_createSweepTransaction(JNIEnv *env, jobject instance,
                                                                     jstring dst_addr,
                                                                     jstring payment_id,
                                                                     jint mixin_count, jint priority,
                                                                     jint account_index) {
    /*const char *_dst_addr = env->GetStringUTFChars(dst_addr, nullptr);
    const char *_payment_id = env->GetStringUTFChars(payment_id, nullptr);
    *//*Bitmonero::PendingTransaction::Priority _priority =
            static_cast<Bitmonero::PendingTransaction::Priority>(priority);*//*
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);

    Wallet::optional<uint64_t> empty;

    Wallet::PendingTransaction *tx = wallet->createTransaction(_dst_addr, _payment_id,
                                                                  empty, (uint32_t) mixin_count,
                                                                  priority,
                                                                  (uint32_t) account_index);

    env->ReleaseStringUTFChars(dst_addr, _dst_addr);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return reinterpret_cast<jlong>(tx);*/

    const char *_dst_addr = env->GetStringUTFChars(dst_addr, nullptr);
    const char *_payment_id = env->GetStringUTFChars(payment_id, nullptr);
    /*Wallet::PendingTransaction::Priority _priority =
            static_cast<Wallet::PendingTransaction::Priority>(priority);*/
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);

    std::optional<uint64_t> empty;
    uint32_t subaddr_account = account_index;
    std::set<uint32_t> subaddr_indices = {};
    Wallet::PendingTransaction *tx = wallet->createTransaction(_dst_addr,
                                                               empty,
                                                               priority,
                                                               subaddr_account,
                                                               subaddr_indices);
    //TODO: something like this transaction->m_pending_tx = m_wallet->create_unmixable_sweep_transactions();

    env->ReleaseStringUTFChars(dst_addr, _dst_addr);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return reinterpret_cast<jlong>(tx);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_createSweepUnmixableTransactionJ(JNIEnv *env,
                                                                               jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    Wallet::PendingTransaction *tx = wallet->createSweepUnmixableTransaction();
    return reinterpret_cast<jlong>(tx);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_disposeTransaction(JNIEnv *env, jobject instance,
                                                                 jobject pending_transaction) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    Wallet::PendingTransaction *_pendingTransaction =
            getHandle<Wallet::PendingTransaction>(env, pending_transaction);
    wallet->disposeTransaction(_pendingTransaction);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getHistoryJ(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return reinterpret_cast<jlong>(wallet->history());
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_setListenerJ(JNIEnv *env, jobject instance,
                                                           jobject javaListener) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->setListener(nullptr); // clear old listener
    // delete old listener
    MyWalletListener *oldListener = getHandle<MyWalletListener>(env, instance,
                                                                "listenerHandle");
    if (oldListener != nullptr) {
        oldListener->deleteGlobalJavaRef(env);
        delete oldListener;
    }
    if (javaListener == nullptr) {
        LOGD("null listener");
        return 0;
    } else {
        MyWalletListener *listener = new MyWalletListener(env, javaListener);
        wallet->setListener(listener);
        return reinterpret_cast<jlong>(listener);
    }
}
/*extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getDefaultMixin(JNIEnv *env, jobject thiz) {
    // TODO: implement getDefaultMixin()
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_setDefaultMixin(JNIEnv *env, jobject thiz,
                                                             jint mixin) {
    // TODO: implement setDefaultMixin()
}*/
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_setUserNote(JNIEnv *env, jobject instance, jstring txid,
                                                          jstring note) {
    const char *_txid = env->GetStringUTFChars(txid, nullptr);
    const char *_note = env->GetStringUTFChars(note, nullptr);

    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);

    bool success = wallet->setUserNote(_txid, _note);

    env->ReleaseStringUTFChars(txid, _txid);
    env->ReleaseStringUTFChars(note, _note);

    return static_cast<jboolean>(success);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getUserNote(JNIEnv *env, jobject instance, jstring txid) {
    const char *_txid = env->GetStringUTFChars(txid, nullptr);

    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);

    std::string note = wallet->getUserNote(_txid);

    env->ReleaseStringUTFChars(txid, _txid);
    return env->NewStringUTF(note.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getTxKey(JNIEnv *env, jobject instance, jstring txid) {
    const char *_txid = env->GetStringUTFChars(txid, nullptr);

    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);

    std::string txKey = wallet->getTxKey(_txid);

    env->ReleaseStringUTFChars(txid, _txid);
    return env->NewStringUTF(txKey.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_addAccount(JNIEnv *env, jobject instance, jstring label) {
    const char *_label = env->GetStringUTFChars(label, nullptr);

    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->addSubaddressAccount(_label);

    env->ReleaseStringUTFChars(label, _label);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getSubaddressLabel(JNIEnv *env, jobject instance,
                                                                 jint account_index,
                                                                 jint address_index) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);

    std::string label = wallet->getSubaddressLabel((uint32_t) account_index,
                                                   (uint32_t) address_index);

    return env->NewStringUTF(label.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_setSubaddressLabel(JNIEnv *env, jobject instance,
                                                                 jint account_index,
                                                                 jint address_index, jstring label) {
    const char *_label = env->GetStringUTFChars(label, nullptr);

    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->setSubaddressLabel(account_index, address_index, _label);

    env->ReleaseStringUTFChars(label, _label);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getNumAccounts(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return static_cast<jint>(wallet->numSubaddressAccounts());
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getNumSubaddresses(JNIEnv *env, jobject instance,
                                                                 jint account_index) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return static_cast<jint>(wallet->numSubaddresses(account_index));
}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_addSubaddress(JNIEnv *env, jobject instance,
                                                            jint account_index, jstring label) {
    const char *_label = env->GetStringUTFChars(label, nullptr);
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    wallet->addSubaddress(account_index, _label);
    env->ReleaseStringUTFChars(label, _label);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getDeviceTypeJ(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    Wallet::Wallet::Device device_type = wallet->getDeviceType();
    return static_cast<jint>(device_type);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getPath(JNIEnv *env, jobject instance) {
    Wallet::Wallet *wallet = getHandle<Wallet::Wallet>(env, instance);
    return env->NewStringUTF(wallet->path().c_str());
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_thoughtcrimes_securesms_util_KeyStoreHelper_slowHash(JNIEnv *env, jclass clazz,
                                                              jbyteArray data, jint broken_variant) {
    char hash[HASH_SIZE];
    jsize size = env->GetArrayLength(data);
    if ((broken_variant > 0) && (size < 200 /*sizeof(union hash_state)*/)) {
        return nullptr;
    }

    jbyte *buffer = env->GetByteArrayElements(data, nullptr);
    switch (broken_variant) {
        case 1:
            slow_hash_broken(buffer, hash, 1);
            break;
        case 2:
            slow_hash_broken(buffer, hash, 0);
            break;
        default: // not broken
            slow_hash(buffer, (size_t) size, hash);
    }
    env->ReleaseByteArrayElements(data, buffer, JNI_ABORT); // do not update java byte[]
    jbyteArray result = env->NewByteArray(HASH_SIZE);
    env->SetByteArrayRegion(result, 0, HASH_SIZE, (jbyte *) hash);
    return result;
}
/*
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_getViewKeyAndSpendKey(JNIEnv *env, jobject instance,
                                                                          jstring address,jlong netWorkType) {
    const char *_address = env->GetStringUTFChars(address, nullptr);
    char a='a';
    std::string viewKey;
    std::string spendKey;
    std::basic_string_view<char> add = _address;
    std::uint64_t _networkType =(uint64_t) netWorkType;

    Wallet::GetKey *wallet = getHandle<Wallet::GetKey>(env, instance);
    bool result = wallet->get_keys_from_address(add,_networkType,viewKey,spendKey);
    LOGD("--> R get_keys_from_address %d",result);
    LOGD("--> R view key %s",viewKey.c_str());
    LOGD("--> R spend key %s",spendKey.c_str());
    env->ReleaseStringUTFChars(address, _address);
}*/

extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_getViewKeyAndSpendKey(JNIEnv *env, jobject instance,
                                                                           jstring s) {
//    const char *_address = env->GetStringUTFChars(s, nullptr);
//    //uint64_t _networkType =0;
//
//    Wallet::GetKey *wallet = getHandle<Wallet::GetKey>(env,instance);
//    auto stat = wallet->get_keys_from_address(_address,0);
//    auto [viewKey, spendKey] = stat;
//
//    LOGD("--> R view key %s",_address);
//    //LOGD("--> R view key %s",viewKey.c_str());
//    //LOGD("--> R spend key %s",spendKey.c_str());
//    env->ReleaseStringUTFChars(s, _address);

}
extern "C"
JNIEXPORT void JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_getViewKeyAndSpendKey(JNIEnv *env, jobject instance,
                                                                    jstring s) {
//    const char *_address = env->GetStringUTFChars(s, nullptr);
//    //uint64_t _networkType =0;
//
//    Wallet::GetKey *wallet = getHandle<Wallet::GetKey>(env,instance);
//    auto stat = wallet->get_keys_from_address(_address,0);
//    auto [viewKey, spendKey] = stat;
//
//    LOGD("--> R view key %s",_address);
//    LOGD("--> R view key %s",viewKey.c_str());
//    LOGD("--> R spend key %s",spendKey.c_str());
//    env->ReleaseStringUTFChars(s, _address);

}

jobject newTransferInstance(JNIEnv *env, uint64_t amount, const std::string &address) {
    jmethodID c = env->GetMethodID(class_Transfer, "<init>",
                                   "(JLjava/lang/String;)V");
    jstring _address = env->NewStringUTF(address.c_str());
    jobject transfer = env->NewObject(class_Transfer, c, static_cast<jlong> (amount), _address);
    env->DeleteLocalRef(_address);
    return transfer;
}

jobject newTransferList(JNIEnv *env, Wallet::TransactionInfo *info) {
    const std::vector<Wallet::TransactionInfo::Transfer> &transfers = info->transfers();
    if (transfers.empty()) { // don't create empty Lists
        LOGD("--> R view key transfers is empty");
        return nullptr;
    }
    // make new ArrayList
    jmethodID java_util_ArrayList_ = env->GetMethodID(class_ArrayList, "<init>", "(I)V");
    jmethodID java_util_ArrayList_add = env->GetMethodID(class_ArrayList, "add",
                                                         "(Ljava/lang/Object;)Z");
    jobject result = env->NewObject(class_ArrayList, java_util_ArrayList_,
                                    static_cast<jint> (transfers.size()));
    // create Transfer objects and stick them in the List
    for (const Wallet::TransactionInfo::Transfer &s: transfers) {
        jobject element = newTransferInstance(env, s.amount, s.address);
        env->CallBooleanMethod(result, java_util_ArrayList_add, element);
        env->DeleteLocalRef(element);
    }
    return result;
}

jobject newTransactionInfo(JNIEnv *env, Wallet::TransactionInfo *info) {
    jmethodID c = env->GetMethodID(class_TransactionInfo, "<init>",
                                   "(IZZJJJLjava/lang/String;JLjava/lang/String;IIJLjava/lang/String;Ljava/util/List;)V");
    jobject transfers = newTransferList(env, info);
    jstring _hash = env->NewStringUTF(info->hash().c_str());
    const char *paymentId = "-";
    jstring _paymentId = env->NewStringUTF(paymentId);
    const char *label = "-";
    jstring _label = env->NewStringUTF(label);
    uint32_t subaddrIndex = 0;
    if (info->direction() == Wallet::TransactionInfo::Direction_In)
        subaddrIndex = *(info->subaddrIndex().begin());
    jobject result = env->NewObject(class_TransactionInfo, c,
                                    info->direction(),
                                    info->isPending(),
                                    info->isFailed(),
                                    static_cast<jlong> (info->amount()),
                                    static_cast<jlong> (info->fee()),
                                    static_cast<jlong> (info->blockHeight()),
                                    _hash,
                                    static_cast<jlong> (info->timestamp()),
                                    _paymentId,
                                    static_cast<jint> (info->subaddrAccount()),
                                    static_cast<jint> (subaddrIndex),
                                    static_cast<jlong> (info->confirmations()),
                                    _label,
                                    transfers);
    env->DeleteLocalRef(transfers);
    env->DeleteLocalRef(_hash);
    env->DeleteLocalRef(_paymentId);
    return result;
}

#include <stdio.h>
#include <stdlib.h>

jobject cpp2java(JNIEnv *env, const std::vector<Wallet::TransactionInfo *>& vector) {

    jmethodID java_util_ArrayList_ = env->GetMethodID(class_ArrayList, "<init>", "(I)V");
    jmethodID java_util_ArrayList_add = env->GetMethodID(class_ArrayList, "add",
                                                         "(Ljava/lang/Object;)Z");

    jobject arrayList = env->NewObject(class_ArrayList, java_util_ArrayList_,
                                       static_cast<jint> (vector.size()));
    LOGD("--> R_view key 6 %lu",vector.size());
    for (Wallet::TransactionInfo *s: vector) {
        LOGD("--> R_view key 1 %d",100);
        LOGD("--> R_view key 2 %lu",s->amount());
        //if(!s->isPending()) {
            jobject info = newTransactionInfo(env, s);
            env->CallBooleanMethod(arrayList, java_util_ArrayList_add, info);
            env->DeleteLocalRef(info);
        //}
    }
    return arrayList;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_thoughtcrimes_securesms_model_TransactionHistory_refreshJ(JNIEnv *env, jobject instance) {
    Wallet::TransactionHistory *history = getHandle<Wallet::TransactionHistory>(env,instance);
    history->refresh();
    LOGD("--> R_view key 5 %d",101);
    return cpp2java(env, history->getAll());
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_TransactionHistory_getCount(JNIEnv *env, jobject instance) {
    Wallet::TransactionHistory *history = getHandle<Wallet::TransactionHistory>(env,instance);
    LOGD("--> R_view key 3 %d",100);
    LOGD("--> R_view key 4 %d",history->count());
    return history->count();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_PendingTransaction_getStatusJ(JNIEnv *env, jobject instance) {
    Wallet::PendingTransaction *tx = getHandle<Wallet::PendingTransaction>(env, instance);
    auto stat = tx->status();
    auto& [status, errorString] = stat;
    LOGD("Java_com_thoughtcrimes_securesms_model_PendingTransaction_getStatusJ status:%d %s",status, errorString.c_str());
    return status;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_PendingTransaction_getErrorStringJ(JNIEnv *env,
                                                                         jobject instance) {
    /*Wallet::PendingTransaction *tx = getHandle<Wallet::PendingTransaction>(env, instance);
    jstring result;
    LOGD("Java_com_thoughtcrimes_securesms_model_PendingTransaction_getErrorString result:%s",result);
    if (tx){
        result = env->NewStringUTF(tx->status().second.c_str());
        LOGD("Java_com_thoughtcrimes_securesms_model_PendingTransaction_getErrorString result:%s",result);
    }
    else {
        result = (jstring) "Java_com_thoughtcrimes_securesms_model_PendingTransaction_getErrorString no TX";
    }
    LOGD("end Java_com_thoughtcrimes_securesms_model_PendingTransaction_getErrorString");
    return result;*/
    LOGD("Java_com_thoughtcrimes_securesms_model_PendingTransaction_getErrorStringJ");
    Wallet::PendingTransaction *tx = getHandle<Wallet::PendingTransaction>(env, instance);
    auto stat = tx->status();
    auto& [status, errorString] = stat;
    return env->NewStringUTF(errorString.c_str());
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_PendingTransaction_commit(JNIEnv *env, jobject instance,
                                                                 jstring filename,
                                                                 jboolean overwrite) {
    const char *_filename = env->GetStringUTFChars(filename, nullptr);

    Wallet::PendingTransaction *tx = getHandle<Wallet::PendingTransaction>(env, instance);
    bool success = tx->commit(_filename, overwrite);
    if (success) {
        LOGD("TX commit success");
    }else{
        LOGD("TX commit failed");
    };
    LOGD("TX commit success==%d",success);
    env->ReleaseStringUTFChars(filename, _filename);
    return static_cast<jboolean>(success);
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_PendingTransaction_getAmount(JNIEnv *env, jobject thiz) {
    LOGD("before Java_com_thoughtcrimes_securesms_model_PendingTransaction_getAmount()");
    auto *tx = getHandle<Wallet::PendingTransaction>(env, thiz);
    LOGD("PendingTransaction pointer:%d",reinterpret_cast<jlong>(tx));
    if(!tx){LOGE("No PendingTransaction");}
    LOGD("PendingTransaction getstatus-doublecheck");
    auto stat = tx->status();
    LOGD("PendingTransaction amount");
    auto amount = tx->amount();

    LOGD("after Java_com_thoughtcrimes_securesms_model_PendingTransaction_getAmount()");
    return amount;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_PendingTransaction_getDust(JNIEnv *env, jobject instance) {
    Wallet::PendingTransaction *tx = getHandle<Wallet::PendingTransaction>(env, instance);
    return tx->dust();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_PendingTransaction_getFee(JNIEnv *env, jobject instance) {
    Wallet::PendingTransaction *tx = getHandle<Wallet::PendingTransaction>(env, instance);
    //long value = 3400000;
    //return value;
    return tx->fee();
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_PendingTransaction_getFirstTxIdJ(JNIEnv *env, jobject instance) {
    Wallet::PendingTransaction *tx = getHandle<Wallet::PendingTransaction>(env, instance);
    std::vector<std::string> txids = tx->txid();
    if (!txids.empty())
        return env->NewStringUTF(txids.front().c_str());
    else
        return nullptr;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_thoughtcrimes_securesms_model_PendingTransaction_getTxCount(JNIEnv *env, jobject instance) {
    Wallet::PendingTransaction *tx = getHandle<Wallet::PendingTransaction>(env, instance);
    return tx->txCount();
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_estimateTransactionFeeJ(JNIEnv *env, jobject instance,
                                                                      jint priority_raw,jint recipients) {
    Wallet::Wallet *tx = getHandle<Wallet::Wallet>(env,instance);
    return static_cast<jint>(tx->estimateTransactionFee(priority_raw,recipients));
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_thoughtcrimes_securesms_model_Wallet_reConnectToDaemon(JNIEnv *env, jobject instance,
                                                                jobject node, jboolean use_ssl,
                                                                jboolean is_light_wallet) {
    Wallet::Wallet *isConnected = getHandle<Wallet::Wallet>(env, instance);
    bool connectionStatus = isConnected->connectToDaemon();
    return static_cast<jboolean>(connectionStatus);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_thoughtcrimes_securesms_model_WalletManager_bChatVersion(JNIEnv *env, jclass clazz) {
    return (jstring) "-";
}