package com.dsd.kosjenka.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.domain.repository.ExerciseRepository
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.defaultOrder
import com.dsd.kosjenka.utils.defaultOrderBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ExerciseRepository,
) : ViewModel() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val currentQuery = MutableLiveData<String?>(null)
    private val currentCategory = MutableLiveData<String?>(null)

    private var currentOrderBy = defaultOrderBy
    private var currentOrder = defaultOrder

    fun getExercises(): LiveData<PagingData<Exercise>> = currentQuery.switchMap { queryString ->
        repository.getExercises(
            orderBy = currentOrderBy,
            order = currentOrder,
            category = currentCategory.value,
            query = queryString,
        ).cachedIn(viewModelScope)
    }

    fun refresh() {
        currentQuery.value = currentQuery.value
    }

    fun searchExercises(query: String?) {
        if (query == "") currentQuery.value = null
        else currentQuery.value = query
    }

    fun getCategories() {
        viewModelScope.launch {
            launch { repository.getCategories().collect() }
        }
    }

    fun sortByComplexity() {
        // If already sorting by complexity in ascending order, switch to descending order
        if (currentOrderBy == "complexity" && currentOrder == "asc") {
            currentOrder = "desc"
        } else {
            // Otherwise, set it to ascending order
            currentOrderBy = "complexity"
            currentOrder = "asc"
        }
        refresh()
    }

    fun sortByCompletion() {
        // If already sorting by completion in ascending order, switch to descending order
        if (currentOrderBy == "completion" && currentOrder == "asc") {
            currentOrder = "desc"
        } else {
            // Otherwise, set it to ascending order
            currentOrderBy = "completion"
            currentOrder = "asc"
        }
        refresh()
    }

    fun filterByCategory(category: String?) {
        currentCategory.value = category
        // Trigger the search by updating the currentQuery value
        currentQuery.value = currentQuery.value
    }
}