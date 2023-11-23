package com.dsd.kosjenka.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.databinding.FooterPagingBinding

class MyLoadStateAdapter(private val retry: () -> Unit) :
    LoadStateAdapter<MyLoadStateAdapter.LoadStateViewHolder>() {

    class LoadStateViewHolder(private val binding: FooterPagingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(loadState: LoadState, onRetryCLickListener: (() -> Unit)) {
            binding.apply {
                footerLoading.isVisible = loadState is LoadState.Loading
                footerErrorTv.isVisible = loadState !is LoadState.Loading
                footerRetryBtn.isVisible = loadState !is LoadState.Loading

                root.setOnClickListener {
                    onRetryCLickListener.invoke()
                }

            }
        }

        companion object {
            fun from(parent: ViewGroup): LoadStateViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =
                    FooterPagingBinding.inflate(layoutInflater, parent, false)
                return LoadStateViewHolder(binding)
            }
        }

    }

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState, retry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        return LoadStateViewHolder.from(parent)
    }
}