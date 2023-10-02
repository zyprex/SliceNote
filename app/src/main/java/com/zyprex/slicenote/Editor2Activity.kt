package com.zyprex.slicenote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import java.text.SimpleDateFormat
import java.util.*

class Editor2Activity : AppCompatActivity() {

    private var hasNewGroup = false
    private var lastModifyGroup = ""
    private var untouched = true
    private var groupList = ArrayList<String>()
    private var adapter: ArrayAdapter<String>? = null

    companion object {
        fun launchWith(context: Context, slice: Slice, sliceId: Long, groupList: ArrayList<String>) {
            val intent = Intent(context, Editor2Activity::class.java).apply {
                putExtra("old_slice", slice)
                putExtra("old_slice_id", sliceId)
                putStringArrayListExtra("group_list", groupList)
            }
            val mainActivity = context as MainActivity
            mainActivity.getResultFromEditor2.launch(intent)
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

        /*toolbar*/
        val toolbarEditor = findViewById<Toolbar>(R.id.toolbarEditor)
        toolbarEditor.title = ""
        setSupportActionBar(toolbarEditor)
        toolbarEditor.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        toolbarEditor.setNavigationOnClickListener {
            this.onBackPressed()
        }

        groupList = intent.getStringArrayListExtra("group_list")  ?: ArrayList<String>()

        val oldSlice = intent.getParcelableExtra<Slice>("old_slice") as Slice

        val theCreatedTime = oldSlice.createTime
        val theCreatedTimeFormat = SimpleDateFormat(State.dateFormat, Locale.getDefault()).format(theCreatedTime)
        findViewById<TextView>(R.id.createdTime).text = String.format("%s: %s", resources.getString(R.string.created_time), theCreatedTimeFormat)

        displayModifiedTime(oldSlice)

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
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, groupList as MutableList<String>)
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

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // groupID, itemID, orderID, title
        menu?.let {
            it.add(0,0,0,resources.getString(R.string.save_change))
            val itemSave = it.findItem(0)
            itemSave.setIcon(R.drawable.baseline_save_24)
            itemSave.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            0 -> submitNewSlice()
            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        distributeNewSlices() // don't put setResult bellow
        super.onBackPressed()
        finish()
    }

    private fun distributeNewSlices(){
        val intent = Intent()
        intent.putExtra("has_new_group", hasNewGroup)
        intent.putExtra("last_modify_group", lastModifyGroup)
        intent.putExtra("untouched", untouched)
        setResult(RESULT_OK, intent)
    }

    var theModifiedTime = 0L
    private fun displayModifiedTime(oldSlice: Slice) {
        theModifiedTime = oldSlice.modifyTime + oldSlice.createTime
        val theModifiedTimeFormat = SimpleDateFormat(State.dateFormat, Locale.getDefault()).format(theModifiedTime)
        findViewById<TextView>(R.id.modifiedTime).text = String.format("%s: %s", resources.getString(R.string.modified_time), theModifiedTimeFormat)
    }

    private fun submitNewSlice() {
        untouched = false

        val oldSlice = intent.getParcelableExtra<Slice>("old_slice") as Slice
        val oldSliceId = intent.getLongExtra("old_slice_id", -1)

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
        // add new group name to group list
        if (!groupList.contains(theGroup)) {
            groupList.add(theGroup)
            adapter?.notifyDataSetChanged()
            hasNewGroup = true
        }
        lastModifyGroup = theGroup

        val theFront = frontEdit.text.toString()
        val theBack = backEdit.text.toString()
        val theMarks = marksEdit.text.toString()
        var theMedia = 0
        if (mediaState0.isChecked) theMedia += 1
        if (mediaState1.isChecked) theMedia += 2
        if (mediaState2.isChecked) theMedia += 4

        val newSlice = oldSlice.copy(
            group = theGroup,
            front = theFront,
            back = theBack,
            marks = theMarks,
            createTime = oldSlice.createTime,
            modifyTime = Date().time - oldSlice.createTime,
            media = theMedia
        )
        newSlice.id = oldSliceId
        MainViewModel().updateSlice(newSlice)
        //Toast.makeText(this, "${theMedia}", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, resources.getString(R.string.changed), Toast.LENGTH_SHORT).show()
    }
}