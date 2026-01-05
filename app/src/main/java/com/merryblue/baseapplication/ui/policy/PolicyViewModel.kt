package com.merryblue.baseapplication.ui.policy

import android.app.Application
import com.merryblue.baseapplication.coredata.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class PolicyViewModel @Inject constructor(
    application: Application,
    private val appRepository: AppRepository
) : BaseViewModel(application) {

}
