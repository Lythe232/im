package com.lythe.media.ui.chats.view.card.impl

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.size
import com.lythe.media.R
import com.lythe.media.ui.chats.view.card.CardFeatureView

class ImageFeatureView: CardFeatureView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)
    interface OnImageFeatureActionListener {
        fun onItemClick();
    }
    private lateinit var listener: OnImageFeatureActionListener
    private lateinit var imageView: ImageView
    private lateinit var titleTv: TextView
    private lateinit var spaceView: View
    private lateinit var descTv: TextView
    private lateinit var arrowIcon: ImageView
    override fun initView() {
        imageView = ImageView(context).apply {
            setBackgroundResource(R.drawable.shape_circle_avatar)
            setImageResource(R.drawable.avatar3)
            layoutParams = ViewGroup.LayoutParams(
                dp2px(40),dp2px(40)
            )
            gravity = Gravity.CENTER

        }
        titleTv = TextView(context).apply {
            textSize = 16f
            setTextColor(0xFF333333.toInt())
        }
        spaceView = View(context).apply {
            layoutParams = LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        descTv = TextView(context).apply {
            textSize = 12f
        }
        arrowIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_right_arrow)
            layoutParams = ViewGroup.LayoutParams(
                dp2px(24),
                dp2px(24)
            )
        }
        addView(imageView)
        addView(titleTv)
        addView(spaceView)
        addView(descTv)
        addView(arrowIcon)
    }

    override fun bindEvents() {
        setOnClickListener {
            this.listener.onItemClick()
        }
    }

    fun setImageFeatureActionListener(listener: OnImageFeatureActionListener) {
        this.listener = listener
    }
    fun setTitle(text: String) {
        this.titleTv.text = text
    }
    fun setDesc(desc: String) {
        this.descTv.text = desc
    }
}