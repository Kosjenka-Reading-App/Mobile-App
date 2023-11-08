package com.dsd.kosjenka.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.ItemExerciseBinding
import com.dsd.kosjenka.domain.models.Exercise

class ExerciseAdapter : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(private val itemBinding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(exercise: Exercise, onExerciseClickListener: ((Exercise) -> Unit)?) {
            itemBinding.apply {

                itemBinding.exerciseTitle.text = exercise.title
                var categoriesString = ""
                exercise.category.forEachIndexed { index, category ->
                    categoriesString += category.category
                    if (index < exercise.category.size - 1)
                        categoriesString += ", "
                }
                itemBinding.exerciseCategory.text = categoriesString


                when (exercise.complexity) {
                    "Easy" -> itemBinding.exerciseComplexity.setTextColor(
                        ContextCompat.getColor(
                            itemBinding.root.context,
                            R.color.easy
                        )
                    )

                    "Medium" -> itemBinding.exerciseComplexity.setTextColor(
                        ContextCompat.getColor(
                            itemBinding.root.context,
                            R.color.medium
                        )
                    )

                    "Hard" -> itemBinding.exerciseComplexity.setTextColor(
                        ContextCompat.getColor(
                            itemBinding.root.context,
                            R.color.hard
                        )
                    )

                    else -> itemBinding.exerciseComplexity.setTextColor(
                        ContextCompat.getColor(
                            itemBinding.root.context,
                            android.R.color.black
                        )
                    )
                }
                itemBinding.exerciseComplexity.text = exercise.complexity

                //Add completion later
                itemBinding.exerciseCompletion.text = "90%"

                root.setOnClickListener {
                    onExerciseClickListener?.let { it(exercise) }
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): ExerciseViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val itemBinding =
                    ItemExerciseBinding.inflate(layoutInflater, parent, false)
                return ExerciseViewHolder(itemBinding)
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(
            oldItem: Exercise,
            newItem: Exercise,
        ): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(
            oldItem: Exercise,
            newItem: Exercise,
        ): Boolean =
            oldItem == newItem
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun getItemCount(): Int = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        return ExerciseViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(differ.currentList[position], onExerciseClickListener)
    }

    private var onExerciseClickListener: ((Exercise) -> Unit)? = null

    fun setOnExerciseClickListener(listener: (Exercise) -> Unit) {
        onExerciseClickListener = listener
    }

}