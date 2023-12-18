package com.dsd.kosjenka.presentation.home.exercise

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentExerciseBinding
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.utils.UiStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseFragment : Fragment() {

    private lateinit var binding: FragmentExerciseBinding
    private val viewModel by viewModels<ExerciseViewModel>()

    private var delayMillis = 1000L
    private var isPlaying = false
    private lateinit var handler: Handler

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

        binding.exerciseToolbar.title = args.exerciseTitle
        handler = Handler(Looper.getMainLooper())

        observeViewModel()
        viewModel.getExercise(args.exerciseId)
        setupSpeedButtons()
        setupPlayPause()
    }

    private fun setupPlayPause() {
        binding.exercisePlayPause.setOnClickListener {
            if (isPlaying) {
                //Pause Exercise
                binding.exercisePlayPause.setImageResource(R.drawable.ic_play)
                handler.removeCallbacksAndMessages(null)
                isPlaying = false
            } else
            //Resume Exercise
                startReadingMode()
        }
    }


    private fun setupSpeedButtons() {
        binding.speedMinus.setOnClickListener {
            delayMillis -= 100
            if (isPlaying)
                resetCallback()
        }
        binding.speedPlus.setOnClickListener {
            delayMillis += 100
            if (isPlaying)
                resetCallback()
        }
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
                                binding.loading.visibility = View.GONE
                                Toast.makeText(
                                    binding.root.context,
                                    getString(R.string.network_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {
                                binding.loading.visibility = View.GONE
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
                    viewModel.exerciseDataFlow.collectLatest {
                        binding.exerciseText.text = it.text
                        startReadingMode()
                    }
                }
            }
        }
    }

    private fun startReadingMode() {
        binding.exercisePlayPause.setImageResource(R.drawable.ic_pause)
        isPlaying = true
        handler.post(object : Runnable {
            override fun run() {
                binding.exerciseText.highlightNextWord()
                handler.postDelayed(this, delayMillis)
            }
        })
    }

    private fun resetCallback() {
        // Remove any existing callbacks in the handler
        handler.removeCallbacksAndMessages(null)
        // Start the highlighting loop with the adjusted speed
        binding.exerciseText.resetCallback()
        startReadingMode()
    }

    private fun toggleProgressBar(isLoading: Boolean) {
        if (isLoading) {
            binding.loading.visibility = View.VISIBLE
            binding.exerciseText.visibility = View.GONE
        } else {
            binding.loading.visibility = View.GONE
            binding.exerciseText.visibility = View.VISIBLE
        }
    }

}