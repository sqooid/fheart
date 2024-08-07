package com.sqooid.fheart

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

fun permissionCheckAndRequest(context: Activity, permission: String): Boolean {
    if (ActivityCompat.checkSelfPermission(
            context,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            0
        )
    } else return true
    return (ActivityCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED)
}
