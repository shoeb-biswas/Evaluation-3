package com.example.data

import android.content.Context
import com.example.data.ai.AiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.SessionDataStore
import com.example.data.repository.FinanceLivingRepository
import com.example.data.repository.RentShareRepository

class RentShareContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val sessionDataStore: SessionDataStore by lazy { SessionDataStore(appContext) }
    val repository: RentShareRepository by lazy { RentShareRepository(database, sessionDataStore) }
    val financeRepository: FinanceLivingRepository by lazy { FinanceLivingRepository(database) }
    val aiRepository: AiRepository by lazy { AiRepository() }
}
