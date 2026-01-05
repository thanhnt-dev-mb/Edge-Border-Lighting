package com.merryblue.baseapplication.ui.setting

import android.graphics.Color
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.Setting
import com.merryblue.baseapplication.databinding.ItemSettingBinding
import org.app.core.base.BaseAdapter
import org.app.core.base.binding.setLayoutHeight
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.disable
import org.app.core.base.extensions.enable
import org.app.core.base.extensions.hide
import org.app.core.base.extensions.setMargins
import org.app.core.base.extensions.show
import org.app.core.base.utils.px

class SettingAdapter(items: List<Setting>) : BaseAdapter<Setting, ItemSettingBinding>(){
    override val itemLayout: Int = R.layout.item_setting
    
    init {
        datas.addAll(items)
    }
    
    override fun bind(binding: ItemSettingBinding, data: Setting, position: Int) {
        binding.data = data

        if (data.code == Setting.Code.PRIVACY) {
            binding.footerView.hide()
        } else {
            binding.footerView.show()
        }

        if (data.hasRightLayout) {
            if (data.enabled) {
                binding.switchBtn.enable()
                binding.switchBtn.setOnSingleClickListener {
                    onItemClickListener?.onItemClick(binding.rightLayout, data, position)
                }
                binding.dropDownBtn.setOnSingleClickListener {
                    onItemClickListener?.onItemClick(binding.rightLayout, data, position)
                }
            } else {
                binding.switchBtn.disable()
            }
            binding.contentLayout.isClickable = false
        } else {
            binding.contentLayout.isClickable = true
            binding.contentLayout.setOnSingleClickListener {
                onItemClickListener?.onItemClick(it, data, position)
            }
        }
    }

    fun updateData(items: List<Setting>, index: Int) {
        datas.clear()
        datas.addAll(items)
        notifyItemChanged(index)
    }
}