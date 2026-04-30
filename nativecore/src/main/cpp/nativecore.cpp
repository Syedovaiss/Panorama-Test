#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/stitching.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/calib3d.hpp>
#include <vector>
#include <android/log.h>
#include <android/log.h>

#define LOG_TAG "NativeCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace cv;

// Helper to send progress updates back to Java
void reportProgress(JNIEnv* env, jobject listener, const std::string& message) {
    if (listener == nullptr) return;
    jclass listenerClass = env->GetObjectClass(listener);
    jmethodID methodId = env->GetMethodID(listenerClass, "onProgressUpdate", "(Ljava/lang/String;)V");
    if (methodId != nullptr) {
        jstring jmsg = env->NewStringUTF(message.c_str());
        env->CallVoidMethod(listener, methodId, jmsg);
        env->DeleteLocalRef(jmsg);
    }
}

Mat cylindricalWarp(const Mat& src, float f) {
    Mat map_x(src.size(), CV_32F);
    Mat map_y(src.size(), CV_32F);
    float cx = src.cols / 2.0f;
    float cy = src.rows / 2.0f;
    for (int y = 0; y < src.rows; y++) {
        float* mx = map_x.ptr<float>(y);
        float* my = map_y.ptr<float>(y);
        for (int x = 0; x < src.cols; x++) {
            float theta = (x - cx) / f;
            float h = (y - cy) / f;
            mx[x] = f * tan(theta) + cx;
            my[x] = f * h / cos(theta) + cy;
        }
    }
    Mat dst;
    remap(src, dst, map_x, map_y, INTER_CUBIC, BORDER_CONSTANT);
    return dst;
}

Mat createFeatherMask(Size sz) {
    Mat mask = Mat::zeros(sz, CV_32F);
    int featherX = sz.width / 20;
    for (int y = 0; y < sz.height; ++y) {
        float* row = mask.ptr<float>(y);
        for (int x = 0; x < sz.width; ++x) {
            float wx = std::min(x, sz.width - 1 - x) / (float)featherX;
            float w = std::max(0.0f, std::min(1.0f, wx));
            row[x] = w * w * w * w * w * w * w * w * w * w;
        }
    }
    return mask;
}

