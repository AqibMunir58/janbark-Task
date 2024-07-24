package com.example.janbarktask.activities

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.janbarktask.adapter.ImageAdapter
import com.example.janbarktask.viewmodel.ImageViewModel
import com.example.janbarktask.MyApplication
import com.example.janbarktask.services.MyForegroundService
import com.example.janbarktask.adsManager.InterstitialAdHelper
import com.example.janbarktask.databinding.ActivityMainBinding
import java.io.File

class HomeActivity : AppCompatActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var manageStorageLauncher: ActivityResultLauncher<Intent>
    private val imageRepository by lazy { (application as MyApplication).imageRepository }
    private val imageViewModel by lazy {
        ImageViewModel(imageRepository)
    }
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var adapter : ImageAdapter
    override fun onDestroy() {
        super.onDestroy()
        Log.d("HomeActivity", "onDestroy: ")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupPermissionLauncher()
        setupManageStorageLauncher()
        checkAndRequestPermissions()
        createNotificationChannel()
        startMyForegroundService()
        setRecyclerView()
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }



    }
    private fun refreshData() {
        setRecyclerView()
        binding.swipeRefreshLayout.isRefreshing = false
    }



    private fun setRecyclerView() {

        val layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.layoutManager = layoutManager
        if (InterstitialAdHelper.isNetworkAvailable(this))
        {
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if ((position + 1) % 7 == 0) {
                        3
                    } else {
                        1
                    }
                }
            }
        }
        imageViewModel.fetchImages()
        imageViewModel.imagePaths.observe(this, Observer {
             adapter = ImageAdapter(this , this,it, object : ImageAdapter.onItemClick {
                override fun onDelete(position: Int, item: String) {
                    imageViewModel.deleteImage(item)
                   val  imagePaths = it.filterIndexed { index, _ -> index != position }
                    adapter.updateImagePaths(imagePaths)
                }

                override fun onShare(item: String) {
                    shareImage(item)
                }

                override fun onOpen(item: String) {
                    InterstitialAdHelper.showAd(
                        this@HomeActivity,
                        this@HomeActivity,
                        object : InterstitialAdHelper.InterstitialCallBack {
                            override fun onAdDismissed() {
                                goToNext(item)
                            }

                            override fun onAdNull() {
                                goToNext(item)
                            }

                        })

                }

            })

            binding.recyclerView.adapter = adapter
        })


/*        val adapter = ImageAdapter(this , this,mutableListOf(), object : ImageAdapter.onItemClick {
            override fun onDelete(position: Int, item: String) {
                imageViewModel.deleteImage(item)
                setRecyclerView()
            }

            override fun onShare(item: String) {
                shareImage(item)
            }

            override fun onOpen(item: String) {
                InterstitialAdHelper.showAd(
                    this@HomeActivity,
                    this@HomeActivity,
                    object : InterstitialAdHelper.InterstitialCallBack {
                        override fun onAdDismissed() {
                            goToNext(item)
                        }

                        override fun onAdNull() {
                            goToNext(item)
                        }

                    })

            }

        })



        binding.recyclerView.adapter = adapter
        imageViewModel.imagePaths.observe(this, Observer {
            if (it.size > 0) {
                binding.noImages.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                Log.d("ImagePaths", "onCreate: $it")
                adapter.updateImagePaths(it)


            } else {
                binding.noImages.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            }

        })*/

    }

    private fun goToNext(item: String) {
        val intent = Intent(this@HomeActivity, ImagePreviewActivity::class.java)
        intent.putExtra("Imagepath", item)
        startActivity(intent)
    }


    private fun shareImage(imagePath: String) {
        val imageFile = File(imagePath)
        val imageUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                imageFile
            )
        } else {
            Uri.fromFile(imageFile)
        }

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share image via"))
    }


    private fun setupPermissionLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allPermissionsGranted = permissions.entries.all { it.value }
                if (allPermissionsGranted) {
                    setRecyclerView()

                } else {
                    // Permissions denied, show a message to the user
                }
            }
    }

    private fun setupManageStorageLauncher() {
        manageStorageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        setRecyclerView()

                    } else {
                        // Permission denied, show a message to the user
                    }
                }
            }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 and above
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + applicationContext.packageName)
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                }
                return
            }
        } else {
            // Below Android 11
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) ||
                shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shouldShowRequestPermissionRationale(
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ))
            ) {
                showRationaleDialog(permissions.toTypedArray())
            } else {
                permissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    private fun showRationaleDialog(permissions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs storage permissions to function properly. Please enable them in the app settings.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(Intent(this, MyForegroundService::class.java))
                    } else {
                        startService(Intent(this, MyForegroundService::class.java))
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                MyForegroundService.CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startMyForegroundService() {
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

}