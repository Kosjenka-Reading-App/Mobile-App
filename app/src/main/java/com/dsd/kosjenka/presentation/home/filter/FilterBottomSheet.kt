package com.dsd.kosjenka.presentation.home.filter

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.BottomSheetFilterBinding
import com.dsd.kosjenka.databinding.ItemChipFilterBinding
import com.dsd.kosjenka.domain.models.Category
import com.dsd.kosjenka.utils.ARG_CATEGORY
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.interfaces.CategoryFilterListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FilterBottomSheet : BottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance(category: Category) =
            FilterBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CATEGORY, category)
                }
            }

        const val TAG = "ModalBottomSheet"
    }

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var binding: BottomSheetFilterBinding
    private lateinit var category: Category

    var filterCategorySelectedListener: CategoryFilterListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding =
            DataBindingUtil.inflate(
                layoutInflater,
                R.layout.bottom_sheet_filter,
                container,
                false
            )

        @Suppress("DEPRECATION")
        category =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                requireArguments().getParcelable(ARG_CATEGORY, Category::class.java)!!
            else
                requireArguments().getParcelable(ARG_CATEGORY)!!

        return binding.root

    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val behavior = dialog.behavior
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initChips
        sharedPreferences.categories.forEach {
            binding.filterChipGroup.addView(
                initCategoryChip(it)
            )
        }

        binding.filterClear.setOnClickListener {
            category.category = null
            filterCategorySelectedListener?.onCategoryFilterSelected(category)
            this.dismiss()
        }

        binding.filterSubmit.setOnClickListener {
            filterCategorySelectedListener?.onCategoryFilterSelected(category)
            this.dismiss()
        }

        binding.filterClose.setOnClickListener {
            this.dismiss()
        }

    }

    private fun initCategoryChip(category: Category): View {
        val binding = ItemChipFilterBinding.inflate(LayoutInflater.from(binding.root.context))

        binding.tagChip.text = category.category

//        binding.tagChip.setOnCheckedChangeListener { _, isChipChecked ->
//            if (isChipChecked) {
//                binding.tagChip.setCheckedColors(binding.root.context)
//                selectedCategories.add(category)
//            } else {
//                binding.tagChip.setUnCheckedColors(binding.root.context)
//                selectedCategories.remove(category)
//            }
//        }
//        if (filterObject.categories?.contains(category) != null)
//            binding.tagChip.isChecked = filterObject.categories?.contains(category)!!
        return binding.root
    }

    private fun createBaseChip(text: String): Chip {
        val chip = Chip(binding.root.context)
        chip.setChipStrokeColorResource(R.color.colorPrimary_50)
        chip.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary_50))
        chip.setChipBackgroundColorResource(R.color.background)
        chip.setEnsureMinTouchTargetSize(false)
        chip.text = text
        chip.isCheckable = true
        return chip
    }

}


