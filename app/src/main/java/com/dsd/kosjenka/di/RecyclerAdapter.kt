package com.dsd.kosjenka.di
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.core.content.ContentProviderCompat
import androidx.core.content.ContextCompat
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
        val complexity=ItemsViewModel.complexity
        if(complexity.equals("Easy")){
            holder.complexity.setTextColor((ContextCompat.getColor(holder.itemView.context, R.color.easy)))
        }else if(complexity.equals("Medium")){
            holder.complexity.setTextColor((ContextCompat.getColor(holder.itemView.context, R.color.normal)))
        }else{
            holder.complexity.setTextColor((ContextCompat.getColor(holder.itemView.context, R.color.hard)))
        }

        // sets the text to the textview from our itemHolder class
        holder.title.text = ItemsViewModel.title
        holder.completion.text=ItemsViewModel.completion.toString()+"%"
        holder.complexity.text=complexity

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