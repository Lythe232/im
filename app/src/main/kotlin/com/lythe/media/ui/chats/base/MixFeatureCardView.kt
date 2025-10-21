package com.lythe.media.ui.chats.base

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lythe.media.R
import com.lythe.media.ui.chats.data.enums.FeatureType
import com.lythe.media.ui.chats.data.model.CardModel
import com.lythe.media.ui.chats.data.model.FeatureModel
import com.lythe.media.ui.chats.viewmodel.MixFeatureCardViewModel
import com.lythe.media.ui.chats.viewmodel.TextFeatureCardViewModel

class MixFeatureCardView: LinearLayout {
    constructor(context: Context?) : super(context) {
        initView()
        initData()
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
        initData()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
        initData()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView()
        initData()
    }

    interface MixFeatureCardListener {
        fun onCardClick(cardModel: CardModel)
        fun onFeatureClick(cardId: String, featureType: FeatureType, featureText: String?)
    }
    private val TAG: String = "MixFeatureCardView"
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: MixFeatureCardViewModel
    private lateinit var observer: Observer<List<FeatureModel>>
    private lateinit var adapter: MixFeatureCardAdapter
    private var listener: MixFeatureCardListener? = null

    private fun initView() {
        Log.d(TAG, "initView")
        inflate(context, R.layout.layout_mix_feature_card, this)
        recyclerView = findViewById(R.id.layout_mix_feature_card_recycler)
        recyclerView.layoutManager = LinearLayoutManager(context)
    }
    private fun initData() {
        Log.d(TAG, "initData")
        viewModel = MixFeatureCardViewModel()
        adapter = MixFeatureCardAdapter(ArrayList())
        observer = Observer {
                featureCard ->
            adapter.updateCards(featureCard)
        }
        viewModel.featureModel.observeForever(observer)
        viewModel.fetchData()
        recyclerView.adapter = adapter
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel.featureModel.removeObserver(observer)
    }
}