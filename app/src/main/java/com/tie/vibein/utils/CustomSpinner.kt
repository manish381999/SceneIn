package com.tie.vibein.utils

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.AppCompatSpinner

class CustomSpinner : AppCompatSpinner {

    interface OnSpinnerEventsListener {
        fun onPopupWindowOpened(spinner: Spinner)
        fun onPopupWindowClosed(spinner: Spinner)
    }

    private var mListener: OnSpinnerEventsListener? = null
    private var mOpenInitiated = false

    constructor(context: Context) : super(context)

    constructor(context: Context, mode: Int) : super(context, mode)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        mode: Int
    ) : super(context, attrs, defStyleAttr, mode)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        mode: Int,
        popupTheme: Resources.Theme?
    ) : super(context, attrs, defStyleAttr, mode, popupTheme)

    override fun performClick(): Boolean {
        mOpenInitiated = true
        mListener?.onPopupWindowOpened(this)
        return super.performClick()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasBeenOpened() && hasFocus) {
            performClosedEvent()
        }
    }

    private fun performClosedEvent() {
        mOpenInitiated = false
        mListener?.onPopupWindowClosed(this)
    }

    private fun hasBeenOpened(): Boolean = mOpenInitiated

    fun setSpinnerEventsListener(listener: OnSpinnerEventsListener?) {
        mListener = listener
    }

    // Add this method to set items in the spinner
    fun setItems(items: List<String>) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        this.adapter = adapter
    }
}
