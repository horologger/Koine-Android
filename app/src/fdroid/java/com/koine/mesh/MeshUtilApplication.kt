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

import com.koine.mesh.android.GeeksvilleApplication
import com.koine.mesh.android.Logging
import com.koine.mesh.service.MeshServiceConnection
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
    }
}