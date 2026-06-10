package com.merryblue.baseapplication.ui.theme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ItemChooseFromGalleryBinding
import com.merryblue.baseapplication.databinding.ItemHomeParallaxPreviewBinding
import com.merryblue.baseapplication.databinding.ItemHomePresetBinding
import com.merryblue.baseapplication.databinding.ItemHomeThemeCustomBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.ThemeUi
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.ui.wallpaper.ParallaxPreviewLoader

class ThemeChildAdapter(
    private val onClickTheme: (Item) -> Unit,
    private val onClickGallery: (ThemeUi.Gallery) -> Unit,
    private val onClickCustom: (ThemeUi.Custom) -> Unit,
) : PagingDataAdapter<ThemeUi, RecyclerView.ViewHolder>(ThemeUiDiff()) {
    private val attachedParallaxHolders = LinkedHashSet<ParallaxThemeVH>()
    private var parallaxThumbMotionEnabled = true

    fun setParallaxThumbMotionEnabled(enabled: Boolean) {
        if (parallaxThumbMotionEnabled == enabled) return
        parallaxThumbMotionEnabled = enabled
        attachedParallaxHolders.forEach { holder ->
            holder.setMotionEnabled(enabled)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ThemeUi.Gallery -> TYPE_GALLERY
            is Item -> if (item.isParallax()) TYPE_PARALLAX_THEME else TYPE_THEME
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
            TYPE_PARALLAX_THEME -> {
                val binding = ItemHomeParallaxPreviewBinding.inflate(inflater, parent, false)
                ParallaxThemeVH(binding)
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
            is ParallaxThemeVH -> (getItem(position) as? Item)?.let(holder::bind)
            is ThemeCustomVH -> (getItem(position) as? ThemeUi.Custom)?.let(holder::bind)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is ParallaxThemeVH) {
            attachedParallaxHolders += holder
            holder.setMotionEnabled(parallaxThumbMotionEnabled)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ParallaxThemeVH) {
            attachedParallaxHolders -= holder
            holder.setMotionEnabled(false)
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ParallaxThemeVH) {
            attachedParallaxHolders -= holder
            holder.recycle()
        }
        super.onViewRecycled(holder)
    }

    inner class ThemeVH(private val binding: ItemHomePresetBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) = with(binding) {
            Glide.with(root.context)
                .load(item.thumbUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(ivHomePreset)

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClickTheme.invoke(item)
                }
            }
        }
    }

    inner class ParallaxThemeVH(private val binding: ItemHomeParallaxPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        private val previewLoader = ParallaxPreviewLoader(binding.parallaxPreview)

        fun bind(item: Item) = with(binding) {
            previewLoader.bind(item.thumbUrl.ifBlank { item.pathUrl }, parallaxThumbMotionEnabled)

            root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClickTheme.invoke(item)
                }
            }
        }

        fun setMotionEnabled(enabled: Boolean) {
            previewLoader.setMotionEnabled(enabled)
        }

        fun recycle() {
            previewLoader.recycle()
        }
    }

    inner class GalleryVH(private val binding: ItemChooseFromGalleryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ThemeUi.Gallery) {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClickGallery.invoke(item)
                }
            }
        }
    }

    inner class ThemeCustomVH(private val binding: ItemHomeThemeCustomBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ThemeUi.Custom) {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClickCustom.invoke(item)
                }
            }
        }
    }

    private fun Item.isParallax(): Boolean {
        return type.equals(WallpaperType.TYPE_PARALLAX, ignoreCase = true)
    }

    companion object {
        private const val TYPE_GALLERY = 0
        private const val TYPE_THEME = 1
        private const val TYPE_CUSTOM = 2
        private const val TYPE_PARALLAX_THEME = 3
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
