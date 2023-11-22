package com.dsd.kosjenka.data.remote

import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.flow

open class BaseRepository {


    protected fun <T> retrieveResourceAsLiveData(response: suspend () -> T) =
        liveData {
            val result = response.invoke()
            emit(result)
        }


    protected fun <T> retrieveResourceAsFlow(response: suspend () -> T) =
        flow {
            val result = response.invoke()
            emit(result)
        }

}