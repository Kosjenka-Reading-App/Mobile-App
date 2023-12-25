package com.dsd.kosjenka.presentation.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.ItemExerciseBinding
import com.dsd.kosjenka.domain.models.Exercise

class PagingExerciseAdapter :
    PagingDataAdapter<Exercise, PagingExerciseAdapter.ExerciseViewHolder>(DiffUtilCallBack()) {

    class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatTextViewDrawableApis")
        fun bind(
            exercise: Exercise,
            onExerciseClickListener: ((Exercise) -> Unit)?,
        ) {
            binding.apply {

                exerciseTitle.text = exercise.title
                var categoriesString = ""
                exercise.category.forEachIndexed { index, category ->
                    categoriesString += category.category
                    if (index < exercise.category.size - 1) categoriesString += ", "
                }
                exerciseCategory.visibility =
                    if (categoriesString != "") View.VISIBLE
                    else View.GONE
                exerciseCategory.text = categoriesString


                when (exercise.complexity) {
                    "easy" -> exerciseComplexity.setTextColor(
                        ContextCompat.getColor(
                            root.context, R.color.easy
                        )
                    )

                    "medium" -> exerciseComplexity.setTextColor(
                        ContextCompat.getColor(
                            root.context, R.color.medium
                        )
                    )

                    "hard" -> exerciseComplexity.setTextColor(
                        ContextCompat.getColor(
                            root.context, R.color.hard
                        )
                    )

                    else -> exerciseComplexity.setTextColor(
                        ContextCompat.getColor(
                            root.context, android.R.color.black
                        )
                    )
                }
                exerciseComplexity.text = exercise.complexity

                val completionString = if (exercise.completion == null) {
                    exerciseCompletion.visibility = View.GONE
                    "0%"
                } else {
                    exerciseCompletion.visibility = View.VISIBLE
                    "${exercise.completion.completion}%"
                }

                exerciseCompletion.text = completionString

                root.setOnClickListener {
                    onExerciseClickListener?.let { it(exercise) }
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): ExerciseViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemExerciseBinding.inflate(layoutInflater, parent, false)
                return ExerciseViewHolder(binding)
            }
        }
    }

    class DiffUtilCallBack : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(
            oldItem: Exercise,
            newItem: Exercise,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: Exercise,
            newItem: Exercise,
        ): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        return ExerciseViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position)!!, onExerciseClickListener)
    }

    private var onExerciseClickListener: ((Exercise) -> Unit)? = null

    fun setOnExerciseClickListener(listener: (Exercise) -> Unit) {
        onExerciseClickListener = listener
    }

}
