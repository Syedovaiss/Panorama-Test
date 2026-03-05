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
 * High-Quality Cylindrical Warp.
 */
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

/**
 * Calculates brightness gain between two images safely.
 */
float calculateGainSafe(const Mat& src, const Mat& target, const Rect& src_roi, const Rect& target_roi) {
    if (src_roi.x < 0 || src_roi.y < 0 || src_roi.x + src_roi.width > src.cols || src_roi.y + src_roi.height > src.rows) return 1.0f;
    if (target_roi.x < 0 || target_roi.y < 0 || target_roi.x + target_roi.width > target.cols || target_roi.y + target_roi.height > target.rows) return 1.0f;
    if (src_roi.area() < 200) return 1.0f;

    Scalar s_mean = mean(src(src_roi));
    Scalar t_mean = mean(target(target_roi));
    float s_lum = (float)(s_mean[0] + s_mean[1] + s_mean[2]) / 3.0f;
    float t_lum = (float)(t_mean[0] + t_mean[1] + t_mean[2]) / 3.0f;

    if (s_lum < 5.0f || t_lum < 5.0f) return 1.0f;

    // Nudge gain towards 1.0 to prevent exposure runaway
    float gain = t_lum / s_lum;
    return 0.8f * gain + 0.2f * 1.0f;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_ovais_nativecore_NativeLib_stitchImages(
        JNIEnv* env, jobject, jobjectArray imageByteArrays) {

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
            if (img.cols > 1600) {
                double scale = 1600.0 / img.cols;
                resize(img, img, Size(), scale, scale, INTER_CUBIC);
            }
            if (focalLength == 0) focalLength = (float)img.cols * 3.0f;
            images.push_back(cylindricalWarp(img, focalLength));
        }
        env->DeleteLocalRef(byteArray);
    }

    if (images.size() < 2) return nullptr;

    // 1. Precise Alignment
    std::vector<Point2f> cumulativeOffsets;
    cumulativeOffsets.push_back(Point2f(0, 0));
    Ptr<ORB> orb = ORB::create(4000);
    BFMatcher matcher(NORM_HAMMING, true);
    Point2f currentOffset(0, 0);

    for (size_t i = 1; i < images.size(); ++i) {
        std::vector<KeyPoint> kps1, kps2;
        Mat desc1, desc2;
        orb->detectAndCompute(images[i], noArray(), kps1, desc1);
        orb->detectAndCompute(images[i-1], noArray(), kps2, desc2);
        if (desc1.empty() || desc2.empty()) { cumulativeOffsets.push_back(currentOffset); continue; }
        std::vector<DMatch> matches;
        matcher.match(desc1, desc2, matches);
        std::vector<float> dxs, dys;
        for (auto& m : matches) {
            if (m.distance < 35.0f) {
                dxs.push_back(kps2[m.trainIdx].pt.x - kps1[m.queryIdx].pt.x);
                dys.push_back(kps2[m.trainIdx].pt.y - kps1[m.queryIdx].pt.y);
            }
        }
        if (dxs.size() < 10) { cumulativeOffsets.push_back(currentOffset); continue; }
        std::sort(dxs.begin(), dxs.end());
        std::sort(dys.begin(), dys.end());
        float mx = dxs[dxs.size() / 2];
        float my = dys[dys.size() / 2];
        if (std::abs(my) < images[i].rows * 0.05f) my = 0;
        currentOffset.x += mx; currentOffset.y += my;
        cumulativeOffsets.push_back(currentOffset);
    }

    float minX = 0, minY = 0, maxX = images[0].cols, maxY = images[0].rows;
    for (size_t i = 0; i < images.size(); ++i) {
        minX = std::min(minX, cumulativeOffsets[i].x); minY = std::min(minY, cumulativeOffsets[i].y);
        maxX = std::max(maxX, cumulativeOffsets[i].x + images[i].cols); maxY = std::max(maxY, cumulativeOffsets[i].y + images[i].rows);
    }

    int canvasW = cvRound(maxX - minX);
    int canvasH = cvRound(maxY - minY);
    if (canvasW > 45000 || canvasH > 12000) return nullptr;

    Mat result = Mat::zeros(Size(canvasW, canvasH), CV_8UC3);
    Mat weightSum = Mat::zeros(result.size(), CV_32F);

    // 2. Natural Blending Accumulation
    for (size_t i = 0; i < images.size(); ++i) {
        int ox = cvRound(cumulativeOffsets[i].x - minX);
        int oy = cvRound(cumulativeOffsets[i].y - minY);
        Mat current = images[i].clone();

        if (i > 0) {
            Rect curRect(ox, oy, current.cols, current.rows);
            Rect canvasRect(0, 0, canvasW, canvasH);
            Rect overlap = curRect & canvasRect;
            if (overlap.area() > 200) {
                Rect localOverlap(overlap.x - ox, overlap.y - oy, overlap.width, overlap.height);
                float gain = calculateGainSafe(current, result, localOverlap, overlap);
                current.convertTo(current, -1, gain);
            }
        }

        int feather = current.cols / 8;
        for (int y = 0; y < current.rows; ++y) {
            const Vec3b* cRow = current.ptr<Vec3b>(y);
            int cy = oy + y;
            if (cy < 0 || cy >= canvasH) continue;
            Vec3b* rRow = result.ptr<Vec3b>(cy);
            float* wRow = weightSum.ptr<float>(cy);

            for (int x = 0; x < current.cols; ++x) {
                if (cRow[x] == Vec3b(0,0,0)) continue;
                int cx = ox + x;
                if (cx < 0 || cx >= canvasW) continue;

                // Smooth linear feathering
                float w = 1.0f;
                if (x < feather) w = (float)x / feather;
                else if (x > current.cols - feather) w = (float)(current.cols - x) / feather;

                if (wRow[cx] == 0) {
                    rRow[cx] = cRow[x];
                    wRow[cx] = w;
                } else {
                    // Smooth Weighted Blend (prevents oil paint edges)
                    float totalW = wRow[cx] + w;
                    rRow[cx][0] = (uchar)((rRow[cx][0] * wRow[cx] + cRow[x][0] * w) / totalW);
                    rRow[cx][1] = (uchar)((rRow[cx][1] * wRow[cx] + cRow[x][1] * w) / totalW);
                    rRow[cx][2] = (uchar)((rRow[cx][2] * wRow[cx] + cRow[x][2] * w) / totalW);
                    wRow[cx] = std::max(wRow[cx], w);
                }
            }
        }
    }

    // 3. Natural Polish
    Mat sharpened;
    GaussianBlur(result, sharpened, Size(0, 0), 1.5);
    addWeighted(result, 1.3, sharpened, -0.3, 0, result);

    Mat gray;
    cvtColor(result, gray, COLOR_BGR2GRAY);
    Rect crop = boundingRect(gray > 20);
    if (crop.width > 0 && crop.height > 0) {
        int vTrim = crop.height * 0.08;
        crop.y += vTrim;
        crop.height -= 2 * vTrim;
        if (crop.height > 0) result = result(crop);
    }

    std::vector<uchar> outBuffer;
    imencode(".jpg", result, outBuffer);
    jbyteArray resArray = env->NewByteArray(outBuffer.size());
    env->SetByteArrayRegion(resArray, 0, outBuffer.size(), (jbyte*)outBuffer.data());
    return resArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovais_nativecore_NativeLib_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("Professional Natural-HDR V35");
}
