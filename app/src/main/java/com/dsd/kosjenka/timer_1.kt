package com.dsd.kosjenka

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.dsd.kosjenka.databinding.FragmentTimer1Binding
import com.dsd.kosjenka.utils.Common

class timer_1 : Fragment() {
    lateinit var binding: FragmentTimer1Binding
    lateinit var timer: CountDownTimer
    private var minutes=0

    private lateinit var Prog: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_timer_1, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        Prog = binding.Prog
        val initialTimeMillis: Long = 60000

        timer = object : CountDownTimer(initialTimeMillis, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (initialTimeMillis - millisUntilFinished)
                if((millisUntilFinished/1000)<=1) {
                    timer.cancel()
                    minutes+=1
                    timer.start()
                }
                val percen = if (secondsRemaining != 0L) {
                    ((secondsRemaining/1000.0)/(initialTimeMillis/1000.0))*100.0
                } else {
                    0
                }
                binding.textView2.setText(minutes.toString()+":"+(secondsRemaining/1000).toString())
                Prog.progress = percen.toInt()
            }

            override fun onFinish() {
                Common.showToast(binding.root.context, "Times ended")
            }
        }

        timer.start()
    }
}
