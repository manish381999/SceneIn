package com.tie.vibein.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircleCropOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayColor = Color.parseColor("#A6000000") // Semi-transparent black
    private var circleRadius: Float = 0f
    private var circlePath = Path()
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // --- THIS IS THE CRITICAL FIX ---
    // We disable hardware acceleration for this specific view because PorterDuffXfermode
    // doesn't always work as expected with it. This forces software rendering.
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
    // --- END OF FIX ---

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Make the crop circle 90% of the view's width
        circleRadius = (width * 0.9f) / 2

        // Update the path for the circle
        circlePath.reset()
        circlePath.addCircle(width / 2f, height / 2f, circleRadius, Path.Direction.CW)
        circlePath.fillType = Path.FillType.INVERSE_EVEN_ODD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw the semi-transparent overlay color
        overlayPaint.color = overlayColor
        // The INVERSE_EVEN_ODD fill type creates the "hole" in the middle of the path
        canvas.drawPath(circlePath, overlayPaint)
    }

    // A helper function for the CropActivity to get the crop bounds
    fun getCropBounds(): RectF {
        val centerX = width / 2f
        val centerY = height / 2f
        return RectF(
            centerX - circleRadius,
            centerY - circleRadius,
            centerX + circleRadius,
            centerY + circleRadius
        )
    }
}