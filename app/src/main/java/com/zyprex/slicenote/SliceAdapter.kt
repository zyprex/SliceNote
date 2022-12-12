package com.zyprex.slicenote

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.Keep
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import java.util.ArrayList

class SliceAdapter(var sliceList: MutableList<Slice>, private var groupList: MutableList<String>) :
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

        fun menuActionSelector(which: Int): Boolean { /* local function */
            when (which) {
                1 -> menuActionEdit(holder.mContext, slice, slice.id, groupList as ArrayList<String>)
                2 -> menuActionPrior(holder.mContext, slice, position)
                3 -> menuActionDelete(holder.itemView, slice, position)
                4 -> menuActionHide(slice, position)
                5 -> menuActionMarks(holder.mContext, slice)
            }
            return true
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
        holder.sliceText.text = slice.front
        holder.flipped = false
        holder.sliceText.setOnClickListener {
            if (!holder.flipped) {
                holder.sliceText.text = slice.back
                holder.seqSelectLayout.visibility = View.VISIBLE
                holder.flipped = true
                flipList.add(slice)
            } else {
                holder.sliceText.text = slice.front
                holder.seqSelectLayout.visibility = View.INVISIBLE
                holder.flipped = false
                flipList.remove(slice)
            }
        }
        holder.sliceText.setOnLongClickListener {
            //menuActionMarks(holder.mContext, slice.marks)
            menuActionSelector(State.sTouchAction[0])
        }

        var startX = 0f
        var once = true
        val longClickTime = 500
        val swipeMoveLen = 100
        holder.sliceText.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action){
                MotionEvent.ACTION_DOWN -> {
                    //Log.d("SliceAdapter", "DOWN ${motionEvent.x}, ${motionEvent.y} ")
                    startX = motionEvent.x
                    once = true
                }
                MotionEvent.ACTION_MOVE -> {
                    //Log.d("SliceAdapter", "MOVE ${motionEvent.x}, ${motionEvent.y} ")
                    //Log.d("SliceAdapter", "downTime: ${motionEvent.downTime}")
                    //Log.d("SliceAdapter", "eventTime: ${motionEvent.eventTime}")
                    if (motionEvent.eventTime - motionEvent.downTime  > longClickTime
                        && motionEvent.x - startX < swipeMoveLen
                        && motionEvent.x - startX > -swipeMoveLen
                        && once) {
                        view.performLongClick()
                        once = false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // Log.d("SliceAdapter", "E cnt: ${motionEvent.pointerCount} ")
                    //Log.d("SliceAdapter", "UP ${motionEvent.x}, ${motionEvent.y} ")
                    if (motionEvent.x - startX >= swipeMoveLen) {
                        //Toast.makeText(holder.mContext, "swipe to right", Toast.LENGTH_SHORT).show()
                        menuActionSelector(State.sTouchAction[2])

                    } else if (motionEvent.x - startX <= -swipeMoveLen) {
                        //Toast.makeText(holder.mContext, "swipe to left", Toast.LENGTH_SHORT).show()
                        menuActionSelector(State.sTouchAction[1])
                    } else {
                        if (motionEvent.eventTime - motionEvent.downTime  <= longClickTime) {
                            view.performClick()
                        }
                    }
                }
            }
            true
        }

        if (State.sTouchAction[0] == 0 && State.sTouchAction[1] == 0 && State.sTouchAction[2] == 0) {
            holder.sliceText.setOnLongClickListener(null)
            holder.sliceText.setOnTouchListener(null)
        }

        /* item btn */
        holder.sliceItemBtn.setBackgroundColor(Color.parseColor(when(slice.prior){
            3 -> "#FF4444"
            2 -> "#FFBB33"
            1 -> "#A4C639"
            0 -> "#2196F3"
            -1 -> "#607D84"
            else -> "#404040"
        }))

        holder.sliceItemBtn.setOnClickListener {
            val popup = PopupMenu(holder.mContext, holder.sliceItemBtn)
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
        if (slice.marks.isEmpty()) {
            val res = context.resources
            Toast.makeText(context, res.getString(R.string.empty_marks), Toast.LENGTH_SHORT).show()
            return
        }
        BottomSheetDialog(context).apply {
            setContentView(R.layout.slice_marks)
            val textView = findViewById<TextView>(R.id.sliceMarksText)
            textView?.text = slice.marks
            textView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, State.sFontSize.toFloat())
            val img = imageInMarks(context, slice)
            if (img.isNotEmpty()) {
               /* val imgView = ImageView(context)
                imgView.setImageURI(img[0].uri)*/
                /*imgView.layoutParams.width = LayoutParams.MATCH_PARENT
                imgView.layoutParams.height = LayoutParams.WRAP_CONTENT*/
                val linearLayout = findViewById<LinearLayout>(R.id.marksLinearLayout) as LinearLayout
                val inflater = layoutInflater
                val imgView = inflater.inflate(R.layout.slice_marks_img, linearLayout, false) as ImageView
                linearLayout.addView(imgView)
                if (img[0].size >= 1024*1024*2) {
                    // image larger than 2M
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = false
                    options.inSampleSize = (img[0].size / (1024*1024*2)) + 1
                    val bitmap =
                        BitmapFactory.decodeStream(context.contentResolver.openInputStream(img[0].uri), null, options)
                    imgView.setImageBitmap(bitmap)
                }
                else {
                    val bitmap =
                        BitmapFactory.decodeStream(context.contentResolver.openInputStream(img[0].uri))
                    imgView.setImageBitmap(bitmap)
                }
                imgView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(img[0].uri, "image/*")
                    context.startActivity(intent)
                }
            }
        }.show()
    }
    data class MarksImage(val uri: Uri, val name: String, val size: Int, val width: Int, val height: Int)
    private fun imageInMarks(context: Context, slice: Slice): MutableList<MarksImage> {
        val imageList = mutableListOf<MarksImage>()
        if (slice.marks == "" || slice.group == "" || slice.front == "") {
            return imageList
        }
        val dirGroupName = legalFileName(slice.group)
        var imgName = legalFileName(slice.front)
        if (imgName.length > 100) {
            imgName = imgName.substring(0, 100)
        }
        val relativePath = "SliceNote/$dirGroupName/$imgName."
        // read image from media store
        val resolver = context.contentResolver
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATA
        )
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val cursor = resolver.query(collection, projection, selection, arrayOf(dirGroupName), null)
        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id)
                val name = cursor.getString(nameColumn)
                val size = cursor.getInt(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val path = cursor.getString(pathColumn)
                //Log.d("SliceAdapter", path)
                if (path.contains(relativePath)) {
                    // stores column values to local object
                    imageList += MarksImage(contentUri, name, size, width, height)
                }
            }
        }
        //Log.d("SliceAdapter", imageList.toString())
        return imageList
    }
    override fun getItemCount() = sliceList.size
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

