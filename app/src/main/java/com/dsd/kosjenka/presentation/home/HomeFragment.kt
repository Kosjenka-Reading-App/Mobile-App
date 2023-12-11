package com.dsd.kosjenka.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentHomeBinding
import com.dsd.kosjenka.domain.models.Category
import com.dsd.kosjenka.presentation.MyLoadStateAdapter
import com.dsd.kosjenka.presentation.home.filter.FilterBottomSheet
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.interfaces.CategoryFilterListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), CategoryFilterListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var pagingAdapter: PagingExerciseAdapter
    private val viewModel by viewModels<HomeViewModel>()

    private var thisCategory: Category? = null
    private var scrollToTop = false
    private var showProgressBar = true

    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_home, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupSearch()
        setupSort()
        setupRefresh()
        setupFAB()
        observeViewModel()
        switchUser()
        viewModel.getCategories()
    }

    private fun setupFAB() {
        binding.filterFab.setOnClickListener {
            if (thisCategory == null) thisCategory = Category(null)
            val bottomSheet = FilterBottomSheet.newInstance(category = thisCategory!!)
            bottomSheet.filterCategorySelectedListener = this
            bottomSheet.show(childFragmentManager, FilterBottomSheet.TAG)
        }
    }


    private fun switchUser() {
        binding.switchUser.setOnClickListener {
            preferences.userId = ""
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToUserProfilesFragment()
            )
        }
    }

    private fun setupRefresh() {
        binding.homeContainer.setOnRefreshListener {
            // Perform your refresh operation here
            viewModel.refresh()
            // Once the refresh is complete, call setRefreshing(false) to stop the refresh animation
            binding.homeContainer.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewModel.getExercises().observe(viewLifecycleOwner) { pagingData ->
            pagingAdapter.submitData(viewLifecycleOwner.lifecycle, pagingData)
        }
    }

    private fun setupSort() {
        binding.homeComplexityBtn.setOnClickListener {
            scrollToTop = true

            val density = resources.displayMetrics.density
            val selectedStroke = (1 * density).toInt() // Converting 2dp to pixels
            binding.homeComplexityBtn.strokeWidth = selectedStroke
            binding.homeCompletionBtn.strokeWidth = 0

            viewModel.sortByComplexity()
        }

        binding.homeCompletionBtn.setOnClickListener {
            scrollToTop = true

            val density = resources.displayMetrics.density
            val selectedStroke = (1 * density).toInt() // Converting 2dp to pixels
            binding.homeCompletionBtn.strokeWidth = selectedStroke
            binding.homeComplexityBtn.strokeWidth = 0

            viewModel.sortByCompletion()
        }
    }

    private fun setupSearch() {
        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                scrollToTop = true
                viewModel.searchExercises(
                    query = binding.search.text?.trim().toString()
                )
                Common.hideSoftKeyboard(requireActivity())
                true
            } else false
        }
        binding.searchLayout.setEndIconOnClickListener {
            scrollToTop = true
            binding.search.text?.clear()
            viewModel.searchExercises(
                query = binding.search.text?.trim().toString()
            )
        }
    }

    private fun setupRecycler() {
        //init adapter
        pagingAdapter = PagingExerciseAdapter()
        pagingAdapter.setOnExerciseClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToExerciseFragment(
                    exerciseId = it.id, exerciseTitle = it.title
                )
            )
        }
        //Init recyclerView
        binding.homeRecycler.apply {
            itemAnimator = null
            setHasFixedSize(true)
            layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
            adapter =
                pagingAdapter.withLoadStateFooter(MyLoadStateAdapter { pagingAdapter.retry() })
        }

        //Load State Listener
        pagingAdapter.addLoadStateListener { loadState ->
            binding.apply {

                //Show progress bar only on the first load
                if (loadState.source.refresh is LoadState.Loading && showProgressBar) {
                    homeLoading.isVisible = true
                    showProgressBar = false
                } else homeLoading.isVisible = false

                //Visible when:
                // 1. The data finished loading
                // 2. The data is loading, but the list is not empty
                homeRecycler.isVisible =
                    loadState.source.refresh is LoadState.NotLoading || (!showProgressBar && loadState.source.refresh is LoadState.Loading)

                // If it is error and no data in adapter
                if (loadState.source.refresh is LoadState.Error) {
                    Toast.makeText(
                        root.context, getString(R.string.default_error), Toast.LENGTH_SHORT
                    ).show()
                }

                //Scroll to the top when a new list is submitted
                if (loadState.refresh is LoadState.NotLoading && scrollToTop) {
                    binding.homeRecycler.scrollToPosition(0)
                    scrollToTop = false
                }


                // Empty (no employers)
                if (loadState.source.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && pagingAdapter.itemCount < 1) {
                    homeRecycler.isVisible = false
                    homeErrorContainer.root.visibility = View.VISIBLE
                } else if (loadState.source.refresh is LoadState.Error && pagingAdapter.itemCount > 0) {
                    homeRecycler.isVisible = true
                    homeErrorContainer.root.visibility = View.GONE
                } else homeErrorContainer.root.visibility = View.GONE
            }
        }
    }

    override fun onCategoryFilterSelected(category: Category) {
        scrollToTop = true
        thisCategory = category
        viewModel.filterByCategory(
            category = thisCategory!!.category
        )
    }
}
