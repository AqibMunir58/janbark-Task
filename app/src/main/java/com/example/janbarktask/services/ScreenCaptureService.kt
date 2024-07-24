package com.example.janbarktask.services

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.janbarktask.R
import com.example.janbarktask.activities.HomeActivity

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ScreenCaptureService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var imageReader: ImageReader
    private lateinit var notificationManager: NotificationManager

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED)
        val data = intent?.getParcelableExtra<Intent>("DATA")

        if (resultCode == null || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        startForegroundService()
        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        Handler().postDelayed({
            startScreenCapture()
        },500)
        return START_NOT_STICKY
    }


    private fun startScreenCapture() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        imageReader = ImageReader.newInstance(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            PixelFormat.RGBA_8888,
            1
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCaptureService",
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * displayMetrics.widthPixels

            val bitmap = Bitmap.createBitmap(
                displayMetrics.widthPixels + rowPadding / pixelStride,
                displayMetrics.heightPixels,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            image.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveBitmapToFileAboveQ(bitmap)
            } else {
                saveBitmapToFileBelowQ(bitmap)
            }

            stopProjection()
            stopSelf()

        }, null)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startForegroundService() {
        val notificationIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture")
            .setContentText("Screen capturing in progress")
            .setSmallIcon(R.drawable.notification)
            .setContentIntent(pendingIntent)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }
        else
        {
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }

    }


    private fun stopProjection() {
        mediaProjection?.stop()
        mediaProjection = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader.close()
    }

    private fun saveBitmapToFileBelowQ(bitmap: Bitmap) {
        val directory = File(Environment.getExternalStorageDirectory().toString() + "/Screenshots")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName =
            "screenshot_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        val file = File(directory, fileName)

        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream?.close()
        }

        Log.d("ScreenCaptureService", "Screenshot saved: ${file.absolutePath}")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveBitmapToFileAboveQ(bitmap: Bitmap) {
        val contentValues = android.content.ContentValues().apply {
            put(
                android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                "screenshot_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
            )
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Screenshots")
        }

        val contentResolver = applicationContext.contentResolver
        val uri = contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            contentResolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                }
            }
        }

        Log.d("ScreenCaptureService", "Screenshot saved to MediaStore: ${uri?.path}")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val CHANNEL_ID = "ForegroundServiceChannel"
        private const val NOTIFICATION_ID = 2
        private const val EXTRA_RESULT_CODE = "RESULT_CODE"
        private const val EXTRA_DATA = "DATA"
    }
}


