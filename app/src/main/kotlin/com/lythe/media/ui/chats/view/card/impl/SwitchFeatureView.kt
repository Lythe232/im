package com.lythe.media.ui.chats.view.card.impl

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lythe.media.R
import com.lythe.media.ui.chats.view.card.CardFeatureView

class SwitchFeatureView: CardFeatureView {
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

    interface OnSwitchFeatureActionListener {
        fun onItemClick();
    }
    private lateinit var listener: OnSwitchFeatureActionListener
    private lateinit var titleTv: TextView
    private lateinit var spaceView: View
    private lateinit var descTv: TextView
    private lateinit var switchMaterial: SwitchMaterial

    override fun initView() {

        titleTv = TextView(context).apply {
            textSize = 16f
            setTextColor(0xFF333333.toInt())
        }
        addView(titleTv)
        spaceView = View(context).apply {
            layoutParams = LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        addView(spaceView)
        descTv = TextView(context).apply {
            textSize = 12f
        }
        addView(descTv)
        switchMaterial = SwitchMaterial(context)
            .apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        switchMaterial.isChecked = false

        addView(switchMaterial)
    }

    override fun bindEvents() {
        setOnClickListener{
            this.listener.onItemClick()
        }

    }
    fun setSwitchFeatureActionListener(listener:  OnSwitchFeatureActionListener) {
        this.listener = listener
    }
    fun setTitle(title: String) {
        this.titleTv.text = title
    }
    fun setSwitchState(flag: Boolean) {
        this.switchMaterial.isChecked = flag
    }
}