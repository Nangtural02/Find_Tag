package com.example.find_tag

import android.icu.text.SimpleDateFormat
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
private val directoryToSave = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Find_Tag"
object FileManager {
    var textFileName: String = "temp.txt"

    fun setFileName(newTextFileName:String){
        this.textFileName = newTextFileName
    }
    fun setFileTime(){
        textFileName = "Log_" + SimpleDateFormat("yyyy_MM_dd HH_mm_ss").format(Date()) + ".txt"
    }

    suspend fun writeTextFile(contents: String) {
        withContext(Dispatchers.IO) {
            try {
                val dir = File(directoryToSave)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                val fos = FileOutputStream(directoryToSave + "/" + textFileName, true)
                fos.write(contents.encodeToByteArray())
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


}