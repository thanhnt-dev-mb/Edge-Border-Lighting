package com.merryblue.baseapplication.ui.widget

import android.content.DialogInterface
import android.widget.CheckBox
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.BottomSheetRateBinding
import org.app.core.ads.CoreAds
import org.app.core.base.BaseBottomSheetFragment
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.disable
import org.app.core.base.extensions.enable
import org.app.core.base.extensions.redirectToPlayStore

class BottomSheetRate(
    private val onConfirm: (Int) -> Unit = {},
    private val onHide: () -> Unit = {}
) : BaseBottomSheetFragment<BottomSheetRateBinding>() {

    private var currentStar: Int = 0
    private var starViews = mutableListOf<CheckBox>()
    private val iconMap = mapOf(
        0 to R.drawable.ic_5_star,
        1 to R.drawable.ic_1_star,
        2 to R.drawable.ic_2_star,
        3 to R.drawable.ic_3_star,
        4 to R.drawable.ic_4_star,
        5 to R.drawable.ic_5_star,
    )
    
    override fun initDialog() {
        starViews.clear()

        binding.btnOk.disable()
        binding.btnOk.setOnSingleClickListener {
            if (currentStar > 0) {
                CoreAds.instance.logFirebaseEvent("EventUserRating_$currentStar")
            }
            dismissAllowingStateLoss()
            if (currentStar > 3) {
                activity?.redirectToPlayStore(activity?.packageName ?: return@setOnSingleClickListener)
            }
            onConfirm.invoke(currentStar)
        }
        
        starViews.add(binding.rdbStar1)
        binding.rdbStar1.setOnClickListener {
            currentStar = 1
            updateStarViews()
        }
        
        starViews.add(binding.rdbStar2)
        binding.rdbStar2.setOnClickListener {
            currentStar = 2
            updateStarViews()
        }
        
        starViews.add(binding.rdbStar3)
        binding.rdbStar3.setOnClickListener {
            currentStar = 3
            updateStarViews()
        }
        
        starViews.add(binding.rdbStar4)
        binding.rdbStar4.setOnClickListener {
            currentStar = 4
            updateStarViews()
        }
        
        starViews.add(binding.rdbStar5)
        binding.rdbStar5.setOnClickListener {
            currentStar = 5
            updateStarViews()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onHide.invoke()
    }
    
    override fun onStart() {
        super.onStart()
        currentStar = 0
        updateStarViews()
    }
    
    private fun updateStarViews() {
        if (currentStar > 0) {
            binding.btnOk.enable()
        } else {
            binding.btnOk.disable()
        }
        
        for (i in 0 until starViews.size) {
            starViews[i].isChecked = i < currentStar
        }
        
        try {
            binding.imageEmoji.setImageResource(iconMap.get(currentStar) ?: R.drawable.ic_5_star)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        val TAG = "bottom_sheet_rate"
    }
}