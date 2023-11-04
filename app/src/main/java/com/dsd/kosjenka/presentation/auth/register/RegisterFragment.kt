package com.dsd.kosjenka.presentation.auth.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.dsd.kosjenka.R

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)


        return view
    }

    private fun verification(password:String):String{
        val check_nums = password.any { it.isDigit() }
        if(password.length<5){
            return "Password must have more than 5 character"
        }else if(check_nums){
            return "Password must contain at least one number"
        }


        return ""
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val back=view.findViewById<ImageView>(R.id.back)

        back.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_mainFragment)
        }
    }

}