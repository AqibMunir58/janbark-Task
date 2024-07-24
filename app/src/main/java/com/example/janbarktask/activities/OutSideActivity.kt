package com.example.janbarktask.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.janbarktask.R
import com.example.janbarktask.services.ScreenCaptureService

class OutSideActivity : AppCompatActivity() {

    private val REQUEST_CODE_SCREENSHOT = 100
    private val startRecordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val value = intent!!.getStringExtra("key")
            Log.d("tag", "Received value is in sub $value")
            if (value == "screen") {
                startProjection()
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter("startRecordingScreen")
        registerReceiver(startRecordingReceiver, filter)
    }

    private fun startProjection() {
        Log.e("TAG", "startProjection: method  called for screenshot")
        val mProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mProjectionManager.createScreenCaptureIntent(),
            REQUEST_CODE_SCREENSHOT
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_CODE_SCREENSHOT) {
                Log.e("TAG", "onActivityResult: screen shot ")
                if (resultCode == RESULT_OK) {
                    startService(
                        Intent(this, ScreenCaptureService::class.java).apply {
                            putExtra("RESULT_CODE", resultCode)
                            putExtra("DATA", data)
                        }
                    )

                  /*  startService(
                        ScreenCaptureService.getStartIntent(
                            this@OutSideActivity,
                            resultCode,
                            data
                        )
                    )*/
                    this@OutSideActivity.finish()
                } else {
                    this@OutSideActivity.finish()
                }
            }

        }


    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("TAG", "onDestroy: ")
        unregisterReceiver(startRecordingReceiver)
    }

}