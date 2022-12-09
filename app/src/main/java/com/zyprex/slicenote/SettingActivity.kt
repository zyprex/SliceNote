package com.zyprex.slicenote

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import java.util.Date


class SettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        when (State.nightMode) {
            0 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
            1 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            2 -> delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val settingPrefs: SharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val arrTouchAction = listOf(
            resources.getString(R.string.none),
            resources.getString(R.string.edit),
            resources.getString(R.string.prior),
            resources.getString(R.string.delete),
            resources.getString(R.string.hide_show),
            resources.getString(R.string.marks))

        var myTexSize = settingPrefs.getInt("text_size", 24)
        var myTouchLong = settingPrefs.getInt("touch_long", 5)
        var myTouchSwipeLeft = settingPrefs.getInt("touch_swipe_left", 0)
        var myTouchSwipeRight = settingPrefs.getInt("touch_swipe_right", 0)
        var myCountdown1 = settingPrefs.getInt("count_down_1", 0)
        var myCountdown2 = settingPrefs.getInt("count_down_2", 0)
        var myCountdown3 = settingPrefs.getInt("count_down_3", 0)
        var myCountdown4 = settingPrefs.getInt("count_down_4", 0)
        var mySeqBtn1 = settingPrefs.getInt("seq_btn_1", 1)
        var mySeqBtn2 = settingPrefs.getInt("seq_btn_2", 2)
        var mySeqBtn3 = settingPrefs.getInt("seq_btn_3", 3)
        var mySeqBtn4 = settingPrefs.getInt("seq_btn_4", 4)
        var myFilterFront = settingPrefs.getBoolean("filter_front", true)
        var myFilterBack = settingPrefs.getBoolean("filter_back", true)
        var myFilterMarks = settingPrefs.getBoolean("filter_marks", true)

        val sampleText = findViewById<TextView>(R.id.sampleText)
        val configTextSize = findViewById<SeekBar>(R.id.configTextSize)
        val configTouchLong = findViewById<Spinner>(R.id.configTouchLong)
        val configTouchSwipeLeft = findViewById<Spinner>(R.id.configTouchSwipeLeft)
        val configTouchSwipeRight = findViewById<Spinner>(R.id.configTouchSwipeRight)
        val configCountDown1 = findViewById<EditText>(R.id.configCountdown1)
        val configCountDown2 = findViewById<EditText>(R.id.configCountdown2)
        val configCountDown3 = findViewById<EditText>(R.id.configCountdown3)
        val configCountDown4 = findViewById<EditText>(R.id.configCountdown4)
        val configSeqBtn1 = findViewById<EditText>(R.id.configSeqBtn1)
        val configSeqBtn2 = findViewById<EditText>(R.id.configSeqBtn2)
        val configSeqBtn3 = findViewById<EditText>(R.id.configSeqBtn3)
        val configSeqBtn4 = findViewById<EditText>(R.id.configSeqBtn4)
        val configFilterFront = findViewById<CheckBox>(R.id.filterFieldFront)
        val configFilterBack = findViewById<CheckBox>(R.id.filterFieldBack)
        val configFilterMarks = findViewById<CheckBox>(R.id.filterFieldMarks)

        val saveSettings = findViewById<Button>(R.id.saveSettings)

