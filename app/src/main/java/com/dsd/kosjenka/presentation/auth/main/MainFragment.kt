package com.dsd.kosjenka.presentation.auth.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.dsd.kosjenka.R

class MainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val register=view.findViewById<Button>(R.id.register)
        val login=view.findViewById<Button>(R.id.login)

        register.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_mainFragment_to_registerFragment)
        }

        login.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_mainFragment_to_loginFragment)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}