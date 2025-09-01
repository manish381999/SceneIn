package com.scenein.settings.data.models

import androidx.annotation.DrawableRes


sealed class SettingsItem {
    data class Header(val title: String) : SettingsItem()
    data class NavigableItem(
        val title: String,
        val subtitle: String,
        @DrawableRes val iconRes: Int,
        val action: () -> Unit
    ) : SettingsItem()
    data class ToggleItem(
        val title: String,
        val subtitle: String,
        @DrawableRes val iconRes: Int,
        var isChecked: Boolean,
        val action: (Boolean) -> Unit
    ) : SettingsItem()
    object Spacer : SettingsItem()
    data class LogoutItem(val username: String, val action: () -> Unit) : SettingsItem()
}