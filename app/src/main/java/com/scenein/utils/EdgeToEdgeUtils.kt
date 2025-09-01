package com.scenein.utils

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

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

}