package com.dsd.kosjenka.presentation.home.exercise

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import com.dsd.kosjenka.BuildConfig
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentExerciseBinding
import com.dsd.kosjenka.domain.models.Completion
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.UiStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ExerciseFragment : Fragment(), HighlightCallback {

    private lateinit var binding: FragmentExerciseBinding
    private val viewModel by viewModels<ExerciseViewModel>()

    private var delayMillis = 1000L
    private var isPlaying = false
    private var hasReachedEnd = false
    private lateinit var handler: Handler

    private val timerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var elapsedSeconds = 0
    private var isTimerRunning = false
    private var timerJob: Job? = null

    private lateinit var thisExercise: Exercise

    @Inject
    lateinit var preferences: SharedPreferences

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
        binding.exerciseText.setHighlightCallback(this)

        observeViewModel()
        viewModel.getExercise(args.exerciseId, preferences.userId)
        setupSpeedButtons()
        setupPlayPause()
        setupFontSlider()
        binding.progressBarTimer.max = 100
    }

    private fun startTimer() {
        isTimerRunning = true
        timerJob = timerScope.launch {
            while (isActive) {
                delay(1000)
                if (isTimerRunning) {
                    elapsedSeconds++
                    updateUIWithElapsedTime(elapsedSeconds)
                }
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val sec = seconds % 60
        return String.format("%02d:%02d", minutes, sec)
    }

    private fun pauseTimer() {
        isTimerRunning = false
    }

    private fun resumeTimer() {
        if (!isTimerRunning && (timerJob == null || timerJob!!.isCompleted))
            startTimer()
        else
            isTimerRunning = true
    }

    private fun updateUIWithElapsedTime(seconds: Int) {
        val progress = (seconds * 100 / 60) % 100
        binding.progressBarTimer.progress = progress
        binding.timerTextView.text = formatTime(seconds)
    }

    private fun setupFontSlider() {
        binding.exerciseText.textSize = 20f

        binding.fontSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = 20 + progress
                binding.exerciseText.textSize = fontSize.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPlayPause() {
        binding.exercisePlayPause.setOnClickListener {
            if (hasReachedEnd)
                return@setOnClickListener
            if (isPlaying) {
                //Pause Exercise
                binding.exercisePlayPause.setImageResource(R.drawable.ic_play)
                handler.removeCallbacksAndMessages(null)
                isPlaying = false
                pauseTimer()
                //Track completion
                updateCompletion()

            } else {
                //Resume Exercise
                startReadingMode()
                resumeTimer()
            }
        }
    }

    private fun updateCompletion() {
        val completionObject = Completion(
            completion = binding.exerciseText.getCompletion(),
            position = binding.exerciseText.currentIndex,
            time_spent = elapsedSeconds,
            user_id = preferences.userId.toInt(),
        )
        Timber.d("test123: $completionObject")
        viewModel.updateCompletion(
            exerciseId = thisExercise.id,
            completion = completionObject
        )
    }

    private fun setupSpeedButtons() {
        binding.speedMinus.setOnClickListener {
            if (delayMillis < 3000) {
                delayMillis += 100
                if (isPlaying) resetCallback()
            } else
                Toast.makeText(context, "Minimum speed reached (3 seconds)", Toast.LENGTH_SHORT)
                    .show()

            // Enable speedPlus if it was disabled
            binding.speedPlus.isEnabled = true
            // Disable speedMinus if the lower limit is reached
            if (delayMillis >= 3000) binding.speedMinus.isEnabled = false
        }
        binding.speedPlus.setOnClickListener {
            if (delayMillis > 200) {
                delayMillis -= 100
                if (isPlaying) resetCallback()
            } else
                Toast.makeText(context, "Maximum speed reached (0.2 seconds)", Toast.LENGTH_SHORT)
                    .show()

            // Enable speedMinus if it was disabled
            binding.speedMinus.isEnabled = true
            // Disable speedPlus if the upper limit is reached
            if (delayMillis <= 200) binding.speedPlus.isEnabled = false
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
                            UiStates.UPDATE -> {
                                //User Exercise completion updated.
                                Timber.d("Exercise ${thisExercise.title} completion updated.")
                            }

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
                        thisExercise = it
                        binding.exerciseText.text = it.text
                        startReadingMode()
                        startTimer()
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
                if (!isPlaying) {
                    handler.removeCallbacksAndMessages(null)
                    return
                }
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

    override fun onHighlightEnd() {
        hasReachedEnd = true
        binding.exercisePlayPause.setImageResource(R.drawable.ic_play)
        handler.removeCallbacksAndMessages(null)
        isPlaying = false
        //Cancel timer
        timerJob?.cancel()
        Toast.makeText(
            binding.root.context,
            "Exercise finished in $elapsedSeconds seconds",
            Toast.LENGTH_SHORT
        ).show()
        //Track completion
        updateCompletion()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerScope.cancel()
    }
}

