package com.zyprex.slicenote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EditorActivity : AppCompatActivity() {

    private var newSlices = ArrayList<Slice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        when (State.nightMode) {
            0 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
            1 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            2 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        var groupList = intent.getStringArrayListExtra("group_list")
        var inGroup = intent.getStringExtra("in_group")

        var theCreatedTime = Date().time
        var theCreatedTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault()).format(theCreatedTime)
        findViewById<TextView>(R.id.createdTime).text = String.format("%s: %s", resources.getString(R.string.created_time), theCreatedTimeFormat)

        var theModifiedTime = theCreatedTime
        var theModifiedTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault()).format(theModifiedTime)
        findViewById<TextView>(R.id.modifiedTime).text = String.format("%s: %s", resources.getString(R.string.modified_time), theModifiedTimeFormat)

        val groupEdit = findViewById<EditText>(R.id.groupEdit)
        val frontEdit = findViewById<EditText>(R.id.frontEdit)
        val backEdit = findViewById<EditText>(R.id.backEdit)
        val marksEdit = findViewById<EditText>(R.id.marksEdit)


        val expandSpinner = findViewById<ImageButton>(R.id.expandSpinner)
        expandSpinner.setOnClickListener {
            val groupSpinner = findViewById<Spinner>(R.id.groupSpinner)
            groupSpinner.performClick()
            //groupSpinner.visibility = View.VISIBLE
        }

        val groupSpinner = findViewById<Spinner>(R.id.groupSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.preference_category, groupList as MutableList<String>)
        groupSpinner.adapter = adapter
        //groupSpinner.setSelection(0, true) // don't trigger spinner
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
        groupSpinner.setSelection(groupList.indexOf(inGroup), true)
        val submitNewSlice = findViewById<Button>(R.id.submitNewSlice)
        submitNewSlice.text = resources.getString(R.string.add)
        submitNewSlice.setOnClickListener {
            var theGroup = groupEdit.text.toString()
            if (theGroup.isEmpty()) {
                theGroup = State.defaultGroupName
            }
            val theFront = frontEdit.text.toString()
            val theBack = backEdit.text.toString()
            val theMarks = marksEdit.text.toString()
            val newSlice = Slice(theGroup, theFront, theBack, theMarks, theCreatedTime, theModifiedTime)

            newSlices.add(newSlice)
            if (!groupList.contains(theGroup)) {
                groupList.add(theGroup)
                adapter.notifyDataSetChanged()
            }

            frontEdit.setText("")
            backEdit.setText("")
            marksEdit.setText("")

            theCreatedTime = Date().time
            theCreatedTimeFormat = SimpleDateFormat(State.dateFormat, Locale.getDefault()).format(theCreatedTime)
            findViewById<TextView>(R.id.createdTime).text = String.format("%s: %s", resources.getString(R.string.created_time), theCreatedTimeFormat)

            theModifiedTime = theCreatedTime
            theModifiedTimeFormat = SimpleDateFormat(State.dateFormat, Locale.getDefault()).format(theModifiedTime)
            findViewById<TextView>(R.id.modifiedTime).text = String.format("%s: %s", resources.getString(R.string.modified_time), theModifiedTimeFormat)
        }
    }

    override fun onBackPressed() {
        //Log.d("EditorActivity", newSlices.toString())
        val intent = Intent()
        intent.putParcelableArrayListExtra("new_slices", newSlices)
        setResult(RESULT_OK, intent)
        // don't put setResult bellow
        super.onBackPressed()
        finish()
    }
}