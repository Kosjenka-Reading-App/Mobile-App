package com.dsd.kosjenka.di

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.domain.models.UserProfile

object AdapterModule {
    class UserProfilesAdapter(
        private val userProfiles: MutableList<UserProfile>,
        //private val profileClickListener: OnProfileClickListener
    ) : RecyclerView.Adapter<UserProfilesAdapter.UserProfileViewHolder>() {

        class UserProfileViewHolder(
            private val view: View//, private val profileClickListener: OnProfileClickListener
        ) : RecyclerView.ViewHolder(view) {
            val textView : TextView = view.findViewById(R.id.profile_list_name)
            val imageView : ImageView = view.findViewById(R.id.profile_image_view)
            //val _ls = view.setOnClickListener(this)
            //val button : Button = view.findViewById()
//            val profileClickListener = View.OnClickListener {
//                profileClickListener.onProfileClick(bindingAdapterPosition)
//            }

//            override fun onClick(v: View?) {
//                profileClickListener.onProfileClick(bindingAdapterPosition)
//            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserProfileViewHolder {
            val adapterLayout : View = if (viewType == R.layout.user_profile_list_item)
                LayoutInflater.from(parent.context).inflate(
                    R.layout.user_profile_list_item, parent, false)
            else
                LayoutInflater.from(parent.context).inflate(
                    R.layout.user_profile_list_item, parent, false)

            return UserProfileViewHolder(adapterLayout)
        }

        override fun getItemCount(): Int {
            return userProfiles.size + 1
        }

        override fun onBindViewHolder(holder: UserProfileViewHolder, position: Int) {
            if (itemCount == position) {
                holder.textView.text = ""
                holder.imageView.setImageResource(R.drawable.main_logo)
                holder.itemView.setOnClickListener { onProfileClickListener }

            } else {
                val userProfile = userProfiles[position]
                holder.textView.text = userProfile.username
                holder.imageView.setImageResource(R.drawable.start_image)
                holder.itemView.setOnClickListener { onNewClickListener }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if(userProfiles.size == position) R.layout.user_profile_list_item else R.layout.user_profile_list_item
        }

//        interface OnProfileClickListener{
//            fun onProfileClick(position: Int)
//            fun OnAddNewClick(position: Int)
//        }

        private var onProfileClickListener: ((UserProfile) -> Unit)? = null
        private var onNewClickListener: (() -> Unit)? = null

        fun setOnProfileClickListener(listener: (UserProfile) -> Unit) {
            onProfileClickListener = listener
        }

        fun setOnNewClickListener(listener: () -> Unit) {
            onNewClickListener = listener
        }

    }
}