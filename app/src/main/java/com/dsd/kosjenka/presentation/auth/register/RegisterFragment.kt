package com.dsd.kosjenka.presentation.auth.register

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.content.ContextCompat
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val back=view.findViewById<ImageView>(R.id.back)

        val submit=view.findViewById<Button>(R.id.button)

        val email=view.findViewById<EditText>(R.id.editTextTextEmailAddress)
        val password=view.findViewById<EditText>(R.id.editTextTextPassword2)
        val confirmed_pwd=view.findViewById<EditText>(R.id.editTextTextPassword3)
        val error_message=view.findViewById<TextView>(R.id.error_message)

        val edit_text_border = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_border)

        val border=GradientDrawable()



        val context=requireContext()
        submit.setOnClickListener{

            val email_val=email_validation(email.text.toString())
            //If Email is not in correct shape
            if(email_val){

                email.setBackgroundColor(ContextCompat.getColor(context, R.color.colorOnPrimary))
                email.background=edit_text_border
                error_message.visibility=View.GONE

            }else{
                border.setColor(ContextCompat.getColor(requireContext(), R.color.wrong_password)) // Postavljamo boju pozadine
                border.setStroke(5, ContextCompat.getColor(requireContext(), R.color.wrong_password_border)) // Postavljamo nove ivice (5dp širine, nova boja)

                email.background=border

                error_message.visibility=View.VISIBLE
                error_message.text="Email is not in correct shape"
            }

            val ver=validation(password.text.toString(),confirmed_pwd.text.toString())

            if(email_val && !ver.equals("Signed In")){
                Log.d("MMS","SSSSSSSSSSSSSSSSSSSSSSS")
                error_message.visibility=View.VISIBLE
                error_message.text=ver


                border.setColor(ContextCompat.getColor(requireContext(), R.color.wrong_password)) // Postavljamo boju pozadine
                border.setStroke(5, ContextCompat.getColor(requireContext(), R.color.wrong_password_border)) // Postavljamo nove ivice (5dp širine, nova boja)


                password.background=border
                confirmed_pwd.background=border

            }else if(ver.equals("Signed In")){
                password.setBackgroundColor(ContextCompat.getColor(context, R.color.colorOnPrimary))
                confirmed_pwd.setBackgroundColor(ContextCompat.getColor(context, R.color.colorOnPrimary))
                password.background=edit_text_border
                confirmed_pwd.background=edit_text_border

                error_message.visibility=View.GONE
            }
        }

        back.setOnClickListener{
            Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_mainFragment)
        }
    }






    //Aux functions
    private fun validation(password: String, confirmed_password: String): String {
        val check_nums = Regex("\\d").containsMatchIn(password)
        val passwordLength = password.length

        if (passwordLength < 5) {
            return "Password must have more than 5 characters"
        } else if (!check_nums) {
            return "Password must contain at least one number"
        } else if (!password.equals(confirmed_password)) {
            return "The confirmation password does not match the original password $check_nums"
        }

        return "Signed In"
    }

    fun email_validation(email: String): Boolean {
        val emailRegex = Regex("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}")
        return emailRegex.matches(email)
    }


}

