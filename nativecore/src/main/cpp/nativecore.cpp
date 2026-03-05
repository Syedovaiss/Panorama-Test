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
 * Enhanced Cylindrical Warping.
 * Uses INTER_CUBIC for maximum sharpness ("Bust Quality").
 */
Mat cylindricalWarp(const Mat& src, float f) {
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
            mx[x] = f * tan(theta) + centerX;
            my[x] = f * h / cos(theta) + centerY;
        }
    }
    Mat dst;
    remap(src, dst, map_x, map_y, INTER_CUBIC, BORDER_CONSTANT);
    return dst;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_ovais_nativecore_NativeLib_stitchImages(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray imageByteArrays) {

    // 1. Forced Safety & Stability
    cv::ocl::setUseOpenCL(false);
    cv::setUseOptimized(false);
    cv::setNumThreads(1);

    int numImages = env->GetArrayLength(imageByteArrays);
    if (numImages < 2) return nullptr;

    std::vector<Mat> images;
    float focalLength = 0;

    for (int i = 0; i < numImages; ++i) {
        jbyteArray byteArray = (jbyteArray) env->GetObjectArrayElement(imageByteArrays, i);
        jsize len = env->GetArrayLength(byteArray);
        std::vector<uchar> buffer(len);
        env->GetByteArrayRegion(byteArray, 0, len, (jbyte*)buffer.data());
        Mat img = imdecode(buffer, IMREAD_COLOR);
        if (!img.empty()) {
            // Resolution at 1200px for high-end detail
            if (img.cols > 1200) {
                double scale = 1200.0 / img.cols;
                resize(img, img, Size(), scale, scale, INTER_CUBIC);
            }
            if (focalLength == 0) focalLength = img.cols * 1.3f; // Slightly longer focal length for less curve

            // Sharpen each frame slightly BEFORE alignment to help ORB
            Mat sharp;
            GaussianBlur(img, sharp, Size(0, 0), 3);
            addWeighted(img, 1.3, sharp, -0.3, 0, img);

            images.push_back(cylindricalWarp(img, focalLength));
        }
        env->DeleteLocalRef(byteArray);
    }

    if (images.size() < 2) return nullptr;

    // 2. Precise Sequential Alignment
    std::vector<Point2f> offsets;
    offsets.push_back(Point2f(0, 0));

    Ptr<ORB> orb = ORB::create(5000); // 5000 features for extreme precision
    BFMatcher matcher(NORM_HAMMING, true);
    Point2f currentOffset(0, 0);

    for (size_t i = 1; i < images.size(); ++i) {
        std::vector<KeyPoint> kps1, kps2;
        Mat desc1, desc2;
        orb->detectAndCompute(images[i], noArray(), kps1, desc1);
        orb->detectAndCompute(images[i-1], noArray(), kps2, desc2);

        if (desc1.empty() || desc2.empty()) {
            offsets.push_back(currentOffset);
            continue;
        }

        std::vector<DMatch> matches;
        matcher.match(desc1, desc2, matches);

        std::vector<float> dxs, dys;
        for (auto& m : matches) {
            // Stronger distance filtering
            if (m.distance < 45.0f) {
                dxs.push_back(kps2[m.trainIdx].pt.x - kps1[m.queryIdx].pt.x);
                dys.push_back(kps2[m.trainIdx].pt.y - kps1[m.queryIdx].pt.y);
            }
        }

        if (dxs.size() < 20) {
            offsets.push_back(currentOffset);
            continue;
        }

        std::sort(dxs.begin(), dxs.end());
        std::sort(dys.begin(), dys.end());

        currentOffset.x += dxs[dxs.size() / 2];
        currentOffset.y += dys[dys.size() / 2];
        offsets.push_back(currentOffset);
    }

    // 3. Dynamic Canvas Calculation
    float minX = 0, minY = 0, maxX = images[0].cols, maxY = images[0].rows;
    for (size_t i = 0; i < images.size(); ++i) {
        minX = std::min(minX, offsets[i].x);
        minY = std::min(minY, offsets[i].y);
        maxX = std::max(maxX, offsets[i].x + images[i].cols);
        maxY = std::max(maxY, offsets[i].y + images[i].rows);
    }

    int canvasW = cvRound(maxX - minX);
    int canvasH = cvRound(maxY - minY);
    if (canvasW > 30000 || canvasH > 8000) return nullptr;

    // 4. Center-Weighted Gaussian Blending
    // This removes the "squared" look and ghosting by giving
    // higher priority to the center of each image.
    std::vector<float> acc(canvasW * canvasH * 3, 0.0f);
    std::vector<float> weightSum(canvasW * canvasH, 0.0f);

    for (size_t i = 0; i < images.size(); ++i) {
        int ox = cvRound(offsets[i].x - minX);
        int oy = cvRound(offsets[i].y - minY);
        int wi = images[i].cols;
        int hi = images[i].rows;

        for (int y = 0; y < hi; ++y) {
            const Vec3b* iRow = images[i].ptr<Vec3b>(y);
            int cy = oy + y;
            if (cy < 0 || cy >= canvasH) continue;

            for (int x = 0; x < wi; ++x) {
                if (iRow[x] == Vec3b(0,0,0)) continue;

                int cx = ox + x;
                if (cx < 0 || cx >= canvasW) continue;

                // Center-Weighted Weighting (Gaussian approximation)
                float dx = (float)(x - wi / 2) / (wi / 2.0f);
                float dy = (float)(y - hi / 2) / (hi / 2.0f);
                float w = 1.0f - (dx * dx + dy * dy);
                w = std::max(0.01f, w * w); // w^2 for sharper center focus

                int aIdx = (cy * canvasW + cx) * 3;
                int wIdx = cy * canvasW + cx;
                acc[aIdx + 0] += iRow[x][0] * w;
                acc[aIdx + 1] += iRow[x][1] * w;
                acc[aIdx + 2] += iRow[x][2] * w;
                weightSum[wIdx] += w;
            }
        }
    }

    // 5. Final Sharpening and Clean Crop
    Mat result = Mat::zeros(Size(canvasW, canvasH), CV_8UC3);
    for (int y = 0; y < canvasH; ++y) {
        Vec3b* rRow = result.ptr<Vec3b>(y);
        for (int x = 0; x < canvasW; ++x) {
            float ws = weightSum[y * canvasW + x];
            if (ws > 0) {
                int idx = (y * canvasW + x) * 3;
                rRow[x][0] = saturate_cast<uchar>(acc[idx + 0] / ws);
                rRow[x][1] = saturate_cast<uchar>(acc[idx + 1] / ws);
                rRow[x][2] = saturate_cast<uchar>(acc[idx + 2] / ws);
            }
        }
    }

    // Final clarity boost
    Mat finalSharp;
    GaussianBlur(result, finalSharp, Size(0, 0), 2);
    addWeighted(result, 1.6, finalSharp, -0.6, 0, result);

    // Strict content crop
    Mat gray;
    cvtColor(result, gray, COLOR_BGR2GRAY);
    Rect content = boundingRect(gray > 5); // Threshold to ignore dark compression noise
    if (content.width > 0 && content.height > 0) result = result(content);

    std::vector<uchar> out;
    imencode(".jpg", result, out);
    jbyteArray resArray = env->NewByteArray(out.size());
    env->SetByteArrayRegion(resArray, 0, out.size(), (jbyte*)out.data());
    return resArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovais_nativecore_NativeLib_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("Professional Clarity Stitcher V14");
}
