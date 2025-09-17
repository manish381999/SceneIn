package com.scenein.discover.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

// In your models package, e.g., discover/data/models
@Parcelize
data class Participant(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("userName") val userName: String?,
    @SerializedName("profilePic") val profilePic: String?,
    @SerializedName("connectionStatus") var connectionStatus: String
) : Parcelable

data class ParticipantsResponse(
    val status: String,
    val participants: List<Participant>
)