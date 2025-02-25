package com.norgic.vdotokcall_mtm.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Created By: Norgic
 * Date & Time: On 1/21/21 At 1:17 PM in 2021
 *
 * Model map class to send request for user login
 */
@Parcelize
data class LoginUserModel (

    @SerializedName("email")
    var email: String? = null,

    @SerializedName("password")
    var password: String? = null

) : Parcelable