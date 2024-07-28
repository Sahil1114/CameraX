package com.example.camerax.presentation

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.camerax.R
import com.example.camerax.databinding.IconLayoutBinding
import com.example.camerax.utils.FlashType

class FlashAdapter(
    private val flashList: List<FlashType>,
    private val context: Context,
    private val listener: OnItemClickListener,
    private val selectFlashMode:FlashType
)
    : RecyclerView.Adapter<FlashAdapter.FlashViewHolder>(){

    inner class FlashViewHolder(private val binding: IconLayoutBinding): RecyclerView.ViewHolder(binding.root){
        val getFlashIconNotSelected:(FlashType)->Int={flashType->
            when(flashType){
                FlashType.ONE_TIME_FLASH -> {
                    R.drawable.flashwhite
                }
                FlashType.NOT_FLASH -> {
                    R.drawable.notflashwhite
                }
                FlashType.AUTO_FLASH -> {
                    R.drawable.autoflashwhite
                }
                FlashType.PERMANENT_FLASH ->{
                    R.drawable.permanentflashwhite
                }
            }
        }

        val getFlashIconSelected:(FlashType)->Int={flashType->
            when(flashType){
                FlashType.ONE_TIME_FLASH -> {
                    R.drawable.flash
                }
                FlashType.NOT_FLASH -> {
                    R.drawable.notflash
                }
                FlashType.AUTO_FLASH -> {
                    R.drawable.autoflash
                }
                FlashType.PERMANENT_FLASH ->{
                    R.drawable.permanentflash
                }
            }
        }

        fun bind(flashType: FlashType){
            binding.apply {

                Log.d("flashListInfo",flashType.toString())
                val iconFlash=if (flashType!=selectFlashMode){
                    getFlashIconNotSelected(flashType)
                }else{
                    getFlashIconSelected(flashType)
                }
                Glide.with(context)
                    .load(iconFlash)
                    .into(ivFlash)

                ivFlash.setOnClickListener {
                    listener.onFlashClicked(flashType)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlashViewHolder=FlashViewHolder(
        IconLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun getItemCount(): Int {
        return flashList.size
    }

    override fun onBindViewHolder(holder: FlashViewHolder, position: Int){
        Log.d("flashListInfo","bindViewHolder $position ${flashList[position]}")
        holder.bind(flashList[position])
    }
}

interface OnItemClickListener {
    fun onFlashClicked(data:FlashType)
}