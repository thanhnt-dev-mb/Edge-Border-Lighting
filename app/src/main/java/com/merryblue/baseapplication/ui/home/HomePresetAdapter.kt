package com.merryblue.baseapplication.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ItemHomeParallaxPreviewBinding
import com.merryblue.baseapplication.databinding.ItemHomePresetBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.ui.wallpaper.ParallaxPreviewLoader

class HomePresetAdapter(
    private val presetOnClick: (Item) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = mutableListOf<Item>()
    private val attachedParallaxHolders = LinkedHashSet<ParallaxPresetViewHolder>()
    private var parallaxThumbMotionEnabled = true

    fun setParallaxThumbMotionEnabled(enabled: Boolean) {
        if (parallaxThumbMotionEnabled == enabled) return
        parallaxThumbMotionEnabled = enabled
        attachedParallaxHolders.forEach { holder ->
            holder.setMotionEnabled(enabled)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isParallax()) TYPE_PARALLAX_PRESET else TYPE_PRESET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PARALLAX_PRESET -> {
                val binding = ItemHomeParallaxPreviewBinding.inflate(inflater, parent, false)
                ParallaxPresetViewHolder(binding)
            }
            else -> {
                val binding = ItemHomePresetBinding.inflate(inflater, parent, false)
                PresetViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ParallaxPresetViewHolder -> holder.bindViews(item)
            is PresetViewHolder -> holder.bindViews(item)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is ParallaxPresetViewHolder) {
            attachedParallaxHolders += holder
            holder.setMotionEnabled(parallaxThumbMotionEnabled)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ParallaxPresetViewHolder) {
            attachedParallaxHolders -= holder
            holder.setMotionEnabled(false)
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ParallaxPresetViewHolder) {
            attachedParallaxHolders -= holder
            holder.recycle()
        }
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = items.size

    fun submitList(items: List<Item>) {
        if (this.items == items) return
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class PresetViewHolder(val binding: ItemHomePresetBinding): RecyclerView.ViewHolder(binding.root) {
        fun bindViews(item: Item) {
            binding.apply {
                Glide.with(root.context).load(item.thumbUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(ivHomePreset)

                itemView.setOnClickListener {
                    presetOnClick.invoke(item)
                }
            }
        }
    }

    inner class ParallaxPresetViewHolder(private val binding: ItemHomeParallaxPreviewBinding): RecyclerView.ViewHolder(binding.root) {
        private val previewLoader = ParallaxPreviewLoader(binding.parallaxPreview)

        fun bindViews(item: Item) = with(binding) {
            previewLoader.bind(item.thumbUrl.ifBlank { item.pathUrl }, parallaxThumbMotionEnabled)

            root.setOnClickListener {
                presetOnClick.invoke(item)
            }
        }

        fun setMotionEnabled(enabled: Boolean) {
            previewLoader.setMotionEnabled(enabled)
        }

        fun recycle() {
            previewLoader.recycle()
        }
    }

    private fun Item.isParallax(): Boolean {
        return type.equals(WallpaperType.TYPE_PARALLAX, ignoreCase = true)
    }

    companion object {
        private const val TYPE_PRESET = 0
        private const val TYPE_PARALLAX_PRESET = 1
    }
}