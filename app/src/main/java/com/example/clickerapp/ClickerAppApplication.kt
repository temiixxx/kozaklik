package com.example.clickerapp

import android.app.Application
import android.util.Log
import com.example.clickerapp.data.database.GameDatabase
import com.example.clickerapp.data.repository.GameRepository

class ClickerAppApplication : Application() {
    private val TAG = "ClickerAppApplication"
    
    val database: GameDatabase by lazy {
        Log.d(TAG, "Initializing database")
        GameDatabase.create(this)
    }
    
    val repository: GameRepository by lazy {
        Log.d(TAG, "Initializing repository")
        GameRepository(database.gameDao(), database.achievementDao())
    }
}

