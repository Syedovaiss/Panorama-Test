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

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_ovais_nativecore_NativeLib_stitchImages(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray imageByteArrays) {

    // 1. Force safety to prevent SIGILL crashes
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
            // Processing at 800px for high feature density and stability
            if (img.cols > 800) {
                double scale = 800.0 / img.cols;
                resize(img, img, Size(), scale, scale, INTER_LINEAR);
            }
            images.push_back(img);
        }
        env->DeleteLocalRef(byteArray);
    }

    if (images.size() < 2) return nullptr;

    // 2. Alignment Pipeline with Vertical Drift Constraint
    std::vector<Point2f> cumulativeOffsets;
    cumulativeOffsets.push_back(Point2f(0, 0));

    Ptr<ORB> orb = ORB::create(2500);
    BFMatcher matcher(NORM_HAMMING, true); // crossCheck=true for reliable matches
    Point2f currentOffset(0, 0);

    for (size_t i = 1; i < images.size(); ++i) {
        std::vector<KeyPoint> kps1, kps2;
        Mat desc1, desc2;
        orb->detectAndCompute(images[i], noArray(), kps1, desc1);
        orb->detectAndCompute(images[i-1], noArray(), kps2, desc2);

        if (desc1.empty() || desc2.empty()) {
            cumulativeOffsets.push_back(currentOffset);
            continue;
        }

        std::vector<DMatch> matches;
        matcher.match(desc1, desc2, matches);

        std::vector<float> dxs, dys;
        for (auto& m : matches) {
            dxs.push_back(kps2[m.trainIdx].pt.x - kps1[m.queryIdx].pt.x);
            dys.push_back(kps2[m.trainIdx].pt.y - kps1[m.queryIdx].pt.y);
        }

        if (dxs.size() < 10) {
            cumulativeOffsets.push_back(currentOffset);
            continue;
        }

        // Robust median shift calculation
        std::sort(dxs.begin(), dxs.end());
        std::sort(dys.begin(), dys.end());
        float mx = dxs[dxs.size() / 2];
        float my = dys[dys.size() / 2];

        // DAMPING: If vertical shift is small, force it to 0 to keep the panorama level
        if (std::abs(my) < images[i].rows * 0.05f) my = 0;

        currentOffset.x += mx;
        currentOffset.y += my;
        cumulativeOffsets.push_back(currentOffset);
    }

    // 3. Determine Canvas and Offsets
    float minX = 0, minY = 0, maxX = images[0].cols, maxY = images[0].rows;
    for (size_t i = 0; i < images.size(); ++i) {
        minX = std::min(minX, cumulativeOffsets[i].x);
        minY = std::min(minY, cumulativeOffsets[i].y);
        maxX = std::max(maxX, cumulativeOffsets[i].x + images[i].cols);
        maxY = std::max(maxY, cumulativeOffsets[i].y + images[i].rows);
    }

    int canvasW = cvRound(maxX - minX);
    int canvasH = cvRound(maxY - minY);
    if (canvasW > 25000 || canvasH > 5000) return nullptr;

    // Use manual pixel accumulation to avoid convertTo/SIGILL crashes
    Mat result = Mat::zeros(Size(canvasW, canvasH), CV_8UC3);
    Mat weightSum = Mat::zeros(Size(canvasW, canvasH), CV_32F);

    // Accumulator canvas in floating point
    std::vector<float> acc(canvasW * canvasH * 3, 0.0f);
    std::vector<float> weights(canvasW * canvasH, 0.0f);

    for (size_t i = 0; i < images.size(); ++i) {
        int startX = cvRound(cumulativeOffsets[i].x - minX);
        int startY = cvRound(cumulativeOffsets[i].y - minY);

        int imgW = images[i].cols;
        int imgH = images[i].rows;
        int feather = imgW / 10; // 10% side feathering

        for (int y = 0; y < imgH; ++y) {
            const Vec3b* row = images[i].ptr<Vec3b>(y);
            int canvasY = startY + y;
            if (canvasY < 0 || canvasY >= canvasH) continue;

            for (int x = 0; x < imgW; ++x) {
                int canvasX = startX + x;
                if (canvasX < 0 || canvasX >= canvasW) continue;

                // Calculate horizontal-only linear feathering weight
                float w = 1.0f;
                if (x < feather) w = (float)x / feather;
                else if (x > imgW - feather) w = (float)(imgW - x) / feather;

                // Manual accumulation (no OpenCV optimized calls)
                int accIdx = (canvasY * canvasW + canvasX) * 3;
                int weightIdx = canvasY * canvasW + canvasX;

                acc[accIdx + 0] += row[x][0] * w;
                acc[accIdx + 1] += row[x][1] * w;
                acc[accIdx + 2] += row[x][2] * w;
                weights[weightIdx] += w;
            }
        }
    }

    // 4. Final Normalization and Clean Crop
    for (int y = 0; y < canvasH; ++y) {
        Vec3b* row = result.ptr<Vec3b>(y);
        for (int x = 0; x < canvasW; ++x) {
            float w = weights[y * canvasW + x];
            if (w > 0.001f) {
                int idx = (y * canvasW + x) * 3;
                row[x][0] = (uchar)(acc[idx + 0] / w);
                row[x][1] = (uchar)(acc[idx + 1] / w);
                row[x][2] = (uchar)(acc[idx + 2] / w);
            }
        }
    }

    // Auto-crop black borders
    Mat gray;
    cvtColor(result, gray, COLOR_BGR2GRAY);
    Rect contentRect = boundingRect(gray > 0);
    if (contentRect.width > 0 && contentRect.height > 0) {
        result = result(contentRect);
    }

    std::vector<uchar> outBuffer;
    imencode(".jpg", result, outBuffer);
    jbyteArray resArray = env->NewByteArray(outBuffer.size());
    env->SetByteArrayRegion(resArray, 0, outBuffer.size(), (jbyte*)outBuffer.data());
    return resArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovais_nativecore_NativeLib_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("Professional Stable Stitcher V12");
}
