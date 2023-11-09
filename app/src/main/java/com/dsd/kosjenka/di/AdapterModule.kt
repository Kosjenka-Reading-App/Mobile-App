package com.dsd.kosjenka.di

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.domain.models.UserProfile

object AdapterModule {
    class UserProfilesAdapter(
        private val userProfiles: List<UserProfile>,
        private val profileClickListener: OnProfileClickListener
    ) : RecyclerView.Adapter<UserProfilesAdapter.UserProfileViewHolder>() {

        class UserProfileViewHolder(private val view: View, private val profileClickListener: OnProfileClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
            val textView : TextView = view.findViewById(R.id.profile_list_name)
            val imageView : ImageView = view.findViewById(R.id.profile_image_view)
            val abd = view.setOnClickListener(this)

            override fun onClick(v: View?) {
                profileClickListener.onProfileClick(bindingAdapterPosition)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserProfileViewHolder {
            val adapterLayout =  LayoutInflater.from(parent.context).inflate(
                R.layout.user_profile_list_item, parent, false)

            return UserProfileViewHolder(adapterLayout, profileClickListener)
        }

        override fun getItemCount(): Int {
            return userProfiles.size
        }

        override fun onBindViewHolder(holder: UserProfileViewHolder, position: Int) {
            val userProfile = userProfiles[position]
            holder.textView.text = userProfile.username
            //holder.imageView.setImageResource(R.drawable.main_logo)
        }

        interface OnProfileClickListener{
            fun onProfileClick(position: Int)
        }
    }
}