package com.dsd.kosjenka.presentation.user_profiles

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Resources.Theme
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.AlertAddProfileBinding
import com.dsd.kosjenka.databinding.FragmentUserProfilesBinding
import com.dsd.kosjenka.di.AdapterModule
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.UiStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
        (requireActivity() as MainActivity).setSupportActionBar(binding.profilesToolbar)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        binding.profilesToolbar.inflateMenu(R.menu.profiles_fragment_menu)
//        binding.profilesToolbar.setOnMenuItemClickListener {
//            context?.let { it1 -> Common.showToast(it1, "Menu item") }
//            when (it.itemId) {
//                R.id.menu_action_logout -> {
//                    context?.let { it1 -> Common.showToast(it1, "Logout") }
//                    executeLogoutAction()
//                    true
//                }
//                else -> {
//                    @Suppress("DEPRECATION")
//                    super.onOptionsItemSelected(it)
//                }
//            }
//        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.profiles_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_action_logout -> {
                        context?.let { Common.showToast(it, "Log Out") }
                        executeLogoutAction()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        setupRecycler()
        observeViewModel()
        getUsers()
    }

    private fun setupRecycler(){
        userProfilesAdapter = AdapterModule.UserProfilesAdapter(this)


        binding.recyclerViewUserProfiles.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this.context, 2)
            adapter = userProfilesAdapter
        }
    }

//    private fun getProfiles(){
//        profilesList = mutableListOf(
//            UserProfile(0, "user1", 5.0),
//            UserProfile(1, "user2", 1.0),
//            UserProfile(2, "user3", 2.0))

//        userProfilesAdapter.differ.submitList(profilesList)
//    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            getUsers()
            viewModel.eventFlow.collectLatest {
                when (it) {
                    UiStates.NO_INTERNET_CONNECTION -> {
                        Common.showToast(binding.root.context, getString(R.string.network_error))
                    }

                    UiStates.SUCCESS -> {
                        //Common.showToast(binding.root.context, "User created")
                        getUsers()
                    }

                    else -> {
                        Common.showToast(binding.root.context, getString(R.string.default_error))
                        return@collectLatest
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.profileDataFlow.collect {profileList ->
                userProfilesAdapter.differ.submitList(profileList)
            }
        }
    }

    private fun getUsers() = executeGetUsersAction()

    override fun onProfileClick(profile: UserProfile) {
        //Toast.makeText(context, profile.username, Toast.LENGTH_SHORT).show()
        context?.let { Common.showToast(it, profile.username) }
        findNavController().navigate(UserProfilesFragmentDirections
            .actionUserProfilesFragmentToHomeFragment(profile.id_user))
    }

    override fun onAddProfileClick() {
        showProfileDialog(null)
    }

    override fun onLongProfileClick(profile: UserProfile) {
        //showProfileDialog(profile)
    }

    override fun onEditProfileClick(profile: UserProfile) {
        showProfileDialog(profile)
    }

    private fun showProfileDialog(profile: UserProfile?){
        val builder = AlertDialog.Builder(this.context)

        val dialogBinding : AlertAddProfileBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.alert_add_profile, null, false)

        if (profile == null){
            with(builder){
                setTitle("Add Profile")
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
        } else {
            val d = with(builder){
                setTitle("Edit Profile")
                setView(dialogBinding.root)
                dialogBinding.addProfileEditText.setText(profile.username)
                setPositiveButton("Save") { _: DialogInterface?, _: Int ->
                    //Toast.makeText(context, "New profile Add", Toast.LENGTH_SHORT).show()
                    if (isValidUsername(dialogBinding.addProfileEditText.text.toString())){
                        //createNewProfile(dialogBinding.addProfileEditText.text.toString())
                        executeEditUserAction(profile, dialogBinding.addProfileEditText.text.toString())
                        userProfilesAdapter.notifyItemChanged(userProfilesAdapter.differ.currentList.indexOf(profile))
                    }
                }
                setNegativeButton("Cancel") {dialog: DialogInterface?, _: Int -> dialog?.cancel()}
                setNeutralButton("Delete Profile") {_: DialogInterface?, _: Int ->
                    executeDeleteUserAction(profile)
                }
                create()
            }
            d.setOnShowListener() {_: DialogInterface? ->
                d.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(resources.getColor(R.color.hard, resources.newTheme()))
            }
            d.show()
        }

    }

    private fun executeAddUserAction(
        username: String
    ) {
        lifecycleScope.launch {
            viewModel.addUser(username)
        }
    }

    private fun executeEditUserAction(
        profile: UserProfile,
        username: String
    ) {
        lifecycleScope.launch {
            viewModel.editUser(profile, username)
        }
    }
    fun executeLogoutAction(){
        sharedPreferences.clearPreferences()
        findNavController().navigate(
            UserProfilesFragmentDirections.actionUserProfilesFragmentToMainFragment()
        )
    }

    fun executeDeleteUserAction(profile: UserProfile){
        lifecycleScope.launch {
            viewModel.deleteUser(profile)
        }
    }

    private fun createNewProfile(username: String){
        val newuser = UserProfile(profilesList.size, 0, username, 0.0)
        profilesList.add(newuser)
        //Timber.tag("UserProfiles").d(profilesList.toString())
        userProfilesAdapter.differ.submitList(profilesList)
    }

    private fun executeGetUsersAction(){
        lifecycleScope.launch {
            viewModel.getUsers()
        }
    }

//    private fun createNewProfile(username: String){
//        val newuser = UserProfile(profilesList.size, 0, username, 0.0)
//        profilesList.add(newuser)
//        //Timber.tag("UserProfiles").d(profilesList.toString())
//        userProfilesAdapter.differ.submitList(profilesList)
//        userProfilesAdapter.notifyDataSetChanged()
//    }

    private fun isValidUsername(username: String) : Boolean{
        return !TextUtils.isEmpty(username)
    }
}