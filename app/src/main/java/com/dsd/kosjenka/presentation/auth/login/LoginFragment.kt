package com.dsd.kosjenka.presentation.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.dsd.kosjenka.R

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view=inflater.inflate(R.layout.fragment_login, container, false)



        return view

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val back=view.findViewById<ImageView>(R.id.back)
        val register=view.findViewById<TextView>(R.id.textView2)

        register.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment)
        }


        val text2=view.findViewById<TextView>(R.id.title)
        text2.text="Login"

        back.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_mainFragment)
        }

    }
}