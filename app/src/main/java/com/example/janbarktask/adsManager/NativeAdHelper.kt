package com.example.janbarktask.adsManager

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.janbarktask.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

object NativeAdHelper {
    private var nativeAd: NativeAd? = null
    private var TAG = "NativeAdHelper"

    fun loadNativeAds(
        context: Context,
        activity: Activity,
        frameLayout: FrameLayout,
        adId: String,
        nativeCallback: NativeCallBack,
    ) {

        if (isNetworkAvailable(context)) {
            val adLoader = AdLoader.Builder(context, adId)
                .forNativeAd { ad ->
                    nativeAd = ad
                    val adView = activity.layoutInflater
                        .inflate(R.layout.admob_small_native, null) as NativeAdView
                    nativeCallback.OnAdLoaded()
                    frameLayout.removeAllViews()
                    frameLayout.addView(adView)
                    populateNativeAdView(ad, adView)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {

                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }

    }

    private fun populateNativeAdView(ad: NativeAd, adView: NativeAdView) {

        adView.headlineView = adView.findViewById<View>(R.id.ad_headline)
        adView.callToActionView = adView.findViewById<View>(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById<View>(R.id.ad_app_icon)
        adView.bodyView = adView.findViewById(R.id.descriptiontextView)

        // Headline
        adView.headlineView?.let {
            (it as TextView).text = ad.headline
            it.isSelected = true
        }

        adView.advertiserView?.let {
            (it as TextView).text = ad.advertiser
        }

        // Body
        adView.bodyView?.let {
            if (ad.body == null) {
                it.visibility = View.INVISIBLE
            } else {
                it.visibility = View.VISIBLE
                (it as TextView).text = ad.body
            }
        }

        // Call to Action
        adView.callToActionView?.let {
            if (ad.callToAction == null) {
                it.visibility = View.INVISIBLE
            } else {
                it.visibility = View.VISIBLE
                (it as TextView).text = ad.callToAction
            }
        }

        // Icon
        adView.iconView?.let {
            if (ad.icon == null) {
                it.visibility = View.GONE
            } else {
                (it as ImageView).setImageDrawable(ad.icon!!.drawable)
                it.visibility = View.VISIBLE
            }
        }
    }

    interface NativeCallBack {
        fun OnAdLoaded()
        fun OnNativeFailure()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
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
}
