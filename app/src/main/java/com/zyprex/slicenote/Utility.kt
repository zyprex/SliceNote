package com.zyprex.slicenote

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