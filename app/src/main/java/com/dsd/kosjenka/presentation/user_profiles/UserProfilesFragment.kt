package com.dsd.kosjenka.presentation.user_profiles

import android.os.Bundle
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
import com.dsd.kosjenka.databinding.FragmentHomeBinding
import com.dsd.kosjenka.databinding.FragmentUserProfilesBinding
import com.dsd.kosjenka.di.AdapterModule
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.presentation.home.ExerciseAdapter

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
        getProfiles()
        setupRecycler()
    }

    private fun setupRecycler(){
        userProfilesAdapter = AdapterModule.UserProfilesAdapter(profilesList, this)
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
    }
//    override fun onAddNewClick() {
//        Toast.makeText(context, "create new", Toast.LENGTH_SHORT).show()
//    }

    override fun onProfileClick(profile: UserProfile) {
        Toast.makeText(context, profile.username, Toast.LENGTH_SHORT).show()
        findNavController().navigate(UserProfilesFragmentDirections
            .actionUserProfilesFragmentToHomeFragment(profile.profileId))
    }

    override fun onAddProfileClick() {
        Toast.makeText(context, "add profile", Toast.LENGTH_SHORT).show()
    }
}