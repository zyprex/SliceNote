package com.zyprex.slicenote

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    private var currentGroup: String = ""
    private var currentShowHide: Boolean = false
    private var currentSort: Int = 0
    private var currentReverseSort: Boolean = false

    private lateinit var rvAdapter: SliceAdapter  // recycler view adapter
    private lateinit var spAdapter: ArrayAdapter<String> // group name spinner adapter

    val mediaPlayer = MediaPlayer()

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
            groupListMenu(toolbar)
            //showGroupSlice()
            //Toast.makeText(this, resources.getString(R.string.refresh), Toast.LENGTH_SHORT).show()
        }
        toolbar.setOnLongClickListener {
            val showHide = findViewById<CheckBox>(R.id.showHide)
            showHide.performClick()
            true
        }
        /* drawerLayout */
        viewModel.updateSliceGroupList()

        val versionName = findViewById<TextView>(R.id.versionName)
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

        versionName.text = "v${BuildConfig.VERSION_NAME}"

        loadAll.setOnClickListener {
            showGroupSlice("")
        }
        renameGroup.setOnClickListener {
            if (currentGroup == "") {
                Toast.makeText(this, "?", Toast.LENGTH_SHORT).show()
            } else {
                val editLine = EditText(this)
                editLine.setText(currentGroup)
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
                            saveToPrefs("cGroup")
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
                        showGroupSlice(parent?.getItemAtPosition(position).toString())
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                //TODO("Not yet implemented")
            }
        }

        showHide.isChecked = currentShowHide
        hideShowBySeq.text = if (currentShowHide)
            resources.getString(R.string.show_all_seq_gt_0)
        else
            resources.getString(R.string.hide_all_seq_gt_0)
        showHide.setOnClickListener {
            currentShowHide = showHide.isChecked
            saveToPrefs("cShowHide")
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
                        saveToPrefs("cSort")
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
            saveToPrefs("cReverseSort")
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
        rvAdapter = SliceAdapter(this, viewModel.sliceList, viewModel.sliceGroupList)
        recyclerView.adapter = rvAdapter

        viewModel.sliceListLiveData.observe(this) {
            rvAdapter.notifyDataSetChanged()
            val str = String.format("%d %s", it.count(),
                if (currentShowHide) " [${resources.getString(R.string.hide)}]" else "")
            supportActionBar?.title =
                if (currentGroup == "") resources.getString(R.string.all) else currentGroup
            supportActionBar?.subtitle = str
        }

        viewModel.sliceGroupListLiveData.observe(this) {
            spAdapter.notifyDataSetChanged()
        }

        bulkOperationOnFlippedSlice.setOnClickListener {
            if (rvAdapter.flipList.isEmpty()) {
                Toast.makeText(this, resources.getString(R.string.no_slice_flipped), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this).apply {
                setTitle("${resources.getString(R.string.bulk_operation_on_flipped_slice)} (${rvAdapter.flipList.count()})")
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
                        }
                    })
            }.show()
        }

    }

    override fun onResume() {
        super.onResume()
        //showGroupSlice()
    }

    private fun groupListMenu(view: View) {
        PopupMenu(this, view).apply {
            for (item in viewModel.sliceGroupList) {
                if (item != currentGroup)
                    menu.add(0, 0, 0, item)
            }
            setOnMenuItemClickListener {
                when(it.itemId) {
                    0 -> showGroupSlice("${it.title}")
                }
                true
            }
        }.show()

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
                    for (slice in rvAdapter.flipList) {
                        val index = viewModel.sliceList.indexOf(slice)
                        viewModel.updateSlice(slice)
                        viewModel.sliceList.find { it.id == slice.id }?.seq = seqNum
                        rvAdapter.notifyItemChanged(index)
                    }
                    rvAdapter.flipList.clear()
                }
            })
            setNegativeButton(res.getString(R.string.cancel), null)
        }.show()

    }

    private fun bulkOpHideShow() {
        for (slice in rvAdapter.flipList) {
            viewModel.sliceList.remove(slice)
            slice.hide = !slice.hide
            viewModel.updateSlice(slice)
            //viewModel.sliceList.find { it.id == slice.id }?.hide = slice.hide
        }
        rvAdapter.flipList.clear()
        rvAdapter.notifyDataSetChanged()
    }

    private fun bulkOpDelete() {
        AlertDialog.Builder(this).apply {
            setTitle("${resources.getString(R.string.bulk_delete)} (${rvAdapter.flipList.count()})")
            setMessage(resources.getString(R.string.caution_cant_undo))
            setPositiveButton(resources.getString(R.string.continue_do), DialogInterface.OnClickListener { _,_->
                for (slice in rvAdapter.flipList) {
                    viewModel.deleteSlice(slice)
                }
                rvAdapter.flipList.clear()
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
            res.getString(R.string.prior_low),
            res.getString(R.string.prior_unset),
        )
        val priorityI2V = arrayOf(5, 4, 3, 2, 1, 0) // index to value
        // don't use message in there
        AlertDialog.Builder(this).apply {
            setTitle(res.getString(R.string.prior_dialog_title))
            setItems(priorityList, DialogInterface.OnClickListener { _, i ->
                for (slice in rvAdapter.flipList) {
                    val index = viewModel.sliceList.indexOf(slice)
                    slice.prior = priorityI2V[i]
                    viewModel.updateSlice(slice)
                    viewModel.sliceList.find { it.id == slice.id }?.prior = slice.prior
                    rvAdapter.notifyItemChanged(index)
                }
                rvAdapter.flipList.clear()
            })
        }.show()

    }

    private fun showGroupSlice() {
        //Log.d("MainActivity", "showSlices: $who")
        viewModel.loadSlices(currentGroup, currentShowHide, currentSort, currentReverseSort)
        rvAdapter.flipList.clear()
    }

    private fun showGroupSlice(group: String) {
        currentGroup = group
        saveToPrefs("cGroup")
        showGroupSlice()
    }

    private fun nightModeSelector(i: Int) {
        when (i) {
            0 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
            1 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            2 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        saveToPrefs("cNightMode")
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

    private fun saveToPrefs(prefName: String?) {
        getSharedPreferences("current_state", Context.MODE_PRIVATE).edit {
            when (prefName) {
                "cGroup" -> putString("cGroup", currentGroup)
                "cShowHide" -> putBoolean("cShowHide", currentShowHide)
                "cSort" -> putInt("cSort", currentSort)
                "cReverseSort" -> putBoolean("cReverseSort", currentReverseSort)
                "cNightMode" -> putInt("cNightMode", State.nightMode)
                else -> {}
            }
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
        //saveToPrefs()
        super.onDestroy()
        val marksLinearLayout = findViewById<LinearLayout>(R.id.marksLinearLayout)
        if (marksLinearLayout != null) {
            if (marksLinearLayout.findViewById<LinearLayout>(R.id.marksAudioView) != null) {
                mediaPlayer.reset()
                mediaPlayer.release()
            }
            marksLinearLayout.findViewById<VideoView>(R.id.videoView)?.suspend()
        }
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

    val getResultFromEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            Log.d("MainActivity", it.data.toString())
            val newSlicesCnt = it.data?.getIntExtra("new_slices_count", 0) as Int
            val hasNewGroup = it.data?.getBooleanExtra("has_new_group", false) as Boolean
            val lastModifyGroup = it.data?.getStringExtra("last_modify_group") as String
            if (newSlicesCnt == 0) {
                return@registerForActivityResult
            } else {
                Toast.makeText(this, "+ ${newSlicesCnt}", Toast.LENGTH_SHORT).show()
            }
            if (hasNewGroup) {
                Toast.makeText(this, resources.getString(R.string.create_new_group), Toast.LENGTH_SHORT).show()
                viewModel.updateSliceGroupList()
            }
            if (lastModifyGroup.isNotEmpty()) {
                showGroupSlice(lastModifyGroup)
            } else {
                showGroupSlice(State.defaultGroupName)
            }
        } else {
            Log.d("MainActivity", "result code: ${it.resultCode}")
        }
    }

    val getResultFromEditor2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val hasNewGroup = it.data?.getBooleanExtra("has_new_group", false) as Boolean
            val lastModifyGroup = it.data?.getStringExtra("last_modify_group") as String
            val untouched = it.data?.getBooleanExtra("untouched", true) as Boolean
            if (untouched) return@registerForActivityResult
            if (hasNewGroup) {
                Toast.makeText(this, resources.getString(R.string.create_new_group), Toast.LENGTH_SHORT).show()
                viewModel.updateSliceGroupList()
            }
            if (lastModifyGroup.isNotEmpty()) {
                showGroupSlice(lastModifyGroup)
            } else {
                showGroupSlice(State.defaultGroupName)
            }
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
        val groupList = mutableSetOf<String>()
        for (uri in uriList) {
            applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                val content = StringBuilder()
                FileInputStream(fd.fileDescriptor).use { fis ->
                    BufferedReader(fis.reader()).forEachLine {
                        content.append(it)
                    }
                }
                val bkList = importStringAsData(content.toString())
                bkList.forEach { s ->
                    groupList.add(s.group)
                }
            }
        }
        if (uriList.isNotEmpty()) {
            //restartActivity()
            changeGroupList(groupList)
        }
    }
    private fun getDataFromClipText(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val description = clipboard.primaryClipDescription
        if (description != null) {
            if (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                val clipData = clipboard.primaryClip as ClipData
                val clipItem = clipData.getItemAt(0) as ClipData.Item
                val text = clipItem.text.toString()
                // get clip text successful!
                val groupList = mutableSetOf<String>()
                val bkList = importStringAsData(text)
                bkList.forEach { s -> groupList.add(s.group) }
                changeGroupList(groupList)
            }
        }
    }
    private fun changeGroupList(groupList: MutableSet<String>) {
        val newGroupList = mutableSetOf<String>()
        newGroupList.addAll(viewModel.sliceGroupList)
        newGroupList.addAll(groupList)
        viewModel.sliceGroupList.clear()
        viewModel.sliceGroupList.addAll(newGroupList)
        spAdapter.notifyDataSetChanged()
    }
    private fun importStringAsData(string: String): List<Slice> {
        val gson = Gson()
        val typeOf = object : TypeToken<List<Slice>>() {}.type
        try {
            val bkList = gson.fromJson<List<Slice>>(string, typeOf)
            MainViewModel().addUniqueSlices(bkList)
            return bkList
        } catch (e: JsonSyntaxException) {
            Toast.makeText(this, "ERROR: $e", Toast.LENGTH_LONG).show()
        }
        return listOf()

    }
    private fun exportToJsonFile() {
        viewModel.cacheGroupSlice(currentGroup)
        var bkpFileName = "SliceNoteBackupFull.json"
        if (currentGroup != "") {
            bkpFileName = "SliceNoteBackup[${legalFileName(currentGroup)}].json"
        }
        getSavedFile.launch(bkpFileName)// doesn't overwrite any exist file
    }
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
            State.tempSliceList.clear()
            State.tempSliceList.addAll(viewModel.sliceList)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {
                return false
            }
            override fun onQueryTextSubmit(query: String?): Boolean {
                rvAdapter.filter.filter(query)
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
                EditorActivity.launchWith(this, viewModel.sliceGroupList as ArrayList<String>, currentGroup)
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
                AlertDialog.Builder(this).apply {
                    setTitle(resources.getString(R.string.choose_import_action))
                    setItems(arrayOf(
                        resources.getString(R.string.import_from_file_m),
                        resources.getString(R.string.import_from_clipboard),
                    ), DialogInterface.OnClickListener { _, i ->
                        when(i) {
                            0 -> getBackupFile.launch(arrayOf("application/json"))
                            1 -> getDataFromClipText(context)
                        }
                    })
                }.show()
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