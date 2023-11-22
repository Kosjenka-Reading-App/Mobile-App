package com.dsd.kosjenka.presentation.reset

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentRegisterBinding
import com.dsd.kosjenka.databinding.FragmentResetPasswordBinding
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.NetManager
import com.dsd.kosjenka.utils.UiStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ResetPasswordFragment : Fragment() {

    private lateinit var binding: FragmentResetPasswordBinding
    private val viewModel by viewModels<ResetPasswordView>()

    @Inject
    lateinit var netManager: NetManager

    companion object {
        fun newInstance() = ResetPasswordFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_reset_password, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = arguments?.getString("token")

        Log.d("SAJ",token.toString())
        binding.reset.setOnClickListener{
            Log.d("SAJ",token.toString())
            ExecuteForgotPassword(binding.resetEmail.text.toString())
        }

        observeViewModel()

    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.eventFlow.collectLatest {
                when (it) {
                    UiStates.NO_INTERNET_CONNECTION -> {
                        Common.showToast(binding.root.context, getString(R.string.network_error))
                    }

                    UiStates.EMAIL_UNAVAILABLE -> {
                        Common.showToast(
                            binding.root.context,
                            getString(R.string.email_unavailable)
                        )
                    }

                    UiStates.RESET_PASSWORD ->
                        findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)

                    else -> {
                        Common.showToast(binding.root.context, getString(R.string.default_error))
                        return@collectLatest
                    }
                }
            }
        }
    }
    /*private fun executeResetAction(
        token: String,
        password: String,
    ) {
        lifecycleScope.launch {
            if (netManager.isConnectedToInternet()) {
                viewModel.reset(token, password)
                Common.showToast(binding.root.context, "GrenzschutzTruppe")
            }else {
                Common.showToast(binding.root.context, getString(R.string.network_error))
                binding.loading.visibility = View.GONE
            }
        }
    }*/

    private fun ExecuteForgotPassword(
        email:String
    ){
        lifecycleScope.launch {
            if (netManager.isConnectedToInternet()) {
                viewModel.forgotPassword(email=email)
                Log.d("MSG","Grenzschutzgruppe")
            }else {
                Common.showToast(binding.root.context, getString(R.string.network_error))
                binding.loading.visibility = View.GONE
            }
        }
    }


}