package com.dsd.kosjenka.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentHomeBinding
import com.dsd.kosjenka.domain.models.Category
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.UiStates
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var exercisesList: MutableList<Exercise>

    private val viewModel by viewModels<HomeViewModel>()

    private var scrollToTop = false

    // Define a custom order for sorting by complexity
    private val complexityAsc = mapOf("Easy" to 0, "Medium" to 1, "Hard" to 2)
    private val complexityDsc = mapOf("Easy" to 2, "Medium" to 1, "Hard" to 0)

    //Complexity starts with TRUE, because it is the default setting
    private var isComplexityAsc = true
    private var isCompletionAsc = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_home, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        createData()
        setupSearch()
        setupSort()
        addData()
//        observeViewModel()
//        viewModel.getExercises()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventFlow.collectLatest {
                        when (it) {
                            UiStates.LOADING -> toggleProgressBar(true)
                            UiStates.SUCCESS -> toggleProgressBar(false)
                            UiStates.NO_INTERNET_CONNECTION -> {
                                binding.homeContainer.isRefreshing = false
                                binding.homeLoading.visibility = View.GONE
                                Toast.makeText(
                                    binding.root.context,
                                    getString(R.string.network_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {
                                binding.homeContainer.isRefreshing = false
                                binding.homeLoading.visibility = View.GONE
                                Toast.makeText(
                                    binding.root.context,
                                    getString(R.string.default_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.mainDataFlow.collectLatest {
                        exerciseAdapter.differ.submitList(it)
                    }
                }
            }
        }
    }

    private fun toggleProgressBar(isLoading: Boolean) {
        if (isLoading) {
            binding.homeLoading.visibility = View.VISIBLE
            binding.homeContainer.visibility = View.GONE
        } else {
            binding.homeLoading.visibility = View.GONE
            binding.homeContainer.visibility = View.VISIBLE
        }
    }

    private fun addData() {
        // Sort the list by complexity using the custom order
        exercisesList = exercisesList.sortedBy { complexityAsc[it.complexity] }.toMutableList()
        exerciseAdapter.differ.submitList(exercisesList)
    }

    private fun setupSort() {
        binding.homeComplexityBtn.setOnClickListener {
            isComplexityAsc = !isComplexityAsc
            exercisesList = if (isComplexityAsc)
                exercisesList.sortedBy { complexityAsc[it.complexity] }.toMutableList()
            else
                exercisesList.sortedBy { complexityDsc[it.complexity] }.toMutableList()
            exerciseAdapter.differ.submitList(exercisesList.toList())
        }
        //Remove comments once completion is added
//        binding.homeCompletionBtn.setOnClickListener {
//            isCompletionAsc = !isCompletionAsc
//            exercisesList = if (isCompletionAsc)
//                exercisesList.sortedBy { it.completion }.toMutableList()
//            else
//                exercisesList.sortedByDescending { it.completion }.toMutableList()
//            exerciseAdapter.differ.submitList(exercisesList)
//        }
    }

    private fun setupSearch() {
        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                scrollToTop = true
//                viewModel.searchJobs(
//                    query = binding.search.text?.trim().toString()
//                )
                Common.hideSoftKeyboard(requireActivity())
                true
            } else false
        }
        binding.searchLayout.setEndIconOnClickListener {
            scrollToTop = true
            binding.search.text?.clear()
//            viewModel.searchJobs(
//                query = binding.search.text?.trim().toString()
//            )
        }
    }

    private fun createData() {
        exercisesList = mutableListOf(
            Exercise(
                category = listOf(Category("Math"), Category("Algebra")),
                complexity = "Medium",
                id = 1,
                title = "Solving Equations"
            ),
            Exercise(
                category = listOf(Category("Science"), Category("Biology")),
                complexity = "Easy",
                id = 2,
                title = "Photosynthesis Process"
            ),
            Exercise(
                category = listOf(Category("History"), Category("World Wars")),
                complexity = "Hard",
                id = 3,
                title = "World War II"
            ),
            Exercise(
                category = listOf(Category("Language"), Category("Grammar")),
                complexity = "Medium",
                id = 4,
                title = "Sentence Structure"
            ),
            Exercise(
                category = listOf(Category("Art"), Category("Painting")),
                complexity = "Easy",
                id = 5,
                title = "Introduction to Watercolors"
            )
        )
    }

    private fun setupRecycler() {
        //init adapter
        exerciseAdapter = ExerciseAdapter()
        exerciseAdapter.setOnExerciseClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToExerciseFragment(
                    it
                )
            )
        }
        //Init recyclerView
        binding.homeRecycler.apply {
            itemAnimator = null
            setHasFixedSize(true)
            layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
            adapter = exerciseAdapter
        }
    }
}