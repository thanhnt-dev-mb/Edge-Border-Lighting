package com.merryblue.baseapplication.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ItemHomePresetBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.getThumbUrl
import timber.log.Timber

class HomePresetAdapter(
    private val presetOnClick: (Item) -> Unit
): RecyclerView.Adapter<HomePresetAdapter.PresetViewHolder>() {

    private var items = mutableListOf<Item>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int, ): PresetViewHolder {
        val binding = ItemHomePresetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PresetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int, ) {
        val item = items[position]
        holder.bindViews(item)
    }

    override fun getItemCount(): Int = items.size

    fun submitList(items: List<Item>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class PresetViewHolder(val binding: ItemHomePresetBinding): RecyclerView.ViewHolder(binding.root) {
        fun bindViews(item: Item) {
            binding.apply {
                Glide.with(root.context).load(item.getThumbUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(ivHomePreset)

                itemView.setOnClickListener {
                    presetOnClick.invoke(item)
                }
            }
        }
    }
}