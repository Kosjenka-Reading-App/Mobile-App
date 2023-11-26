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
    private val orderBy: String,
    private val order: String,
    private val category: String?,
    private val query: String?,
) : PagingSource<Int, Exercise>() {

    private var currentOffset = 0

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Exercise> {
        val position = params.key ?: STARTING_PAGE_INDEX

        return try {
            val response = apiService.getExercises(
                skip = currentOffset,
                limit = PAGE_SIZE,
                orderBy = orderBy,
                order = order,
                category = category,
                query = query,
            )
            val resBody = response.body()
            val exercises = resBody?.items ?: emptyList()

            currentOffset += exercises.size

            val nextKey = if (resBody?.page == resBody?.pages) null else position + 1

            LoadResult.Page(
                data = exercises,
                prevKey = if (position == STARTING_PAGE_INDEX) null else position - 1,
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