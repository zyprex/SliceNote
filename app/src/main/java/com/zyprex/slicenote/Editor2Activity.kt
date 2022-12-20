package com.zyprex.slicenote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.text.SimpleDateFormat
import java.util.*

class Editor2Activity : AppCompatActivity() {

    companion object {
        fun actionStart(context: Context, slice: Slice,  sliceId: Long, groupList: ArrayList<String>) {
            val intent = Intent(context, Editor2Activity::class.java).apply {
                putExtra("old_slice", slice)
                putExtra("old_slice_id", sliceId)
                putStringArrayListExtra("group_list", groupList)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        when (State.nightMode) {
            0 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
            1 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            2 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val oldSlice = intent.getParcelableExtra<Slice>("old_slice") as Slice
        val oldSliceId = intent.getLongExtra("old_slice_id", -1)
        val groupList = intent.getStringArrayListExtra("group_list")

        val theCreatedTime = oldSlice.createTime
        val theCreatedTimeFormat = SimpleDateFormat(State.dateFormat, Locale.getDefault()).format(theCreatedTime)
        findViewById<TextView>(R.id.createdTime).text = String.format("%s: %s", resources.getString(R.string.created_time), theCreatedTimeFormat)

        val theModifiedTime = oldSlice.modifyTime + oldSlice.createTime
        val theModifiedTimeFormat = SimpleDateFormat(State.dateFormat, Locale.getDefault()).format(theModifiedTime)
        findViewById<TextView>(R.id.modifiedTime).text = String.format("%s: %s", resources.getString(R.string.modified_time), theModifiedTimeFormat)

        val groupEdit = findViewById<EditText>(R.id.groupEdit)
        val frontEdit = findViewById<EditText>(R.id.frontEdit)
        val backEdit = findViewById<EditText>(R.id.backEdit)
        val marksEdit = findViewById<EditText>(R.id.marksEdit)

        val mediaState0 = findViewById<CheckBox>(R.id.mediaState0)
        val mediaState1 = findViewById<CheckBox>(R.id.mediaState1)
        val mediaState2 = findViewById<CheckBox>(R.id.mediaState2)

        groupEdit.setText(oldSlice.group)
        frontEdit.setText(oldSlice.front)
        backEdit.setText(oldSlice.back)
        marksEdit.setText(oldSlice.marks)

        val expandSpinner = findViewById<ImageButton>(R.id.expandSpinner)
        expandSpinner.setOnClickListener {
            val groupSpinner = findViewById<Spinner>(R.id.groupSpinner)
            groupSpinner.performClick()
            //groupSpinner.visibility = View.VISIBLE
        }

        val groupSpinner = findViewById<Spinner>(R.id.groupSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, groupList as MutableList<String>)
        groupSpinner.adapter = adapter
        groupSpinner.setSelection(groupList.indexOf(oldSlice.group), true)
        groupSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(view?.id) {
                    else -> {
                        groupEdit.setText(parent?.getItemAtPosition(position).toString())
                        frontEdit.requestFocus()
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                //TODO("Not yet implemented")
            }
        }

        mediaState0.isChecked = arrayOf(1, 3, 5, 7).contains(oldSlice.media)
        mediaState1.isChecked = arrayOf(2, 3, 6, 7).contains(oldSlice.media)
        mediaState2.isChecked = arrayOf(4, 5, 6, 7).contains(oldSlice.media)

        val submitNewSlice = findViewById<Button>(R.id.submitNewSlice)
        submitNewSlice.text = resources.getString(R.string.save_change)
        submitNewSlice.setOnClickListener {
            var theGroup = groupEdit.text.toString()
            if (theGroup.isEmpty()) {
                theGroup = State.defaultGroupName
            }
            val theFront = frontEdit.text.toString()
            val theBack = backEdit.text.toString()
            val theMarks = marksEdit.text.toString()
            var theMedia = 0
            if (mediaState0.isChecked) {
                theMedia += 1
            }
            if (mediaState1.isChecked) {
                theMedia += 2
            }
            if (mediaState2.isChecked) {
                theMedia += 4
            }
            val newSlice = oldSlice.copy(
                group = theGroup,
                front = theFront,
                back = theBack,
                marks = theMarks,
                createTime = theCreatedTime,
                modifyTime = Date().time - theCreatedTime,
                media = theMedia
            )
            newSlice.id = oldSliceId
            MainViewModel().updateSlice(newSlice)
            Toast.makeText(this, resources.getString(R.string.changed), Toast.LENGTH_SHORT).show()
        }
    }
}