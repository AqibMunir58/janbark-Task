package com.example.janbarktask.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.janbarktask.databinding.ActivityImagePreviewBinding

class ImagePreviewActivity : AppCompatActivity() {
    private val binding : ActivityImagePreviewBinding by lazy { ActivityImagePreviewBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val imagePath = intent.getStringExtra("Imagepath")
        Glide.with(this)
            .load(imagePath)
            .into(binding.PreviewImage)
    }
}