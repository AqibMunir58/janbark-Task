package com.example.janbarktask.repository

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class ImageRepository(private val context: Context) {

    fun getAllImages(): Flow<List<String>> = flow {
        val imagePaths = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getImagesFromMediaStore()
        } else {
            getImagesFromFileSystem()
        }
        emit(imagePaths)
    }

    private fun getImagesFromFileSystem(): List<String> {
        val directory = File(context.getExternalFilesDir(null)?.parent, "Screenshots")
        val imagePaths = mutableListOf<String>()
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    imagePaths.add(file.absolutePath)
                }
            }
        }
        return imagePaths
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getImagesFromMediaStore(): List<String> {
        val imagePaths = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("DCIM/Screenshots%"),
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )
        cursor?.use {
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(dataColumn)
                imagePaths.add(imagePath)
            }
        }
        return imagePaths
    }

    // Method to delete image
     fun deleteImage(imagePath: String) {
            val file = File(imagePath)
            if (file.exists()) {
                if (file.delete()) {
                    Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()                    }

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val selection = "${MediaStore.Images.Media.DATA} = ?"
                    val selectionArgs = arrayOf(imagePath)
                    val rowsDeleted = context.contentResolver.delete(uri, selection, selectionArgs)
                    if (rowsDeleted > 0) {
                        Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()                    }
                } else {
                    Log.d("ImageRepository", "File does not exist: $imagePath")
                }
            }

    }


}