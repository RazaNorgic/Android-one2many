package com.norgic.vdotokcall_mtm.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Created By: Norgic
 * Date & Time: On 1/21/21 At 1:17 PM in 2021
 *
 * Response model class for mapping group information
 */
@Parcelize
data class UpdateGroupNameModel (

    @SerializedName("group_id")
    var groupId: Int? = null,

    @SerializedName("group_title")
    var groupTitle: String? = null

): Parcelable