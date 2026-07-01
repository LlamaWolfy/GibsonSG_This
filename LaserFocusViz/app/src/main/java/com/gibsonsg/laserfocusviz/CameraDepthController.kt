package com.gibsonsg.laserfocusviz

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface

/**
 * Owns the Camera2 device/session lifecycle. Prefers a camera that reports the
 * DEPTH_OUTPUT capability (a ToF/structured-light sensor, sometimes the same
 * module marketed as "laser autofocus") and streams DEPTH16 frames. If no such
 * capability exists on the device, falls back to reporting the live autofocus
 * lens distance so the app still visualizes *something* the AF/laser system
 * is sensing.
 */
class CameraDepthController(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onModeDetermined(depthCapable: Boolean)
        fun onDepthFrame(depthMm: ShortArray, confidence: ByteArray, width: Int, height: Int)
        fun onFocusDistanceMeters(distanceMeters: Float?)
        fun onError(message: String)
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var depthImageReader: ImageReader? = null
    private var cameraId: String? = null
    private var depthCapable = false

    fun startBackgroundThread() {
        val thread = HandlerThread("CameraDepthBackground").also { it.start() }
        backgroundThread = thread
        backgroundHandler = Handler(thread.looper)
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        backgroundThread = null
        backgroundHandler = null
    }

    @SuppressLint("MissingPermission")
    fun openCamera(surfaceTexture: SurfaceTexture, previewWidth: Int, previewHeight: Int) {
        if (cameraDevice != null) return
        val id = pickCameraId()
        if (id == null) {
            listener.onError("No usable camera found on this device.")
            return
        }
        cameraId = id
        val characteristics = cameraManager.getCameraCharacteristics(id)
        depthCapable = supportsDepthOutput(characteristics)
        listener.onModeDetermined(depthCapable)

        if (depthCapable) {
            val depthSize = pickDepthSize(characteristics)
            if (depthSize == null) {
                depthCapable = false
                listener.onModeDetermined(false)
            } else {
                depthImageReader = ImageReader.newInstance(
                    depthSize.width, depthSize.height, ImageFormat.DEPTH16, 2
                ).apply {
                    setOnImageAvailableListener({ reader ->
                        val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                        try {
                            decodeAndDispatch(image)
                        } finally {
                            image.close()
                        }
                    }, backgroundHandler)
                }
            }
        }

        surfaceTexture.setDefaultBufferSize(previewWidth, previewHeight)

        try {
            cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    createSession(device, surfaceTexture)
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                    cameraDevice = null
                }

                override fun onError(device: CameraDevice, error: Int) {
                    device.close()
                    cameraDevice = null
                    listener.onError("Camera error code: $error")
                }
            }, backgroundHandler)
        } catch (e: SecurityException) {
            listener.onError("Camera permission not granted.")
        } catch (e: Exception) {
            listener.onError("Failed to open camera: ${e.message}")
        }
    }

    private fun createSession(device: CameraDevice, surfaceTexture: SurfaceTexture) {
        val previewSurface = Surface(surfaceTexture)
        val targets = mutableListOf(previewSurface)
        depthImageReader?.surface?.let { targets.add(it) }

        try {
            device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    startRepeatingRequest(device, session, previewSurface)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    listener.onError("Camera session configuration failed.")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            listener.onError("Failed to create capture session: ${e.message}")
        }
    }

    private fun startRepeatingRequest(
        device: CameraDevice,
        session: CameraCaptureSession,
        previewSurface: Surface
    ) {
        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                depthImageReader?.let { if (depthCapable) addTarget(it.surface) }
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }

            val callback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    if (!depthCapable) {
                        val diopters = result.get(CaptureResult.LENS_FOCUS_DISTANCE)
                        val meters = if (diopters != null && diopters > 0f) 1f / diopters else null
                        listener.onFocusDistanceMeters(meters)
                    }
                }
            }

            session.setRepeatingRequest(builder.build(), callback, backgroundHandler)
        } catch (e: Exception) {
            listener.onError("Failed to start preview: ${e.message}")
        }
    }

    private fun decodeAndDispatch(image: Image) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = image.width
        val height = image.height

        val depthMm = ShortArray(width * height)
        val confidence = ByteArray(width * height)

        for (row in 0 until height) {
            val rowStart = row * rowStride
            for (col in 0 until width) {
                val idx = rowStart + col * pixelStride
                val low = buffer.get(idx).toInt() and 0xFF
                val high = buffer.get(idx + 1).toInt() and 0xFF
                val sample = (high shl 8) or low
                val depthSample = sample and 0x1FFF
                val confidenceBits = (sample ushr 13) and 0x7
                val outIdx = row * width + col
                depthMm[outIdx] = depthSample.toShort()
                confidence[outIdx] = confidenceBits.toByte()
            }
        }

        listener.onDepthFrame(depthMm, confidence, width, height)
    }

    fun closeCamera() {
        try {
            captureSession?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing session", e)
        }
        captureSession = null

        try {
            cameraDevice?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing device", e)
        }
        cameraDevice = null

        depthImageReader?.close()
        depthImageReader = null
    }

    private fun pickCameraId(): String? {
        val ids = cameraManager.cameraIdList
        val backFacing = ids.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
        return backFacing ?: ids.firstOrNull()
    }

    private fun supportsDepthOutput(characteristics: CameraCharacteristics): Boolean {
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        return capabilities?.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT
        ) == true
    }

    private fun pickDepthSize(characteristics: CameraCharacteristics): Size? {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = map?.getOutputSizes(ImageFormat.DEPTH16) ?: return null
        // Smallest available resolution keeps per-pixel decode fast enough for real time.
        return sizes.minByOrNull { it.width * it.height }
    }

    companion object {
        private const val TAG = "CameraDepthController"
    }
}
