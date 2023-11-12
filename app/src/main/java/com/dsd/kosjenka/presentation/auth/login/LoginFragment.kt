package com.dsd.kosjenka.presentation.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentLoginBinding
import com.dsd.kosjenka.presentation.MainActivity

class LoginFragment : Fragment() {
    private lateinit var bind: FragmentLoginBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bind =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_login, container, false)
        return bind.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Setup fragment toolbar and display UP button
        val activity = requireActivity() as MainActivity
        activity.setSupportActionBar(bind.mytoolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bind.textView2.setOnClickListener {
            Navigation.findNavController(view)
                .navigate(R.id.action_loginFragment_to_registerFragment)
        }

        bind.mytoolbar.setNavigationOnClickListener { view ->
            requireActivity().onBackPressed()
        }

        bind.button.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_homeFragment)
        }

    }
}