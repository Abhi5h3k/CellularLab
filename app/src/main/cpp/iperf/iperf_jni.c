#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <android/log.h>

#include "iperf.h"
#include "iperf_api.h"

// Logging macros
#define TAG "iperfJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Globals
static struct iperf_test *global_test = NULL;
static pthread_t reader_thread;

// Structure for passing data to thread
struct CallbackArgs {
    JavaVM *jvm;
    jobject callback_global;
    int pipe_fd;
};

// Thread function to read iperf output and forward to Java callback
void *readerThreadFunc(void *args_ptr) {
    struct CallbackArgs *args = (struct CallbackArgs *) args_ptr;
    JNIEnv *env;
    (*args->jvm)->AttachCurrentThread(args->jvm, &env, NULL);

    jclass callbackClass = (*env)->GetObjectClass(env, args->callback_global);
    jmethodID onOutput = (*env)->GetMethodID(env, callbackClass, "onOutput",
                                             "(Ljava/lang/String;)V");

    char buffer[1024];
    FILE *fp = fdopen(args->pipe_fd, "r");

    while (fgets(buffer, sizeof(buffer), fp)) {
        jstring line = (*env)->NewStringUTF(env, buffer);
        (*env)->CallVoidMethod(env, args->callback_global, onOutput, line);
        (*env)->DeleteLocalRef(env, line);
    }

    fclose(fp);
    (*env)->DeleteGlobalRef(env, args->callback_global);
    (*args->jvm)->DetachCurrentThread(args->jvm);
    free(args);

    return NULL;
}

// Forcefully stop a running iperf test
JNIEXPORT void JNICALL
Java_com_abhishek_cellularlab_ui_RunTestFragment_forceStopIperfTest(JNIEnv *env, jobject thiz,
                                                              jobject callback) {
    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    jmethodID onOutput = (*env)->GetMethodID(env, callbackClass, "onOutput",
                                             "(Ljava/lang/String;)V");
    jmethodID onError = (*env)->GetMethodID(env, callbackClass, "onError", "(Ljava/lang/String;)V");

    jstring statusMsg = (*env)->NewStringUTF(env,
                                             "[iPerf JNI] Requested force stop of ongoing iPerf test.");
    (*env)->CallVoidMethod(env, callback, onOutput, statusMsg);
    (*env)->DeleteLocalRef(env, statusMsg);

    if (global_test) {
        global_test->done = 1;
        global_test = NULL;

        jstring errMsg = (*env)->NewStringUTF(env,
                                              "[iPerf JNI] iPerf test was stopped and cleaned up successfully.");
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
    }
}

// Main iperf JNI entrypoint: runs iperf3 test and sends output to callback
JNIEXPORT void JNICALL
Java_com_abhishek_cellularlab_ui_RunTestFragment_runIperfLive(JNIEnv *env, jobject thiz,
                                                        jobjectArray arguments, jobject callback) {

    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    jmethodID onOutput = (*env)->GetMethodID(env, callbackClass, "onOutput",
                                             "(Ljava/lang/String;)V");
    jmethodID onError = (*env)->GetMethodID(env, callbackClass, "onError", "(Ljava/lang/String;)V");
    jmethodID onComplete = (*env)->GetMethodID(env, callbackClass, "onComplete", "()V");

    // Convert Java arguments to native argv[]
    int argc = (*env)->GetArrayLength(env, arguments);
    if (argc > 64) argc = 64;

    char *argv[64];
    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring) (*env)->GetObjectArrayElement(env, arguments, i);
        const char *arg_str = (*env)->GetStringUTFChars(env, arg, 0);
        argv[i] = strdup(arg_str);
        (*env)->ReleaseStringUTFChars(env, arg, arg_str);
    }

    // Initialize iperf test
    global_test = iperf_new_test();
    if (!global_test) {
        jstring errMsg = (*env)->NewStringUTF(env, "Failed to create iperf test");
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
        return;
    }
    iperf_defaults(global_test);

    // Setup pipe for capturing output
    int pipefd[2];
    pipe(pipefd);
    FILE *fp = fdopen(pipefd[1], "w");
    setvbuf(fp, NULL, _IOLBF, 0);  // Line-buffered output
    global_test->outfile = fp;

    if (iperf_parse_arguments(global_test, argc, argv) < 0) {
        fflush(fp);
        fclose(fp);

        const char *err_str = iperf_strerror(i_errno);
        LOGE("iperf_parse_arguments failed: %s", err_str);
        jstring errMsg = (*env)->NewStringUTF(env, err_str);
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);

        iperf_free_test(global_test);
        return;
    }

    // Start output reading thread
    struct CallbackArgs *cb_args = malloc(sizeof(struct CallbackArgs));
    (*env)->GetJavaVM(env, &cb_args->jvm);
    cb_args->callback_global = (*env)->NewGlobalRef(env, callback);
    cb_args->pipe_fd = pipefd[0];
    pthread_create(&reader_thread, NULL, readerThreadFunc, cb_args);

    // Notify start
    jstring initMsg = (*env)->NewStringUTF(env, "🚀 Initiating iPerf3 client request...\n");
    (*env)->CallVoidMethod(env, callback, onOutput, initMsg);
    (*env)->DeleteLocalRef(env, initMsg);

    // Run the actual test
    int result = iperf_run_client(global_test);
    if (result < 0) {
        jstring errMsg = (*env)->NewStringUTF(env, iperf_strerror(i_errno));
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
    }

    fflush(fp);
    fclose(fp);
    iperf_free_test(global_test);
    global_test = NULL;

    pthread_join(reader_thread, NULL);

    // Notify completion
    (*env)->CallVoidMethod(env, callback, onComplete);

    // Free memory
    for (int i = 0; i < argc; i++) {
        free(argv[i]);
    }
}
