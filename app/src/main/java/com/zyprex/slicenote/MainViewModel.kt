package com.zyprex.slicenote

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Date
import kotlin.concurrent.thread

class MainViewModel : ViewModel() {

    companion object {
        var newAdd = false
    }

    var sliceListLiveData = MutableLiveData<MutableList<Slice>>()
    var sliceGroupListLiveData = MutableLiveData<MutableList<String>>()
    var sliceList = mutableListOf<Slice>()
    var sliceGroupList = mutableListOf<String>()
    private val sliceDao = AppDatabase.getDatabase(MyApplication.context).sliceDao()
    fun setOfSliceList(slices: MutableList<Slice>) {
        sliceList.clear()
        sliceList.addAll(slices)
        sliceListLiveData.value = sliceList
    }
    fun updateSlice(newSlice: Slice) {
        thread {
            sliceDao.updateSlice(newSlice)
        }
    }
    fun cacheGroupSlice(group: String = "") {
        State.tempSliceList.clear()
        thread {
            State.tempSliceList.addAll(
                if (group == "")
                    sliceDao.loadAllSlices()
                else
                    sliceDao.loadGroupSlice(group))
        }
    }
    fun renameGroup(group: String, newName: String) {
        if (group == "") {
            return
        }
        thread {
            val oldGroup = sliceDao.loadGroupSlice(group)
            oldGroup.forEach { s ->
                s.group = newName
                sliceDao.updateSlice(s)
            }
        }
        //updateSliceGroupList()
    }
    fun removeGroup(group: String) {
        thread {
            if (group == "") {
                sliceDao.loadAllSlices().forEach { s ->
                    sliceDao.deleteSlice(s)
                }
            } else {
                sliceDao.loadGroupSlice(group).forEach { s ->
                    sliceDao.deleteSlice(s)
                }
            }
        }
    }
    fun loadSlices(group: String = "",
                   hide: Boolean = false,
                   sort: Int = 0,
                   revSort: Boolean = false) {
        sliceList.clear()
        thread {
            sliceList.addAll((
                    if (group == "")
                        sliceDao.loadAllSlices()
                    else
                        sliceDao.loadGroupSlice(group)
                    ).filter { s -> s.hide == hide })
            if (sort != 0) {
                when (sort) {
                    1 -> sliceList.sortBy(Slice::createTime)
                    2 -> sliceList.sortBy(Slice::modifyTime)
                    3 -> sliceList.sortBy(Slice::seq)
                    4 -> sliceList.sortBy(Slice::prior)
                    5 -> sliceList.sortBy(Slice::front)
                }
            }
            if (revSort) {
                sliceList.reverse()
            }
            sliceListLiveData.postValue(sliceList)
        }
    }
    fun shuffleSlice() {
        sliceList.shuffle()
        sliceListLiveData.value = sliceList
    }
    fun deleteSlice(slice: Slice) {
        sliceList.remove(slice)
        sliceListLiveData.postValue(sliceList)
        thread {
            sliceDao.deleteSlice(slice)
        }
    }
    fun addUniqueSlices(slices: List<Slice>) {
        val nowTime = Date().time
        thread {
            slices.forEach { slice ->
                val sameSlices = sliceDao.querySameSlice(slice.group, slice.front, slice.back, slice.marks)
                if (sameSlices.isEmpty()) {
                    sliceDao.insertSlice(
                        Slice(
                            if (slice.group == "") {
                                State.defaultGroupName
                            } else {
                                slice.group
                            }, slice.front, slice.back, slice.marks,
                            // property neglected
                            if (slice.createTime == 0L) {
                                nowTime
                            } else {
                                slice.createTime
                            },
                            // compatible for v1.x backup file
                            if (nowTime < slice.createTime + slice.modifyTime) {
                                slice.modifyTime - slice.createTime
                            } else {
                                slice.modifyTime
                            },
                            slice.seq, slice.prior, slice.hide, slice.media
                        )
                    )
                }
            }
        }
    }
    fun addNewSlices(slices: MutableList<Slice>) {
        thread {
            for (s in slices) {
                sliceDao.insertSlice(s)
            }
            newAdd = true
            updateSliceGroupList()
        }
    }
    fun updateSliceGroupList() {
        sliceGroupList.clear()
        thread {
            sliceGroupList.addAll(sliceDao.loadGroupList())
            sliceGroupListLiveData.postValue(sliceGroupList)
        }
    }
    private val expiredCnt = 0
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == expiredCnt && msg.arg1 > 0) {
                Toast.makeText(MyApplication.context, msg.arg1.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun refreshSeq(total: Int) {
        thread {
            val allSliceList = sliceDao.loadAllSlices()
            var cnt = 0
            allSliceList.forEach { s ->
                if (s.hide && s.seq > 0) {
                    if (s.seq <= total) {
                        s.seq = -1
                        s.hide = false
                        cnt++
                    } else {
                        s.seq = s.seq - total
                    }
                    sliceDao.updateSlice(s)
                }

            }
            val msg = Message()
            msg.what = expiredCnt
            msg.arg1 = cnt
            handler.sendMessage(msg)
        }
    }
}
