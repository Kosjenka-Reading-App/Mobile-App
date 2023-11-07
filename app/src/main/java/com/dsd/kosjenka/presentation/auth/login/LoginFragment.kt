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
import com.dsd.kosjenka.databinding.FragmentLoginBinding
import com.dsd.kosjenka.databinding.FragmentRegisterBinding

class LoginFragment : Fragment() {
    private lateinit var bind:FragmentLoginBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view=inflater.inflate(R.layout.fragment_login, container, false)
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val register=view.findViewById<TextView>(R.id.textView2)
        bind = FragmentLoginBinding.bind(view)
        register.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment)
        }
        val cont_btn=bind.button
        bind=FragmentLoginBinding.bind(view)

        var toolbar: androidx.appcompat.widget.Toolbar =bind.mytoolbar
        toolbar.setNavigationOnClickListener { view ->
            requireActivity().onBackPressed()
        }

        cont_btn.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_homeFragment)
        }

    }
}