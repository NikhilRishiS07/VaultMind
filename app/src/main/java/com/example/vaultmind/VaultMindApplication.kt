package com.example.vaultmind

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VaultMindApplication : Application() {
	private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onCreate() {
		super.onCreate()

		val lockManager = AppGraph.lockManager(this)
		lockManager.ensureTempUser()

		appScope.launch {
			AppGraph.repository(this@VaultMindApplication).ensureSeedData()
		}
	}
}