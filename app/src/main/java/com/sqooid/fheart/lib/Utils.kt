package com.sqooid.fheart.lib

import android.app.Activity
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
            arrayOf(permission),
            0
        )
    } else return true
    return (ActivityCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED)
}
