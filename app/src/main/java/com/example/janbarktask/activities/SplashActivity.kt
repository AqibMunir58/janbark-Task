package com.example.janbarktask.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.example.janbarktask.adsManager.InterstitialAdHelper
import com.example.janbarktask.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private val binding : ActivitySplashBinding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        InterstitialAdHelper.loadAd(this)
        Handler().postDelayed({
            val intent = Intent(this@SplashActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        },2000)
    }
}