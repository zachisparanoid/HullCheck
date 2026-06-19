package com.hullcheck

import android.app.Application
import com.hullcheck.di.AppInitializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class HullCheckApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface InitializerEntryPoint {
        fun appInitializer(): AppInitializer
    }

    override fun onCreate() {
        super.onCreate()
        // Hilt does not member-inject the Application, so resolve via an EntryPoint.
        EntryPointAccessors
            .fromApplication(this, InitializerEntryPoint::class.java)
            .appInitializer()
            .initialize()
    }
}
