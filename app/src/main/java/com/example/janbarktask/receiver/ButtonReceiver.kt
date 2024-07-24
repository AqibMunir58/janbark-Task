package com.example.janbarktask.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.janbarktask.activities.OutSideActivity
import com.example.janbarktask.services.MyForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ButtonReceiver : BroadcastReceiver() {
    private fun collapseNotificationTray(context: Context) {
        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(closeIntent)
    }
    private fun removeNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(MyForegroundService.NOTIFICATION_ID)
    }



    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            "button1_action" -> {
                collapseNotificationTray(context)
                val openOutsideActivityIntent = Intent(context, OutSideActivity::class.java)
                openOutsideActivityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                openOutsideActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(openOutsideActivityIntent)

                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    val intentScreen = Intent("startRecordingScreen")
                    intentScreen.putExtra("key", "screen")
                    context.sendBroadcast(intentScreen)
                }

            }
            "button2_action" -> {
                collapseNotificationTray(context)
                removeNotification(context)
            }
        }
    }
}