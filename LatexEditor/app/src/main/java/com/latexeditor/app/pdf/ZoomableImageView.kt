package com.latexeditor.app.pdf

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/** Simple pinch-to-zoom + pan ImageView used for each rendered PDF page. */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val matrixValues = FloatArray(9)
    private var scaleFactor = 1f
    private val minScale = 1f
    private val maxScale = 5f
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var mode = NONE

    companion object { const val NONE = 0; const val DRAG = 1; const val ZOOM = 2 }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (scaleFactor * detector.scaleFactor).coerceIn(minScale, maxScale)
            val factor = newScale / scaleFactor
            scaleFactor = newScale
            imageMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
            imageMatrix = imageMatrix
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x; lastTouchY = event.y; mode = DRAG
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG && scaleFactor > 1f) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        imageMatrix.postTranslate(dx, dy)
                        imageMatrix = imageMatrix
                        lastTouchX = event.x; lastTouchY = event.y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> mode = NONE
            }
            true
        }
    }

    fun resetZoom() {
        scaleFactor = 1f
        imageMatrix = Matrix()
    }
}
