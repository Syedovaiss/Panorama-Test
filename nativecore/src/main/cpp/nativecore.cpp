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
 * Enhanced Cylindrical Warp.
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

/**
 * Generates a Crisp Feather Mask.
 * Uses Quadratic weighting to give more priority to the sharp center pixels.
 */
Mat createCrispMask(Size sz) {
    Mat mask = Mat::zeros(sz, CV_32F);
    // Tighter 5% feathering reduces ghosting blur
    int featherX = sz.width / 20;
    int featherY = sz.height / 20;

    for (int y = 0; y < sz.height; ++y) {
        float* row = mask.ptr<float>(y);
        for (int x = 0; x < sz.width; ++x) {
            float wx = std::min(x, sz.width - 1 - x) / (float)featherX;
            float wy = std::min(y, sz.height - 1 - y) / (float)featherY;
            float w = std::max(0.0f, std::min(1.0f, std::min(wx, wy)));
            // Quadratic ramp for sharper transitions
            row[x] = w * w;
        }
    }
    return mask;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_ovais_nativecore_NativeLib_stitchImages(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray imageByteArrays) {

    cv::ocl::setUseOpenCL(false);
    cv::setUseOptimized(false);
    cv::setNumThreads(1);

    int numImages = env->GetArrayLength(imageByteArrays);
    if (numImages < 2) return nullptr;

    std::vector<Mat> images;
    std::vector<Mat> masks;
    float focalLength = 0;

    for (int i = 0; i < numImages; ++i) {
        jbyteArray byteArray = (jbyteArray) env->GetObjectArrayElement(imageByteArrays, i);
        jsize len = env->GetArrayLength(byteArray);
        std::vector<uchar> buffer(len);
        env->GetByteArrayRegion(byteArray, 0, len, (jbyte*)buffer.data());
        Mat img = imdecode(buffer, IMREAD_COLOR);
        if (!img.empty()) {
            // Processing at 1280px for crisp details
            if (img.cols > 1280) {
                double scale = 1280.0 / img.cols;
                resize(img, img, Size(), scale, scale, INTER_AREA);
            }

            // Subtle sharpen frame to help alignment
            Mat sharpened;
            GaussianBlur(img, sharpened, Size(0, 0), 3);
            addWeighted(img, 1.2, sharpened, -0.2, 0, img);

            if (focalLength == 0) focalLength = img.cols * 1.5f;
            Mat warped = cylindricalWarp(img, focalLength);
            images.push_back(warped);
            masks.push_back(createCrispMask(warped.size()));
        }
        env->DeleteLocalRef(byteArray);
    }

    if (images.size() < 2) return nullptr;

    // 1. Precise Alignment
    std::vector<Point2f> offsets;
    offsets.push_back(Point2f(0, 0));
    Ptr<ORB> orb = ORB::create(4000);
    BFMatcher matcher(NORM_HAMMING, true);
    Point2f currentOffset(0, 0);

    for (size_t i = 1; i < images.size(); ++i) {
        std::vector<KeyPoint> kps1, kps2;
        Mat desc1, desc2;
        orb->detectAndCompute(images[i], noArray(), kps1, desc1);
        orb->detectAndCompute(images[i-1], noArray(), kps2, desc2);

        if (desc1.empty() || desc2.empty()) {
            offsets.push_back(currentOffset); continue;
        }

        std::vector<DMatch> matches;
        matcher.match(desc1, desc2, matches);

        std::vector<float> dxs, dys;
        for (auto& m : matches) {
            dxs.push_back(kps2[m.trainIdx].pt.x - kps1[m.queryIdx].pt.x);
            dys.push_back(kps2[m.trainIdx].pt.y - kps1[m.queryIdx].pt.y);
        }

        if (dxs.size() < 10) {
            offsets.push_back(currentOffset); continue;
        }

        std::sort(dxs.begin(), dxs.end());
        std::sort(dys.begin(), dys.end());

        float mx = dxs[dxs.size() / 2];
        float my = dys[dys.size() / 2];
        if (std::abs(my) < images[i].rows * 0.03f) my = 0;

        currentOffset.x += mx;
        currentOffset.y += my;
        offsets.push_back(currentOffset);
    }

    // 2. Determine Canvas Size
    float minX = 0, minY = 0, maxX = images[0].cols, maxY = images[0].rows;
    for (size_t i = 0; i < images.size(); ++i) {
        minX = std::min(minX, offsets[i].x);
        minY = std::min(minY, offsets[i].y);
        maxX = std::max(maxX, offsets[i].x + images[i].cols);
        maxY = std::max(maxY, offsets[i].y + images[i].rows);
    }

    int canvasW = cvRound(maxX - minX);
    int canvasH = cvRound(maxY - minY);
    if (canvasW > 25000 || canvasH > 5000) return nullptr;

    // 3. High-Quality Weighted Blending
    std::vector<float> acc(canvasW * canvasH * 3, 0.0f);
    std::vector<float> weightSum(canvasW * canvasH, 0.0f);

    for (size_t i = 0; i < images.size(); ++i) {
        int ox = cvRound(offsets[i].x - minX);
        int oy = cvRound(offsets[i].y - minY);
        Mat current = images[i];
        Mat mask = masks[i];

        for (int y = 0; y < current.rows; ++y) {
            const Vec3b* cRow = current.ptr<Vec3b>(y);
            const float* mRow = mask.ptr<float>(y);
            int cy = oy + y;
            if (cy < 0 || cy >= canvasH) continue;

            for (int x = 0; x < current.cols; ++x) {
                if (cRow[x] == Vec3b(0,0,0)) continue;
                int cx = ox + x;
                if (cx < 0 || cx >= canvasW) continue;

                float w = mRow[x];
                int aIdx = (cy * canvasW + cx) * 3;
                int wIdx = cy * canvasW + cx;
                acc[aIdx+0] += cRow[x][0] * w;
                acc[aIdx+1] += cRow[x][1] * w;
                acc[aIdx+2] += cRow[x][2] * w;
                weightSum[wIdx] += w;
            }
        }
    }

    // 4. Final Polish
    Mat result = Mat::zeros(Size(canvasW, canvasH), CV_8UC3);
    for (int y = 0; y < canvasH; ++y) {
        Vec3b* rRow = result.ptr<Vec3b>(y);
        for (int x = 0; x < canvasW; ++x) {
            float ws = weightSum[y * canvasW + x];
            if (ws > 0.001f) {
                int idx = (y * canvasW + x) * 3;
                rRow[x][0] = saturate_cast<uchar>(acc[idx+0] / ws);
                rRow[x][1] = saturate_cast<uchar>(acc[idx+1] / ws);
                rRow[x][2] = saturate_cast<uchar>(acc[idx+2] / ws);
            }
        }
    }

    // Unsharp mask pass for clarity
    Mat finalSharpened;
    GaussianBlur(result, finalSharpened, Size(0, 0), 3);
    addWeighted(result, 1.5, finalSharpened, -0.5, 0, result);

    Mat gray;
    cvtColor(result, gray, COLOR_BGR2GRAY);
    Rect crop = boundingRect(gray > 15);
    if (crop.width > 0 && crop.height > 0) result = result(crop);

    std::vector<uchar> out;
    imencode(".jpg", result, out);
    jbyteArray resArray = env->NewByteArray(out.size());
    env->SetByteArrayRegion(resArray, 0, out.size(), (jbyte*)out.data());
    return resArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovais_nativecore_NativeLib_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("Professional Crisp V18");
}