        sampleText.setText(resources.getString(R.string.text_size) + myTexSize.toString() + resources.getString(R.string.dp))
        sampleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, myTexSize.toFloat())
        sampleText.requestFocus()
        configTextSize.progress = ((myTexSize - 8) / 0.42).toInt()

        configTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
                // min = 8dp, max = 50dp
                val newTextSize: Float = i.toFloat() * 0.42f + 8f
                sampleText.setText(resources.getString(R.string.text_size) + myTexSize.toString() + resources.getString(R.string.dp))
                sampleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, newTextSize)
                myTexSize = newTextSize.toInt()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val touchLongAdapter = ArrayAdapter(this, android.R.layout.preference_category, arrTouchAction)
        configTouchLong.apply {
            adapter = touchLongAdapter
            setSelection(myTouchLong)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (view?.id) {
                        else -> {
                            if (position != myTouchLong) {
                                myTouchLong = position
                            }
                        }
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?){}
            }
        }

        val touchSwipeLeftAdapter = ArrayAdapter(this, android.R.layout.preference_category, arrTouchAction)
        configTouchSwipeLeft.apply {
            adapter = touchSwipeLeftAdapter
            setSelection(myTouchSwipeLeft)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (view?.id) {
                        else -> {
                            if (position != myTouchSwipeLeft) {
                                myTouchSwipeLeft = position
                            }
                        }
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?){}
            }
        }

        val touchSwipeRightAdapter = ArrayAdapter(this, android.R.layout.preference_category, arrTouchAction)
        configTouchSwipeRight.apply {
            adapter = touchSwipeRightAdapter
            setSelection(myTouchSwipeRight)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (view?.id) {
                        else -> {
                            if (position != myTouchSwipeRight) {
                                myTouchSwipeRight = position
                            }
                        }
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?){}
            }
        }

        configCountDown1.setText(if (myCountdown1 == 0) "" else myCountdown1.toString())
        configCountDown2.setText(if (myCountdown2 == 0) "" else myCountdown2.toString())
        configCountDown3.setText(if (myCountdown3 == 0) "" else myCountdown3.toString())
        configCountDown4.setText(if (myCountdown4 == 0) "" else myCountdown4.toString())

        configSeqBtn1.setText(mySeqBtn1.toString())
        configSeqBtn2.setText(mySeqBtn2.toString())
        configSeqBtn3.setText(mySeqBtn3.toString())
        configSeqBtn4.setText(mySeqBtn4.toString())

        configFilterFront.isChecked = myFilterFront
        configFilterBack.isChecked = myFilterBack
        configFilterMarks.isChecked = myFilterMarks

        saveSettings.setOnClickListener {
            myCountdown1 = ed2int(configCountDown1)
            myCountdown2 = ed2int(configCountDown2)
            myCountdown3 = ed2int(configCountDown3)
            myCountdown4 = ed2int(configCountDown4)

            mySeqBtn1 = ed2int(configSeqBtn1)
            mySeqBtn2 = ed2int(configSeqBtn2)
            mySeqBtn3 = ed2int(configSeqBtn3)
            mySeqBtn4 = ed2int(configSeqBtn4)

            myFilterFront = configFilterFront.isChecked
            myFilterBack = configFilterBack.isChecked
            myFilterMarks = configFilterMarks.isChecked

            if (myCountdown1 != State.sCountdownStep[0] ||
                myCountdown2 != State.sCountdownStep[1] ||
                myCountdown3 != State.sCountdownStep[2] ||
                myCountdown4 != State.sCountdownStep[3]) {
                // reset last open time, due to countdown step changed
                val newLastOpenTime = Date().time
                getSharedPreferences("global_state", Context.MODE_PRIVATE).edit {
                    putLong("last_open_time", newLastOpenTime)
                }
                State.lastOpenTime = newLastOpenTime
            }

            settingPrefs.edit {
                putInt("text_size", myTexSize)
                putInt("touch_long", myTouchLong)
                putInt("touch_swipe_left", myTouchSwipeLeft)
                putInt("touch_swipe_right", myTouchSwipeRight)
                putInt("count_down_1", myCountdown1)
                putInt("count_down_2", myCountdown2)
                putInt("count_down_3", myCountdown3)
                putInt("count_down_4", myCountdown4)
                putInt("seq_btn_1", mySeqBtn1)
                putInt("seq_btn_2", mySeqBtn2)
                putInt("seq_btn_3", mySeqBtn3)
                putInt("seq_btn_4", mySeqBtn4)
                putBoolean("filter_front", myFilterFront)
                putBoolean("filter_back", myFilterBack)
                putBoolean("filter_marks", myFilterMarks)
            }
            State.apply {
                sFontSize = myTexSize
                sTouchAction[0] = myTouchLong
                sTouchAction[1] = myTouchSwipeLeft
                sTouchAction[2] = myTouchSwipeRight
                sCountdownStep[0] = myCountdown1
                sCountdownStep[1] = myCountdown2
                sCountdownStep[2] = myCountdown3
                sCountdownStep[3] = myCountdown4
                sSeqBtn[0] = mySeqBtn1
                sSeqBtn[1] = mySeqBtn2
                sSeqBtn[2] = mySeqBtn3
                sSeqBtn[3] = mySeqBtn4
                sFilterBox[0] = myFilterFront
                sFilterBox[1] = myFilterBack
                sFilterBox[2] = myFilterMarks
            }
        }
    }
    private fun ed2int(editText: EditText): Int {
        val str = editText.text.toString()
        return if (str.isEmpty()) 0 else str.toInt()
    }
}