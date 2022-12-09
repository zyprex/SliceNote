package com.zyprex.slicenote

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    private var currentGroup: String = ""
    private var currentShowHide: Boolean = false
    private var currentSort: Int = 0
    private var currentReverseSort: Boolean = false

    private lateinit var rvAdapter: SliceAdapter
    private lateinit var spAdapter: ArrayAdapter<String>

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        if (savedInstanceState != null) {
            restoreFromBundle(savedInstanceState)
        } else {
            restoreFromPrefs()
            restoreFromSettings()
        }
        nightModeSelector(State.nightMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calcCountdown()

        /*
        * INIT UI
        * */

        /* Toolbar */
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24)
        }

        toolbar.setOnClickListener {
            if (currentGroup != "") {
                showGroupSlice()
                Toast.makeText(this, resources.getString(R.string.refresh), Toast.LENGTH_SHORT).show()
            }
        }
        toolbar.setOnLongClickListener {
            val showHide = findViewById<CheckBox>(R.id.showHide)
            showHide.performClick()
            true
        }
        /* drawerLayout */
        viewModel.updateSliceGroupList()

        val loadAll = findViewById<Button>(R.id.loadAll)
        val renameGroup = findViewById<Button>(R.id.renameGroup)
        val groupSpinner = findViewById<Spinner>(R.id.groupSpinnerFilter)
        val showHide = findViewById<CheckBox>(R.id.showHide)
        val sortControl = findViewById<Spinner>(R.id.sortControl)
        val reverseSort = findViewById<CheckBox>(R.id.reverseSort)
        val shuffleBtn = findViewById<Button>(R.id.shuffleBtn)
        val hideShowBySeq = findViewById<Button>(R.id.hideShowBySeq)
        val bulkOperationOnFlippedSlice = findViewById<Button>(R.id.bulkOperationOnFlippedSlice)
        val lightMode = findViewById<Button>(R.id.lightMode)
        val nightMode = findViewById<Button>(R.id.nightMode)
        val autoNightMode = findViewById<Button>(R.id.autoNightMode)

        loadAll.setOnClickListener {
            currentGroup = ""
            showGroupSlice()
        }
        renameGroup.setOnClickListener {
            if (currentGroup == "") {
                Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
            } else {
                val editLine = EditText(this)
                AlertDialog.Builder(this)
                    .setView(editLine)
                    .setTitle(resources.getString(R.string.rename_group))
                    .setMessage(currentGroup)
                    .setPositiveButton(resources.getString(R.string.OK), DialogInterface.OnClickListener { _, _ ->
                        val s = editLine.text.toString()
                        if (s != "") {
                            viewModel.renameGroup(currentGroup, s)
                            viewModel.sliceGroupList[viewModel.sliceGroupList.indexOf(currentGroup)] = s
                            currentGroup = s
                            spAdapter.notifyDataSetChanged()
                        }
                    })
                    .setNegativeButton(resources.getString(R.string.cancel), null)
                    .show()
            }

        }

        spAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, viewModel.sliceGroupList)
        groupSpinner.adapter = spAdapter
        if (currentGroup != "") {
            groupSpinner.post {
                groupSpinner.setSelection(viewModel.sliceGroupList.indexOf(currentGroup))
            }
        } else {
            //Log.d("MainActivity", currentGroup)
            groupSpinner.setSelection(0, true)
        }
        groupSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (view?.id) {
                    else -> {
                        currentGroup = parent?.getItemAtPosition(position).toString()
                        showGroupSlice()
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        showHide.isChecked = currentShowHide
        hideShowBySeq.text = if (currentShowHide)
            resources.getString(R.string.show_all_seq_gt_0)
        else
            resources.getString(R.string.hide_all_seq_gt_0)
        showHide.setOnClickListener {
            currentShowHide = showHide.isChecked
            hideShowBySeq.text = if (currentShowHide)
                resources.getString(R.string.show_all_seq_gt_0)
            else
                resources.getString(R.string.hide_all_seq_gt_0)
            showGroupSlice()
        }

        sortControl.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf(resources.getString(R.string.sort_no_sort),
                resources.getString(R.string.sort_create_time),
                resources.getString(R.string.sort_modify_time),
                resources.getString(R.string.sort_seq),
                resources.getString(R.string.sort_prior),
                resources.getString(R.string.front)))

        // pre-selected to prevent default trigger
        sortControl.setSelection(currentSort, true)
        sortControl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (view?.id) {
                    else -> {
                        currentSort = position
                        showGroupSlice()
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        reverseSort.isChecked = currentReverseSort
        reverseSort.setOnClickListener {
            currentReverseSort = reverseSort.isChecked
            showGroupSlice()
        }

        shuffleBtn.setOnClickListener {
            viewModel.shuffleSlice()
        }

        hideShowBySeq.setOnClickListener {
            val movedSliceList = viewModel.sliceList.toMutableList()
            if (currentShowHide) {
                movedSliceList.forEach { slice ->
                    if (slice.hide && slice.seq > 0) {
                        slice.hide = false
                        viewModel.updateSlice(slice)
                    }
                }
            } else {
                movedSliceList.forEach { slice ->
                    if (!slice.hide && slice.seq > 0) {
                        slice.hide = true
                        viewModel.updateSlice(slice)
                    }
                }
            }
            movedSliceList.removeAll { s -> s.hide == !currentShowHide }
            if (movedSliceList != viewModel.sliceList) {
                viewModel.setOfSliceList(movedSliceList)
            }
        }


        lightMode.setOnClickListener {
            State.nightMode = 0
            nightModeSelector(0)
        }
        nightMode.setOnClickListener {
            State.nightMode = 1
            nightModeSelector(1)
        }
        autoNightMode.setOnClickListener {
            State.nightMode = 2
            nightModeSelector(2)
        }

        /* recyclerView */
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        rvAdapter = SliceAdapter(viewModel.sliceList, viewModel.sliceGroupList)
        recyclerView.adapter = rvAdapter

        viewModel.sliceListLiveData.observe(this) {
            rvAdapter.notifyDataSetChanged()
            val str = String.format("%s%s (%d)",
                if (currentGroup == "") resources.getString(R.string.all) else currentGroup,
                if (currentShowHide) " [" + resources.getString(R.string.hide) + "]" else "",
                it.count())
            supportActionBar?.subtitle = str
        }

        viewModel.sliceGroupListLiveData.observe(this) {
            spAdapter.notifyDataSetChanged()
        }

        bulkOperationOnFlippedSlice.setOnClickListener {
            State.tempSliceList.clear()
            for (i in 0 until rvAdapter.itemCount) {
                val view = layoutManager.getChildAt(i) as View
                val holder = recyclerView.getChildViewHolder(view) as SliceAdapter.ViewHolder
                if (holder.flipped) {
                    State.tempSliceList.add(rvAdapter.sliceList[i])
                    //Log.d("MainActivity", rvAdapter.sliceList[i].toString())
                }
            }
            if (State.tempSliceList.isEmpty()) {
                Toast.makeText(this, resources.getString(R.string.no_slice_flipped), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this).apply {
                setTitle("${resources.getString(R.string.bulk_operation_on_flipped_slice)} (${State.tempSliceList.count()})")
                setItems(arrayOf(
                    resources.getString(R.string.prior),
                    resources.getString(R.string.delete),
                    resources.getString(R.string.hide_show),
                    resources.getString(R.string.seq)
                ),
                    DialogInterface.OnClickListener { _, i ->
                        when (i) {
                            0 -> bulkOpPrior()
                            1 -> bulkOpDelete()
                            2 -> bulkOpHideShow()
                            3 -> bulkOpSeq()
                            else -> State.tempSliceList.clear()
                        }
                    })
            }.show()
        }

    }

    private fun requestReadStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You denied permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bulkOpSeq() {
        val res = resources
        val editLine = EditText(this)
        editLine.inputType = InputType.TYPE_CLASS_NUMBER // only allowed input number
        AlertDialog.Builder(this).apply {
            setView(editLine)
            setTitle(res.getString(R.string.seq_input_title))
            setMessage(res.getString(R.string.seq_input_message))
            setPositiveButton(res.getString(R.string.OK), DialogInterface.OnClickListener { _, _ ->
                val s = editLine.text.toString()
                if (s != "") {
                    val seqNum = s.toInt()
                    for (slice in State.tempSliceList) {
                        val index = viewModel.sliceList.indexOf(slice)
                        viewModel.updateSlice(slice)
                        viewModel.sliceList.find { it.id == slice.id }?.seq = seqNum
                        rvAdapter.notifyItemChanged(index)
                    }
                }
            })
            setNegativeButton(res.getString(R.string.cancel), null)
        }.show()
    }

    private fun bulkOpHideShow() {
        for (slice in State.tempSliceList) {
            viewModel.sliceList.remove(slice)
            slice.hide = !slice.hide
            viewModel.updateSlice(slice)
            //viewModel.sliceList.find { it.id == slice.id }?.hide = slice.hide
        }
        rvAdapter.notifyDataSetChanged()
    }

    private fun bulkOpDelete() {
        AlertDialog.Builder(this).apply {
            setTitle("${resources.getString(R.string.bulk_delete)} (${State.tempSliceList.count()})")
            setMessage(resources.getString(R.string.caution_cant_undo))
            setPositiveButton(resources.getString(R.string.continue_do), DialogInterface.OnClickListener { _,_->
                for (slice in State.tempSliceList) {
                    viewModel.deleteSlice(slice)
                }
            })
            setNegativeButton(resources.getString(R.string.cancel), null)
        }.show()
        rvAdapter.notifyDataSetChanged()
    }

    private fun bulkOpPrior() {
        val res = resources
        val priorityList = arrayOf(
            res.getString(R.string.prior_critical),
            res.getString(R.string.prior_high),
            res.getString(R.string.prior_medium),
            res.getString(R.string.prior_normal),
            res.getString(R.string.prior_low)
        )
        val priorityI2V = arrayOf(3, 2, 1, 0, -1) // index to value
        // don't use message in there
        AlertDialog.Builder(this).apply {
            setTitle(res.getString(R.string.prior_dialog_title))
            setItems(priorityList, DialogInterface.OnClickListener { _, i ->
                for (slice in State.tempSliceList) {
                    val index = viewModel.sliceList.indexOf(slice)
                    slice.prior = priorityI2V[i]
                    viewModel.updateSlice(slice)
                    viewModel.sliceList.find { it.id == slice.id }?.prior = slice.prior
                    rvAdapter.notifyItemChanged(index)
                }
            })
        }.show()

    }

    private fun showGroupSlice() {
        //Log.d("MainActivity", "showSlices: $who")
        viewModel.loadSlices(currentGroup, currentShowHide, currentSort, currentReverseSort)
    }

    private fun nightModeSelector(i: Int) {
        when (i) {
            0 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
            1 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            2 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    private fun saveLastOpenTime(reminder: Long) {
        getSharedPreferences("global_state", Context.MODE_PRIVATE).edit {
            putLong("last_open_time", Date().time + reminder)
        }
    }

    private fun readLastOpenTime() {
        val globalState = getSharedPreferences("global_state", Context.MODE_PRIVATE)
        State.lastOpenTime = globalState.getLong("last_open_time", -1L)
    }

    private fun calcCountdown() {
        readLastOpenTime()
        if (State.lastOpenTime == -1L) {
            saveLastOpenTime(0L)
            return
        }
        val nowTime = Date().time
        val passedSec = (nowTime - State.lastOpenTime) / 1000
        val step = (State.sCountdownStep[0] * 86400
                + State.sCountdownStep[1] * 3600
                + State.sCountdownStep[2] * 60
                + State.sCountdownStep[3])
        if (step == 0) {
            return
        }
        val n = (passedSec / step).toInt()
        //Log.d("MainActivity", "$passedSec, $step, $n")
        if (n >= 1) {
            viewModel.refreshSeq(n)
            saveLastOpenTime(passedSec - n * step)
        }
    }

    private fun restoreFromSettings() {
        val settingPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        State.apply {
            sFontSize = settingPrefs.getInt("text_size", 24)
            sTouchAction[0] = settingPrefs.getInt("touch_long", 5)
            sTouchAction[1] = settingPrefs.getInt("touch_swipe_left", 0)
            sTouchAction[2] = settingPrefs.getInt("touch_swipe_right", 0)
            sCountdownStep[0] = settingPrefs.getInt("count_down_1", 0)
            sCountdownStep[1] = settingPrefs.getInt("count_down_2", 0)
            sCountdownStep[2] = settingPrefs.getInt("count_down_3", 0)
            sCountdownStep[3] = settingPrefs.getInt("count_down_4", 0)
            sSeqBtn[0] = settingPrefs.getInt("seq_btn_1", 1)
            sSeqBtn[1] = settingPrefs.getInt("seq_btn_2", 2)
            sSeqBtn[2] = settingPrefs.getInt("seq_btn_3", 3)
            sSeqBtn[3] = settingPrefs.getInt("seq_btn_4", 4)
        }
    }

    private fun restoreFromPrefs() {
        val currentState = getSharedPreferences("current_state", Context.MODE_PRIVATE)
        currentGroup = currentState.getString("cGroup", "").toString()
        currentShowHide = currentState.getBoolean("cShowHide", false)
        currentSort = currentState.getInt("cSort", 0)
        currentReverseSort = currentState.getBoolean("cReverseSort", false)
        State.nightMode = currentState.getInt("cNightMode", 0)
    }

    private fun saveToPrefs() {
        getSharedPreferences("current_state", Context.MODE_PRIVATE).edit {
            putString("cGroup", currentGroup)
            putBoolean("cShowHide", currentShowHide)
            putInt("cSort", currentSort)
            putBoolean("cReverseSort", currentReverseSort)
            putInt("cNightMode", State.nightMode)
        }
    }

    private fun restoreFromBundle(b: Bundle?) {
        if (b != null) {
            currentGroup = b.getString("cGroup").toString()
            currentShowHide = b.getBoolean("cShowHide")
            currentSort = b.getInt("cSort")
            currentReverseSort = b.getBoolean("cReverseSort")
            State.nightMode = b.getInt("cNightMode")
        }
    }

    override fun onDestroy() {
        saveToPrefs()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("cGroup", currentGroup)
        outState.putBoolean("cShowHide", currentShowHide)
        outState.putInt("cSort", currentSort)
        outState.putBoolean("cReverseSort", currentReverseSort)
        outState.putInt("cNightMode", State.nightMode)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoreFromBundle(savedInstanceState)
    }

    /*
   * TOOLBAR MENU
   * */

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val newSlices = it.data?.getParcelableArrayListExtra<Slice>("new_slices")
            viewModel.addNewSlices(newSlices as MutableList<Slice>, currentGroup)
        } else {
            Log.d("MainActivity", "result code: ${it.resultCode}")
        }
    }
    private val getSavedFile = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }
        applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use { fd ->
            FileOutputStream(fd.fileDescriptor).use {
                //it.write("OK ${System.currentTimeMillis()}\n".toByteArray())
                val jsonList : String = Gson().toJson(State.tempSliceList)
                it.write(jsonList.toByteArray())
            }
            State.tempSliceList.clear()
        }
    }
    private val getBackupFile = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uriList ->
        val newGroupList = mutableSetOf<String>()
        for (uri in uriList) {
            applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                val content = StringBuilder()

                FileInputStream(fd.fileDescriptor).use { fis ->
                    BufferedReader(fis.reader()).forEachLine {
                        content.append(it)
                    }
                }
                val gson = Gson()
                val typeOf = object : TypeToken<List<Slice>>() {}.type
                val bkList = gson.fromJson<List<Slice>>(content.toString(), typeOf)
                bkList.forEach { s ->
                    MainViewModel().addUniqueSlice(s)
                    newGroupList.add(s.group)
                }
            }
        }
        if (uriList.isNotEmpty()) {
            //restartActivity()
            newGroupList.addAll(viewModel.sliceGroupList)
            viewModel.sliceGroupList.clear()
            viewModel.sliceGroupList.addAll(newGroupList)
            spAdapter.notifyDataSetChanged()
        }
    }
    private fun exportToJsonFile() {
        viewModel.cacheGroupSlice(currentGroup)
        var bkpFileName = "SliceNoteBackupFull.json"
        if (currentGroup != "") {
            bkpFileName = "SliceNoteBackup[${legalFileName(currentGroup)}].json"
        }
        getSavedFile.launch(bkpFileName)// doesn't overwrite any exist file
    }
    /*private fun restartActivity() {
        val intent = intent
        finish()
        startActivity(intent)
    }*/
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        // Build Version Code >= M
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            menu?.add(0, 400, 4, resources.getString(R.string.need_permission))
        }
        val searchViewItem = menu?.findItem(R.id.searchView)
        val searchView = searchViewItem?.actionView as SearchView
        searchView.queryHint = resources.getString(R.string.search)
        searchView.setOnSearchClickListener {
//            val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
//            searchEditText.setTextColor(ContextCompat.getColor(this, R.color.white))
//            searchEditText.setHintTextColor(ContextCompat.getColor(this, R.color.white))
            State.tempSliceList.clear()
            State.tempSliceList.addAll(viewModel.sliceList)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {
                return false
            }
            override fun onQueryTextSubmit(query: String?): Boolean {
                rvAdapter.filter.filter(query)
                //Toast.makeText(applicationContext, p0, Toast.LENGTH_SHORT).show()
                return false
            }
        })
        searchView.setOnCloseListener {
            viewModel.sliceList.clear()
            viewModel.sliceList.addAll(State.tempSliceList)
            State.tempSliceList.clear()
            false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findViewById<DrawerLayout>(R.id.drawerLayout).openDrawer(GravityCompat.START)
            }
            R.id.insert -> {
                val intent = Intent(this, EditorActivity::class.java)
                intent.putStringArrayListExtra("group_list", viewModel.sliceGroupList as ArrayList<String>)
                intent.putExtra("in_group", currentGroup)
                getResult.launch(intent)
            }
            R.id.exportList -> {
                AlertDialog.Builder(this).apply {
                    setTitle(resources.getString(R.string.choose_export_action))
                    setItems(arrayOf(resources.getString(R.string.just_export), resources.getString(R.string.export_and_remove)),
                        DialogInterface.OnClickListener {_, i ->
                        when(i) {
                            0 -> exportToJsonFile()
                            1 -> {
                                exportToJsonFile()
                                viewModel.removeGroup(currentGroup)
                            }
                        }
                    })
                }.show()
            }
            R.id.importList -> {
                getBackupFile.launch(arrayOf("application/json"))
            }
            R.id.settings -> {
                startActivity(Intent(this, SettingActivity::class.java))
            }
            400 -> {
                requestReadStoragePermission()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}