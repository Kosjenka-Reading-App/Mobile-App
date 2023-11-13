package com.dsd.kosjenka.presentation.user_profiles

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.AlertAddProfileBinding
import com.dsd.kosjenka.databinding.FragmentUserProfilesBinding
import com.dsd.kosjenka.di.AdapterModule
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.UiStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UserProfilesFragment : Fragment(),
    AdapterModule.UserProfilesAdapter.ProfileItemClickListener {

    private lateinit var profilesList : MutableList<UserProfile>
    private lateinit var userProfilesAdapter: AdapterModule.UserProfilesAdapter
    private lateinit var binding: FragmentUserProfilesBinding
    private val viewModel by viewModels<UserProfilesViewModel>()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_user_profiles, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        //getProfiles()
        observeViewModel()
    }

    private fun setupRecycler(){
        userProfilesAdapter = AdapterModule.UserProfilesAdapter(this)

        binding.recyclerViewUserProfiles.adapter = userProfilesAdapter
        binding.recyclerViewUserProfiles.setHasFixedSize(true)
    }

    private fun getProfiles(){
//        profilesList = mutableListOf(
//            UserProfile(0, "user1", 5.0),
//            UserProfile(1, "user2", 1.0),
//            UserProfile(2, "user3", 2.0))

//        userProfilesAdapter.differ.submitList(profilesList)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            getUsers()
            viewModel.eventFlow.collectLatest {
                when (it) {
                    UiStates.NO_INTERNET_CONNECTION -> {
                        Common.showToast(binding.root.context, getString(R.string.network_error))
                    }

                    UiStates.SUCCESS -> {
                        Common.showToast(binding.root.context, "User created")
                        getUsers()
                    }

                    else -> {
                        Common.showToast(binding.root.context, getString(R.string.default_error))
                        return@collectLatest
                    }
                }
            }
            launch {
                viewModel.profileDataFlow.collectLatest {

                }
            }
        }
    }

    private fun getUsers() = viewModel.getUsers().observe(viewLifecycleOwner) { profileData ->
        userProfilesAdapter.differ.submitList(profileData)
    }

    override fun onProfileClick(profile: UserProfile) {
        Toast.makeText(context, profile.username, Toast.LENGTH_SHORT).show()
        findNavController().navigate(UserProfilesFragmentDirections
            .actionUserProfilesFragmentToHomeFragment(profile.id_user))
    }

    override fun onAddProfileClick() {
        addProfileDialog()
    }

    private fun addProfileDialog(){
        val builder = AlertDialog.Builder(this.context)

        val dialogBinding : AlertAddProfileBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.alert_add_profile, null, false)

        with(builder){
            setTitle("Add Profile")
            setMessage("Please enter the new user-name")
            setView(dialogBinding.root)
            setPositiveButton("Add") { _: DialogInterface?, _: Int ->
                //Toast.makeText(context, "New profile Add", Toast.LENGTH_SHORT).show()
                if (isValidUsername(dialogBinding.addProfileEditText.text.toString())){
                    //createNewProfile(dialogBinding.addProfileEditText.text.toString())
                    executeAddUserAction(dialogBinding.addProfileEditText.text.toString())
                }
            }
            setNegativeButton("Cancel") {dialog: DialogInterface?, _: Int -> dialog?.cancel()}
            show()
        }

    }

    private fun executeAddUserAction(
        username: String
    ) {
        lifecycleScope.launch {
            viewModel.addUser(username)
        }
    }

    private fun createNewProfile(username: String){
        val newuser = UserProfile(profilesList.size, 0, username, 0.0)
        profilesList.add(newuser)
        //Timber.tag("UserProfiles").d(profilesList.toString())
        userProfilesAdapter.differ.submitList(profilesList)
        userProfilesAdapter.notifyDataSetChanged()
    }

    private fun isValidUsername(username: String) : Boolean{
        return !TextUtils.isEmpty(username)
    }
}