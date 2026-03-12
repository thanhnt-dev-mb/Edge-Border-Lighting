package com.merryblue.baseapplication.ui.home.color

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.merryblue.baseapplication.coredata.model.edge.EdgeColorItem
import com.merryblue.baseapplication.databinding.ItemEdgeColorBinding

class EdgeColorAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<EdgeColorAdapter.EdgeColorViewHolder>() {
    private var items: List<EdgeColorItem> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<EdgeColorItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class EdgeColorViewHolder(val binding: ItemEdgeColorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindViews(item: EdgeColorItem) = with (binding) {
            val dots = listOf(ivColor1, ivColor2, ivColor3, ivColor4)

            dots.forEach { it.visibility = View.GONE }

            item.colors.forEachIndexed { index, colorInt ->
                if (index < dots.size) {
                    dots[index].visibility = View.VISIBLE
                    dots[index].imageTintList = ColorStateList.valueOf(colorInt)
                }
            }

            root.isSelected = item.isSelected

            itemView.setOnClickListener {
                onClick.invoke(bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EdgeColorViewHolder {
        val binding = ItemEdgeColorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EdgeColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EdgeColorViewHolder, position: Int) {
        val item = items[position]
        holder.bindViews(item)
    }

    override fun getItemCount(): Int = items.size
}