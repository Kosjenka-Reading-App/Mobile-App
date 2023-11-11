package com.dsd.kosjenka.presentation.auth.register

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentRegisterBinding
import com.dsd.kosjenka.presentation.MainActivity
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {
    private lateinit var bind:FragmentRegisterBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bind =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_register, container, false)
        return bind.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Setup fragment toolbar and display UP button
        val activity = requireActivity() as MainActivity
        activity.setSupportActionBar(bind.mytoolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)





        val submit=view.findViewById<Button>(R.id.button)

        submit.setOnClickListener{
            if(!email_validation(bind.email.editText?.text.toString())){
                bind.email.error="Email is not correct"
            }else{
                bind.email.error=null
            }
            val err_msg=pwd_validation(bind.password1.editText?.text.toString(), bind.confirmPwd.editText?.text.toString())
            if(!err_msg.equals("Signed In")) {
                if (err_msg.equals("The confirmation password does not match the original password")) {
                    bind.confirmPwd.error = err_msg
                    bind.password1.error=null
                } else {
                    bind.password1.error = err_msg
                    bind.confirmPwd.error=null
                }
            }else{
                bind.password1.error=null
                bind.confirmPwd.error=null
            }
        }

        //Go Back
        var toolbar: androidx.appcompat.widget.Toolbar =bind.mytoolbar
        toolbar.setNavigationOnClickListener { view ->
            requireActivity().onBackPressed()
        }

    }







    //Aux functions
    private fun pwd_validation(password: String, confirmed_password: String): String {
        val check_nums = Regex("\\d").containsMatchIn(password)
        val passwordLength = password.length

        if (passwordLength < 5) {
            return "Password must have more than 5 characters"
        } else if (!check_nums) {
            return "Password must contain at least one number"
        } else if (!password.equals(confirmed_password)) {
            return "The confirmation password does not match the original password"
        }

        return "Signed In"
    }

    fun email_validation(email: String): Boolean {
        if (email == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }
    }


}

