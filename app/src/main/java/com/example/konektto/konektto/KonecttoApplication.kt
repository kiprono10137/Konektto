package com.example.konektto.konektto

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.konektto.konektto.utils.NotificationHelper
import com.example.konektto.konektto.utils.PresenceManager

class KonecttoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(PresenceManager)

        NotificationHelper.createNotificationChannels(this)

    }

}
