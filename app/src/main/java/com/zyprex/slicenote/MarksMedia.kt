package com.zyprex.slicenote

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

class MarksMedia(val context: Context, private val dirName: String, private val fName: String) {

    private val appDirName = "SliceNote"
    private val dirGroupName = legalFileName(dirName)
    private val fileName = legalFileName(fName)
    private val pathPattern = "$appDirName/$dirGroupName/$fileName."

    data class Image(val uri: Uri, val name: String, val size: Int, val width: Int, val height: Int)
    // DCIM/ Pictures/
    fun getImageList(): MutableList<Image> {
        val imageList = mutableListOf<Image>()
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
                if (path.contains(pathPattern)) {
                    // stores column values to local object
                    imageList += Image(contentUri, name, size, width, height)
                }
            }
        }
        return imageList
    }

    data class Audio(val uri: Uri, val title: String, val albumArt: Bitmap?)
    // Alarms/ Audiobooks/ Music/ Notifications/ Podcasts/ Ringtones/
    fun getAudioList(): MutableList<Audio> {
        val audioList = mutableListOf<Audio>()
        // read audio from media store
        val resolver = context.contentResolver
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            //MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
        )
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '$fileName.%'"
        //val selection = null
        val cursor = resolver.query(collection, projection, selection, null, null)
        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            //val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val albumIdColumn  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                //val name = cursor.getString(nameColumn)
                val title = cursor.getString(titleColumn)
                //val size = cursor.getInt(sizeColumn)
                val albumId = cursor.getInt(albumIdColumn)
                val path = cursor.getString(pathColumn)
                if (path.contains(pathPattern)) {
                    audioList += Audio(contentUri, title, getAlbumIdBitMap(albumId))
                }
            }
        }
        return audioList
    }
    private fun getAlbumIdBitMap(albumId: Int): Bitmap? {
        val projection = arrayOf("album_art")
        val cursor = context.contentResolver.query(
            Uri.parse("content://media/external/audio/albums/$albumId"),
            projection, null, null, null)
        var albumArt = ""
        cursor?.use { cursor ->
            while (cursor.moveToNext()) {
                albumArt = cursor.getString(0) ?: ""
            }
        }
        if (albumArt.isNotEmpty()) {
            return BitmapFactory.decodeFile(albumArt)
        }
        return null
    }
    //data class Video(val uri: Uri, val name: String)
    // DCIM/ Movies/ Pictures/
    data class Video(val uri: Uri, val name: String, val width: Int, val height: Int)
    fun getVideoList(): MutableList<Video> {
        val videoList = mutableListOf<Video>()
        val resolver = context.contentResolver
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATA,
        )
        val selection = "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = ?"
        val cursor = resolver.query(collection, projection, selection, arrayOf(dirGroupName), null)
        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val title = cursor.getString(titleColumn)
                val path = cursor.getString(pathColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                if (path.contains(pathPattern)) {
                    videoList += Video(contentUri, title, width, height)
                }
            }
        }
        return videoList
     }

}