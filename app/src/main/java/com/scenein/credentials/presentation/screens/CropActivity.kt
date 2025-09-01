package com.scenein.credentials.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide

import com.scenein.databinding.ActivityCropBinding
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropBinding
    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private var mode = NONE
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString == null) {
            Toast.makeText(this, "No image URI provided", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        Glide.with(this).load(Uri.parse(imageUriString)).into(binding.ivCroppableImage)

        setupTouchListeners()
        binding.btnCancel.setOnClickListener { setResult(Activity.RESULT_CANCELED); finish() }
        binding.btnChoose.setOnClickListener { cropAndSaveImage() }
    }

    private fun setupTouchListeners() {
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        binding.ivCroppableImage.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            val view = binding.ivCroppableImage
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> { savedMatrix.set(matrix); startPoint.set(event.x, event.y); mode = DRAG }
                MotionEvent.ACTION_POINTER_DOWN -> mode = ZOOM
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> mode = NONE
                MotionEvent.ACTION_MOVE -> { if (mode == DRAG) { matrix.set(savedMatrix); matrix.postTranslate(event.x - startPoint.x, event.y - startPoint.y) } }
            }
            view.imageMatrix = matrix
            return@setOnTouchListener true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            matrix.postScale(detector.scaleFactor, detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    }

    private fun cropAndSaveImage() {
        binding.cropContainer.isDrawingCacheEnabled = true
        val containerBitmap = Bitmap.createBitmap(binding.cropContainer.drawingCache)
        binding.cropContainer.isDrawingCacheEnabled = false

        val cropBounds = binding.cropOverlay.getCropBounds()

        if (cropBounds.right > containerBitmap.width || cropBounds.bottom > containerBitmap.height || cropBounds.left < 0 || cropBounds.top < 0) {
            Toast.makeText(this, "Please position the image fully within the circle.", Toast.LENGTH_SHORT).show()
            return
        }

        val croppedBitmap = Bitmap.createBitmap(
            containerBitmap, cropBounds.left.toInt(), cropBounds.top.toInt(),
            cropBounds.width().toInt(), cropBounds.height().toInt()
        )

        // This function now correctly uses FileProvider.
        val finalUri = saveBitmapToCacheAndGetContentUri(this, croppedBitmap)

        if (finalUri != null) {
            // --- THIS IS THE DEFINITIVE FIX ---
            // We must create a new Intent object to hold the result.
            // We cannot reuse the original intent.
            val resultIntent = Intent()
            resultIntent.data = finalUri
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
            // --- END OF FIX ---
        } else {
            Toast.makeText(this, "Failed to save cropped image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToCacheAndGetContentUri(context: Context, bitmap: Bitmap): Uri? {
        val filename = "VibeIn_Profile_Crop_${System.currentTimeMillis()}.jpg"
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, filename)
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val authority = "${context.applicationContext.packageName}.provider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        const val EXTRA_IMAGE_URI = "IMAGE_URI"
    }
}
