package com.merryblue.baseapplication.ui.home.advanced

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.EdgeAdvanced
import com.merryblue.baseapplication.databinding.ItemEdgeDirectionBinding

class EdgeDirectionAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<EdgeDirectionAdapter.EdgeDirectionViewHolder>() {
    private var items: List<EdgeAdvanced.EdgeDirection> = emptyList()

    fun submitList(newItems: List<EdgeAdvanced.EdgeDirection>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class EdgeDirectionViewHolder(val binding: ItemEdgeDirectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindViews(item: EdgeAdvanced.EdgeDirection) = with (binding) {
            root.isSelected = item.isSelected
            ivItemDirection.setImageResource(item.resId)
            tvItemDirection.text = root.context.getString(item.title)
            ivCheckFour.setImageResource(if (item.isSelected) R.drawable.ic_check_selected else R.drawable.ic_check_unselected)
            root.setOnClickListener { onClick.invoke(bindingAdapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EdgeDirectionViewHolder {
        val binding = ItemEdgeDirectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EdgeDirectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EdgeDirectionViewHolder, position: Int) {
        val item = items[position]
        holder.bindViews(item)
    }

    override fun getItemCount(): Int = items.size
}