package com.tie.vibein.utils

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object EdgeToEdgeUtils {
    /**
     * This is the definitive, reusable function to set up edge-to-edge display for any activity.
     * @param activity The activity to apply the edge-to-edge configuration to.
     */
    fun setUpEdgeToEdge(activity: Activity) {
        // Step 1: Tell the system that our app will handle drawing behind the system bars.
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        // Step 2: Get the main content view of the activity's layout.
        val rootView = activity.findViewById<View>(android.R.id.content)

        // Step 3: Set a listener to receive the sizes of the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Step 4: Apply these sizes as padding to the root view.
            // This pushes your content down from the status bar and up from the navigation bar,
            // preventing the overlap you are seeing.
            view.setPadding(
                insets.left,
                insets.top,
                insets.right,
                insets.bottom
            )

            // Indicate that we've consumed the insets.
            WindowInsetsCompat.CONSUMED
        }
    }
}