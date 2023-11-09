package com.dsd.kosjenka.presentation.auth.register

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentRegisterBinding
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {
    private lateinit var bind:FragmentRegisterBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)


        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentRegisterBinding.bind(view)
        //val back=view.findViewById<ImageView>(R.id.back)
        val email:TextInputLayout=bind.email
        val pwd=bind.password1
        val conf_pwd=bind.confirmPwd

        val submit=view.findViewById<Button>(R.id.button)

        submit.setOnClickListener{
            Log.d("SS", email.editText?.text.toString())
            if(!email_validation(email.editText?.text.toString())){
                email.error="Email is not correct"
            }else{
                email.error=null
            }
            val err_msg=pwd_validation(pwd.editText?.text.toString(), conf_pwd.editText?.text.toString())
            if(!err_msg.equals("Signed In")) {
                if (err_msg.equals("The confirmation password does not match the original password")) {
                    conf_pwd.error = err_msg
                    pwd.error=null
                } else {
                    pwd.error = err_msg
                    conf_pwd.error=null
                }
            }else{
                pwd.error=null
                conf_pwd.error=null
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

