package com.gibsonsg.laserfocusviz

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.TextureView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), CameraDepthController.Listener {

    private lateinit var textureView: TextureView
    private lateinit var depthView: DepthVisualizerView
    private lateinit var statusText: TextView
    private lateinit var cameraController: CameraDepthController

    private var surfaceReady = false
    private var pendingSurfaceTexture: SurfaceTexture? = null
    private var pendingWidth = 0
    private var pendingHeight = 0

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            statusText.text = getString(R.string.mode_depth)
            tryOpenCamera()
        } else {
            statusText.text = getString(R.string.permission_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        depthView = findViewById(R.id.depthView)
        statusText = findViewById(R.id.statusText)

        cameraController = CameraDepthController(this, this)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                pendingSurfaceTexture = surface
                pendingWidth = width
                pendingHeight = height
                surfaceReady = true
                tryOpenCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                surfaceReady = false
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    override fun onResume() {
        super.onResume()
        cameraController.startBackgroundThread()
        if (hasCameraPermission()) {
            tryOpenCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onPause() {
        cameraController.closeCamera()
        cameraController.stopBackgroundThread()
        super.onPause()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun tryOpenCamera() {
        val texture = pendingSurfaceTexture
        if (!surfaceReady || texture == null || !hasCameraPermission()) return
        cameraController.openCamera(texture, pendingWidth, pendingHeight)
    }

    override fun onModeDetermined(depthCapable: Boolean) {
        runOnUiThread {
            statusText.text = if (depthCapable) {
                getString(R.string.mode_depth)
            } else {
                getString(R.string.mode_focus_fallback)
            }
        }
    }

    override fun onDepthFrame(depthMm: ShortArray, confidence: ByteArray, width: Int, height: Int) {
        runOnUiThread {
            depthView.updateDepthFrame(depthMm, confidence, width, height)
        }
    }

    override fun onFocusDistanceMeters(distanceMeters: Float?) {
        runOnUiThread {
            depthView.updateFocusDistanceMeters(distanceMeters)
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }
}
