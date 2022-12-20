package com.zyprex.slicenote

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity
data class Slice(
    @SerializedName("g", alternate = ["group"])
    var group: String = "",
    @SerializedName("f", alternate = ["front"])
    var front: String = "",
    @SerializedName("b", alternate = ["back"])
    var back: String = "",
    @SerializedName("m", alternate = ["marks"])
    var marks: String = "",
    @SerializedName("t0", alternate = ["createTime"])
    var createTime: Long = 0,
    @SerializedName("t1", alternate = ["modifyTime"])
    var modifyTime: Long = 0,
    @SerializedName("s", alternate = ["seq"])
    var seq: Int = 0,
    @SerializedName("p", alternate = ["prior"])
    var prior: Int = 0,
    @SerializedName("h", alternate = ["hide"])
    var hide: Boolean = false,
    @SerializedName("u", alternate = ["media"])
    var media: Int = 0, /* multi-media */
) : Parcelable {
    @IgnoredOnParcel
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
