package com.lythe.media.ui.chats.view.card.impl

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import com.lythe.media.R
import com.lythe.media.ui.chats.view.card.CardFeatureView

class TextFeatureView: CardFeatureView {
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

    interface OnTextFeatureActionListener {
        fun onItemClick()
    }
    private lateinit var listener: OnTextFeatureActionListener
    private lateinit var titleTv: TextView
    private lateinit var spaceView: View
    private lateinit var descTv: TextView
    private lateinit var arrowIcon: ImageView
    private val TAG = "TextFeatureView";

    override fun initView() {
        background = ContextCompat.getDrawable(context, R.drawable.selector_click_feedback)
        isClickable = true
        isFocusable = true

        orientation = VERTICAL
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val contentLayout = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = HORIZONTAL
            titleTv = TextView(context).apply {
                textSize = 16f
                setTextColor(0xFF333333.toInt())
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = dp2px(8) // 与右侧控件保持距离
                }
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
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = dp2px(8) // 与箭头保持距离
                    gravity = android.view.Gravity.CENTER_VERTICAL // 垂直居中
                }
            }
            addView(descTv)

            arrowIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_right_arrow)
                layoutParams = ViewGroup.LayoutParams(
                    dp2px(24),
                    dp2px(24)
                )
            }
            addView(arrowIcon)
        }
        addView(contentLayout)
        setTextFeatureActionListener(object : OnTextFeatureActionListener {
            override fun onItemClick() {
                Log.d(TAG, "Not yet implemented")
            }

        })
//        val dividerView = View(context).apply {
//            layoutParams = LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT, // 宽度填满
//                dp2px(1)  // 高度为1dp
//            )
//            setBackgroundColor(0xFFCCCCCC.toInt()) // 设置分割线颜色
//        }
//        addView(dividerView)
    }
    override fun bindEvents() {
        setOnClickListener {
                this.listener.onItemClick()
        }
    }
    fun setTextFeatureActionListener(listener: OnTextFeatureActionListener) {
        this.listener = listener
    }
    fun setTitle(text: String) {
        this.titleTv.text = text
    }
    fun setDesc(desc: String) {
        this.descTv.text = desc
    }
}