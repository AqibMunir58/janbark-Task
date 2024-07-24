package com.example.janbarktask.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.janbarktask.repository.ImageRepository
import kotlinx.coroutines.launch

class ImageViewModel(private val imageRepository: ImageRepository) : ViewModel() {

    private val _imagePaths = MutableLiveData<List<String>>()
    val imagePaths: LiveData<List<String>> get() = _imagePaths


     fun fetchImages() {
        viewModelScope.launch {
            imageRepository.getAllImages().collect { paths ->
                _imagePaths.value = paths
            }
        }
    }

    fun deleteImage(imagePath: String) {
        viewModelScope.launch {
            imageRepository.deleteImage(imagePath)
            fetchImages()
        }
    }
}