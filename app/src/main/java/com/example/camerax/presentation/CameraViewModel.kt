package com.example.camerax.presentation

import androidx.lifecycle.ViewModel
import com.example.camerax.utils.FlashType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor():ViewModel(){

    private var isSelfieCameraSelect:Boolean=false
    fun setReverseCamera(){
        isSelfieCameraSelect=!isSelfieCameraSelect
    }
    fun getReverseCamera():Boolean{
        return isSelfieCameraSelect
    }
    private var flashModeSelected: FlashType =FlashType.NOT_FLASH

    fun setFlashMode(flashType: FlashType){
        flashModeSelected=flashType
    }

    fun getFlashMode()=flashModeSelected}