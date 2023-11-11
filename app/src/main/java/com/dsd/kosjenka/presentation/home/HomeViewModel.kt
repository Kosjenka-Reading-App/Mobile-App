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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val currentQuery = MutableLiveData<String?>(null)
    private val currentCategory = MutableLiveData<String?>(null)

    private var currentOrderBy = "Completion"
    private var currentOrder = "asc"

    fun getExercises(orderBy: String, order: String): LiveData<PagingData<Exercise>> =
        currentQuery.switchMap { queryString ->
            currentOrderBy = orderBy
            currentOrder = order
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

    fun changeOrder(order: String) {
        currentOrder = order
        refresh()
    }

    fun changeOrderBy(orderBy: String) {
        currentOrderBy = orderBy
        refresh()
    }

//    fun filterByCategory(category: String?) {
//        currentCategory.value = category
//        // Trigger the search by updating the currentQuery value
//        currentQuery.value = currentQuery.value
//    }
}