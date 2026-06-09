package com.merryblue.baseapplication.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ItemHomeParallaxPreviewBinding
import com.merryblue.baseapplication.databinding.ItemHomePresetBinding
import com.merryblue.baseapplication.databinding.ItemHomeThemeCustomBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.ThemeUi
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.ui.wallpaper.ParallaxPreviewLoader

class HomeThemeAdapter(
    private val customOnClick: () -> Unit,
    private val normalOnClick: (Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val listTheme = mutableListOf<ThemeUi>()
    private val attachedParallaxHolders = LinkedHashSet<ThemeParallaxViewHolder>()
    private var parallaxThumbMotionEnabled = true

    fun setParallaxThumbMotionEnabled(enabled: Boolean) {
        if (parallaxThumbMotionEnabled == enabled) return
        parallaxThumbMotionEnabled = enabled
        attachedParallaxHolders.forEach { holder ->
            holder.setMotionEnabled(enabled)
        }
    }

    fun submitList(list: List<ThemeUi>) {
        if (listTheme == list) return
        listTheme.clear()
        listTheme.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (val item = listTheme[position]) {
        is ThemeUi.Custom -> TYPE_THEME_CUSTOM
        is Item -> if (item.isParallax()) TYPE_THEME_PARALLAX else TYPE_THEME_NORMAL
        else -> TYPE_THEME_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_THEME_CUSTOM -> ThemeCustomViewHolder(ItemHomeThemeCustomBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_THEME_PARALLAX -> ThemeParallaxViewHolder(ItemHomeParallaxPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> ThemeNormalViewHolder(ItemHomePresetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = listTheme[position]) {
            is ThemeUi.Custom -> (holder as ThemeCustomViewHolder).bind()
            is Item -> when (holder) {
                is ThemeParallaxViewHolder -> holder.bind(item)
                is ThemeNormalViewHolder -> holder.bind(item)
            }
            else -> {}
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is ThemeParallaxViewHolder) {
            attachedParallaxHolders += holder
            holder.setMotionEnabled(parallaxThumbMotionEnabled)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is ThemeParallaxViewHolder) {
            attachedParallaxHolders -= holder
            holder.setMotionEnabled(false)
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ThemeParallaxViewHolder) {
            attachedParallaxHolders -= holder
            holder.recycle()
        }
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = listTheme.size

    inner class ThemeCustomViewHolder(private val binding: ItemHomeThemeCustomBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.root.setOnClickListener {
                customOnClick.invoke()
            }
        }
    }

    inner class ThemeNormalViewHolder(private val binding: ItemHomePresetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) = with(binding) {
            Glide.with(root.context)
                .load(item.thumbUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(ivHomePreset)

            root.setOnClickListener {
                normalOnClick.invoke(item)
            }
        }
    }

    inner class ThemeParallaxViewHolder(
        private val binding: ItemHomeParallaxPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val previewLoader = ParallaxPreviewLoader(binding.parallaxPreview)

        fun bind(item: Item) = with(binding) {
            previewLoader.bind(item.thumbUrl.ifBlank { item.pathUrl }, parallaxThumbMotionEnabled)

            root.setOnClickListener {
                normalOnClick.invoke(item)
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
        private const val TYPE_THEME_CUSTOM = 0
        private const val TYPE_THEME_NORMAL = 1
        private const val TYPE_THEME_PARALLAX = 2
    }
}
