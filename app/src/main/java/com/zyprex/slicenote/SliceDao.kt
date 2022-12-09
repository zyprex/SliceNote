package com.zyprex.slicenote

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SliceDao {
    @Insert
    fun insertSlice(slice: Slice): Long

    @Update
    fun updateSlice(newSlice: Slice)

    @Query("select * from Slice")
    fun loadAllSlices(): List<Slice>

    @Query("select * from Slice where `group` = :group")
    fun loadGroupSlice(group: String): List<Slice>

    @Query("select distinct `group` from Slice")
    fun loadGroupList(): List<String>

    @Query("select * from Slice where " +
            "`group` = :group and `front` = :front and `back` = :back and `marks` = :marks")
    fun querySameSlice(group: String, front: String, back: String, marks: String): List<Slice>

    @Delete
    fun deleteSlice(slice: Slice)

}