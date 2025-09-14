package com.scenein.utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView

object EdgeToEdgeUtils {

    /**
     * For simple, static Activities.
     */
    fun setUpEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val rootView = activity.findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    /**
     * The powerful version for complex screens like your CreateEventActivity.
     */
    fun setUpInteractiveEdgeToEdge(
        rootView: View,
        contentView: View,
        floatingView: View
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            // 1. Only translate floating view if keyboard is visible
            floatingView.translationY = if (isKeyboardVisible) {
                -imeInsets.bottom.toFloat()
            } else {
                0f
            }

            // 2. Pad content so it scrolls above floating view
            val floatingViewHeight = floatingView.height
            contentView.updatePadding(
                bottom = floatingViewHeight + systemBars.bottom
            )

            // 3. Add padding for status bar
            rootView.updatePadding(top = systemBars.top)

            WindowInsetsCompat.CONSUMED
        }
    }

    fun setUpChatEdgeToEdge(
        rootView: View,
        recyclerView: RecyclerView,
        fakeStatusBar: View, // Parameter for the top bar
        fakeNavBar: View     // Parameter for the bottom bar
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            // 1. Set the height of our fake bars to match the real system bars
            fakeStatusBar.updateLayoutParams { height = systemBars.top }
            fakeNavBar.updateLayoutParams { height = systemBars.bottom }

            // 2. Pad the entire root view ONLY at the bottom to push content
            // up when the keyboard appears. The top is handled by the fake bar.
            rootView.updatePadding(bottom = imeInsets.bottom)

            // 3. Scroll to the bottom when the keyboard opens
            if (insets.isVisible(WindowInsetsCompat.Type.ime()) && recyclerView.adapter?.itemCount ?: 0 > 0) {
                recyclerView.post {
                    recyclerView.scrollToPosition(recyclerView.adapter!!.itemCount - 1)
                }
            }

            WindowInsetsCompat.CONSUMED
        }
    }
}