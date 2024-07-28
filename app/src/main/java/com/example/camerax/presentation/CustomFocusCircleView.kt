package com.example.camerax.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CustomFocusCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr){

    private var radius=0f
    private var centerX=0f
    private var centerY=0f

    private val paint: Paint = Paint().apply {
        color= Color.parseColor("#FFC20F")
        style= Paint.Style.STROKE
        strokeWidth=5f
        isAntiAlias=false
    }

    fun setCircle(x:Float,y:Float,radius:Float){
        this.centerY=y
        this.centerX=x
        this.radius=radius
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX,centerY,radius,paint)
    }
}