//extern "C"
//JNIEXPORT jbyteArray JNICALL
//Java_com_ovais_nativecore_NativeLib_stitchImages(
//        JNIEnv* env,
//        jobject /* this */,
//        jobjectArray imageByteArrays,
//        jobject listener) {
//
//
////    cv::ocl::setUseOpenCL(false);
////    cv::setUseOptimized(false);
////    cv::setNumThreads(1);
////
////    int numImages = env->GetArrayLength(imageByteArrays);
////    if (numImages < 2) return nullptr;
////
////    reportProgress(env, listener, "Decoding " + std::to_string(numImages) + " images...");
////
////    std::vector<Mat> images;
////    std::vector<Mat> masks;
////    float focalLength = 0;
////
////    for (int i = 0; i < numImages; ++i) {
////        jbyteArray byteArray = (jbyteArray) env->GetObjectArrayElement(imageByteArrays, i);
////        jsize len = env->GetArrayLength(byteArray);
////        std::vector<uchar> buffer(len);
////        env->GetByteArrayRegion(byteArray, 0, len, (jbyte*)buffer.data());
////        Mat img = imdecode(buffer, IMREAD_COLOR);
////        if (!img.empty()) {
////            if (img.cols > 1800) {
////                double scale = 1800.0 / img.cols;
////                resize(img, img, Size(), scale, scale, INTER_CUBIC);
////            }
////            if (focalLength == 0) focalLength = img.cols * 1.5f;
////            Mat warped = cylindricalWarp(img, focalLength);
////            images.push_back(warped);
////            masks.push_back(createFeatherMask(warped.size()));
////        }
////        env->DeleteLocalRef(byteArray);
////    }
////
////    if (images.size() < 2) return nullptr;
////
////    reportProgress(env, listener, "Matching features...");
////
////    std::vector<Point2f> offsets;
////    offsets.push_back(Point2f(0, 0));
////    Ptr<ORB> orb = ORB::create(4000);
////    BFMatcher matcher(NORM_HAMMING, true);
////    Point2f currentOffset(0, 0);
////
////    for (size_t i = 1; i < images.size(); ++i) {
////        std::vector<KeyPoint> kps1, kps2;
////        Mat desc1, desc2;
////        orb->detectAndCompute(images[i], noArray(), kps1, desc1);
////        orb->detectAndCompute(images[i-1], noArray(), kps2, desc2);
////        if (desc1.empty() || desc2.empty()) { offsets.push_back(currentOffset); continue; }
////        std::vector<DMatch> matches;
////        matcher.match(desc1, desc2, matches);
////        std::vector<float> dxs, dys;
////        for (auto& m : matches) {
////            dxs.push_back(kps2[m.trainIdx].pt.x - kps1[m.queryIdx].pt.x);
////            dys.push_back(kps2[m.trainIdx].pt.y - kps1[m.queryIdx].pt.y);
////        }
////        if (dxs.size() < 10) { offsets.push_back(currentOffset); continue; }
////        std::sort(dxs.begin(), dxs.end());
////        std::sort(dys.begin(), dys.end());
////        float mx = dxs[dxs.size() / 2];
////        float my = dys[dys.size() / 2];
////        if (std::abs(my) < images[i].rows * 0.04f) my = 0;
////        currentOffset.x += mx; currentOffset.y += my;
////        offsets.push_back(currentOffset);
////
////        reportProgress(env, listener, "Aligned " + std::to_string(i + 1) + " / " + std::to_string(images.size()) + " frames");
////    }
////
////    reportProgress(env, listener, "Finalizing panorama...");
////
////    float minX = 0, minY = 0, maxX = images[0].cols, maxY = images[0].rows;
////    for (size_t i = 0; i < images.size(); ++i) {
////        minX = std::min(minX, offsets[i].x); minY = std::min(minY, offsets[i].y);
////        maxX = std::max(maxX, offsets[i].x + images[i].cols); maxY = std::max(maxY, offsets[i].y + images[i].rows);
////    }
////
////    int canvasW = cvRound(maxX - minX);
////    int canvasH = cvRound(maxY - minY);
////    if (canvasW > 45000 || canvasH > 12000) return nullptr;
////
////    std::vector<float> acc(canvasW * canvasH * 3, 0.0f);
////    std::vector<float> weightSum(canvasW * canvasH, 0.0f);
////
////    for (size_t i = 0; i < images.size(); ++i) {
////        int ox = cvRound(offsets[i].x - minX);
////        int oy = cvRound(offsets[i].y - minY);
////        Mat current = images[i];
////        Mat mask = masks[i];
////        for (int y = 0; y < current.rows; ++y) {
////            const Vec3b* cRow = current.ptr<Vec3b>(y);
////            const float* mRow = mask.ptr<float>(y);
////            int cy = oy + y;
////            if (cy < 0 || cy >= canvasH) continue;
////            for (int x = 0; x < current.cols; ++x) {
////                if (cRow[x] == Vec3b(0,0,0)) continue;
////                int cx = ox + x;
////                if (cx < 0 || cx >= canvasW) continue;
////                float w = mRow[x];
////                int aIdx = (cy * canvasW + cx) * 3;
////                acc[aIdx + 0] += cRow[x][0] * w;
////                acc[aIdx + 1] += cRow[x][1] * w;
////                acc[aIdx + 2] += cRow[x][2] * w;
////                weightSum[cy * canvasW + cx] += w;
////            }
////        }
////    }
////
////    Mat result = Mat::zeros(Size(canvasW, canvasH), CV_8UC3);
////    for (int y = 0; y < canvasH; ++y) {
////        Vec3b* rRow = result.ptr<Vec3b>(y);
////        for (int x = 0; x < canvasW; ++x) {
////            float ws = weightSum[y * canvasW + x];
////            if (ws > 0.001f) {
////                int idx = (y * canvasW + x) * 3;
////                rRow[x][0] = saturate_cast<uchar>(acc[idx + 0] / ws);
////                rRow[x][1] = saturate_cast<uchar>(acc[idx + 1] / ws);
////                rRow[x][2] = saturate_cast<uchar>(acc[idx + 2] / ws);
////            }
////        }
////    }
////
////    Mat sharpened;
////    GaussianBlur(result, sharpened, Size(0, 0), 2.5);
////    addWeighted(result, 1.8, sharpened, -0.8, 0, result);
////
////    Mat gray;
////    cvtColor(result, gray, COLOR_BGR2GRAY);
////    Rect crop = boundingRect(gray > 10);
////    if (crop.width > 0 && crop.height > 0) result = result(crop);
////
////    reportProgress(env, listener, "Encoding JPEG...");
//
//    std::vector<uchar> out;
//    imencode(".jpg", 1, out);
//    jbyteArray resArray = env->NewByteArray(out.size());
//    env->SetByteArrayRegion(resArray, 0, out.size(), (jbyte*)out.data());
//    Stitcher stitcher = Stitcher();
//    stitcher.stitch(InputArrayOfArrays(imageByteArrays), noArray(), OutputArray(resArray));
//    return resArray;
//}

#include <jni.h>
#include <vector>
#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/stitching.hpp>

