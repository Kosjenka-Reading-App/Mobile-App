package com.dsd.kosjenka.presentation.home.filter

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private lateinit var thisCategory: Category

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
        thisCategory =
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
                createChip(it)
            )
        }

        binding.filterClear.setOnClickListener {
            thisCategory.category = null
            filterCategorySelectedListener?.onCategoryFilterSelected(thisCategory)
            this.dismiss()
        }

        binding.filterSubmit.setOnClickListener {
            filterCategorySelectedListener?.onCategoryFilterSelected(thisCategory)
            this.dismiss()
        }

        binding.filterClose.setOnClickListener {
            this.dismiss()
        }

    }

    private fun createChip(category: Category): Chip {
        val chip = Chip(binding.root.context)
        chip.text = category.category
        chip.isCheckable = true
        chip.setOnCheckedChangeListener { _, isChipChecked ->
            if (isChipChecked)
                thisCategory = category
            else {
                if (thisCategory == category)
                    thisCategory = Category(null)
            }
        }
        if (thisCategory.category != null)
            chip.isChecked = thisCategory == category
        return chip
    }

}


