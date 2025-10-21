package com.lythe.media.ui.chats.view.card

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.lythe.media.R

class InfoCardView: LinearLayout {
    private val featureViews = mutableListOf<CardFeatureView>()
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
        orientation = VERTICAL
        setBackgroundResource(R.drawable.shape_card_bg)
        setPadding(
            dp2px(16),
            dp2px(16),
            dp2px(16),
            dp2px(16)
        )
    }

    fun addFeatureView(featureView: CardFeatureView) {
        if(featureViews.isNotEmpty()) {
            addView(DividerView(context))
        }
        addView(featureView)
        featureViews.add(featureView)
    }
    fun addFeatureViews(views: List<CardFeatureView>) {
        views.forEach{addFeatureView(it)}
    }

    private fun dp2px(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}