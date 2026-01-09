package com.merryblue.baseapplication.ui.home.effect

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.merryblue.baseapplication.coredata.model.edge.EdgeEffectItem
import com.merryblue.baseapplication.databinding.ItemEdgeEffectBinding

@SuppressLint("NotifyDataSetChanged")
class EdgeEffectAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<EdgeEffectAdapter.EdgeEffectViewHolder>() {
    private var items: List<EdgeEffectItem> = emptyList()

    fun submitList(newItems: List<EdgeEffectItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class EdgeEffectViewHolder(val binding: ItemEdgeEffectBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindViews(item: EdgeEffectItem) = with (binding) {
            ivEffectItem.setImageResource(item.resId)
            root.isSelected = item.isSelected
            root.setOnClickListener {
                onClick.invoke(bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EdgeEffectViewHolder {
        val binding = ItemEdgeEffectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EdgeEffectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EdgeEffectViewHolder, position: Int) {
        val item = items[position]
        holder.bindViews(item)
    }

    override fun getItemCount(): Int = items.size
}