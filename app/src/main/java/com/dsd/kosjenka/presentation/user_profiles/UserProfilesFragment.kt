package com.dsd.kosjenka.presentation.user_profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.di.AdapterModule
import com.dsd.kosjenka.domain.models.UserProfile

class UserProfilesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profiles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val profilesList = listOf<UserProfile>(
            UserProfile("user1", 5.0),
            UserProfile("user1", 5.0),
            UserProfile("user1", 5.0)
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_user_profiles)
        recyclerView.adapter = AdapterModule.UserProfilesAdapter(profilesList)
        recyclerView.setHasFixedSize(true)
    }

}