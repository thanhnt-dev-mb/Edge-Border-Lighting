package com.merryblue.baseapplication.ui.theme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ItemChooseFromGalleryBinding
import com.merryblue.baseapplication.databinding.ItemHomePresetBinding
import com.merryblue.baseapplication.databinding.ItemHomeThemeCustomBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.ThemeUi

class ThemeChildAdapter(
    private val onClickTheme: (Item) -> Unit,
    private val onClickGallery: (ThemeUi.Gallery) -> Unit,
    private val onClickCustom: (ThemeUi.Custom) -> Unit,
) : PagingDataAdapter<ThemeUi, RecyclerView.ViewHolder>(ThemeUiDiff()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ThemeUi.Gallery -> TYPE_GALLERY
            is Item -> TYPE_THEME
            else -> TYPE_CUSTOM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_GALLERY -> {
                val binding = ItemChooseFromGalleryBinding.inflate(inflater, parent, false)
                GalleryVH(binding)
            }
            TYPE_THEME -> {
                val binding = ItemHomePresetBinding.inflate(inflater, parent, false)
                ThemeVH(binding)
            }
            else -> {
                val binding = ItemHomeThemeCustomBinding.inflate(inflater, parent, false)
                ThemeCustomVH(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GalleryVH -> (getItem(position) as? ThemeUi.Gallery)?.let(holder::bind)
            is ThemeVH -> (getItem(position) as? Item)?.let(holder::bind)
            is ThemeCustomVH -> (getItem(position) as? ThemeUi.Custom)?.let(holder::bind)
        }
    }

    inner class ThemeVH(private val binding: ItemHomePresetBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) = with(binding) {
            Glide.with(root.context)
                .load(item.thumbUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(ivHomePreset)

            itemView.setOnClickListener { onClickTheme.invoke(item) }
        }
    }

    inner class GalleryVH(private val binding: ItemChooseFromGalleryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ThemeUi.Gallery) {
            binding.root.setOnClickListener { onClickGallery.invoke(item) }
        }
    }

    inner class ThemeCustomVH(private val binding: ItemHomeThemeCustomBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ThemeUi.Custom) {
            binding.root.setOnClickListener { onClickCustom.invoke(item) }
        }
    }

    companion object {
        private const val TYPE_GALLERY = 0
        private const val TYPE_THEME = 1
        private const val TYPE_CUSTOM = 2
    }
}

class ThemeUiDiff : DiffUtil.ItemCallback<ThemeUi>() {

    override fun areItemsTheSame(oldItem: ThemeUi, newItem: ThemeUi): Boolean {
        return when {
            oldItem is Item && newItem is Item -> oldItem.id == newItem.id
            oldItem is ThemeUi.Gallery && newItem is ThemeUi.Gallery -> oldItem.id == newItem.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ThemeUi, newItem: ThemeUi): Boolean {
        return oldItem == newItem
    }
}
