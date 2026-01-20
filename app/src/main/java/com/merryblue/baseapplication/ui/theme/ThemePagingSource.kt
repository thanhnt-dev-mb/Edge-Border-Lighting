package com.merryblue.baseapplication.ui.theme

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.repository.EdgeDataRepository
import com.merryblue.baseapplication.helpers.RIPPLE_ABSTRACT_ABSCT
import com.merryblue.baseapplication.helpers.RIPPLE_ABSTRACT_CQ
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_D
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_INDS
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_LIVE
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_SPAZ

data class ChainKey(
    val typeIndex: Int,
    val page: Int
)

class ThemePagingSource(
    private val type: String,
    private val repo: EdgeDataRepository
) : PagingSource<ChainKey, Item>() {

    private fun chainForType(type: String): List<String>? = when (type) {

        RIPPLE_NATURE_SPAZ, RIPPLE_NATURE_INDS, RIPPLE_NATURE_D, RIPPLE_NATURE_LIVE -> listOf(
            RIPPLE_NATURE_SPAZ,
            RIPPLE_NATURE_INDS,
            RIPPLE_NATURE_D,
            RIPPLE_NATURE_LIVE
        )

        RIPPLE_ABSTRACT_ABSCT, RIPPLE_ABSTRACT_CQ -> listOf(
            RIPPLE_ABSTRACT_ABSCT,
            RIPPLE_ABSTRACT_CQ
        )

        else -> null
    }

    override fun getRefreshKey(state: PagingState<ChainKey, Item>): ChainKey? {
        val anchor = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchor)?.prevKey ?: state.closestPageToPosition(anchor)?.nextKey
    }

    override suspend fun load(params: LoadParams<ChainKey>): LoadResult<ChainKey, Item> {
        return try {
            val chain = chainForType(type)

            if (chain == null) {
                val page = params.key?.page ?: 1
                val items = repo.getItems(type, page)
                return LoadResult.Page(
                    data = items,
                    prevKey = if (page == 1) null else ChainKey(typeIndex = 0, page = page - 1),
                    nextKey = if (items.isEmpty()) null else ChainKey(typeIndex = 0, page = page + 1)
                )
            }

            val key = params.key ?: ChainKey(typeIndex = 0, page = 1)

            if (key.typeIndex !in chain.indices) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }

            val currentType = chain[key.typeIndex]
            val items = repo.getItems(currentType, key.page)

            val nextKey = if (items.isNotEmpty()) {
                ChainKey(typeIndex = key.typeIndex, page = key.page + 1)
            } else {
                val nextIndex = key.typeIndex + 1
                if (nextIndex in chain.indices) ChainKey(typeIndex = nextIndex, page = 1) else null
            }

            val prevKey = if (key.typeIndex == 0 && key.page == 1) null else null

            LoadResult.Page(
                data = items,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}