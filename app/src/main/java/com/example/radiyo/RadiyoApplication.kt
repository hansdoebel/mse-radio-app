package com.example.radiyo

import android.app.Application
import com.clerk.api.Clerk

class RadiyoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Clerk.initialize(this, publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY)
    }

    companion object {
        lateinit var instance: RadiyoApplication
            private set
    }
}
