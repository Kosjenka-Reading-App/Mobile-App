package com.dsd.kosjenka.presentation.home

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.data.exercise
import com.dsd.kosjenka.di.RecyclerAdapter

class HomeFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerview = view.findViewById<RecyclerView>(R.id.recyclerview)

        val context=requireContext()
        recyclerview.layoutManager = LinearLayoutManager(context)

        val data = ArrayList<exercise>()
        data.add(exercise(1,56,"Horor","Hard","Alibaba i 40 hajduka","Lorem Ipsum"))
        data.add(exercise(2,70,"Bajka","Hard","Mumijevi","Lorem Ipsum"))
        data.add(exercise(3,0,"Uspavanka","Hard","Hajdi","Lorem ipsum"))


        val adapter = RecyclerAdapter(data)

        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter
    }

}