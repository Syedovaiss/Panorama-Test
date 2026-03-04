#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/stitching.hpp>
#include <vector>
#include <opencv2/imgcodecs.hpp>

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovais_nativecore_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

using namespace cv;

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovais_nativecore_NativeLib_testOpenCV(JNIEnv* env, jobject) {
    Mat mat = Mat::eye(3,3,CV_32F);
    std::string msg = "OpenCV Mat size: " + std::to_string(mat.rows) + "x" + std::to_string(mat.cols);
    return env->NewStringUTF(msg.c_str());
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_ovais_nativecore_NativeLib_stitchImages(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray imageByteArrays) {

    int numImages = env->GetArrayLength(imageByteArrays);
    std::vector<cv::Mat> images;

    for (int i = 0; i < numImages; ++i) {
        jbyteArray byteArray = (jbyteArray) env->GetObjectArrayElement(imageByteArrays, i);
        jsize len = env->GetArrayLength(byteArray);
        std::vector<uchar> buffer(len);
        env->GetByteArrayRegion(byteArray, 0, len, reinterpret_cast<jbyte*>(buffer.data()));

        cv::Mat img = cv::imdecode(buffer, cv::IMREAD_COLOR);
        if (!img.empty()) images.push_back(img);
        env->DeleteLocalRef(byteArray);
    }

    if (images.empty()) return nullptr;

    cv::Mat pano;
    cv::Ptr<cv::Stitcher> stitcher = cv::Stitcher::create(cv::Stitcher::PANORAMA);
    Stitcher::Status status = stitcher->stitch(images, pano);

    if (status != Stitcher::OK) return nullptr;

    std::vector<uchar> buf;
    cv::imencode(".jpg", pano, buf);

    jbyteArray result = env->NewByteArray(buf.size());
    env->SetByteArrayRegion(result, 0, buf.size(), reinterpret_cast<jbyte*>(buf.data()));
    return result;
}