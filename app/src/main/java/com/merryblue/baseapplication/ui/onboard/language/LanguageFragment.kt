package com.merryblue.baseapplication.ui.onboard.language

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.LanguageModel
import com.merryblue.baseapplication.databinding.FragmentLanguageBinding
import com.merryblue.baseapplication.databinding.ItemSettingLanguageBinding
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseFragment
import org.app.core.base.extensions.backToPreviousScreen
import org.app.core.base.extensions.getMyColor
import org.app.core.base.extensions.hide
import org.app.core.base.utils.StringResId
import timber.log.Timber


@AndroidEntryPoint
class LanguageFragment : BaseFragment<FragmentLanguageBinding>() {

    private val viewModel: LanguageViewModel by viewModels()
    lateinit var adapter: SettingLanguageAdapter

    override
    fun getLayoutId() = R.layout.fragment_language

    override
    fun setBindingVariables() {
        binding.viewModel = viewModel
    }

    override fun onFragmentResume() {
        initRecyclerView()
        binding.layoutCard.hide()
    }

    override fun setUpViews() {
        setupToolbar()
    }

    private fun setupToolbar() {
        try {
            activity?.let { fragmentActivity ->
                val menuHost = fragmentActivity as? MenuHost
                menuHost?.let {
                    menuHost.addMenuProvider(object : MenuProvider {
                        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                            menuInflater.inflate(R.menu.menu_language, menu)
                            val spannableString = SpannableString(getString(StringResId.done))
                            spannableString.setSpan(
                                ForegroundColorSpan(getMyColor(R.color.colorPrimary)),
                                0,
                                spannableString.length,
                                0
                            )

                            menu.findItem(R.id.languageDone)?.title = spannableString
                            menu.findItem(R.id.languageDone)?.icon = null
                        }

                        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                            return if (menuItem.itemId == R.id.languageDone) {
                                viewModel.updateUserLanguage()
                                val language = viewModel.selectedLanguage
                                language?.let {
                                    setLanguage(language.value)
                                }
                                backToPreviousScreen()
                                true
                            } else {
                                false
                            }
                        }
                    }, viewLifecycleOwner)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private fun initRecyclerView() {
        val recyclerView = binding.languageRv

        context?.let {
            val languages = viewModel.getLanguage(it, false)
            adapter = SettingLanguageAdapter(languages,
                itemClick = { item ->
                    viewModel.selectedLanguage = item
                })

            val mLayoutManage = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = mLayoutManage
        } ?: kotlin.run {
            Timber.i(TAG, "Null context -> Should not go here!!!")
        }
    }

    inner class SettingLanguageAdapter internal constructor(
        private var items: List<LanguageModel>,
        val itemClick: (LanguageModel) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        inner class ViewHolder(binding: ItemSettingLanguageBinding) :
            RecyclerView.ViewHolder(binding.root) {

            var itemBinding: ItemSettingLanguageBinding = binding

            fun bind(obj: LanguageModel) {
                itemBinding.data = obj
                itemBinding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding: ItemSettingLanguageBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_setting_language, parent, false
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
}