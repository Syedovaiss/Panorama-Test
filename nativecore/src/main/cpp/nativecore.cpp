#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/calib3d.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/core/ocl.hpp>
#include <vector>
#include <android/log.h>

#define LOG_TAG "NativeCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace cv;

/**
 * Projects a flat image onto a sphere.
 * This is the "Secret Sauce" that makes panoramas straight and professional.
 */
Mat sphericalWarp(const Mat& src, float f) {
    Mat map_x(src.size(), CV_32F);
    Mat map_y(src.size(), CV_32F);
    float centerX = src.cols / 2.0f;
    float centerY = src.rows / 2.0f;

    for (int y = 0; y < src.rows; y++) {
        float* mx = map_x.ptr<float>(y);
        float* my = map_y.ptr<float>(y);
        for (int x = 0; x < src.cols; x++) {
            float theta = (x - centerX) / f;
            float h = (y - centerY) / f;
            mx[x] = f * sin(theta) + centerX;
            my[x] = f * h * cos(theta) + centerY;
        }
    }
    Mat dst;
    remap(src, dst, map_x, map_y, INTER_LINEAR, BORDER_CONSTANT);
    return dst;
}

/**
 * Robustly stitches two images using translation and soft alpha blending.
 */
Mat stitchTwoImages(const Mat& img_new, const Mat& img_pano) {
    Ptr<ORB> orb = ORB::create(2000);
    std::vector<KeyPoint> kps1, kps2;
    Mat desc1, desc2;

    orb->detectAndCompute(img_new, noArray(), kps1, desc1);
    orb->detectAndCompute(img_pano, noArray(), kps2, desc2);

    if (desc1.empty() || desc2.empty()) return Mat();

    BFMatcher matcher(NORM_HAMMING);
    std::vector<DMatch> matches;
    matcher.match(desc1, desc2, matches);

    if (matches.size() < 10) return Mat();

    // Use median shift to ignore shaky movement and outliers
    std::vector<float> dx, dy;
    for (auto& m : matches) {
        dx.push_back(kps2[m.trainIdx].pt.x - kps1[m.queryIdx].pt.x);
        dy.push_back(kps2[m.trainIdx].pt.y - kps1[m.queryIdx].pt.y);
    }
    std::sort(dx.begin(), dx.end());
    std::sort(dy.begin(), dy.end());
    float shiftX = dx[dx.size() / 2];
    float shiftY = dy[dy.size() / 2];

    // Calculate new canvas size and offsets
    float min_x = std::min(0.0f, shiftX);
    float min_y = std::min(0.0f, shiftY);
    float max_x = std::max((float)img_pano.cols, shiftX + (float)img_new.cols);
    float max_y = std::max((float)img_pano.rows, shiftY + (float)img_new.rows);

    int canvas_w = cvRound(max_x - min_x);
    int canvas_h = cvRound(max_y - min_y);

    // Safety check for mobile memory
    if (canvas_w > 15000 || canvas_h > 4000) return Mat();

    Mat result = Mat::zeros(Size(canvas_w, canvas_h), CV_8UC3);

    // Place Pano
    Rect panoRect(cvRound(-min_x), cvRound(-min_y), img_pano.cols, img_pano.rows);
    img_pano.copyTo(result(panoRect));

    // Place New Image with Alpha Blending to remove black cuts
    Rect imgRect(cvRound(shiftX - min_x), cvRound(shiftY - min_y), img_new.cols, img_new.rows);
    Mat target = result(imgRect);

    int blendWidth = 100; // Large 100px feather for high quality
    for (int y = 0; y < img_new.rows; ++y) {
        const Vec3b* srcRow = img_new.ptr<Vec3b>(y);
        Vec3b* dstRow = target.ptr<Vec3b>(y);
        for (int x = 0; x < img_new.cols; ++x) {
            if (srcRow[x] == Vec3b(0,0,0)) continue; // Skip warped background

            if (dstRow[x] == Vec3b(0,0,0)) {
                dstRow[x] = srcRow[x];
            } else {
                // Linear blend in overlap zone
                float alpha = 1.0f;
                if (x < blendWidth) alpha = (float)x / blendWidth;
                else if (x > img_new.cols - blendWidth) alpha = (float)(img_new.cols - x) / blendWidth;

                dstRow[x] = srcRow[x] * alpha + dstRow[x] * (1.0f - alpha);
            }
        }
    }

    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_ovais_nativecore_NativeLib_stitchImages(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray imageByteArrays) {

    // Disable all features that trigger the 'glob' error in base.apk
    cv::ocl::setUseOpenCL(false);
    cv::setUseOptimized(false);
    cv::setNumThreads(1);

    int numImages = env->GetArrayLength(imageByteArrays);
    if (numImages < 2) return nullptr;

    std::vector<Mat> images;
    for (int i = 0; i < numImages; ++i) {
        jbyteArray byteArray = (jbyteArray) env->GetObjectArrayElement(imageByteArrays, i);
        jsize len = env->GetArrayLength(byteArray);
        std::vector<uchar> buffer(len);
        env->GetByteArrayRegion(byteArray, 0, len, (jbyte*)buffer.data());
        Mat img = imdecode(buffer, IMREAD_COLOR);
        if (!img.empty()) {
            if (img.cols > 1080) {
                double scale = 1080.0 / img.cols;
                resize(img, img, Size(), scale, scale, INTER_AREA);
            }
            images.push_back(sphericalWarp(img, (float)img.cols * 1.0f));
        }
        env->DeleteLocalRef(byteArray);
    }

    if (images.size() < 2) return nullptr;

    Mat pano = images[0];
    for (size_t i = 1; i < images.size(); ++i) {
        LOGI("Stitching Frame %zu...", i);
        Mat next = stitchTwoImages(images[i], pano);
        if (!next.empty()) pano = next;
    }

    // Final crop to content
    Mat gray;
    cvtColor(pano, gray, COLOR_BGR2GRAY);
    Rect crop = boundingRect(gray > 0);
    if (crop.width > 0) pano = pano(crop);

    std::vector<uchar> buf;
    imencode(".jpg", pano, buf);
    jbyteArray res = env->NewByteArray(buf.size());
    env->SetByteArrayRegion(res, 0, buf.size(), (jbyte*)buf.data());
    return res;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovais_nativecore_NativeLib_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("Professional Spherical Stitcher V7");
}
