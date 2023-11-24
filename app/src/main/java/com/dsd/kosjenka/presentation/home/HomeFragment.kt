package com.dsd.kosjenka.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentHomeBinding
import com.dsd.kosjenka.domain.models.Category
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.presentation.MyLoadStateAdapter
import com.dsd.kosjenka.presentation.auth.main.MainFragmentDirections
import com.dsd.kosjenka.presentation.home.exercise.ExerciseFragmentArgs
import com.dsd.kosjenka.presentation.home.filter.FilterBottomSheet
import com.dsd.kosjenka.presentation.user_profiles.UserProfilesFragmentDirections
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.interfaces.CategoryFilterListener
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.userAgent

@AndroidEntryPoint
class HomeFragment : Fragment(), CategoryFilterListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var pagingAdapter: PagingExerciseAdapter
    private var  profiles: MutableList<UserProfile> = mutableListOf()
    private val viewModel by viewModels<HomeViewModel>()

    private var category: Category? = null
    private var scrollToTop = false
    private var showProgressBar = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_home, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.greeting.text="Welcome "+ userAgent//Username
        setupRecycler()
        setupSearch()
//        setupSort()
        setupRefresh()
        setupFAB()
        observeViewModel()
        switchUser()
        //viewModel.getCategories()
    }

    private fun setupFAB() {
        binding.filterFab.setOnClickListener {
            if (category == null)
                category = Category(null)
            val bottomSheet = FilterBottomSheet.newInstance(category = category!!)
            bottomSheet.filterCategorySelectedListener = this
            bottomSheet.show(childFragmentManager, FilterBottomSheet.TAG)
        }
    }




    private fun switchUser() { //=viewModel.getUsers().observe(viewLifecycleOwner){profileData ->
        binding.switchUser.setOnClickListener{
            findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToUserProfilesFragment()
                    )
//            val popupMenu=PopupMenu(requireContext(), it)
//            popupMenu.menuInflater.inflate(R.menu.users_menu, popupMenu.menu)
//
//            val a=viewModel.getUsers()
//
//            if (profileData != null && profileData.isNotEmpty()) {
//                for ((index, userProfile) in profileData.withIndex()) {
//                    popupMenu.menu.add(Menu.NONE, index, Menu.NONE, userProfile.username)
//                }
//                popupMenu.setOnMenuItemClickListener { menuItem ->
//                    val selectedUserProfile = profileData[menuItem.itemId]
//                    findNavController().navigate(
//                        HomeFragmentDirections.actionHomeFragmentSelf(selectedUserProfile.id_user)
//                    )
//                    true
//                }
//                popupMenu.show()
//            } else {
//                Toast.makeText(requireContext(), "There is no user profiles", Toast.LENGTH_SHORT).show()
//            }
//
//            popupMenu.show()
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

//    private fun setupSort() {
//        binding.homeComplexityBtn.setOnClickListener {
//            viewModel.sortByComplexity()
//        }
//        //Remove comments once completion is added
//        binding.homeCompletionBtn.setOnClickListener {
//            viewModel.sortByCompletion()
//        }
//    }

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
                HomeFragmentDirections.actionHomeFragmentToExerciseFragment(exercise = it)
            )
        }
        //Init recyclerView
        //Initialize Recycler
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


//                // Empty (no employers)
//                if (loadState.source.refresh is LoadState.NotLoading &&
//                    loadState.append.endOfPaginationReached &&
//                    pagingAdapter.itemCount < 1
//                ) {
//                    homeRecycler.isVisible = false
//                    jobsErrorContainer.root.visibility = View.VISIBLE
//                } else if (loadState.source.refresh is LoadState.Error &&
//                    pagingJobsAdapter.itemCount > 0
//                ) {
//                    jobsRecycler.isVisible = true
//                    jobsErrorContainer.root.visibility = View.GONE
//                } else
//                    jobsErrorContainer.root.visibility = View.GONE
            }
        }
    }

    override fun onCategoryFilterSelected(category: Category) {
        Toast.makeText(
            binding.root.context,
            "Category ${category.category} selected!",
            Toast.LENGTH_SHORT
        ).show()
    }
}
