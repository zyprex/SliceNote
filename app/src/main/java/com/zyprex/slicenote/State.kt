package com.zyprex.slicenote

object State {
    var dateFormat = "yyyy-MM-dd HH:mm:ss.SSSS"
    var defaultGroupName = ".default"
    var tempSliceList = mutableListOf<Slice>()
    var lastOpenTime = -1L
    var nightMode = 0
    var sFontSize = 26
    var sTouchAction = arrayOf(5, 0, 0)
    var sCountdownStep = arrayOf(0, 0, 0, 0)
    var sSeqBtn = arrayOf(1, 2, 3, 4)
    var sFilterBox = arrayOf(true, true, true)
}


