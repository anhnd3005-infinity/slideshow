package com.ynsuper.slideshowver1.util

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    
    const val REQUEST_CODE_AUDIO_PERMISSION = 2001
    const val REQUEST_CODE_STORAGE_PERMISSION = 2002
    
    fun getAudioPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    fun hasAudioPermission(activity: Activity): Boolean {
        val permissions = getAudioPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun hasStoragePermission(activity: Activity): Boolean {
        val permissions = getStoragePermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestAudioPermission(activity: Activity) {
        val permissions = getAudioPermissions()
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_AUDIO_PERMISSION)
    }
    
    fun requestStoragePermission(activity: Activity) {
        val permissions = getStoragePermissions()
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_STORAGE_PERMISSION)
    }
}

