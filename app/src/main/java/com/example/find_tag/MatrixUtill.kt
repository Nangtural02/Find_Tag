package com.example.find_tag

import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


// 필요한 함수들 구현

fun transposeMatrix(matrix: Array<FloatArray>): Array<FloatArray> {
    val rowCount = matrix.size
    val colCount = matrix[0].size
    val transposed = Array(colCount) { FloatArray(rowCount) }
    for (i in 0 until rowCount) {
        for (j in 0 until colCount) {
            transposed[j][i] = matrix[i][j]
        }
    }
    return transposed
}

fun multiplyMatrixMatrix(a: Array<FloatArray>, b: Array<FloatArray>): Array<FloatArray> {
    val resultRows = a.size
    val resultCols = b[0].size
    val bRows = b.size
    val result = Array(resultRows) { FloatArray(resultCols) }
    for (i in 0 until resultRows) {
        for (j in 0 until resultCols) {
            var sum = 0f
            for (k in 0 until bRows) {
                sum += a[i][k] * b[k][j]
            }
            result[i][j] = sum
        }
    }
    return result
}
fun addMatrices(a: Array<FloatArray>, b: Array<FloatArray>): Array<FloatArray> {
    val rowCount = a.size
    val colCount = a[0].size
    val result = Array(rowCount) { FloatArray(colCount) }

    for (i in 0 until rowCount) {
        for (j in 0 until colCount) {
            result[i][j] = a[i][j] + b[i][j]
        }
    }
    return result
}
fun scalarMultiplyMatrix(matrix: Array<FloatArray>, scalar: Float): Array<FloatArray> {
    val rowCount = matrix.size
    val colCount = matrix[0].size
    val result = Array(rowCount) { FloatArray(colCount) }

    for (i in 0 until rowCount) {
        for (j in 0 until colCount) {
            result[i][j] = matrix[i][j] * scalar
        }
    }
    return result
}

fun multiplyMatrixVector(matrix: Array<FloatArray>, vector: FloatArray): FloatArray {
    val resultSize = matrix.size
    val vectorSize = vector.size
    val result = FloatArray(resultSize)
    for (i in 0 until resultSize) {
        var sum = 0f
        for (j in 0 until vectorSize) {
            sum += matrix[i][j] * vector[j]
        }
        result[i] = sum
    }
    return result
}

fun invertMatrix(matrix: Array<FloatArray>): Array<FloatArray>? {
    val size = matrix.size
    val augmented = Array(size) { FloatArray(2 * size) }
    // 증분 행렬 생성 [A | I]
    for (i in 0 until size) {
        for (j in 0 until size) {
            augmented[i][j] = matrix[i][j]
        }
        augmented[i][size + i] = 1f
    }
    // 가우스 조던 소거법 적용
    for (i in 0 until size) {
        // 피벗 선택
        var maxRow = i
        for (k in i + 1 until size) {
            if (abs(augmented[k][i]) > abs(augmented[maxRow][i])) {
                maxRow = k
            }
        }
        // 행 교환
        val temp = augmented[i]
        augmented[i] = augmented[maxRow]
        augmented[maxRow] = temp

        // 특이 행렬 확인
        if (abs(augmented[i][i]) < 1e-8f) {
            return null // 역행렬 없음
        }

        // 피벗 행 정규화
        val pivot = augmented[i][i]
        for (j in 0 until 2 * size) {
            augmented[i][j] /= pivot
        }

        // 열 제거
        for (k in 0 until size) {
            if (k != i) {
                val factor = augmented[k][i]
                for (j in 0 until 2 * size) {
                    augmented[k][j] -= factor * augmented[i][j]
                }
            }
        }
    }
    // 역행렬 추출
    val inverse = Array(size) { FloatArray(size) }
    for (i in 0 until size) {
        for (j in 0 until size) {
            inverse[i][j] = augmented[i][j + size]
        }
    }
    return inverse
}

