package com.dsd.kosjenka.presentation.user_profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.di.AdapterModule
import com.dsd.kosjenka.domain.models.UserProfile

class UserProfilesFragment : Fragment(), AdapterModule.UserProfilesAdapter.OnProfileClickListener {

    private val profilesList = listOf<UserProfile>(
        UserProfile("user1", 5.0),
        UserProfile("user2", 1.0),
        UserProfile("user3", 2.0))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profiles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
<<<<<<< HEAD
=======
        val profilesList = listOf<UserProfile>(
            UserProfile("user1", 5.0),
            UserProfile("user1", 5.0),
            UserProfile("user1", 5.0)
        )
>>>>>>> 6aaff30560c360f17bdad804642be1c9f62d93a5

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_user_profiles)
        recyclerView.adapter = AdapterModule.UserProfilesAdapter(profilesList, this)
        recyclerView.setHasFixedSize(true)

    }

    override fun onProfileClick(position: Int) {
        findNavController().navigate(R.id.action_userProfilesFragment_to_homeFragment)
    }

}