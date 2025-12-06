package com.llfbandit.record.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import com.llfbandit.record.R

class AudioRecordingService : Service() {
  companion object {
    private const val CHANNEL_ID = "AudioRecordingChannel"
    private const val NOTIFICATION_ID = 1
    const val DEFAULT_TITLE = "Audio Capture"
  }

  private val binder: IBinder = LocalBinder()
  private lateinit var notificationManager: NotificationManager

  inner class LocalBinder : Binder() {
//        fun getService(): AudioRecordingService = this@AudioRecordingService
  }

  override fun onBind(intent: Intent): IBinder {
    return binder
  }

  override fun onCreate() {
    super.onCreate()
    notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    createNotificationChannel()
  }

  override fun onDestroy() {
    super.onDestroy()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == null) {
      val notification = createNotification(
        intent?.getStringExtra("title"),
        intent?.getStringExtra("content"),
        intent?.getBooleanExtra("openAppOnTap", true) ?: true
      )
      startForeground(NOTIFICATION_ID, notification)

      notificationManager.notify(NOTIFICATION_ID, notification)
    }

    return START_NOT_STICKY
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        DEFAULT_TITLE,
        NotificationManager.IMPORTANCE_LOW
      )
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun createNotification(title: String?, content: String?, openAppOnTap: Boolean): Notification {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(title ?: DEFAULT_TITLE)
      .setContentText(content)
      .setSmallIcon(R.drawable.ic_mic)
      .setSilent(true)
      .setOngoing(true)
      .setVisibility(VISIBILITY_PUBLIC)

    if (openAppOnTap) {
      val pendingIntent = createPendingIntent()
      if (pendingIntent != null) {
        builder.setContentIntent(pendingIntent)
      }
    }

    return builder.build()
  }

  private fun createPendingIntent(): PendingIntent? {
    val packageManager = applicationContext.packageManager
    val launchIntent = packageManager.getLaunchIntentForPackage(applicationContext.packageName)
    
    return if (launchIntent != null) {
      launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      
      val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }
      
      PendingIntent.getActivity(
        applicationContext,
        0,
        launchIntent,
        flags
      )
    } else {
      null
    }
  }
}