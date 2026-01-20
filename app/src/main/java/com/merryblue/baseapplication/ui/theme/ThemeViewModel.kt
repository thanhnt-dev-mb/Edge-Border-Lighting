package com.merryblue.baseapplication.ui.theme

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.merryblue.baseapplication.domain.repository.EdgeDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    application: Application,
    private val repo: EdgeDataRepository
) : BaseViewModel(application) {

    fun getPaging(type: String) = Pager(PagingConfig(pageSize = 15, enablePlaceholders = false)) {
        ThemePagingSource(type, repo)
    }.flow.cachedIn(viewModelScope)

}