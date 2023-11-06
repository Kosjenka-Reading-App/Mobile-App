package com.dsd.kosjenka.presentation.home

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
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

        var completion_btn=view.findViewById<Button>(R.id.btn_completion)
        var complexity_btn=view.findViewById<Button>(R.id.btn_complexity)


        val recyclerview = view.findViewById<RecyclerView>(R.id.recyclerview)

        val context=requireContext()
        recyclerview.layoutManager = LinearLayoutManager(context)

        val data = ArrayList<exercise>()//This is imtation of the database
        data.add(exercise(1,56,"Horor","Hard","Sleeping Beauty","Lorem Ipsum"))
        data.add(exercise(2,70,"Bajka","Medium","The Frog Prince","Lorem Ipsum"))
        data.add(exercise(3,0,"Uspavanka","Easy","Aesops Fables","Lorem ipsum"))
        data.add(exercise(4,56,"Horor","Hard","Sleeping Beauty","Lorem Ipsum"))
        data.add(exercise(5,70,"Bajka","Medium","The Frog Prince","Lorem Ipsum"))
        data.add(exercise(6,0,"Uspavanka","Easy","Aesops Fables","Lorem ipsum"))


        val adapter = RecyclerAdapter(data)

        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter

        ////On click
        completion_btn.setOnClickListener{
            val data=sortByCompletion(data)
            val adapter = RecyclerAdapter(data)

            // Setting the Adapter with the recyclerview
            recyclerview.adapter = adapter
        }

        complexity_btn.setOnClickListener{
            val data=sortByComplexity(data)
            val adapter = RecyclerAdapter(data)

            recyclerview.adapter = adapter
        }
    }

    fun sortByComplexity(arr: ArrayList<exercise>): ArrayList<exercise> {
        val complexityOrder = mapOf("Easy" to 1, "Medium" to 2, "Hard" to 3)

        arr.sortWith(compareBy { complexityOrder[it.complexity] })

        return arr
    }
    fun sortByCompletion(arr:ArrayList<exercise>):ArrayList<exercise> {
        for (i in 0 until arr.size-1) {
            for (j in i+1 until arr.size) {
                if (arr[i].completion<arr[j].completion) {
                    val temp=arr[i]
                    arr[i]=arr[j]
                    arr[j]=temp
                }
            }
        }
        return arr
    }
}