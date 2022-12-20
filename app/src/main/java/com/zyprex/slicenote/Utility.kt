package com.zyprex.slicenote

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat

fun legalFileName(str: String): String {
    var s = str
    if (s.contains("\n")) {
        s = s.substringBefore("\n")
    }
    if (s.contains("/")) {
        s = s.replace("/", " ")
    }
    if (s.length > 100) {
        s = s.substring(0, 100)
    }
    return s
}
fun imageOnButton (context: Context, resId: Int): Drawable? {
    return ResourcesCompat.getDrawable(context.resources, resId, null)
}