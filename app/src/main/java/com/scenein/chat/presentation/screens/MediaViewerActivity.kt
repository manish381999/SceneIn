package com.scenein.chat.presentation.screens

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.chat.data.model.MediaItem
import com.scenein.databinding.ActivityMediaViewerBinding
import com.scenein.databinding.ItemMediaViewerPageBinding
import com.scenein.utils.EdgeToEdgeUtils
import java.text.SimpleDateFormat
import java.util.*

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaViewerBinding
    private lateinit var mediaItems: List<MediaItem>
    private var isUiVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply padding to the root layout to handle system bars initially.
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Only apply top and bottom padding to the root, not left/right for edge-to-edge.
            view.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        @Suppress("DEPRECATION")
        mediaItems = intent.getSerializableExtra("media_items") as? ArrayList<MediaItem> ?: emptyList()
        val startPosition = intent.getIntExtra("start_position", 0)

        binding.ivBack.setOnClickListener { finish() }

        val mediaAdapter = MediaViewerAdapter(mediaItems) {
            toggleUiVisibility()
        }
        binding.viewPagerMedia.adapter = mediaAdapter
        binding.viewPagerMedia.setCurrentItem(startPosition, false)

        binding.viewPagerMedia.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateToolbar(position)
            }
        })

        updateToolbar(startPosition)
        // Set the initial UI state correctly
        setSystemBars(true)
    }

    private fun updateToolbar(position: Int) {
        if (mediaItems.isNotEmpty() && position < mediaItems.size) {
            val currentItem = mediaItems[position]
            binding.tvToolbarTitle.text = currentItem.senderName
            binding.tvToolbarSubtitle.text = formatMediaTimestamp(currentItem.timestamp)
        }
    }

    private fun toggleUiVisibility() {
        isUiVisible = !isUiVisible

        val transition = Fade().apply {
            duration = 250
            addTarget(binding.toolbar)
        }
        TransitionManager.beginDelayedTransition(binding.rootLayout, transition)

        binding.toolbar.visibility = if (isUiVisible) View.VISIBLE else View.GONE

        // Show or hide the system bars to match the toolbar's state.
        setSystemBars(isUiVisible)
    }

    // ======================================================================
    // == THE DEFINITIVE AND CORRECTED STATUS BAR LOGIC ==
    // ======================================================================
    private fun setSystemBars(visible: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (visible) {
            // SHOW the status bar.
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())

            // CRITICAL FIX: Set the status bar color to match the SOLID toolbar background.
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorSurface)

            // CRITICAL FIX: Make status bar icons (clock, etc.) dark on light themes.
            val isLightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO
            windowInsetsController.isAppearanceLightStatusBars = isLightMode
        } else {
            // HIDE the status bar for immersive mode.
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // All helper functions for formatting dates remain the same
    private fun formatMediaTimestamp(timestamp: String): String {
        if (timestamp.isBlank()) return ""
        try {
            val date: Date? = try {
                // Primary: ISO 8601 UTC format
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                isoFormat.parse(timestamp)
            } catch (e: Exception) {
                // Fallback: MySQL-style "yyyy-MM-dd HH:mm:ss"
                val mysqlFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                mysqlFormat.parse(timestamp)
            }

            if (date == null) return ""

            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val messageDate = Calendar.getInstance().apply { time = date }

            val datePart = when {
                isSameDay(messageDate.time, today.time) -> "Today"
                isSameDay(messageDate.time, yesterday.time) -> "Yesterday"
                else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
            }

            val timeOutputFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault() // show in user’s local time
            }
            val timePart = timeOutputFormat.format(date)
            return "$datePart at $timePart"
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

// The MediaViewerAdapter is now simpler as it only needs to handle the tap
class MediaViewerAdapter(
    private val mediaItems: List<MediaItem>,
    private val onPhotoTap: () -> Unit
) : RecyclerView.Adapter<MediaViewerAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(private val binding: ItemMediaViewerPageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.photoView.setOnPhotoTapListener { _, _, _ ->
                onPhotoTap()
            }
        }
        fun bind(item: MediaItem) {
            Glide.with(itemView.context)
                .load(item.url)
                .into(binding.photoView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaViewerPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaItems[position])
    }

    override fun getItemCount(): Int = mediaItems.size
}