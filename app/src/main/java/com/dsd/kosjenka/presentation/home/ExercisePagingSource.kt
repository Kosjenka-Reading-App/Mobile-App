package com.dsd.kosjenka.presentation.home


import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dsd.kosjenka.data.remote.ApiService
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.utils.PAGE_SIZE
import com.dsd.kosjenka.utils.STARTING_PAGE_INDEX
import retrofit2.HttpException
import java.io.IOException

class ExercisePagingSource(
    private val apiService: ApiService,
    private val userId: String,
    private val orderBy: String,
    private val order: String,
    private val category: String?,
    private val query: String?,
) : PagingSource<Int, Exercise>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Exercise> {
        val page = params.key ?: STARTING_PAGE_INDEX

        return try {
            val response = apiService.getExercises(
                page = page,
                size = PAGE_SIZE,
                userId = userId,
                orderBy = orderBy,
                order = order,
                category = category,
                query = query,
            )
            val exercises = response.body()?.items ?: emptyList()

            val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
            val nextKey = if (exercises.isEmpty()) null else page + 1

            LoadResult.Page(
                data = exercises,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Exercise>): Int? {
        return state.anchorPosition
    }

}