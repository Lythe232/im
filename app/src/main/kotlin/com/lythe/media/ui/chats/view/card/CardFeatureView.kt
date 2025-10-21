package com.lythe.media.ui.chats.view.card

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

abstract class CardFeatureView: LinearLayout {
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

    init {
        orientation = HORIZONTAL
        setPadding(dp2px(12), dp2px(12), dp2px(12), dp2px(12))
        initView()
        bindEvents()
    }

    protected abstract fun initView()
    protected abstract fun bindEvents()

    protected fun dp2px(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

}