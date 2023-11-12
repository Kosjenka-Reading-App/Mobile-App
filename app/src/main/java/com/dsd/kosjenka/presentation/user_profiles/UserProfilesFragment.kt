package com.dsd.kosjenka.presentation.user_profiles

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.AlertAddProfileBinding
import com.dsd.kosjenka.databinding.FragmentHomeBinding
import com.dsd.kosjenka.databinding.FragmentUserProfilesBinding
import com.dsd.kosjenka.di.AdapterModule
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.presentation.auth.login.LoginFragmentDirections
import com.dsd.kosjenka.presentation.home.ExerciseAdapter
import timber.log.Timber
import java.util.logging.Logger

class UserProfilesFragment : Fragment(),
    AdapterModule.UserProfilesAdapter.ProfileItemClickListener {

    private lateinit var profilesList : MutableList<UserProfile>
    private lateinit var userProfilesAdapter: AdapterModule.UserProfilesAdapter
    private lateinit var binding: FragmentUserProfilesBinding
    private val viewModel by lazy { ViewModelProvider(this)[UserProfilesViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_user_profiles, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //First get the list, then you add it to the adapter
        getProfiles()
        setupRecycler()
    }

    private fun setupRecycler(){
        userProfilesAdapter = AdapterModule.UserProfilesAdapter(this)
//        userProfilesAdapter.setOnProfileClickListener {
//            findNavController().navigate(UserProfilesFragmentDirections
//                .actionUserProfilesFragmentToHomeFragment())
//        }
//        userProfilesAdapter.setOnNewClickListener {
////            findNavController()//.navigate
//            Toast.makeText(context, "create new", Toast.LENGTH_SHORT).show()
//        }

        binding.recyclerViewUserProfiles.adapter = userProfilesAdapter
        binding.recyclerViewUserProfiles.setHasFixedSize(true)
    }

    private fun getProfiles(){
        profilesList = mutableListOf(
            UserProfile(0, "user1", 5.0),
            UserProfile(1, "user2", 1.0),
            UserProfile(2, "user3", 2.0))

        userProfilesAdapter.differ.submitList(profilesList)
    }

    override fun onProfileClick(profile: UserProfile) {
        Toast.makeText(context, profile.username, Toast.LENGTH_SHORT).show()
        findNavController().navigate(UserProfilesFragmentDirections
            .actionUserProfilesFragmentToHomeFragment(profile.profileId))
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
            setPositiveButton("Add") {dialog: DialogInterface?, which: Int ->
                //Toast.makeText(context, "New profile Add", Toast.LENGTH_SHORT).show()
                if (isValidUsername(dialogBinding.addProfileEditText.text.toString())){
                    createNewProfile(dialogBinding.addProfileEditText.text.toString())
                }
            }
            setNegativeButton("Cancel") {dialog: DialogInterface?, _: Int -> dialog?.cancel()}
            show()
        }

    }

    private fun createNewProfile(username: String){
        val newuser = UserProfile(profilesList.size, username, 0.0)
        profilesList.add(newuser)
        //Timber.tag("UserProfiles").d(profilesList.toString())
        userProfilesAdapter.differ.submitList(profilesList)
        userProfilesAdapter.notifyDataSetChanged()
    }

    private fun isValidUsername(username: String) : Boolean{
        return !TextUtils.isEmpty(username)
    }
}