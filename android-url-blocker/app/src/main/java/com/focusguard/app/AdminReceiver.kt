package com.focusguard.app

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context

/**
 * Being an active device admin blocks one-tap uninstall: the user must first
 * deactivate admin, and the accessibility service guards that screen while locked.
 */
class AdminReceiver : DeviceAdminReceiver() {
    companion object {
        fun component(context: Context) = ComponentName(context, AdminReceiver::class.java)
    }
}
