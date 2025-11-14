package com.example.edgeviewer

import android.Manifest
import android.content.pm.PackageManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.edgeviewer.gl.GLTextureRenderer
import android.opengl.GLSurfaceView
import android.hardware.camera2.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    companion object {
        init { System.loadLibrary("native-lib") }
        private const val TAG = "MainActivity"
        private const val CAMERA_REQ = 1001
    }

    external fun processToGrayscaleJNI(input: ByteArray, width: Int, height: Int): ByteArray

    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: GLTextureRenderer

    // Camera2 API
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    // Executor for background work
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // Handler for Camera2 (required!)
    private val cameraHandler = Handler(Looper.getMainLooper())

    private lateinit var imageReader: ImageReader
    private lateinit var yuvConverter: YuvToRgbConverter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // OpenCV test
        if (org.opencv.android.OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded")
        }

        // OpenGL renderer setup
        glView = GLSurfaceView(this)
        glView.setEGLContextClientVersion(2)
        renderer = GLTextureRenderer { glView.requestRender() }
        glView.setRenderer(renderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        setContentView(glView)

        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQ
            )
        } else {
            startCameraPipeline()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_REQ) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraPipeline()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startCameraPipeline() {
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        yuvConverter = YuvToRgbConverter(this)

        // Pick back camera
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
        } ?: cameraManager.cameraIdList.first()

        val width = 640
        val height = 480

        // ImageReader
        imageReader = ImageReader.newInstance(
            width,
            height,
            android.graphics.ImageFormat.YUV_420_888,
            2
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            try {
                val rgba = yuvConverter.yuvToRgba(image)

                val processed = processToGrayscaleJNI(rgba, width, height)

                runOnUiThread {
                    renderer.pushRGBA(processed, width, height)
                }

            } catch (ex: Exception) {
                Log.e(TAG, "Processing error", ex)
            } finally {
                image.close()
            }
        }, cameraHandler)

        // *** FIXED openCamera signature ***
        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device

                    val surface = imageReader.surface
                    val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    requestBuilder.addTarget(surface)

                    // *** FIXED createCaptureSession signature ***
                    device.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                captureSession = session
                                try {
                                    requestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                    )
                                    val request = requestBuilder.build()

                                    // *** FIXED setRepeatingRequest signature ***
                                    session.setRepeatingRequest(
                                        request,
                                        null,
                                        cameraHandler
                                    )

                                } catch (e: Exception) {
                                    Log.e(TAG, "Preview failed", e)
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.e(TAG, "Configuration failed")
                            }
                        },
                        cameraHandler
                    )
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                    cameraDevice = null
                }

                override fun onError(device: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error $error")
                    device.close()
                    cameraDevice = null
                }
            },
            cameraHandler  // MUST use Handler, not Executor
        )
    }

    override fun onPause() {
        super.onPause()
        captureSession?.close()
        cameraDevice?.close()
        imageReader.close()
        glView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }
}
