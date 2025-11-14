#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "native-lib", __VA_ARGS__)

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_edgeviewer_MainActivity_processToGrayscaleJNI(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray input_,
        jint width,
        jint height) {

    jsize len = env->GetArrayLength(input_);
    jbyte* inputBytes = env->GetByteArrayElements(input_, nullptr);

    // Create an OpenCV Mat pointing to input RGBA bytes
    cv::Mat rgba(height, width, CV_8UC4, (unsigned char*)inputBytes);

    // Convert to grayscale
    cv::Mat gray;
    cv::cvtColor(rgba, gray, cv::COLOR_RGBA2GRAY);

    // Convert grayscale back to RGBA
    cv::Mat outputRGBA;
    cv::cvtColor(gray, outputRGBA, cv::COLOR_GRAY2RGBA);

    // Create byte array for output
    jbyteArray outArray = env->NewByteArray(len);
    env->SetByteArrayRegion(outArray, 0, len, (jbyte*)outputRGBA.data);

    env->ReleaseByteArrayElements(input_, inputBytes, JNI_ABORT);

    LOGI("Processed frame: %dx%d", width, height);
    return outArray;
}
