#include <jni.h>
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
 * Cylindrical Warp
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

    remap(
            src,
            dst,
            map_x,
            map_y,
            INTER_LANCZOS4,     // sharper than cubic
            BORDER_CONSTANT
    );

    return dst;
}

/**
 * Horizontal feather mask
 */
Mat createFeatherMask(Size sz) {

    Mat mask = Mat::zeros(sz, CV_32F);
    int featherX = sz.width / 20;

    for (int y = 0; y < sz.height; ++y) {

        float* row = mask.ptr<float>(y);

        for (int x = 0; x < sz.width; ++x) {

            float wx = std::min(x, sz.width - 1 - x) / (float)featherX;

            float w = std::max(0.0f, std::min(1.0f, wx));

            row[x] = pow(w, 10);
        }
    }

    return mask;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_ovais_nativecore_NativeLib_stitchImages(
        JNIEnv* env,
        jobject,
        jobjectArray imageByteArrays) {

    cv::ocl::setUseOpenCL(false);
    cv::setUseOptimized(false);
    cv::setNumThreads(1);

    int numImages = env->GetArrayLength(imageByteArrays);

    if (numImages < 2) return nullptr;

    std::vector<Mat> images;
    std::vector<Mat> masks;

    float focalLength = 0;

    /**
     * Load + preprocess images
     */
    for (int i = 0; i < numImages; ++i) {

        jbyteArray byteArray =
                (jbyteArray) env->GetObjectArrayElement(imageByteArrays, i);

        jsize len = env->GetArrayLength(byteArray);

        std::vector<uchar> buffer(len);

        env->GetByteArrayRegion(byteArray, 0, len, (jbyte*)buffer.data());

        Mat img = imdecode(buffer, IMREAD_COLOR);

        if (!img.empty()) {

            if (img.cols > 2000) {

                double scale = 2000.0 / img.cols;

                resize(img, img, Size(), scale, scale, INTER_LANCZOS4);
            }

            /**
             * CLAHE contrast improvement
             */
            Mat lab;

            cvtColor(img, lab, COLOR_BGR2Lab);

            std::vector<Mat> lab_planes(3);

            split(lab, lab_planes);

            Ptr<CLAHE> clahe = createCLAHE(3.0, Size(8,8));

            clahe->apply(lab_planes[0], lab_planes[0]);

            merge(lab_planes, lab);

            cvtColor(lab, img, COLOR_Lab2BGR);

            if (focalLength == 0)
                focalLength = img.cols * 1.5f;

            Mat warped = cylindricalWarp(img, focalLength);

            images.push_back(warped);

            masks.push_back(createFeatherMask(warped.size()));
        }

        env->DeleteLocalRef(byteArray);
    }

    if (images.size() < 2) return nullptr;

    /**
     * Alignment
     */
    std::vector<Point2f> offsets;

    offsets.push_back(Point2f(0,0));

    Ptr<ORB> orb = ORB::create(4000);

    BFMatcher matcher(NORM_HAMMING, true);

    Point2f currentOffset(0,0);

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

            dxs.push_back(
                    kps2[m.trainIdx].pt.x -
                            kps1[m.queryIdx].pt.x);

            dys.push_back(
                    kps2[m.trainIdx].pt.y -
                            kps1[m.queryIdx].pt.y);
        }

        if (dxs.size() < 10) {

            offsets.push_back(currentOffset);

            continue;
        }

        std::sort(dxs.begin(), dxs.end());
        std::sort(dys.begin(), dys.end());

        float mx = dxs[dxs.size()/2];
        float my = dys[dys.size()/2];

        if (std::abs(my) < images[i].rows * 0.04f)
            my = 0;

        currentOffset.x += mx;
        currentOffset.y += my;

        offsets.push_back(currentOffset);
    }

    /**
     * Canvas size
     */
    float minX = 0, minY = 0;
    float maxX = images[0].cols;
    float maxY = images[0].rows;

    for (size_t i = 0; i < images.size(); ++i) {

        minX = std::min(minX, offsets[i].x);
        minY = std::min(minY, offsets[i].y);

        maxX = std::max(maxX, offsets[i].x + images[i].cols);
        maxY = std::max(maxY, offsets[i].y + images[i].rows);
    }

    int canvasW = cvRound(maxX - minX);
    int canvasH = cvRound(maxY - minY);

    if (canvasW > 45000 || canvasH > 12000)
        return nullptr;

    /**
     * Result canvas
     */
    Mat result = Mat::zeros(Size(canvasW, canvasH), CV_8UC3);
    Mat weight = Mat::zeros(Size(canvasW, canvasH), CV_32F);

    /**
     * Stitching loop (NO BLUR)
     */
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

            Vec3b* rRow = result.ptr<Vec3b>(cy);
            float* wRow = weight.ptr<float>(cy);

            for (int x = 0; x < current.cols; ++x) {

                if (cRow[x] == Vec3b(0,0,0)) continue;

                int cx = ox + x;

                if (cx < 0 || cx >= canvasW) continue;

                float w = mRow[x];

                if (w > wRow[cx]) {

                    rRow[cx] = cRow[x];
                    wRow[cx] = w;
                }
            }
        }
    }

    /**
     * Final sharpening
     */
    Mat blur;

    GaussianBlur(result, blur, Size(0,0), 1.0);

    addWeighted(result, 2.0, blur, -1.0, 0, result);

    /**
     * Crop black borders
     */
    Mat gray;

    cvtColor(result, gray, COLOR_BGR2GRAY);

    Rect crop = boundingRect(gray > 10);

    if (crop.width > 0 && crop.height > 0)
        result = result(crop);

    /**
     * Encode JPEG
     */
    std::vector<uchar> out;

    imencode(".jpg", result, out);

    jbyteArray resArray = env->NewByteArray(out.size());

    env->SetByteArrayRegion(
            resArray,
            0,
            out.size(),
            (jbyte*)out.data());

    return resArray;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ovais_nativecore_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject) {

    return env->NewStringUTF(
            "Professional Ultra-Sharp Panorama V31");
}