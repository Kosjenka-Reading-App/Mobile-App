package com.dsd.kosjenka.di

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.UserProfileListItemBinding
import com.dsd.kosjenka.domain.models.UserProfile

object AdapterModule {
    class UserProfilesAdapter(
        private val userProfiles: MutableList<UserProfile>,
        private val mListener: ProfileItemClickListener
    ) : RecyclerView.Adapter<UserProfilesAdapter.UserProfileViewHolder>() {

        class UserProfileViewHolder(
            val binding: UserProfileListItemBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(currentProfile: UserProfile, listener: ProfileItemClickListener, isSpecialItem: Boolean){
                if (isSpecialItem) {
                    binding.profileListName.text = ""
                    binding.profileImageView.setImageResource(R.drawable.outline_add_box_24)
                    binding.root.setOnClickListener {
                        listener.let {
                            it.onAddProfileClick()
                        }
                    }
                    binding.executePendingBindings()
                }
                else {
                    binding.profileListName.text = currentProfile.username
                    binding.profileImageView.setImageResource(R.drawable.start_image)
                    binding.root.setOnClickListener {
                        listener.let {
                            it.onProfileClick(
                                currentProfile
                            )
                        }
                    }
                    binding.executePendingBindings()
                }
            }

            companion object {
                fun from(parent: ViewGroup): UserProfileViewHolder {
                    val layoutInflater = LayoutInflater.from(parent.context)
                    val itemBinding =
                        UserProfileListItemBinding
                            .inflate(layoutInflater, parent, false)
                    return UserProfileViewHolder(itemBinding)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserProfileViewHolder {
//            val adapterLayout : View = if (viewType == R.layout.item_button_add_profile)
//                    LayoutInflater.from(parent.context).inflate(
//                        R.layout.user_profile_list_item, parent, false)
//            else
//                LayoutInflater.from(parent.context).inflate(
//                        R.layout.user_profile_list_item, parent, false)

            return UserProfileViewHolder.from(parent)
        }

        override fun getItemCount(): Int {
            return userProfiles.size + 1
        }

        override fun onBindViewHolder(holder: UserProfileViewHolder, position: Int) {
//            if (holder.itemViewType == R.layout.item_button_add_profile) {
//                holder.bind(userProfiles[position], mListener)
//            } else {
//                holder.bind(userProfiles[position], mListener)
//            }
            val isSpecialItem : Boolean = (position == userProfiles.size)
            if (isSpecialItem){
                holder.bind(UserProfile(-1, "", 0.0), mListener, isSpecialItem)
            } else {
                holder.bind(userProfiles[position], mListener, isSpecialItem)
            }

        }

//        override fun getItemViewType(position: Int): Int {
//            return if(userProfiles.size == position) R.layout.item_button_add_profile else R.layout.user_profile_list_item
//        }

        interface ProfileItemClickListener{
            fun onProfileClick(profile: UserProfile)
            fun onAddProfileClick()
        }
//
//        private var onProfileClickListener: ((UserProfile) -> Unit)? = null
//        private var onNewClickListener: (() -> Unit)? = null
//
//        fun setOnProfileClickListener(listener: (UserProfile) -> Unit) {
//            onProfileClickListener = listener
//        }
//
//        fun setOnNewClickListener(listener: () -> Unit) {
//            onNewClickListener = listener
//        }

    }
}