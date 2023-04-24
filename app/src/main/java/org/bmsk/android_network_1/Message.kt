package org.bmsk.android_network_1

import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("message")
    val message: String,
)