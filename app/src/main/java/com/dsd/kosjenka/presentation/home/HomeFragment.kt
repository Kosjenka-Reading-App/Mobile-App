package com.dsd.kosjenka.presentation.home

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation
import com.dsd.kosjenka.R

class HomeFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val register=view.findViewById<Button>(R.id.register)
        val login=view.findViewById<Button>(R.id.login)
        register.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_mainFragment_to_registerFragment)
        }

        login.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_mainFragment_to_loginFragment)
        }
    }

}