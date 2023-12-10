package com.dsd.kosjenka.presentation.auth.ForgotPassword

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentResetPasswordBinding
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.Common.Companion.isValidEmail
import com.dsd.kosjenka.utils.NetManager
import com.dsd.kosjenka.utils.UiStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private lateinit var binding: FragmentResetPasswordBinding
    private val viewModel by viewModels<ForgotPasswordViewModel>()

    @Inject
    lateinit var netManager: NetManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_reset_password, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reset.setOnClickListener{
            binding.loading.visibility = View.VISIBLE
            ExecuteForgotPassword(binding.resetEmail.text.toString())
        }
        initOnTextChange()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.eventFlow.collectLatest {
                when (it) {
                    UiStates.NO_INTERNET_CONNECTION -> {
                        binding.loading.visibility = View.GONE
                        Common.showToast(binding.root.context, getString(R.string.network_error))
                    }

                    UiStates.RESET_PASSWORD ->
                        findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)

                    UiStates.EMAIL_UNAVAILABLE ->{
                        binding.loading.visibility = View.GONE
                        Common.showToast(binding.root.context,"No that email address")
                    }

                    else -> {
                        binding.loading.visibility = View.GONE
                        Common.showToast(binding.root.context, getString(R.string.default_error))
                        return@collectLatest
                    }
                }
            }
        }
    }

    private fun initOnTextChange() {
        binding.resetEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(email: Editable?) {
                isEmailValid(email.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun ExecuteForgotPassword(
        email:String
    ){
        lifecycleScope.launch {
            if (netManager.isConnectedToInternet()) {
                viewModel.forgotPassword(email=email)
            }else {
                Common.showToast(binding.root.context, getString(R.string.network_error))
                binding.loading.visibility = View.GONE
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return if (TextUtils.isEmpty(email)) {
            binding.resetEmailLayout.error = getString(R.string.email_required)
            false
        } else if (!email.isValidEmail()) {
            binding.resetEmailLayout.error = getString(R.string.email_invalid)
            false
        } else {
            binding.resetEmailLayout.error = ""
            true
        }
    }


}