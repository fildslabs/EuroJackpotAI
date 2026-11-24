package com.eurojackpotai.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.eurojackpotai.app.core.AppContainer
import kotlinx.coroutines.launch

class EuroJackpotApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Guarantees the sample dataset exists before any screen queries the database,
        // without blocking app startup on it.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            container.lotteryRepository.ensureSeeded()
        }
    }
}