//fun determinant(matrix: Array<FloatArray>): Float {
//    when(matrix.size){
//        1 -> return matrix[0][0]
//        2 -> return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
//        3 -> return matrix[0][0] * (matrix[1][1] * matrix[2][2] - matrix[1][2] * matrix[2][1]) -
//                matrix[0][1] * (matrix[1][0] * matrix[2][2] - matrix[1][2] * matrix[2][0]) +
//                matrix[0][2] * (matrix[1][0] * matrix[2][1] - matrix[1][1] * matrix[2][0])
//        else -> return -66.66f //todo: 그 이상 크기의 determinant도 계산할 수 있게
//    }
//}
//
//fun adjoint(matrix: Array<FloatArray>): Array<FloatArray> {
//    val adj = Array(3) { FloatArray(3) }
//    adj[0][0] =  matrix[1][1] * matrix[2][2] - matrix[1][2] * matrix[2][1]
//    adj[0][1] = -(matrix[1][0] * matrix[2][2] - matrix[1][2] * matrix[2][0])
//    adj[0][2] =  matrix[1][0] * matrix[2][1] - matrix[1][1] * matrix[2][0]
//
//    adj[1][0] = -(matrix[0][1] * matrix[2][2] - matrix[0][2] * matrix[2][1])
//    adj[1][1] =  matrix[0][0] * matrix[2][2] - matrix[0][2] * matrix[2][0]
//    adj[1][2] = -(matrix[0][0] * matrix[2][1] - matrix[0][1] * matrix[2][0])
//
//    adj[2][0] =  matrix[0][1] * matrix[1][2] - matrix[0][2] * matrix[1][1]
//    adj[2][1] = -(matrix[0][0] * matrix[1][2] - matrix[0][2] * matrix[1][0])
//    adj[2][2] =  matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
//
//    // adjoint의 전치
//    return arrayOf(
//        floatArrayOf(adj[0][0], adj[1][0], adj[2][0]),
//        floatArrayOf(adj[0][1], adj[1][1], adj[2][1]),
//        floatArrayOf(adj[0][2], adj[1][2], adj[2][2])
//    )
//}
//fun invertMatrix(matrix: Array<FloatArray>): Array<FloatArray>?{
//    if(matrix.isEmpty()) return emptyArray()
//    val m = matrix.size
//    val n = matrix[0].size
//    if(m != n) return emptyArray()
//    val det = determinant(matrix)
//    if(det == 0f) return null
//    when(m){
//        1 -> return matrix
//        2 -> {
//            return arrayOf(
//                floatArrayOf(matrix[1][1] / det, -matrix[0][1] / det),
//                floatArrayOf(-matrix[1][0] / det, matrix[0][0] / det)
//            )
//        }
//        else -> {
//            val adjoint = adjoint(matrix)
//            val inverse = Array(m) { FloatArray(n) }
//            for (i in 0..< m) {
//                for (j in 0..< n) {
//                    inverse[i][j] = adjoint[i][j] / det
//                }
//            }
//            return inverse
//        }
//    }
//}
//
//fun multiplyMatrixVector(matrix: Array<FloatArray>?, vector: FloatArray): FloatArray {
//    val result = FloatArray(vector.size)
//    if(matrix == null){
//
//        return result
//    }
//    for (i in matrix!!.indices) {
//        for (j in vector.indices) {
//            result[i] += matrix[i][j] * vector[j]
//        }
//    }
//    return result
//}
//fun multiplyMatrixMatrix(matrix1: Array<FloatArray>?, matrix2: Array<FloatArray>?): Array<FloatArray> {
//    if(matrix1 == null || matrix2 == null){
//        return emptyArray()
//    }
//
//    val result = Array(matrix1.size) {FloatArray(matrix2[0].size)}
//    //println("${matrix1!!.size} x ${matrix2!!.size} matrix multiply")
//
//
//    for (i in matrix1.indices) {
//        for (j in matrix2[0].indices) {
//            for(k in matrix2.indices){
//                result[i][j] += matrix1[i][k] * matrix2[k][j]
//            }
//        }
//    }
//    return result
//
//}
//fun transitionMatrix(matrix: Array<FloatArray>?): Array<FloatArray>{
//    if(matrix == null){
//        return emptyArray()
//    }
//    else{
//        val result = Array(matrix[0].size) { FloatArray(matrix.size) }
//        for (i in result.indices) {
//            for (j in result[i].indices) {
//                result[i][j] = matrix[j][i]
//            }
//        }
//        return result
//    }
//}