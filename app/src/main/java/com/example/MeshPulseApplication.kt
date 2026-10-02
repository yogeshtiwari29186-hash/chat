package com.example

import android.app.Application
import com.example.data.MeshRepository
import com.example.data.local.AppDatabase
import com.example.mesh.MeshTransportManager
import com.example.service.MeshForegroundService

class MeshPulseApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var transportManager: MeshTransportManager
        private set

    lateinit var repository: MeshRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        transportManager = MeshTransportManager(this)
        repository = MeshRepository(this, database, transportManager)

        // Start background mesh router service
        try {
            MeshForegroundService.start(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: MeshPulseApplication
            private set
    }
}
