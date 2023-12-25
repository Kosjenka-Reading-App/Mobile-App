package com.dsd.kosjenka.presentation.auth.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.dsd.kosjenka.BuildConfig
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentMainBinding
import com.dsd.kosjenka.utils.SharedPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    @Inject
    lateinit var preferences: SharedPreferences

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_main, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (preferences.isLoggedIn)
            if (preferences.userId != "")
                findNavController().navigate(R.id.action_mainFragment_to_homeFragment)
            else
                findNavController().navigate(R.id.action_mainFragment_to_userProfilesFragment)

        binding.registerBtn.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_mainFragment_to_registerFragment)
        )
        binding.loginBtn.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_mainFragment_to_loginFragment)
        )

//        if (BuildConfig.DEBUG) {
        binding.startCameraBtn.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_mainFragment_to_cameraFragment))
        binding.startCalibrateBtn.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_mainFragment_to_calibrateFragment))
//        }

    }
}