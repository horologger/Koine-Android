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

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.koine.mesh.repository.SatochipRepository
import com.koine.mesh.satochip.SatochipManager
import com.koine.mesh.service.MeshService
import com.koine.mesh.service.MeshServiceConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ApplicationModule {
    @Provides
    fun provideSharedPreferences(application: Application): SharedPreferences {
        return application.getSharedPreferences("ui-prefs", Context.MODE_PRIVATE)
    }

    @Provides
    fun provideProcessLifecycleOwner(): LifecycleOwner {
        return ProcessLifecycleOwner.get()
    }

    @Provides
    fun provideProcessLifecycle(processLifecycleOwner: LifecycleOwner): Lifecycle {
        return processLifecycleOwner.lifecycle
    }

    @Provides
    @Singleton
    fun provideSatochipRepository(): SatochipRepository {
        return SatochipRepository()
    }

    @Provides
    @Singleton
    fun provideSatochipManager(
        application: Application,
        satochipRepository: SatochipRepository
    ): SatochipManager {
        return SatochipManager(application, satochipRepository)
    }

    @Provides
    @Singleton
    fun provideMeshServiceConnection(application: Application): MeshServiceConnection {
        return MeshServiceConnection(application)
    }

    @Provides
    fun provideIMeshService(meshServiceConnection: MeshServiceConnection): IMeshService? {
        // Return the currently bound instance (may be null if not yet bound)
        return meshServiceConnection.service.value
    }
}