package com.merryblue.baseapplication.ui.home.advanced

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.merryblue.baseapplication.coredata.model.edge.EdgeAdvanced
import com.merryblue.baseapplication.databinding.ItemEdgeNotchTypeBinding

class EdgeNotchTypeAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<EdgeNotchTypeAdapter.EdgeNotchTypeViewHolder>() {
    private var items: List<EdgeAdvanced.EdgeNotchType> = emptyList()

    fun submitList(newItems: List<EdgeAdvanced.EdgeNotchType>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class EdgeNotchTypeViewHolder(val binding: ItemEdgeNotchTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindViews(item: EdgeAdvanced.EdgeNotchType) = with (binding) {
            frItemNotchType.isSelected = item.isSelected
            ivItemNotchType.setImageResource(item.resId)
            tvItemNotchType.text = root.context.getString(item.title)
            root.setOnClickListener { onClick.invoke(bindingAdapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EdgeNotchTypeViewHolder {
        val binding = ItemEdgeNotchTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EdgeNotchTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EdgeNotchTypeViewHolder, position: Int) {
        val item = items[position]
        holder.bindViews(item)
    }

    override fun getItemCount(): Int = items.size
}