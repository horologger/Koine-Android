package com.koine.mesh.service

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.koine.mesh.IMeshService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshServiceConnection @Inject constructor(
    private val application: Application
) {
    private val _service = MutableStateFlow<IMeshService?>(null)
    val service: StateFlow<IMeshService?> = _service

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            _service.value = IMeshService.Stub.asInterface(binder)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            _service.value = null
        }
    }

    fun bind() {
        val intent = Intent(application, MeshService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        application.unbindService(connection)
        _service.value = null
    }
} 