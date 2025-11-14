# 📱 EdgeViewer  
### Android + OpenCV (C++) + OpenGL ES + TypeScript Web Viewer

This project implements a real-time edge detection viewer using OpenCV (C++), OpenGL ES for rendering, and a minimal TypeScript web viewer.  
It demonstrates camera capture → C++ processing → OpenGL rendering → web preview.

---

## ✅ Features Implemented (Android + Web)

### 📸 Android Features
- Real-time camera feed using **TextureView**
- JNI bridge transferring frames between Kotlin ↔ C++
- OpenCV C++ processing:
  - Grayscale conversion  
  - Canny Edge Detection  
- OpenGL ES 2.0 rendering of processed frame
- Modular folder structure (`app/`, `gl/`, `jni/`)

### 🌐 TypeScript Web Viewer
- Minimal TypeScript web page
- Displays a sample processed frame (PNG/Base64)
- Shows stats (FPS/resolution)
- Simple TypeScript build using `tsc`

---

## 📷 Screenshots / GIFs

Create a folder `/screenshots` and include:

- app_raw_frame.jpg
- app_edge_frame.jpg
- gl_render_output.jpg
- web_viewer_output.png
*(Add them before submission.)*

---

## ⚙️ Setup Instructions

### 1️⃣ Install Required SDK Components
In **Android Studio → SDK Manager**:
- Check **NDK (Side by side)**
- Check **CMake**
- Install latest **Android SDK Platform**

### 2️⃣ Add OpenCV Android SDK
Place the extracted OpenCV SDK inside:
- /OpenCV/
- sdk/
- native/

### 3️⃣ Build Native C++ Code
CMake + NDK are already configured (`CMakeLists.txt`, `externalNativeBuild`).

Android Studio will compile native code automatically.

### 4️⃣ Web Viewer Setup
Inside `/web` folder:
```sh
npm install
tsc
```

### 🧠 Architecture Overview

The project follows a clean modular architecture that separates camera capture, native image processing, rendering, and web preview.  
The complete data flow is:
- Camera Feed → Kotlin (Android) → JNI → C++ OpenCV → Kotlin → OpenGL Renderer → Screen

### **1️⃣ Camera Capture Layer (Android – Kotlin)**  
- Uses `TextureView` to access camera frames in real time.  
- Converts camera frame into a byte array or `Image` buffer.  
- Sends raw frame data to the native layer through a JNI bridge.

### **2️⃣ JNI Bridge (Java/Kotlin ↔ C++)**  
- Connects Android (Kotlin) code to C++ OpenCV code.  
- Passes the input frame buffer, width, and height.  
- Receives the processed image from the native side.  
- Ensures efficient memory transfer for real-time performance.

### **3️⃣ Native Processing Layer (C++ with OpenCV)**  
- Converts incoming byte array to `cv::Mat`.  
- Applies image-processing algorithms:
  - Grayscale conversion (`cv::cvtColor`)
  - Canny Edge Detection (`cv::Canny`)  
- Converts processed matrix back into a format suitable for OpenGL rendering.  
- Designed for high performance using NDK and OpenCV compiled in C++.

### **4️⃣ Rendering Layer (OpenGL ES 2.0)**  
- The processed frame is uploaded as a texture.  
- `GLSurfaceView` + custom `GLRenderer` draws the image on screen.  
- Ensures smooth rendering at 10–15 FPS minimum.  
- Allows easy extension using GLSL shaders (optional bonus work).

### **5️⃣ Web Viewer (TypeScript + HTML)**  
- Shows a static processed frame exported from Android.  
- Displays simple metrics: resolution, FPS (mock), or image name.  
- Demonstrates ability to integrate native output with a web-based layer.  
- Uses modular TypeScript and compiled to JavaScript using `tsc`.

---

### ⭐ Summary
The architecture clearly separates:
- **Camera (Android)**  
- **Processing (OpenCV C++)**  
- **Rendering (OpenGL)**  
- **Debug/Preview (Web)**  





