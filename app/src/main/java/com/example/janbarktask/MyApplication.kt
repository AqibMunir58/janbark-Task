package com.example.janbarktask

import android.app.Application
import com.example.janbarktask.repository.ImageRepository
import com.google.android.gms.ads.MobileAds

class MyApplication : Application() {

    lateinit var imageRepository: ImageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        imageRepository = ImageRepository(this)
    }
}