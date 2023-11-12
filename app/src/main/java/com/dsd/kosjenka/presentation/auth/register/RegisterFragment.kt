package com.dsd.kosjenka.presentation.auth.register

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentRegisterBinding
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.utils.Common.Companion.showToast
import com.dsd.kosjenka.utils.NetManager
import com.dsd.kosjenka.utils.UiStates.EMAIL_UNAVAILABLE
import com.dsd.kosjenka.utils.UiStates.NO_INTERNET_CONNECTION
import com.dsd.kosjenka.utils.UiStates.REGISTER
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val viewModel by viewModels<RegisterViewModel>()

    @Inject
    lateinit var netManager: NetManager

    companion object {
        fun newInstance() = RegisterFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_register, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity
        activity.setSupportActionBar(binding.registerToolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.register.setOnClickListener {
//            submitAction()
        }

        initOnTextChange()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.eventFlow.collectLatest {
                when (it) {
                    NO_INTERNET_CONNECTION -> {
                        binding.register.visibility = View.VISIBLE
                        binding.loading.visibility = View.INVISIBLE
                        showToast(binding.root.context, getString(R.string.network_error))
                    }

                    EMAIL_UNAVAILABLE -> {
                        binding.register.visibility = View.VISIBLE
                        binding.loading.visibility = View.INVISIBLE
                        showToast(binding.root.context, getString(R.string.email_unavailable))
                    }

                    REGISTER ->
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)

                    else -> {
                        binding.register.visibility = View.VISIBLE
                        binding.loading.visibility = View.INVISIBLE
                        showToast(binding.root.context, getString(R.string.default_error))

                        return@collectLatest
                    }
                }
            }
        }
    }

    private fun submitAction() {
        binding.register.visibility = View.INVISIBLE
        binding.loading.visibility = View.VISIBLE

        val email = binding.registerEmail.text.toString().trim()
        val password = binding.registerPassword.text.toString().trim()
        val repeatPassword = binding.registerPasswordRepeat.text.toString().trim()

        //Validate Credentials
        if (!validateCredentials(email, password, repeatPassword)) return

        executeRegisterAction(email, password)

    }

    private fun initOnTextChange() {
        binding.registerEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(email: Editable?) {
                isEmailValid(email.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.registerPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(password: Editable?) {
                isPasswordValid(password.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.registerPasswordRepeat.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(repeatPassword: Editable?) {
                val password = binding.registerPassword.text.toString().trim()
                isRepeatPasswordValid(password, repeatPassword.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun executeRegisterAction(
        email: String,
        password: String,
    ) {
        lifecycleScope.launch {
            if (netManager.isConnectedToInternet())
                viewModel.register(email, password)
            else {
                showToast(binding.root.context, getString(R.string.network_error))
                binding.register.visibility = View.VISIBLE
                binding.loading.visibility = View.GONE
            }
        }
    }

    private fun validateCredentials(
        email: String,
        password: String,
        repeatPassword: String,
    ): Boolean {
        val isEmailValid = isEmailValid(email)
        val isPasswordValid = isPasswordValid(password)
        val isRepeatPasswordValid = isRepeatPasswordValid(password, repeatPassword)

        val isValid = isEmailValid && isPasswordValid && isRepeatPasswordValid

        if (!isValid) {
            binding.register.visibility = View.VISIBLE
            binding.loading.visibility = View.INVISIBLE
        }

        return isValid
    }

    private fun isRepeatPasswordValid(password: String, repeatPassword: String): Boolean {
        return if (TextUtils.isEmpty(repeatPassword) || password != repeatPassword) {
            binding.registerPasswordRepeatLayout.error =
                getString(R.string.passwords_dont_match)
            false
        } else {
            binding.registerPasswordRepeatLayout.error = ""
            true
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return if (TextUtils.isEmpty(password)) {
            binding.registerPasswordLayout.error = getString(R.string.password_required)
            false
        } else if (password.length < 8) {
            binding.registerPasswordLayout.error = getString(R.string.password_short)
            false
        } else {
            binding.registerPasswordLayout.error = ""
            true
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return if (TextUtils.isEmpty(email)) {
            binding.registerEmailLayout.error = getString(R.string.email_required)
            false
        } else if (!email.isValidEmail()) {
            binding.registerEmailLayout.error = getString(R.string.email_invalid)
            false
        } else {
            binding.registerEmailLayout.error = ""
            true
        }
    }

    private fun String.isValidEmail(): Boolean {
        return !TextUtils.isEmpty(this) && Patterns.EMAIL_ADDRESS.matcher(this)
            .matches()
    }
}