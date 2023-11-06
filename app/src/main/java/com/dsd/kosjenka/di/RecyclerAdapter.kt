package com.dsd.kosjenka.di
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.data.exercise
import com.dsd.kosjenka.R

class RecyclerAdapter(private val mList: List<exercise>) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_exercise, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]

        // sets the text to the textview from our itemHolder class
        holder.title.text = ItemsViewModel.title
        holder.completion.text=ItemsViewModel.completion.toString()
        holder.complexity.text=ItemsViewModel.complexity
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val complexity: TextView = itemView.findViewById(R.id.complexity)
        val completion:TextView = itemView.findViewById(R.id.completion)
        //val title: TextView = itemView.findViewById(R.id.title)
    }
}