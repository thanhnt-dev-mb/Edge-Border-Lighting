package com.merryblue.baseapplication.helpers

import android.net.Uri
import android.widget.Toast
import java.io.File

object FileHelper {
    fun getUriFromPath(path: String): Uri? {
        val file = File(path)
        return if(!file.exists()) null
        else Uri.fromFile(file)
    }
}