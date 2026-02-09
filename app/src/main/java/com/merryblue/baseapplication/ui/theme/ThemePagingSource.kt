package com.merryblue.baseapplication.ui.theme

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.ThemeUi
import com.merryblue.baseapplication.domain.repository.EdgeDataRepository
import com.merryblue.baseapplication.helpers.EDGE_MOST
import com.merryblue.baseapplication.helpers.EDGE_REWARD_DAY
import com.merryblue.baseapplication.helpers.EDGE_TRENDING
import com.merryblue.baseapplication.helpers.KEY_ALL
import com.merryblue.baseapplication.helpers.RIPPLE_ABSTRACT_ABSCT
import com.merryblue.baseapplication.helpers.RIPPLE_ABSTRACT_CQ
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_D
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_INDS
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_LIVE
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_SPAZ
import com.merryblue.baseapplication.helpers.RIPPLE_RIPPLE
import com.merryblue.baseapplication.helpers.RIPPLE_TOP_PICS

data class ChainKey(
    val typeIndex: Int,
    val page: Int
)

class ThemePagingSource(
    private val type: String,
    private val isGallery: Boolean,
    private val isCustom: Boolean,
    private val repo: EdgeDataRepository
) : PagingSource<ChainKey, ThemeUi>() {

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

        RIPPLE_RIPPLE -> listOf(RIPPLE_RIPPLE)

        RIPPLE_TOP_PICS -> listOf(
            RIPPLE_TOP_PICS,
            EDGE_REWARD_DAY,
            EDGE_TRENDING,
            EDGE_MOST
        )

        KEY_ALL -> listOf(
            RIPPLE_RIPPLE,
//            RIPPLE_TOP_PICS,
            RIPPLE_NATURE_SPAZ,
            RIPPLE_NATURE_INDS,
            RIPPLE_NATURE_D,
            RIPPLE_NATURE_LIVE,
            RIPPLE_ABSTRACT_ABSCT,
            RIPPLE_ABSTRACT_CQ
        )

        else -> null
    }

    private fun headerIfNeeded(isFirstPage: Boolean): ThemeUi? {
        if (!isFirstPage) return null
        return when {
//            isCustom -> ThemeUi.Custom(id = "custom_header")      // todo: comment custom
            isGallery -> ThemeUi.Gallery(id = "gallery_header")
            else -> null
        }
    }

    override fun getRefreshKey(state: PagingState<ChainKey, ThemeUi>): ChainKey? {
        val anchor = state.anchorPosition ?: return null
        val closest = state.closestPageToPosition(anchor)
        return closest?.prevKey ?: closest?.nextKey
    }

    override suspend fun load(params: LoadParams<ChainKey>): LoadResult<ChainKey, ThemeUi> {
        return try {
            require(!(isGallery && isCustom)) { "isGallery và isCustom không được cùng true" }

            val chain = chainForType(type)

            if (chain == null) {
                val page = params.key?.page ?: 1
                val items: List<Item> = repo.getItems(type, page)

                val header = headerIfNeeded(isFirstPage = (page == 1))

                val data: List<ThemeUi> = buildList {
                    header?.let { add(it) }
                    addAll(items)
                }

                return LoadResult.Page(
                    data = data,
                    prevKey = if (page == 1) null else ChainKey(typeIndex = 0, page = page - 1),
                    nextKey = if (items.isEmpty()) null else ChainKey(typeIndex = 0, page = page + 1)
                )
            }

            val key = params.key ?: ChainKey(typeIndex = 0, page = 1)

            if (key.typeIndex !in chain.indices) return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

            val currentType = chain[key.typeIndex]
            val items: List<Item> = repo.getItems(currentType, key.page)

            val isFirstOverallPage = (key.typeIndex == 0 && key.page == 1)
            val header = headerIfNeeded(isFirstPage = isFirstOverallPage)

            val data: List<ThemeUi> = buildList {
                header?.let { add(it) }
                addAll(items)
            }

            val nextKey = if (items.isNotEmpty()) {
                ChainKey(typeIndex = key.typeIndex, page = key.page + 1)
            } else {
                val nextIndex = key.typeIndex + 1
                if (nextIndex in chain.indices) ChainKey(typeIndex = nextIndex, page = 1) else null
            }

            val prevKey: ChainKey? = null

            LoadResult.Page(
                data = data,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
