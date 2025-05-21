/*
 * Copyright (c) 2025 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.koine.mesh

import android.os.Debug
import com.koine.mesh.android.AppPrefs
import com.koine.mesh.android.BuildUtils.isEmulator
import com.koine.mesh.android.GeeksvilleApplication
import com.koine.mesh.android.Logging
import com.koine.mesh.service.MeshServiceConnection
import com.koine.mesh.util.Exceptions
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.Firebase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MeshUtilApplication : GeeksvilleApplication() {
    @Inject
    lateinit var meshServiceConnection: MeshServiceConnection

    override fun onCreate() {
        super.onCreate()
        Logging.showLogs = BuildConfig.DEBUG
        meshServiceConnection.bind()

        // We default to off in the manifest - we turn on here if the user approves
        // leave off when running in the debugger
        if (!isEmulator && (!BuildConfig.DEBUG || !Debug.isDebuggerConnected())) {
            val crashlytics = Firebase.crashlytics
            crashlytics.setCrashlyticsCollectionEnabled(isAnalyticsAllowed)
            crashlytics.setCustomKey("debug_build", BuildConfig.DEBUG)

            val pref = AppPrefs(this)
            crashlytics.setUserId(pref.getInstallId()) // be able to group all bugs per anonymous user

            // We always send our log messages to the crashlytics lib, but they only get sent to the server if we report an exception
            // This makes log messages work properly if someone turns on analytics just before they click report bug.
            // send all log messages through crashyltics, so if we do crash we'll have those in the report
            val standardLogger = Logging.printlog
            Logging.printlog = { level, tag, message ->
                crashlytics.log("$tag: $message")
                standardLogger(level, tag, message)
            }

            fun sendCrashReports() {
                if (isAnalyticsAllowed)
                    crashlytics.sendUnsentReports()
            }

            // Send any old reports if user approves
            sendCrashReports()

            // Attach to our exception wrapper
            Exceptions.reporter = { exception, _, _ ->
                crashlytics.recordException(exception)
                sendCrashReports() // Send the new report
            }
        }
    }
}
