package com.merryblue.baseapplication.ui.text

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ItemTextScrollerBackgroundBinding
import com.merryblue.baseapplication.databinding.ItemTextScrollerEffectBinding
import com.merryblue.baseapplication.databinding.ItemTextScrollerTextBinding
import com.merryblue.baseapplication.databinding.ItemTextScrollerThemeBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.ui.text.model.Background
import com.merryblue.baseapplication.ui.text.model.Text
import com.merryblue.baseapplication.ui.text.model.TextEffect
import com.merryblue.baseapplication.ui.text.model.Themes

class TextScrollerOptionAdapter(
    private val fontProvider: (String) -> Typeface,
    private val onClick: (TextScrollerOption) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<TextScrollerOption> = emptyList()
    private var selectedKeys: Set<String> = emptySet()

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(newItems: List<TextScrollerOption>, selected: Set<String>) {
        items = newItems
        selectedKeys = selected
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateSelection(selected: Set<String>) {
        selectedKeys = selected
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TextScrollerOption.ThemeOption -> VIEW_TYPE_THEME
            is TextScrollerOption.TextOption -> VIEW_TYPE_TEXT
            is TextScrollerOption.BackgroundOption -> VIEW_TYPE_BACKGROUND
            is TextScrollerOption.EffectGroupOption -> VIEW_TYPE_EFFECT
            is TextScrollerOption.EffectOption -> VIEW_TYPE_EFFECT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_THEME -> ThemeVH(ItemTextScrollerThemeBinding.inflate(inflater, parent, false))
            VIEW_TYPE_TEXT -> TextVH(ItemTextScrollerTextBinding.inflate(inflater, parent, false))
            VIEW_TYPE_BACKGROUND -> BackgroundVH(ItemTextScrollerBackgroundBinding.inflate(inflater, parent, false))
            else -> EffectVH(ItemTextScrollerEffectBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when {
            holder is ThemeVH && item is TextScrollerOption.ThemeOption -> holder.bind(item)
            holder is TextVH && item is TextScrollerOption.TextOption -> holder.bind(item)
            holder is BackgroundVH && item is TextScrollerOption.BackgroundOption -> holder.bind(item)
            holder is EffectVH && item is TextScrollerOption.EffectGroupOption -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ThemeVH(private val binding: ItemTextScrollerThemeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: TextScrollerOption.ThemeOption) = with(binding) {
            val context = root.context
            val item = option.item
            val selected = selectedKeys.contains(option.key)
            root.background = roundedDrawable(
                fillColor = ContextCompat.getColor(context, item.background),
                selected = selected,
                radiusDp = 8f
            )
            tvName.text = context.getString(item.content)
            tvName.setTextColor(ContextCompat.getColor(context, item.contentColor))
            tvName.typeface = fontProvider(item.font)
            root.setOnClickListener { onClick(option) }
        }
    }

    inner class TextVH(private val binding: ItemTextScrollerTextBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: TextScrollerOption.TextOption) = with(binding) {
            val context = root.context
            val item = option.item
            val selected = selectedKeys.contains(option.key)
            root.background = roundedDrawable(
                fillColor = ContextCompat.getColor(context, R.color.color_0f0f30),
                selected = selected,
                radiusDp = 8f
            )
            tvName.text = context.getString(item.content)
            tvName.setTextColor(ContextCompat.getColor(context, R.color.colorWhite))
            tvName.typeface = fontProvider(item.font)
            root.setOnClickListener { onClick(option) }
        }
    }

    inner class BackgroundVH(private val binding: ItemTextScrollerBackgroundBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: TextScrollerOption.BackgroundOption) = with(binding) {
            val context = root.context
            val selected = selectedKeys.contains(option.key)
            viewColor.background = ovalDrawable(
                fillColor = ContextCompat.getColor(context, option.item.color),
                selected = selected
            )
            root.setOnClickListener { onClick(option) }
        }
    }

    inner class EffectVH(private val binding: ItemTextScrollerEffectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: TextScrollerOption.EffectGroupOption) = with(binding) {
            val effects = option.items
            bindEffect(
                root = itemScroller,
                iconContainer = scrollerIconContainer,
                icon = ivScrollerIcon,
                title = tvScrollerName,
                effect = effects.getOrNull(0)
            )
            bindEffect(
                root = itemBlink,
                iconContainer = blinkIconContainer,
                icon = ivBlinkIcon,
                title = tvBlinkName,
                effect = effects.getOrNull(1)
            )
        }

        private fun bindEffect(
            root: android.view.View,
            iconContainer: android.view.View,
            icon: androidx.appcompat.widget.AppCompatImageView,
            title: androidx.appcompat.widget.AppCompatTextView,
            effect: TextEffect?
        ) {
            if (effect == null) return
            val context = root.context
            val selected = selectedKeys.contains("effect_${effect.type.name}")
            iconContainer.background = roundedDrawable(
                fillColor = EFFECT_BOX_COLOR,
                selected = selected,
                radiusDp = 12f
            )
            icon.setImageResource(effect.icon)
            icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorWhite))
            title.text = context.getString(effect.content)
            root.setOnClickListener { onClick(TextScrollerOption.EffectOption(effect)) }
        }
    }

    private fun roundedDrawable(
        @ColorInt fillColor: Int,
        selected: Boolean,
        radiusDp: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp.dpToPx
            setColor(fillColor)
            setStroke(
                if (selected) 1.5f.dpToPx.toInt().coerceAtLeast(1) else 0,
                if (selected) Color.WHITE else Color.TRANSPARENT
            )
        }
    }

    private fun ovalDrawable(@ColorInt fillColor: Int, selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
            setStroke(
                if (selected) 3f.dpToPx.toInt().coerceAtLeast(1) else 0,
                if (selected) Color.WHITE else Color.TRANSPARENT
            )
        }
    }

    companion object {
        private const val VIEW_TYPE_THEME = 1
        private const val VIEW_TYPE_TEXT = 2
        private const val VIEW_TYPE_BACKGROUND = 3
        private const val VIEW_TYPE_EFFECT = 4
        private val EFFECT_BOX_COLOR = Color.rgb(52, 52, 87)
    }
}

sealed class TextScrollerOption {
    abstract val key: String

    data class ThemeOption(val item: Themes) : TextScrollerOption() {
        override val key: String = "theme_${item.id}"
    }

    data class TextOption(val item: Text) : TextScrollerOption() {
        override val key: String = "text_${item.id}"
    }

    data class BackgroundOption(val item: Background) : TextScrollerOption() {
        override val key: String = "background_${item.id}"
    }

    data class EffectGroupOption(val items: List<TextEffect>) : TextScrollerOption() {
        override val key: String = "effect_group"
    }

    data class EffectOption(val item: TextEffect) : TextScrollerOption() {
        override val key: String = "effect_${item.type.name}"
    }
}