/*
class ScreenCaptureService : Service() {
    private var mMediaProjection: MediaProjection? = null
    private var mStoreDir: String? = null
    private var mImageReader: ImageReader? = null
    private var mHandler: Handler? = null
    private var mDisplay: Display? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mDensity = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation = 0
    private var mOrientationChangeCallback: OrientationChangeCallback? = null
    private var isAlreadyRun: Boolean? = false

    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        private var imageCount = 0
        override fun onImageAvailable(reader: ImageReader) {
            try {
                mImageReader!!.acquireLatestImage().use { image ->
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * mWidth

                        // Create bitmap
                        val bitmap = Bitmap.createBitmap(
                            mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)
                        imageCount++
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (imageCount == 15) {
                                // Save the 15th image using the appropriate method based on Android version
                                saveImageUsingMediaStore(bitmap)
                                stopProjection()


                            }

                        } else {
                            if (imageCount == 4) {
                                saveImageUsingFile(bitmap)
                                stopProjection()
//
                            }
                        }


                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // Method to save image using Media Store API (Android 10 and higher)
    private fun saveImageUsingMediaStore(bitmap: Bitmap) {
        var fileName = generateFileName()
        // Create content values
        val subFolderName = "Screen Recorder Video Recorder"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "myscreen_${fileName}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${subFolderName}")
            }
        }
        // Insert image using content resolver
        val contentResolver = applicationContext.contentResolver
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        contentResolver.openOutputStream(imageUri!!).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
        }

*/
/*        val intent =
            Intent("com.screenrecorder.videoeditor.videorecorder.screencast.MY_CUSTOM_ACTION")
        intent.`package` = "com.screenrecorder.videoeditor.videorecorder.screencast"
        intent.putExtra("key", "KeyScreenShotShow")
        intent.putExtra("imageUri", imageUri.toString())
        intent.putExtra("isBallRun", isAlreadyRun)
        sendBroadcast(intent)*//*

    }
    // Method to save image using old file-saving method (Android below 10)
    private fun saveImageUsingFile(bitmap: Bitmap) {
        var fileName = generateFileName()
        var fos: FileOutputStream? = null
        try {
            // Get the Pictures directory
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val subFolderName = "Screen Recorder Video Recorder"

            // Create the sub-folder using your app's package name
            val subFolder = File(picturesDir, subFolderName)
            if (!subFolder.exists()) {
                subFolder.mkdirs()
            }
            // Create the image file within the sub-folder
            val imageFile = File(subFolder, "myscreen_${fileName}.png")

            // Write bitmap to the image file
            fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            val imageUri = Uri.fromFile(imageFile)

*/
/*            val intent =
                Intent("com.screenrecorder.videoeditor.videorecorder.screencast.MY_CUSTOM_ACTION")
            intent.`package` = "com.screenrecorder.videoeditor.videorecorder.screencast"
            intent.putExtra("key", "KeyScreenShotShow")
            intent.putExtra("imageUri", imageUri.toString())
            intent.putExtra("isBallRun", isAlreadyRun)
            sendBroadcast(intent)*//*



        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fos?.close()
        }
    }


    private fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        return formatter.format(curDate).replace(" ", "")
    }


    private inner class OrientationChangeCallback internal constructor(context: Context?) :
        OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = mDisplay!!.rotation
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay!!.release()
                    if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            Log.e(TAG, "stopping projection.")
            mHandler!!.post {
                if (mVirtualDisplay != null) mVirtualDisplay!!.release()
                if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)
                if (mOrientationChangeCallback != null) mOrientationChangeCallback!!.disable()
                mMediaProjection!!.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // Create sub-folder within Pictures directory
        val subFolderName = "Screen Recorder Video Recorder"
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val subFolder = File(picturesDir, subFolderName)

        if (!subFolder.exists()) {
            val success = subFolder.mkdirs()
            if (!success) {
                Log.e(TAG, "Failed to create sub-folder.")
                stopSelf()
                return
            }
        }

        // create store dir
        val externalFilesDir = getExternalFilesDir(null)
        if (externalFilesDir != null) {
            mStoreDir = externalFilesDir.absolutePath + "/screenshots/"
            val storeDirectory = File(mStoreDir)
            if (!storeDirectory.exists()) {
                val success = storeDirectory.mkdirs()
                if (!success) {
                    Log.e(TAG, "failed to create file storage directory.")
                    stopSelf()
                }
            }
        } else {
            Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.")
            stopSelf()
        }

        // start capture handling thread
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()
    }

    //
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (isStartCommand(intent)) {
            // start projection
            val resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
            val data = intent.getParcelableExtra<Intent>(DATA)
            startProjection(resultCode, data)
        } else if (isStopCommand(intent)) {
            stopProjection()
            stopSelf()
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }


    private fun startProjection(resultCode: Int, data: Intent?) {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            if (mMediaProjection == null) {
                // delay needed because getMediaProjection() throws an error if it's called too soon
                Handler().postDelayed({
                    mMediaProjection = mpManager.getMediaProjection(resultCode, data!!)
                    if (mMediaProjection != null) {
                        // display metrics
                        mDensity = Resources.getSystem().displayMetrics.densityDpi
                        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                        mDisplay = windowManager.defaultDisplay

                        // create virtual display depending on device width / height
                        createVirtualDisplay()

                        // register orientation change callback
                        mOrientationChangeCallback = OrientationChangeCallback(this)
                        if (mOrientationChangeCallback!!.canDetectOrientation()) {
                            mOrientationChangeCallback!!.enable()
                        }

                        // register media projection stop callback
                        mMediaProjection!!.registerCallback(MediaProjectionStopCallback(), mHandler)
                    }
                }, 1000)

            }


    }

    private fun stopProjection() {
        if (mHandler != null) {
            mHandler!!.post {
                if (mMediaProjection != null) {
                    mMediaProjection!!.stop()
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {
        // get width and height
        mWidth = Resources.getSystem().displayMetrics.widthPixels
        mHeight = Resources.getSystem().displayMetrics.heightPixels

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            SCREENCAP_NAME, mWidth, mHeight,
            mDensity, virtualDisplayFlags, mImageReader!!.surface, null, mHandler
        )
        mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val RESULT_CODE = "RESULT_CODE"
        private const val DATA = "DATA"
        private const val ACTION = "ACTION"
        private const val START = "START"
        private const val STOP = "STOP"
        private const val SCREENCAP_NAME = "screencap"
        private var IMAGES_PRODUCED = 0
        fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, START)
            intent.putExtra(RESULT_CODE, resultCode)
            intent.putExtra(DATA, data)
            return intent
        }

        fun getStopIntent(context: Context?): Intent {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, STOP)
            return intent
        }

        private fun isStartCommand(intent: Intent): Boolean {
            return (intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
                    && intent.hasExtra(ACTION)) && intent.getStringExtra(ACTION) == START
        }

        private fun isStopCommand(intent: Intent): Boolean {
            return intent.hasExtra(ACTION) && intent.getStringExtra(ACTION).equals("STOP")
        }

        private val virtualDisplayFlags: Int
            private get() = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }



}*/
