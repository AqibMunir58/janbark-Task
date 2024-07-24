package com.example.janbarktask.adsManager

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdHelper {

    var adRequest: AdRequest? = null
    var mInterstitialAd: InterstitialAd? = null
    private val TAG = "InterstitialAdHelper"

    fun loadAd(context: Context) {
        if (isNetworkAvailable(context) && mInterstitialAd == null) {
            adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                Ad_Ids.interstitialAdId,
                adRequest!!,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        mInterstitialAd = null
                    }
                })
        }
    }

    fun showAd(
        activity: Activity,
        context: Context,
        interstitialCallBack: InterstitialCallBack
    ) {
        if (mInterstitialAd != null) {
            mInterstitialAd!!.show(activity)
            mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    mInterstitialAd = null
                    loadAd(context)
                    interstitialCallBack.onAdDismissed()
                }
            }
        } else {
            interstitialCallBack.onAdNull()
            loadAd(context)

        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    interface InterstitialCallBack {
        fun onAdDismissed()
        fun onAdNull()
    }
}