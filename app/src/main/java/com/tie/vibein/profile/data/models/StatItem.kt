package com.tie.vibein.profile.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class StatItem(
    val count: String,
    val label: String
) : Parcelable
