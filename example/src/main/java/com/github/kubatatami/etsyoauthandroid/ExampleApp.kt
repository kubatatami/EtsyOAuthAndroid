package com.github.kubatatami.etsyoauthandroid

import android.app.Application
import com.github.kubatatami.lib.EtsyOAuth

class ExampleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        EtsyOAuth.initialize(this, CONSUMER_KEY, CONSUMER_SECRET)
    }

    companion object {
        private const val CONSUMER_KEY = ""
        private const val CONSUMER_SECRET = ""
    }
}