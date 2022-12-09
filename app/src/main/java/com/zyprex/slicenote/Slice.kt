package com.zyprex.slicenote

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity
data class Slice(
    var group: String = "",
    var front: String = "",
    var back: String = "",
    var marks: String = "",
    var createTime: Long = 0,
    var modifyTime: Long = 0,
    var seq: Int = 0,
    var prior: Int = 0,
    var hide: Boolean = false,
) : Parcelable {
    @IgnoredOnParcel
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
