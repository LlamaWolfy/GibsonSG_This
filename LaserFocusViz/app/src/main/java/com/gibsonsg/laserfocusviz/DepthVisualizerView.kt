package com.gibsonsg.laserfocusviz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Renders either a live depth/ToF map (color-coded by distance) or, on devices
 * without a depth-output camera, a simple gauge of the current autofocus
 * lens distance as reported by the laser/PDAF-assisted focus system.
 */
class DepthVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        // Visualization range; distances outside this are clamped for coloring.
        const val MIN_DEPTH_MM = 150f
        const val MAX_DEPTH_MM = 4000f
    }

    private var depthBitmap: Bitmap? = null
    private var pixelBuffer: IntArray = IntArray(0)

    private val destRect = RectF()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 200 }

    private val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }
    private val gaugeTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(80, 255, 255, 255)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 56f
        textAlign = Paint.Align.CENTER
    }

    private var showGauge = false
    private var gaugeFraction = 0f // 0 (near) .. 1 (far)
    private var gaugeLabel = ""

    /** Push a new decoded depth frame. Distances are in millimeters; 0 = no data. */
    fun updateDepthFrame(depthMm: ShortArray, confidence: ByteArray, width: Int, height: Int) {
        showGauge = false
        if (pixelBuffer.size != width * height) {
            pixelBuffer = IntArray(width * height)
        }
        for (i in depthMm.indices) {
            val mm = depthMm[i].toInt() and 0x1FFF
            val conf = confidence[i].toInt() and 0x7
            pixelBuffer[i] = colorForDepth(mm, conf)
        }
        var bmp = depthBitmap
        if (bmp == null || bmp.width != width || bmp.height != height) {
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            depthBitmap = bmp
        }
        bmp.setPixels(pixelBuffer, 0, width, 0, 0, width, height)
        postInvalidateOnAnimation()
    }

    /** Fallback: no depth camera available, show current lens focus distance instead. */
    fun updateFocusDistanceMeters(distanceMeters: Float?) {
        showGauge = true
        if (distanceMeters == null || distanceMeters <= 0f) {
            gaugeFraction = 0f
            gaugeLabel = "Focusing..."
        } else {
            val clampedM = distanceMeters.coerceIn(MIN_DEPTH_MM / 1000f, MAX_DEPTH_MM / 1000f)
            gaugeFraction = (clampedM - MIN_DEPTH_MM / 1000f) / (MAX_DEPTH_MM / 1000f - MIN_DEPTH_MM / 1000f)
            gaugeLabel = String.format("%.2f m", distanceMeters)
        }
        postInvalidateOnAnimation()
    }

    private fun colorForDepth(mm: Int, confidenceBits: Int): Int {
        if (mm <= 0 || confidenceBits == 0) {
            return Color.TRANSPARENT
        }
        val clamped = mm.coerceIn(MIN_DEPTH_MM.toInt(), MAX_DEPTH_MM.toInt())
        val fraction = (clamped - MIN_DEPTH_MM) / (MAX_DEPTH_MM - MIN_DEPTH_MM)
        // Hue 0 (red) = close, hue 270 (violet) = far.
        val hue = fraction * 270f
        val confidenceScale = (confidenceBits.coerceIn(1, 7)) / 7f
        val hsv = floatArrayOf(hue, 1f, confidenceScale.coerceAtLeast(0.35f))
        return Color.HSVToColor(230, hsv)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showGauge) {
            drawGauge(canvas)
            return
        }
        val bmp = depthBitmap ?: return
        destRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bmp, null, destRect, bitmapPaint)
    }

    private fun drawGauge(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = (width.coerceAtMost(height)) / 3f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(rect, 135f, 270f, false, gaugeTrackPaint)

        val sweep = 270f * gaugeFraction
        val gradient = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            Color.RED, Color.MAGENTA, Shader.TileMode.CLAMP
        )
        gaugePaint.shader = gradient
        canvas.drawArc(rect, 135f, sweep, false, gaugePaint)

        canvas.drawText(gaugeLabel, cx, cy, textPaint)
        canvas.drawText("Near", rect.left, rect.bottom + 60f, textPaint)
        canvas.drawText("Far", rect.right, rect.bottom + 60f, textPaint)
    }
}
