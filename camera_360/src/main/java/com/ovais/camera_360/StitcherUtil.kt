package com.ovais.camera_360

import android.graphics.Bitmap
import androidx.core.graphics.scale
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import androidx.core.graphics.createBitmap
import org.opencv.core.CvType
import org.opencv.core.Rect
import kotlin.text.get

object StitcherUtil {

    init {
        System.loadLibrary("opencv_java4")
    }

    private const val MAX_INPUT_WIDTH = 1000
    private const val MAX_RESULT_WIDTH = 6000

    fun stitchBitmaps(bitmaps: List<Bitmap>): Bitmap? {

        if (bitmaps.size < 2) return bitmaps.firstOrNull()

        var result = bitmapToMat(resize(bitmaps[0]))

        for (i in 1 until bitmaps.size) {

            val next = bitmapToMat(resize(bitmaps[i]))

            val stitched = stitchTwoSafe(result, next) ?: return null

            result.release()
            result = stitched
        }

        return matToBitmap(result)
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_INPUT_WIDTH) return bitmap

        val ratio = MAX_INPUT_WIDTH.toFloat() / bitmap.width
        val height = (bitmap.height * ratio).toInt()

        return bitmap.scale(MAX_INPUT_WIDTH, height)
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val bmp = createBitmap(mat.cols(), mat.rows())
        Utils.matToBitmap(mat, bmp)
        return bmp
    }

    private fun stitchTwoSafe(img1: Mat, img2: Mat): Mat? {
        // Convert to 3-channel RGB
        val mat1 = Mat()
        val mat2 = Mat()
        Imgproc.cvtColor(img1, mat1, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(img2, mat2, Imgproc.COLOR_RGBA2RGB)

        val gray1 = Mat()
        val gray2 = Mat()
        Imgproc.cvtColor(mat1, gray1, Imgproc.COLOR_RGB2GRAY)
        Imgproc.cvtColor(mat2, gray2, Imgproc.COLOR_RGB2GRAY)

        val orb = ORB.create(1500)
        val kp1 = MatOfKeyPoint()
        val kp2 = MatOfKeyPoint()
        val des1 = Mat()
        val des2 = Mat()
        orb.detectAndCompute(gray1, Mat(), kp1, des1)
        orb.detectAndCompute(gray2, Mat(), kp2, des2)

        if (des1.empty() || des2.empty()) return null

        val matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING, true)
        val matches = MatOfDMatch()
        matcher.match(des1, des2, matches)

        val goodMatches = matches.toList().sortedBy { it.distance }.take(50)
        if (goodMatches.size < 4) return null

        val pts1 = mutableListOf<Point>()
        val pts2 = mutableListOf<Point>()
        val kp1Array = kp1.toArray()
        val kp2Array = kp2.toArray()
        for (m in goodMatches) {
            pts1.add(kp1Array[m.queryIdx].pt)
            pts2.add(kp2Array[m.trainIdx].pt)
        }

        val matPts1 = MatOfPoint2f(*pts1.toTypedArray())
        val matPts2 = MatOfPoint2f(*pts2.toTypedArray())
        val homography = Calib3d.findHomography(matPts2, matPts1, Calib3d.RANSAC, 5.0) ?: return null

        val width = minOf(img1.cols() + img2.cols(), MAX_RESULT_WIDTH)
        val height = maxOf(img1.rows(), img2.rows())
        val result = Mat.zeros(height, width, CvType.CV_8UC3)

        // Warp img2
        Imgproc.warpPerspective(mat2, result, homography, Size(width.toDouble(), height.toDouble()))

        // Copy img1 safely using mask
        val roi = Rect(0, 0, img1.cols(), img1.rows())
        val submat = result.submat(roi)
        mat1.copyTo(submat)

        return result
    }
}