package com.dsd.kosjenka.presentation.home.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentExerciseBinding
import com.dsd.kosjenka.presentation.MainActivity

class ExerciseFragment : Fragment() {

    private lateinit var binding: FragmentExerciseBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_exercise, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity
        activity.setSupportActionBar(binding.exerciseToolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val args = ExerciseFragmentArgs.fromBundle(requireArguments())

        binding.exerciseToolbar.title = args.exercise.title
        binding.exerciseText.text = args.exercise.text
    }

}