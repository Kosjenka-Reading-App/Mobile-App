package com.dsd.kosjenka.presentation.auth.login

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentLoginBinding
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.utils.Common.Companion.isValidEmail
import com.dsd.kosjenka.utils.Common.Companion.showToast
import com.dsd.kosjenka.utils.NetManager
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.UiStates.NO_INTERNET_CONNECTION
import com.dsd.kosjenka.utils.UiStates.SUCCESS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel by viewModels<LoginViewModel>()

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var netManager: NetManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_login, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity
        activity.setSupportActionBar(binding.loginToolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.resetPwd.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_loginFragment_to_resetPasswordFragment)
        )

        binding.registerLink.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_loginFragment_to_registerFragment)
        )

        binding.login.setOnClickListener {
            submitAction()
        }

        initOnTextChange()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.eventFlow.collectLatest {
                when (it) {
                    NO_INTERNET_CONNECTION -> {
                        binding.loading.visibility = View.GONE
                        binding.login.visibility = View.VISIBLE
                        showToast(binding.root.context, getString(R.string.network_error))
                    }
                    SUCCESS -> {
                        preferences.isLoggedIn = true
                        findNavController().navigate(R.id.action_loginFragment_to_userProfilesFragment)
                    }
                    else -> {
                        binding.loading.visibility = View.GONE
                        binding.login.visibility = View.VISIBLE
                        showToast(binding.root.context, getString(R.string.default_error))
                        return@collectLatest
                    }
                }
            }
            launch {
                viewModel.tokenDataFlow.collectLatest {
                    Timber.d("Saved access token: ${it.access_token}")
                    preferences.accessToken = it.access_token
                    Timber.d("Saved refresh token: ${it.refresh_token}")
                    preferences.refreshToken = it.refresh_token
                }
            }
        }
    }

    private fun initOnTextChange() {
        binding.loginEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(email: Editable?) {
                isEmailValid(email.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.loginPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(password: Editable?) {
                isPasswordValid(password.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun submitAction() {
        binding.login.visibility = View.INVISIBLE
        binding.loading.visibility = View.VISIBLE

        val email = binding.loginEmail.text.toString().trim()
        val password = binding.loginPassword.text.toString().trim()

        //Validate credentials
        if (!validateCredentials(email, password)) return

        executeLoginAction(email, password)

    }

    private fun validateCredentials(
        email: String,
        password: String,
    ): Boolean {
        val isEmailValid = isEmailValid(email)
        val isPasswordValid = isPasswordValid(password)

        val isValid = isEmailValid && isPasswordValid

        if (!isValid) {
            binding.login.visibility = View.VISIBLE
            binding.loading.visibility = View.INVISIBLE
        }

        return isValid
    }

    private fun executeLoginAction(
        email: String,
        password: String,
    ) {
        lifecycleScope.launch {
            if (netManager.isConnectedToInternet()) viewModel.login(email, password)
            else {
                showToast(binding.root.context, getString(R.string.network_error))
                binding.login.visibility = View.VISIBLE
                binding.loading.visibility = View.GONE
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return if (TextUtils.isEmpty(email)) {
            binding.loginEmailLayout.error = getString(R.string.email_required)
            false
        } else if (!email.isValidEmail()) {
            binding.loginEmailLayout.error = getString(R.string.email_invalid)
            false
        } else {
            binding.loginEmailLayout.error = ""
            true
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return if (TextUtils.isEmpty(password)) {
            binding.loginPasswordLayout.error = getString(R.string.password_required)
            false
        } else {
            binding.loginPasswordLayout.error = ""
            true
        }
    }

}