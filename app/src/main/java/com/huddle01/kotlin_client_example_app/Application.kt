package com.huddle01.kotlin_client_example_app

import android.app.Application
import com.huddle01.kotlin_client.HuddleClient

class Application : Application() {
    private lateinit var huddleClient: HuddleClient
    override fun onCreate() {
        super.onCreate()
        huddleClient = HuddleClient("YOUR_PROJECT_ID", this)
    }
}
