package com.dsd.kosjenka.presentation.home.calibrate

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.stream.IntStream
import java.util.stream.Stream

class CalibrateViewModel: ViewModel() {


    val calibScreenPointList: MutableLiveData<MutableList<FloatArray?>> by lazy {
        MutableLiveData<MutableList<FloatArray?>>()
    }

    val calibrationCount: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    fun getScreenGridPoints(): MutableList<FloatArray?> {
        //val ratio: Float = width.toFloat() / height.toFloat()
        val xNum = 5
        val yNum = 10
        val xs : Array<Float> =
            linspace(-0.95f, 0.95f, xNum).toArray {size -> arrayOfNulls<Float>(size) }
        val ys : Array<Float> =
            linspace(-0.95f, 0.95f, yNum).toArray {size -> arrayOfNulls<Float>(size) }

        val pointsMatrix : Array<Array<FloatArray?>> = Array(yNum) { arrayOfNulls(xNum) }

        for ((row, y) in ys.withIndex()){
            for ((col,x) in xs.withIndex()){
                pointsMatrix[row][col] = floatArrayOf(x, y, 0f)
            }
        }

        return pointsMatrix.flatten().shuffled().toMutableList()
    }

    private fun linspace(start: Float, end: Float, numPoints: Int): Stream<Float> {
        return IntStream.range(0, numPoints)
            .boxed()
            .map { i: Int -> start + i * (end - start) / (numPoints - 1) }
    }

}