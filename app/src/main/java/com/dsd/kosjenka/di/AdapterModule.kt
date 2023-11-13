package com.dsd.kosjenka.di

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.UserProfileListItemBinding
import com.dsd.kosjenka.domain.models.UserProfile

object AdapterModule {
    class UserProfilesAdapter(

        private val mListener: ProfileItemClickListener
    ) : RecyclerView.Adapter<UserProfilesAdapter.UserProfileViewHolder>() {

        class UserProfileViewHolder(
            val binding: UserProfileListItemBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(currentProfile: UserProfile, listener: ProfileItemClickListener, isSpecialItem: Boolean){
                if (isSpecialItem) {
                    binding.profileListName.text = "Add Profile"
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
            return UserProfileViewHolder.from(parent)
        }

        override fun getItemCount(): Int {
            return differ.currentList.size + 1
        }

        override fun onBindViewHolder(holder: UserProfileViewHolder, position: Int) {
            val isSpecialItem : Boolean = (position == differ.currentList.size)

            if (isSpecialItem){
                holder.bind(UserProfile(-1, 0,"", 0.0), mListener, isSpecialItem)
            } else {
                holder.bind(differ.currentList[position], mListener, isSpecialItem)
            }

        }

        private val differCallback = object : DiffUtil.ItemCallback<UserProfile>() {
            override fun areItemsTheSame(
                oldItem: UserProfile,
                newItem: UserProfile,
            ): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(
                oldItem: UserProfile,
                newItem: UserProfile,
            ): Boolean =
                oldItem.username == newItem.username && oldItem.id_user == newItem.id_user
        }

        val differ = AsyncListDiffer(this, differCallback)

        interface ProfileItemClickListener{
            fun onProfileClick(profile: UserProfile)
            fun onAddProfileClick()
        }

    }
}