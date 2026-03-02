# Panorama Project

An Android application and library for capturing and stitching panoramic images using OpenCV and CameraX.

## 🌟 Features

*   **Sensor-Driven Capture:** Automatically triggers image capture based on device rotation (yaw) to ensure consistent overlap for stitching.
*   **OpenCV Stitching:** Utilizes ORB (Oriented FAST and Rotated BRIEF) feature detection and BRUTEFORCE_HAMMING matching for high-quality image alignment.
*   **Real-time Processing:** Includes experimental support for live panorama generation and video-to-panorama conversion.
*   **360° Viewer:** Integrated viewer to interact with the captured panoramic results.
*   **Modern Android Stack:** Built with Jetpack Compose for UI, CameraX for robust camera interactions, and Kotlin Coroutines for background processing.

## 📂 Project Structure

*   **`:app`**: The main demonstration application containing various screens for capture and viewing.
*   **`:camera_360`**: A dedicated module for the 360-degree camera experience, including sensor logic and specialized stitching utilities.
*   **`:open-cv`**: A local Android library module wrapping OpenCV functionality for the project.

## 🛠 Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Camera API:** CameraX
*   **Computer Vision:** OpenCV (v4.x)
*   **Sensors:** Android Rotation Vector Sensor
*   **Visualization:** PanoramaGL

## 🚀 Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-repo/panorama.git
    ```
2.  **Open in Android Studio:**
    Import the project as a Gradle project.
3.  **Permissions:**
    The app requires `CAMERA` and `WRITE_EXTERNAL_STORAGE` permissions.
4.  **Run:**
    Deploy the `:app` or `:camera_360` module to a physical device (sensors are required for the best experience).

## 🧩 How it Works

1.  **Capture:** The `Camera360Activity` monitors the rotation sensor. When the user rotates the device by a certain threshold (e.g., 25°), a frame is captured automatically.
2.  **Stitching:** Captured frames are processed in `StitcherUtil`. 
    *   Keypoints are detected using ORB.
    *   Features are matched between consecutive frames.
    *   A homography matrix is calculated to warp and align the images.
3.  **Blending:** The aligned images are blended to create a seamless panoramic strip.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

[Specify License, e.g., MIT]
