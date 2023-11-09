package com.dsd.kosjenka.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentHomeBinding
import com.dsd.kosjenka.domain.models.Category
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.utils.Common

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var exercisesList: MutableList<Exercise>

    private var scrollToTop = false

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
        addData()
        setupSearch()
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

    private fun addData() {
        createData()
        exerciseAdapter.differ.submitList(exercisesList)
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

        // Define a custom order for sorting by complexity
        val customOrder = mapOf("Easy" to 0, "Medium" to 1, "Hard" to 2)

        // Sort the list by complexity using the custom order
        exercisesList = exercisesList.sortedBy { customOrder[it.complexity] }.toMutableList()

    }

    private fun setupRecycler() {
        //init adapter
        exerciseAdapter = ExerciseAdapter()
        exerciseAdapter.setOnExerciseClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToExerciseFragment(
                    it.title
                )
            )
        }
        //Init recyclerView
        binding.jobsRecycler.apply {
            itemAnimator = null
            setHasFixedSize(true)
            layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
            adapter = exerciseAdapter
        }
    }
}