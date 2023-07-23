package com.zyprex.slicenote

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs

class SliceAdapter(
    var sliceList: MutableList<Slice>,
    private var groupList: MutableList<String>
) :
    RecyclerView.Adapter<SliceAdapter.ViewHolder>(), Filterable {

    private lateinit var delSlice: Slice
    var flipList = mutableSetOf<Slice>()


    inner class ViewHolder(val mContext: Context, view: View) : RecyclerView.ViewHolder(view) {
        val sliceText: TextView = view.findViewById(R.id.sliceText)
        val sliceItemBtn: ImageButton = view.findViewById(R.id.sliceItemBtn)
        val sliceSeqNum: TextView = view.findViewById(R.id.seqNum)
        val seqSelectLayout: LinearLayout = view.findViewById(R.id.seqSelectLayout)
        val seqNumBtn1: TextView = view.findViewById(R.id.seqNumSetBtn1)
        val seqNumBtn2: TextView = view.findViewById(R.id.seqNumSetBtn2)
        val seqNumBtn3: TextView = view.findViewById(R.id.seqNumSetBtn3)
        val seqNumBtn4: TextView = view.findViewById(R.id.seqNumSetBtn4)
        val seqNumBtn5: TextView = view.findViewById(R.id.seqNumSetBtn5)
        var flipped = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.slice_item, parent, false)
        return ViewHolder(parent.context, view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val slice = sliceList[position]
        val res = holder.mContext.resources

        /*setting restore start*/
        holder.sliceText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, State.sFontSize.toFloat())
        holder.seqNumBtn1.text = State.sSeqBtn[0].toString()
        holder.seqNumBtn2.text = State.sSeqBtn[1].toString()
        holder.seqNumBtn3.text = State.sSeqBtn[2].toString()
        holder.seqNumBtn4.text = State.sSeqBtn[3].toString()

        fun menuActionSelector(which: Int, end: Boolean = false): Boolean { /* local function */
            when (which) {
                1 -> menuActionEdit(holder.mContext, slice, slice.id, groupList as ArrayList<String>)
                2 -> menuActionPrior(holder.mContext, slice, position)
                3 -> menuActionDelete(holder.itemView, slice, position)
                4 -> menuActionHide(slice, position)
                5 -> menuActionMarks(holder.mContext, slice)
            }
            return end
        }
        /*setting restore end*/

        if (slice.seq != 0) {
            holder.sliceSeqNum.text = slice.seq.toString()
        } else {
            holder.sliceSeqNum.text = ""
        }

        /* seqNumBtn 1~5 */
        holder.seqSelectLayout.visibility = View.INVISIBLE
        fun onSeqNumChange(s: String) { /* local function */
            slice.seq = s.toInt()
            holder.sliceSeqNum.text = s
            MainViewModel().updateSlice(slice)
        }
        holder.seqNumBtn1.setOnClickListener { onSeqNumChange(holder.seqNumBtn1.text.toString()) }
        holder.seqNumBtn2.setOnClickListener { onSeqNumChange(holder.seqNumBtn2.text.toString()) }
        holder.seqNumBtn3.setOnClickListener { onSeqNumChange(holder.seqNumBtn3.text.toString()) }
        holder.seqNumBtn4.setOnClickListener { onSeqNumChange(holder.seqNumBtn4.text.toString()) }
        holder.seqNumBtn5.setOnClickListener {
            val editLine = EditText(holder.seqNumBtn5.context)
            editLine.inputType = InputType.TYPE_CLASS_NUMBER // only allowed input number
            AlertDialog.Builder(holder.seqNumBtn5.context)
                .setView(editLine)
                .setTitle(res.getString(R.string.seq_input_title))
                .setMessage(res.getString(R.string.seq_input_message))
                .setPositiveButton(res.getString(R.string.OK), DialogInterface.OnClickListener { _, _ ->
                    val s = editLine.text.toString()
                    if (s != "") {
                        onSeqNumChange(s)
                    }
                })
                .setNegativeButton(res.getString(R.string.cancel), null)
                .show()
        }

        /* slice text */
        holder.sliceText.text = prettyText(slice.front)
        holder.flipped = false
        fun onTextClick() : Boolean { /* local function */
            if (!holder.flipped) {
                holder.sliceText.text = prettyText(slice.back)
                holder.seqSelectLayout.visibility = View.VISIBLE
                holder.flipped = true
                flipList.add(slice)
            } else {
                holder.sliceText.text = prettyText(slice.front)
                holder.seqSelectLayout.visibility = View.INVISIBLE
                holder.flipped = false
                flipList.remove(slice)
            }
            return true
        }

        var startX = 0f
        var isFirst = true
        val longClickTime = ViewConfiguration.getLongPressTimeout()
        val swipeMoveLen = 50
        holder.sliceText.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action){
                MotionEvent.ACTION_DOWN -> {
                    startX = motionEvent.x
                    isFirst = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if ((motionEvent.eventTime - motionEvent.downTime  > longClickTime) &&
                            abs(motionEvent.x - startX) < swipeMoveLen) {
                        // long click
                        if (isFirst) {
                            isFirst = false
                            menuActionSelector(State.sTouchAction[0], true)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (motionEvent.x - startX <= -swipeMoveLen) {
                        //Toast.makeText(holder.mContext, "swipe to left", Toast.LENGTH_SHORT).show()
                        menuActionSelector(State.sTouchAction[1], true)
                    } else if (motionEvent.x - startX >= swipeMoveLen) {
                        //Toast.makeText(holder.mContext, "swipe to right", Toast.LENGTH_SHORT).show()
                        menuActionSelector(State.sTouchAction[2], true)
                    } else {
                        if (motionEvent.eventTime - motionEvent.downTime  <= longClickTime) {
                            //view.performClick()
                            onTextClick()
                        }
                    }
                }
            }
            false
        }

        /* item btn */
        fun priorColor(prior: Int): String  { /*inline*/
            return when(prior) {
                3 -> "#FF4444"
                2 -> "#FFBB33"
                1 -> "#A4C639"
                0 -> "#2196F3"
                -1 -> "#607D84"
                else -> "#404040"
            }
        }
        holder.sliceSeqNum.setBackgroundColor(Color.parseColor(priorColor(slice.prior)))
        holder.sliceItemBtn.setBackgroundColor(Color.parseColor(priorColor(slice.prior)))

        fun showSideMenu(view: View) { /*inline*/
            val popup = PopupMenu(holder.mContext, view)
            //popup.inflate(R.menu.popup)
            // groupId = 0, itemId = 0, orderId = 0, title = "..."
            popup.menu.apply {
                add(0, 0, 0, res.getString(R.string.edit))
                add(0, 1, 1, res.getString(R.string.prior))
                add(0, 2, 2, res.getString(R.string.delete))
                // load a hide list should have different menu item name
                // seq < 0 can't be hide
                if (slice.seq >= 0) {
                    add(0, 3, 3, if (slice.hide) res.getString(R.string.show) else res.getString(R.string.hide))
                }
                add(0, 4, 4, res.getString(R.string.marks))
                /*findItem(2).setTitle(SpannableString("delete")
                    .setSpan(ForegroundColorSpan(Color.RED), 0, 6, 0))*/
            }

            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    0 -> menuActionSelector(it.itemId + 1)
                    1 -> menuActionSelector(it.itemId + 1)
                    2 -> menuActionSelector(it.itemId + 1)
                    3 -> menuActionSelector(it.itemId + 1)
                    4 -> menuActionSelector(it.itemId + 1)
                    else -> false
                }
            }
            popup.show()
        }

        holder.sliceSeqNum.setOnClickListener { showSideMenu(holder.sliceSeqNum) }
        holder.sliceItemBtn.setOnClickListener { showSideMenu(holder.sliceItemBtn) }

        if (slice.marks.isNotEmpty() || slice.media > 0) {
            holder.sliceItemBtn.setImageResource(R.drawable.baseline_segment_24)
        } else {
            holder.sliceItemBtn.setImageDrawable(null)
        }

    }

    private fun menuActionEdit(context: Context, slice: Slice, id: Long, groupList: ArrayList<String>) {
        Editor2Activity.actionStart(context, slice, id, groupList)
    }
    private fun menuActionPrior(context: Context, slice: Slice, position: Int) {
        val res = context.resources
        val priorityList = arrayOf(
            res.getString(R.string.prior_critical),
            res.getString(R.string.prior_high),
            res.getString(R.string.prior_medium),
            res.getString(R.string.prior_normal),
            res.getString(R.string.prior_low)
        )
        val priorityI2V = arrayOf(3, 2, 1, 0, -1) // index to value
        // don't use message in there
        AlertDialog.Builder(context).apply {
            setTitle(res.getString(R.string.prior_dialog_title))
            setItems(priorityList, DialogInterface.OnClickListener { _, i ->
                slice.prior = priorityI2V[i]
                notifyItemChanged(position)
                MainViewModel().updateSlice(slice)
            })
        }.show()
    }
    private fun menuActionDelete(view: View, slice: Slice, position: Int) {
        val res = view.context.resources
        delSlice = slice
        sliceList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, sliceList.size)
        Snackbar.make(view, res.getString(R.string.deleted), Snackbar.LENGTH_LONG)
            .setAction(res.getString(R.string.undo)) {
                sliceList.add(position, delSlice)
                notifyItemInserted(position)
                notifyItemRangeChanged(position, sliceList.size)
            }
            .addCallback(object: Snackbar.Callback() {
                override fun onDismissed(
                    transientBottomBar: Snackbar?,
                    event: Int
                ) {
                    super.onDismissed(transientBottomBar, event)
                    if (event == DISMISS_EVENT_SWIPE ||
                        event == DISMISS_EVENT_TIMEOUT ||
                        event == DISMISS_EVENT_MANUAL ||
                        event == DISMISS_EVENT_CONSECUTIVE) {
                        MainViewModel().deleteSlice(slice)
                    }
                }
            }).show()
    }
    private fun menuActionHide(slice: Slice, position: Int) {
        slice.hide = !slice.hide
        sliceList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, sliceList.size)
        MainViewModel().updateSlice(slice)
    }
    private fun menuActionMarks(context: Context, slice: Slice) {
        if (slice.marks.isEmpty() && slice.media == 0) {
            val res = context.resources
            Toast.makeText(context, res.getString(R.string.empty_marks), Toast.LENGTH_SHORT).show()
            return
        }
        val mediaPlayer = MainActivity().mediaPlayer
        val timer = Timer()
        val timer2 = Timer()
        val bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setOnDismissListener {
            val audioView = bottomSheetDialog.findViewById<LinearLayout>(R.id.marksAudioView)
            if (audioView != null) {
                mediaPlayer.reset()
                mediaPlayer.release()
                timer.cancel()
            }
            val videoView = bottomSheetDialog.findViewById<VideoView>(R.id.videoView)
            videoView?.suspend()
            if (videoView != null) {
                timer2.cancel()
            }
        }
        bottomSheetDialog.apply {
            setContentView(R.layout.slice_marks)
            findViewById<View>(R.id.bottomSheetDialogClose)?.setOnClickListener {
                dismiss()
            }
            if (slice.marks.isNotEmpty()) {
                displayTextInMarks(this, slice.marks)
            }
            if (slice.media == 0) {
                return show()
            }
            val marksMedia = MarksMedia(context, slice.group, slice.front)
            if (arrayOf(1, 3, 5, 7).contains(slice.media)) {
                val img = marksMedia.getImageList()
                if (img.isNotEmpty()) {
                    displayImageInMarks(this, img[0])
                }
            }
            if (arrayOf(2, 3, 6, 7).contains(slice.media)) {
                val aud = marksMedia.getAudioList()
                if (aud.isNotEmpty()) {
                    displayAudioInMarks(this, aud[0], mediaPlayer, timer)
                }
            }
            if (arrayOf(4, 5, 6, 7).contains(slice.media)) {
                val vid = marksMedia.getVideoList()
                if (vid.isNotEmpty()) {
                    displayVideoInMarks(this, vid[0], timer2)
                }
            }
        }.show()
    }
    private fun displayExtraView(context: Context, view: BottomSheetDialog, resId: Int): View {
        val linearLayout = view.findViewById<LinearLayout>(R.id.marksLinearLayout)
        val inflater = LayoutInflater.from(context)
        val newView = inflater.inflate(resId, linearLayout, false)
        linearLayout?.addView(newView)
        return newView
    }
    private fun displayTextInMarks(view: BottomSheetDialog, txt: String) {
        val context = view.context
        val txtView = displayExtraView(context, view, R.layout.slice_marks_txt) as TextView
        txtView.text = txt
        txtView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, State.sFontSize.toFloat())
    }
    private fun displayImageInMarks(view: BottomSheetDialog, img: MarksMedia.Image) {
        val context = view.context
        val imgView = displayExtraView(context, view, R.layout.slice_marks_img) as ImageView
        if (img.size >= 1024*1024*2) {
            // image larger than 2M
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            options.inSampleSize = (img.size / (1024*1024*2)) + 1
            val bitmap =
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(img.uri), null, options)
            imgView.setImageBitmap(bitmap)
        }
        else {
            val bitmap =
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(img.uri))
            imgView.setImageBitmap(bitmap)
        }
        imgView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(img.uri, "image/*")
            context.startActivity(intent)
        }
    }
    private fun displayAudioInMarks(view: BottomSheetDialog, aud: MarksMedia.Audio, mediaPlayer: MediaPlayer, timer: Timer) {
        val context = view.context
        val audView = displayExtraView(context, view, R.layout.slice_marks_aud) as LinearLayout
        fun initMediaPlayer(aud: MarksMedia.Audio) { /* local */
            mediaPlayer.apply{
                setDataSource(context, aud.uri)
                prepareAsync()
            }
        }
        val albumArt = audView.findViewById<ImageView>(R.id.albumArt)
        val audioTitle = audView.findViewById<TextView>(R.id.audioTitle)
        val audioSeekBar = audView.findViewById<SeekBar>(R.id.audioSeekBar)
        val audioPlay = audView.findViewById<ImageButton>(R.id.audioPlay)
        val audioStop = audView.findViewById<ImageButton>(R.id.audioStop)
        val audioOpenBy = audView.findViewById<ImageButton>(R.id.audioOpenBy)
        audioOpenBy.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(aud.uri, "audio/*")
            context.startActivity(intent)
        }
        if (aud.albumArt != null) {
            albumArt.setImageBitmap(aud.albumArt)
        }
        initMediaPlayer(aud)
        mediaPlayer.setOnPreparedListener {
            audioSeekBar.max = mediaPlayer.duration
            audioPlay.setImageDrawable(imageOnButton(context, R.drawable.ic_baseline_play_arrow_24))
            audioTitle.text = aud.title
            audioPlay.setOnClickListener {
                if (!mediaPlayer.isPlaying) {
                    audioPlay.setImageDrawable(imageOnButton(context, R.drawable.ic_baseline_pause_24))
                    mediaPlayer.start()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            audioSeekBar.progress = mediaPlayer.currentPosition
                        }
                    }, 0, 20)
                } else {
                    audioPlay.setImageDrawable(imageOnButton(context, R.drawable.ic_baseline_play_arrow_24))
                    mediaPlayer.pause()
                }
            }
            audioStop.setOnClickListener {
                mediaPlayer.reset()
                initMediaPlayer(aud)
            }
            audioSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(SeekBar: SeekBar?, i: Int, b: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.progress?.let { it -> mediaPlayer.seekTo(it) }
                }
            })
        }
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.reset()
            initMediaPlayer(aud)
            audioPlay.setImageDrawable(imageOnButton(context, R.drawable.ic_baseline_play_arrow_24))
        }
        mediaPlayer.setOnErrorListener { mediaPlayer, _, _ ->
            mediaPlayer.reset()
            initMediaPlayer(aud)
            true
        }
    }
    private fun displayVideoInMarks(view: BottomSheetDialog, vid: MarksMedia.Video, timer: Timer) {
        val context = view.context
        val vidView = displayExtraView(context, view, R.layout.slice_marks_vid) as LinearLayout
        val videoView = vidView.findViewById<VideoView>(R.id.videoView)

        val videoPlay = vidView.findViewById<ImageView>(R.id.videoPlay)
        val videoOpenBy = vidView.findViewById<ImageView>(R.id.videoOpenBy)
        val videoSeekBar = vidView.findViewById<SeekBar>(R.id.videoSeekBar)


        val videoViewContainer = vidView.findViewById<FrameLayout>(R.id.videoViewContainer)
        val display = context.resources.displayMetrics
        var videoHeight = display.widthPixels * vid.height / vid.width
        if (videoHeight > display.heightPixels) {
            videoHeight = display.heightPixels
        }
        //Log.d("SliceAdapter", "video(w*h): ${vid.width}*${vid.height}, screen(w*h): ${display.widthPixels}*${display.heightPixels}")
        videoViewContainer.layoutParams.height = videoHeight

        videoView.setVideoURI(vid.uri)
        videoView.setZOrderOnTop(true)
        videoView.setOnPreparedListener {
            videoSeekBar.max = videoView.duration
            videoSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(SeekBar: SeekBar?, i: Int, b: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.progress?.let { it -> videoView.seekTo(it) }
                }
            })
            videoPlay.setOnClickListener {
                if (videoView.isPlaying) {
                    videoView.pause()
                    videoPlay.setImageDrawable(imageOnButton(context, R.drawable.ic_baseline_play_arrow_24))
                } else {
                    videoView.start()
                    videoPlay.setImageDrawable(imageOnButton(context, R.drawable.ic_baseline_pause_24))
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            videoSeekBar.progress = videoView.currentPosition
                        }
                    }, 0, 20)
                }
            }
        }

        videoView.setOnCompletionListener {
            videoPlay.setImageDrawable(imageOnButton(context, R.drawable.ic_baseline_play_arrow_24))
        }
        videoOpenBy.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(vid.uri, "video/*")
            context.startActivity(intent)
        }
    }

    private fun prettyText(str: String) : SpannableString {
        val spannableString = SpannableString(str)
        var isEven = true
        for (i in 0 until str.length)  {
            if (i > 0 && str[i - 1] == '\n') {
                var index = str.indexOf('\n', i)
                if (isEven) {
                    spannableString.setSpan(
                        ForegroundColorSpan(
                            ContextCompat.getColor(MyApplication.context, R.color.evenLineFg)
                        ),
                        i,
                        if (index == -1) str.length else index,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
                isEven = if (isEven) false else true
            }
        }
        return spannableString
    }

    override fun getItemCount() = sliceList.count()
    override fun getFilter(): Filter {
        return object : Filter() {
            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(chars: CharSequence?, filterResults: FilterResults?) {
                sliceList.clear()
                sliceList.addAll(filterResults?.values as MutableList<Slice>)
                notifyDataSetChanged()
            }

            override fun performFiltering(chars: CharSequence?): FilterResults {
                sliceList.clear()
                sliceList.addAll(State.tempSliceList)
                val queryString = chars?.toString()
                val filterResults = FilterResults()
                filterResults.values = if (queryString == null || queryString.isEmpty()) {
                    sliceList
                } else {
                    sliceList.filter {
                        (if (State.sFilterBox[0]) it.front.contains(queryString) else false) ||
                                (if (State.sFilterBox[1]) it.back.contains(queryString) else false) ||
                                (if (State.sFilterBox[2]) it.marks.contains(queryString) else false)
                    }
                }
                return filterResults
            }
        }
    }
}
