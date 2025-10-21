package com.lythe.media.ui.chats.base

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.lythe.media.R
import com.lythe.media.databinding.FragmentTextFeatureCardBinding
import com.lythe.media.ui.chats.data.model.CardModel
import com.lythe.media.ui.chats.viewmodel.TextFeatureCardViewModel

class TextFeatureCardFragment : Fragment() {

    private lateinit var binding: FragmentTextFeatureCardBinding
    private lateinit var viewModel: TextFeatureCardViewModel
    private val TAG: String = "TextFeatureCardFragment"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentTextFeatureCardBinding.inflate(LayoutInflater.from(context))
        viewModel = TextFeatureCardViewModel()
        arguments?.let {
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView")
        initView()

        setupRecyclerView()

        initData()
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TextFeatureCardFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
    private fun initView() {
        val view = binding.root
        view.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.fragment_background
            )
        )
        if(view.layoutParams is ViewGroup.MarginLayoutParams) {
            val marginParams = view.layoutParams as ViewGroup.MarginLayoutParams
            val margin = dp2px(16)
            marginParams.setMargins(margin, margin, margin, margin)
            view.layoutParams = marginParams
        }
    }
    private fun setupRecyclerView() {
        val recyclerView = binding.fragmentTextFeatureCardRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setBackgroundResource(R.drawable.shape_recycler_bg)
    }
    private fun initData() {
        val recyclerView = binding.fragmentTextFeatureCardRecyclerView
        val adapter = TextFeatureCardAdapter(ArrayList<CardModel>())

        viewModel.cardModel.observe(viewLifecycleOwner, Observer {
            viewModel.cardModel.value?.let { it1 -> adapter.updateCards(it1) }
        })
        viewModel.fetchData()

        recyclerView.adapter = adapter
    }
    protected fun dp2px(dp: Int): Int {
        val density = binding.root.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}