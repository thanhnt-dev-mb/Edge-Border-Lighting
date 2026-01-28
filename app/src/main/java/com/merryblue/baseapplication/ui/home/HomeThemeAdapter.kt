package com.merryblue.baseapplication.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ItemHomePresetBinding
import com.merryblue.baseapplication.databinding.ItemHomeThemeCustomBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.ThemeUi

class HomeThemeAdapter(
    private val customOnClick: () -> Unit,
    private val normalOnClick: (Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val listTheme = mutableListOf<ThemeUi>()

    fun submitList(list: List<ThemeUi>) {
        listTheme.clear()
        listTheme.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        when (listTheme[position]) {
            is ThemeUi.Custom -> TYPE_THEME_CUSTOM
            is Item -> TYPE_THEME_NORMAL
            else -> TYPE_THEME_NORMAL
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_THEME_CUSTOM -> ThemeCustomViewHolder(ItemHomeThemeCustomBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> ThemeNormalViewHolder(ItemHomePresetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = listTheme[position]) {
            is ThemeUi.Custom -> (holder as ThemeCustomViewHolder).bind()
            is Item -> (holder as ThemeNormalViewHolder).bind(item)
            else -> {}
        }
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

    companion object {
        private const val TYPE_THEME_CUSTOM = 0
        private const val TYPE_THEME_NORMAL = 1
    }
}