using namespace cv;
using namespace std;

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_ovais_nativecore_NativeLib_stitchImages(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray imageByteArrays,
        jobject listener) {

    jsize count = env->GetArrayLength(imageByteArrays);
    if (count < 2) {
        return nullptr;  // need at least 2 images
    }

    reportProgress(env, listener, "Decoding " + std::to_string(count) + " images...");
    vector<Mat> images;
    images.reserve(count);

    // Decode each byte[] into cv::Mat
    for (jsize i = 0; i < count; i++) {
        jbyteArray byteArray =
                (jbyteArray) env->GetObjectArrayElement(imageByteArrays, i);

        jsize len = env->GetArrayLength(byteArray);
        jbyte* data = env->GetByteArrayElements(byteArray, nullptr);

        vector<uchar> buffer(data, data + len);
        env->ReleaseByteArrayElements(byteArray, data, JNI_ABORT);

        Mat img = imdecode(buffer, IMREAD_COLOR);
        if (img.empty()) {
            return nullptr;
        }

        images.push_back(img);
    }
    reportProgress(env, listener, "Matching features...\n" + std::to_string(count) + " images...");
    // Stitch panorama
    Ptr<Stitcher> stitcher = Stitcher::create(Stitcher::PANORAMA);
//    stitcher->setRegistrationResol(-1); /// 0.6
//    stitcher->setSeamEstimationResol(-1);   /// 0.1
//    stitcher->setCompositingResol(-1);   //1
//    stitcher->setPanoConfidenceThresh(0.6);   //1
//    stitcher->setWarper(new cv::SphericalWarper());
    stitcher->setWaveCorrection(true);
    stitcher->setWaveCorrectKind(detail::WAVE_CORRECT_AUTO);
    reportProgress(env, listener, "Finalizing panorama...\n" + std::to_string(count) + " images...");
    Mat pano;
    Stitcher::Status status = stitcher->stitch(images, pano);
    stitcher->setBlender(
            makePtr<detail::MultiBandBlender>(true, 5)
    );
    if (status != Stitcher::OK || pano.empty()) {
        return nullptr;
    }
    reportProgress(env, listener, "Generating result as JPEG...\n" + std::to_string(count) + " images...");
    // Encode panorama to JPEG
    vector<uchar> output;
    imencode(".jpg", pano, output);

    jbyteArray result = env->NewByteArray(output.size());
    env->SetByteArrayRegion(
            result,
            0,
            output.size(),
            reinterpret_cast<jbyte*>(output.data())
    );

    return result;
}

//JNIEXPORT jbyteArray JNICALL
//Java_com_ovais_nativecore_NativeLib_stitchImages(
//        JNIEnv* env,
//        jobject /* this */,
//        jobjectArray imageByteArrays,
//        jobject listener) {
//
//    jsize count = env->GetArrayLength(imageByteArrays);
//    if (count < 2) return nullptr;
//
//    vector<Mat> images;
//    images.reserve(count);
//
//    for (jsize i = 0; i < count; i++) {
//        jbyteArray byteArray =
//                (jbyteArray) env->GetObjectArrayElement(imageByteArrays, i);
//
//        jsize len = env->GetArrayLength(byteArray);
//        jbyte* data = env->GetByteArrayElements(byteArray, nullptr);
//
//        vector<uchar> buffer(data, data + len);
//        env->ReleaseByteArrayElements(byteArray, data, JNI_ABORT);
//
//        Mat img = imdecode(buffer, IMREAD_COLOR);
//        if (img.empty()) return nullptr;
//
//        images.push_back(img);
//    }
//
//    Ptr<Stitcher> stitcher = Stitcher::create(Stitcher::PANORAMA);
//
//    // ✅ Full-resolution stitching
//    stitcher->setRegistrationResol(-1);
//    stitcher->setSeamEstimationResol(-1);
//    stitcher->setCompositingResol(-1);
//
//
//    // 👇 These two matter when images are large
//    stitcher->setPanoConfidenceThresh(0.0);
//
//// Better seam/blend at high res
//    stitcher->setBlender(
//            makePtr<detail::MultiBandBlender>(false, 5)
//    );
//
//
//    stitcher->setWaveCorrection(true);
//    stitcher->setWaveCorrectKind(detail::WAVE_CORRECT_HORIZ);
//
//    Mat pano;
//    Stitcher::Status status = stitcher->stitch(images, pano);
//    if (status != Stitcher::OK || pano.empty()) return nullptr;
//
//    // ✅ Highest quality JPEG output
//    vector<uchar> output;
//    vector<int> params = { IMWRITE_JPEG_QUALITY, 100 };
//    imencode(".jpg", pano, output, params);
//
//    jbyteArray result = env->NewByteArray(output.size());
//    env->SetByteArrayRegion(
//            result, 0, output.size(),
//            reinterpret_cast<jbyte*>(output.data())
//    );
//
//    return result;
//}