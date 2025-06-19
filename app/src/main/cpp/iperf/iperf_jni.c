#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <android/log.h>

#include "iperf.h"
#include "iperf_api.h"

#define TAG "iperfJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static volatile int test_running = 0;
static struct iperf_test *global_test = NULL;
static pthread_t reader_thread;
static int reader_thread_running = 0;


struct CallbackArgs {
    JavaVM *jvm;
    jobject callback_global;
    int pipe_fd;
};

void *reader_thread_func(void *args_ptr) {
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

    free(args);  // ✅ Last step after everything else

    return NULL;
}

JNIEXPORT void JNICALL
Java_com_abhishek_cellularlab_MainActivity_forceStopIperfTest(JNIEnv *env, jobject thiz,
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
//        if (reader_thread_running) {
//            pthread_cancel(reader_thread);  // ⚠️ Consider this carefully
//            reader_thread_running = 0;
//        }
//        iperf_free_test(global_test);
        global_test = NULL;

        jstring errMsg = (*env)->NewStringUTF(env,
                                              "[iPerf JNI] iPerf test was stopped and cleaned up successfully.");
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
    }
    return;  // Exit early to allow Kotlin side to detect that run ended

}

JNIEXPORT void JNICALL
Java_com_abhishek_cellularlab_MainActivity_runIperfLive(JNIEnv *env, jobject thiz,
                                                        jobjectArray arguments, jobject callback) {

    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    jmethodID onOutput = (*env)->GetMethodID(env, callbackClass, "onOutput",
                                             "(Ljava/lang/String;)V");
    jmethodID onError = (*env)->GetMethodID(env, callbackClass, "onError", "(Ljava/lang/String;)V");

    jmethodID onComplete = (*env)->GetMethodID(env, callbackClass, "onComplete", "()V");


    int argc = (*env)->GetArrayLength(env, arguments);
    if (argc > 64) argc = 64;

    char *argv[64];
    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring) (*env)->GetObjectArrayElement(env, arguments, i);
        const char *arg_str = (*env)->GetStringUTFChars(env, arg, 0);
        argv[i] = strdup(arg_str);
        (*env)->ReleaseStringUTFChars(env, arg, arg_str);
    }

    global_test = iperf_new_test();
    if (!global_test) {
        jstring errMsg = (*env)->NewStringUTF(env, "Failed to create iperf test");
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        return;
    }

    iperf_defaults(global_test);

    int pipefd[2];
    pipe(pipefd);

    FILE *fp = fdopen(pipefd[1], "w");
    setvbuf(fp, NULL, _IOLBF, 0);  // Force line buffering
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

    // Prepare thread for reading output
    struct CallbackArgs *cb_args = malloc(sizeof(struct CallbackArgs));

    (*env)->GetJavaVM(env, &cb_args->jvm);
    cb_args->callback_global = (*env)->NewGlobalRef(env, callback);
    cb_args->pipe_fd = pipefd[0];


    pthread_create(&reader_thread, NULL, reader_thread_func, cb_args);

    // Notify starting test

    jstring initMsg = (*env)->NewStringUTF(env, "🚀 Initiating iPerf3 client request...\n");
    (*env)->CallVoidMethod(env, callback, onOutput, initMsg);
    (*env)->DeleteLocalRef(env, initMsg);

    // Simulate stuck logic
//    time_t now = time(NULL);
//    if (now % 2 == 1) {
//        LOGI("[iPerf JNI] Simulating stuck condition (sleeping 99999s)...");
//        jstring stuckMsg = (*env)->NewStringUTF(env, "\n\n[iPerf JNI] Simulating stuck condition...");
//        (*env)->CallVoidMethod(env, callback, onOutput, stuckMsg);
//        (*env)->DeleteLocalRef(env, stuckMsg);
//
//        sleep(99999);  // Simulate hang
//    }
//    sleep(99999);  // Simulate hang
    int result = iperf_run_client(global_test);

    if (result < 0) {
        jstring errMsg = (*env)->NewStringUTF(env, iperf_strerror(i_errno));
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
    }

    fflush(fp);
    fclose(fp);
    iperf_free_test(global_test);

    pthread_join(reader_thread, NULL);

    // Notify complete

    (*env)->CallVoidMethod(env, callback, onComplete);

    // Free memory
    for (int i = 0; i < argc; i++) {
        free(argv[i]);
    }
}
