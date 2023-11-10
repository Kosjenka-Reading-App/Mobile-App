package com.dsd.kosjenka.presentation.user_profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class UserProfilesFragment : Fragment(){

    private lateinit var profilesList : MutableList<UserProfile>
    private lateinit var userProfilesAdapter: AdapterModule.UserProfilesAdapter
    private lateinit var binding: FragmentUserProfilesBinding
    private val viewModel by lazy { ViewModelProvider(this)[UserProfilesViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_user_profiles, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        getProfiles()
    }

    private fun setupRecycler(){
        userProfilesAdapter = AdapterModule.UserProfilesAdapter(profilesList)
        userProfilesAdapter.setOnProfileClickListener {
            findNavController() //navigate
        }
        userProfilesAdapter.setOnNewClickListener {
            findNavController() //navigate
        }

        binding.recyclerViewUserProfiles.adapter = userProfilesAdapter
        binding.recyclerViewUserProfiles.setHasFixedSize(true)
    }

    private fun getProfiles(){
        profilesList = mutableListOf<UserProfile>(
            UserProfile("user1", 5.0),
            UserProfile("user2", 1.0),
            UserProfile("user3", 2.0))
    }

//    override fun onProfileClick(position: Int) {
//        findNavController().navigate(R.id.action_userProfilesFragment_to_homeFragment)
//    }
//
//    override fun OnAddNewClick(position: Int) {
//        TODO("Not yet implemented")
//    }
}