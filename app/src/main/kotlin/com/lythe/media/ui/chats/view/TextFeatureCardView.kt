package com.lythe.media.ui.chats.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Observer
import com.lythe.media.R
import com.lythe.media.ui.chats.data.enums.FeatureType
import com.lythe.media.ui.chats.data.model.CardModel
import com.lythe.media.ui.chats.view.card.impl.TextFeatureView
import com.lythe.media.ui.chats.viewmodel.TextFeatureCardViewModel


class TextFeatureCardView: LinearLayout {
    private lateinit var viewModel: TextFeatureCardViewModel
    private lateinit var observer: Observer<List<CardModel>>
    private var listener: TextFeatureCardListener? = null
    private lateinit var textFeatureView: TextFeatureView
    private lateinit var context: Context
    private val TAG: String = "TextFeatureCardView"
    constructor(context: Context?) : super(context) {
        if (context != null) {
            this.context = context
        }
        initView()
        initData()
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        if (context != null) {
            this.context = context
        }
        initView()
        initData()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        if (context != null) {
            this.context = context
        }
        initView()
        initData()
    }
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        if (context != null) {
            this.context = context
        }
        initView()
        initData()
    }
    private fun initView() {
        textFeatureView = TextFeatureView(context).apply {
            setPadding(dp2px(24), dp2px(8), dp2px(24), dp2px(8))
        }
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        textFeatureView.layoutParams = layoutParams
        addView(textFeatureView)
    }
    private fun initData() {
        viewModel = TextFeatureCardViewModel()
        observer = Observer {
                cardModels ->
            textFeatureView.setTitle(cardModels[0].title.toString())
            textFeatureView.setDesc(cardModels[0].desc.toString())
        }
        viewModel.cardModel.observeForever(observer)
        viewModel.fetchData()
    }
    fun setCardData(cardModels: List<CardModel>) {
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel.cardModel.removeObserver(observer)
    }
    protected fun dp2px(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
interface TextFeatureCardListener {
    fun onCardClick(cardModel: CardModel)
    fun onFeatureClick(cardId: String, featureType: FeatureType, featureText: String?)
}

