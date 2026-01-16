package com.merryblue.baseapplication.ui.theme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ItemHomePresetBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.getThumbUrl

class ThemeChildAdapter(
    private val onClick: (Item) -> Unit
): PagingDataAdapter<Item, ThemeChildAdapter.ThemeChildViewHolder>(DIFF()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int, ): ThemeChildViewHolder {
        val binding = ItemHomePresetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeChildViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeChildViewHolder, position: Int, ) {
        getItem(position)?.let { holder.bindView(item = it) }
    }

    inner class ThemeChildViewHolder(val binding: ItemHomePresetBinding): RecyclerView.ViewHolder(binding.root) {
        fun bindView(item: Item) {
            binding.apply {
                Glide.with(root.context).load(item.getThumbUrl())
                    .error(R.drawable.disptrending1)
                    .into(ivHomePreset)

                itemView.setOnClickListener {
                    onClick.invoke(item)
                }
            }

        }
    }
}

class DIFF: DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem

}