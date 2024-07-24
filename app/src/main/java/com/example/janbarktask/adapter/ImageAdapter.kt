package com.example.janbarktask.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.janbarktask.R
import com.example.janbarktask.adsManager.Ad_Ids
import com.example.janbarktask.adsManager.InterstitialAdHelper.isNetworkAvailable
import com.example.janbarktask.adsManager.NativeAdHelper
import com.example.janbarktask.databinding.ItemScreenshotImageBinding
import com.example.janbarktask.databinding.LayoutNativeAdsBinding

class ImageAdapter(
    private val context : Context,
    private val activity : Activity,
    private var imagePaths: List<String>,
    private val itemClickListener: onItemClick

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_IMAGE = 1
        const val VIEW_TYPE_AD = 2
    }

    interface onItemClick {
        fun onDelete(position : Int ,item : String)
        fun onShare(item : String)
        fun onOpen(item: String)
    }

    override fun getItemViewType(position: Int): Int {

        return if ((position + 1) % 7 == 0) {
            VIEW_TYPE_AD
        } else {
            VIEW_TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (!isNetworkAvailable(parent.context))
        {
            val binding = ItemScreenshotImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
           return  ImageViewHolder(binding)
        }
        else
        {
            return when (viewType) {
                VIEW_TYPE_AD -> {
                    val binding = LayoutNativeAdsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    AdViewHolder(binding)
                }
                else -> {
                    val binding = ItemScreenshotImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    ImageViewHolder(binding)
                }
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (isNetworkAvailable(holder.itemView.context))
        {
            when (holder) {
                is ImageViewHolder -> {
                    val imagePosition = position - position / 7
                    val imagePath = imagePaths[imagePosition]
                    Glide.with(holder.binding.imageView.context)
                        .load(imagePath)
                        .into(holder.binding.imageView)
                    holder.binding.menuBtn.setOnClickListener {
                        if (holder.binding.cardViewoptionmenu.isVisible) {
                            holder.binding.cardViewoptionmenu.visibility = View.GONE
                        } else {
                            holder.binding.cardViewoptionmenu.visibility = View.VISIBLE
                        }

                    }
                    holder.binding.OpenImage.setOnClickListener {
                        itemClickListener.onOpen(imagePath)
                    }
                    holder.binding.ShareImage.setOnClickListener {
                        itemClickListener.onShare(imagePath)
                    }
                    holder.binding.DeleteImage.setOnClickListener {
                        itemClickListener.onDelete(imagePosition, imagePath)
                    }
                    holder.itemView.setOnClickListener {
                        itemClickListener.onOpen(imagePath)
                    }
                }
                is AdViewHolder -> {
                    (holder as AdViewHolder).bin()
                }
            }
        }
        else
        {
            val holder = holder as ImageViewHolder
            val imagePath = imagePaths[position]
            Glide.with(holder.binding.imageView.context)
                .load(imagePath)
                .into(holder.binding.imageView)
            holder.binding.menuBtn.setOnClickListener {
                if (holder.binding.cardViewoptionmenu.isVisible) {
                    holder.binding.cardViewoptionmenu.visibility = View.GONE
                } else {
                    holder.binding.cardViewoptionmenu.visibility = View.VISIBLE
                }

            }
            holder.binding.OpenImage.setOnClickListener {
                itemClickListener.onOpen(imagePath)
            }
            holder.binding.ShareImage.setOnClickListener {
                itemClickListener.onShare(imagePath)
            }
            holder.binding.DeleteImage.setOnClickListener {
                itemClickListener.onDelete(position,imagePath)
            }
            holder.itemView.setOnClickListener {
                itemClickListener.onOpen(imagePath)
            }
        }

    }

    override fun getItemCount(): Int {
        return if (isNetworkAvailable(context)) {
            imagePaths.size + imagePaths.size / 6
        } else {
            imagePaths.size
        }


    }
    fun updateImagePaths(paths: List<String>) {
        imagePaths = paths
        notifyDataSetChanged()
    }


    class ImageViewHolder(val binding: ItemScreenshotImageBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AdViewHolder(private val binding: LayoutNativeAdsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bin() {
            val frameLayout = itemView.findViewById<FrameLayout>(R.id.admob_native_container)
            val loadingAd = itemView.findViewById<TextView>(R.id.loading_ad)
            val mainLayout = itemView.findViewById<ConstraintLayout>(R.id.parent_native_container)

            if (isNetworkAvailable(activity))
            {
                NativeAdHelper.loadNativeAds(
                    itemView.context,
                    activity,
                    frameLayout,
                    Ad_Ids.nativeAdId,
                    object : NativeAdHelper.NativeCallBack{
                        override fun OnAdLoaded() {
                            loadingAd.visibility = View.GONE
                            frameLayout.visibility = View.VISIBLE
                        }

                        override fun OnNativeFailure() {
                        }
                    }
                )
            }
            else
            {
                mainLayout.visibility = View.GONE
            }

        }
    }

}

