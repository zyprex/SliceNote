package com.zyprex.slicenote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EditorActivity : AppCompatActivity() {

    private var newSlicesCount = 0
    private var groupList = ArrayList<String>()
    private var adapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        when (State.nightMode) {
            0 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
            1 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            2 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        /*toolbar*/
        val toolbarEditor = findViewById<Toolbar>(R.id.toolbarEditor)
        toolbarEditor.title = ""
        setSupportActionBar(toolbarEditor)
        toolbarEditor.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        toolbarEditor.setNavigationOnClickListener {
            onBackPressed()
        }

        groupList = intent.getStringArrayListExtra("group_list") ?: ArrayList<String>()
        val inGroup = intent.getStringExtra("in_group")

        displayCreatedTime()

        val groupEdit = findViewById<EditText>(R.id.groupEdit)
        val frontEdit = findViewById<EditText>(R.id.frontEdit)

        val expandSpinner = findViewById<ImageButton>(R.id.expandSpinner)
        expandSpinner.setOnClickListener {
            val groupSpinner = findViewById<Spinner>(R.id.groupSpinner)
            groupSpinner.performClick()
            //groupSpinner.visibility = View.VISIBLE
        }

        val groupSpinner = findViewById<Spinner>(R.id.groupSpinner)
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, groupList as MutableList<String>)
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

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // groupID, itemID, orderID, title
        menu?.let {
            it.add(0,0,1,resources.getString(R.string.save_change))
            it.add(0,1,0,resources.getString(R.string.save_and_clone))
            val itemSave = it.findItem(0)
            val itemSaveClone = it.findItem(1)
            itemSave.setIcon(R.drawable.baseline_save_24)
            itemSave.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            itemSaveClone.setIcon(R.drawable.baseline_copy_all_24)
            itemSaveClone.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            0 -> submitNewSlice(false)
            1 -> submitNewSlice(true)
            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        //Log.d("EditorActivity", newSlices.toString())
        distributeNewSlices() // don't put setResult bellow
        super.onBackPressed()
        finish()
    }

    private fun distributeNewSlices(){
        val intent = Intent()
        intent.putExtra("new_slices_count", newSlicesCount)
        setResult(RESULT_OK, intent)
    }

    var theCreatedTime = 0L
    private fun displayCreatedTime() {
        theCreatedTime = Date().time
        var theCreatedTimeFormat = SimpleDateFormat(State.dateFormat, Locale.getDefault()).format(theCreatedTime)
        findViewById<TextView>(R.id.createdTime).text = String.format("%s: %s", resources.getString(R.string.created_time), theCreatedTimeFormat)
        findViewById<TextView>(R.id.modifiedTime).text = String.format("%s: %s", resources.getString(R.string.modified_time), theCreatedTimeFormat)
    }

    private fun submitNewSlice(clone: Boolean) {
        val groupEdit = findViewById<EditText>(R.id.groupEdit)
        val frontEdit = findViewById<EditText>(R.id.frontEdit)
        val backEdit = findViewById<EditText>(R.id.backEdit)
        val marksEdit = findViewById<EditText>(R.id.marksEdit)

        val mediaState0 = findViewById<CheckBox>(R.id.mediaState0)
        val mediaState1 = findViewById<CheckBox>(R.id.mediaState1)
        val mediaState2 = findViewById<CheckBox>(R.id.mediaState2)

        var theGroup = groupEdit.text.toString()
        if (theGroup.isEmpty()) {
            theGroup = State.defaultGroupName
        }
        val theFront = frontEdit.text.toString()
        val theBack = backEdit.text.toString()
        val theMarks = marksEdit.text.toString()
        var theMedia = 0
        if (mediaState0.isChecked) theMedia += 1
        if (mediaState1.isChecked) theMedia += 2
        if (mediaState2.isChecked) theMedia += 4

        val newSlice = Slice(
            group = theGroup,
            front = theFront,
            back = theBack,
            marks = theMarks,
            createTime = theCreatedTime,
            modifyTime = 0L,
            media = theMedia
        )

        newSlicesCount++
        MainViewModel().addSlice(newSlice)

        // add new group name to group list
        if (!groupList.contains(theGroup)) {
            groupList.add(theGroup)
            adapter?.notifyDataSetChanged()
        }

        // clear
        if (!clone) {
            frontEdit.setText("")
            backEdit.setText("")
            marksEdit.setText("")
            mediaState0.isChecked = false
            mediaState1.isChecked = false
            mediaState2.isChecked = false
        }

        displayCreatedTime()

        Toast.makeText(this, resources.getString(R.string.added), Toast.LENGTH_SHORT).show()
    }
}