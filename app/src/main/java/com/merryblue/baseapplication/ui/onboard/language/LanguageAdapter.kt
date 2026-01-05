package com.merryblue.baseapplication.ui.onboard.language

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.LanguageModel
import com.merryblue.baseapplication.databinding.ItemLanguageBinding

class LanguageAdapter internal constructor(
    private var items: List<LanguageModel>,
    val itemClick: (LanguageModel) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class ViewHolder(binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var itemBinding: ItemLanguageBinding = binding

        fun bind(obj: LanguageModel) {
            itemBinding.data = obj
            itemBinding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: ItemLanguageBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_language, parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is ViewHolder) return

        holder.bind(items[position])
        holder.itemBinding.root.setOnClickListener {
            itemClick(items[position])
            reloadSelectedBy(position)
        }
    }

    override fun getItemCount() = items.size

    @SuppressLint("NotifyDataSetChanged")
    private fun reloadSelectedBy(position: Int) {
        items.forEachIndexed { index, item ->
            item.isSelected = index == position
        }

        notifyDataSetChanged()
    }
}
