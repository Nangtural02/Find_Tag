package com.example.find_tag

import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

fun refinePositionByGaussNewton(
    distances: List<Float>,
    anchorPosition: List<Point>,
    lastPosition: Point? = null
): Point {
    // 초기 위치 설정
    val initialPosition = Point(
        x = anchorPosition.map { it.x }.average().toFloat(),
        y = anchorPosition.map { it.y }.average().toFloat(),
        z = anchorPosition.map { it.z }.average().toFloat()
    )
    var currentPosition = lastPosition ?: initialPosition

    // 허용 오차와 최대 반복 횟수 설정
    val tolerance = 1e-6f
    val maxIterations = 100
    var iteration = 0

    val epsilon = 1e-8f

    Log.d("refine_gauss_newton", "===== Start refinePositionByGaussNewton =====")
    Log.d("refine_gauss_newton", "Initial position: $initialPosition")
    Log.d("refine_gauss_newton", "Last position: $lastPosition")
    Log.d("refine_gauss_newton", "Distances: $distances")
    Log.d("refine_gauss_newton", "Anchor positions: $anchorPosition")

    while (iteration < maxIterations) {
        val numAnchors = distances.size
        val jacobian = Array(numAnchors) { FloatArray(3) } // 자코비안 행렬
        val residuals = FloatArray(numAnchors) // 잔차 벡터

        // 현재 iteration과 현재 추정 위치 로그
        Log.d("refine_gauss_newton", "----- Iteration $iteration -----")
        Log.d("refine_gauss_newton", "Current position: $currentPosition")

        // 잔차와 자코비안 계산
        for (i in distances.indices) {
            val dx = currentPosition.x - anchorPosition[i].x
            val dy = currentPosition.y - anchorPosition[i].y
            val dz = currentPosition.z - anchorPosition[i].z

            // 혹시 sqrt 전후로 NaN, Inf가 나오는지 체크
            val distanceBeforeSqrt = dx * dx + dy * dy + dz * dz
            if (distanceBeforeSqrt.isNaN() || distanceBeforeSqrt.isInfinite()) {
                Log.w("refine_gauss_newton", "distanceBeforeSqrt is abnormal: $distanceBeforeSqrt at anchor $i")
            }

            val predictedDistance = sqrt(distanceBeforeSqrt) + epsilon
            if (predictedDistance.isNaN() || predictedDistance.isInfinite()) {
                Log.w("refine_gauss_newton", "predictedDistance is abnormal: $predictedDistance at anchor $i")
            }

            val residual = distances[i] - predictedDistance
            residuals[i] = residual

            // distanceInverse 계산
            val distanceInverse = 1f / predictedDistance
            if (distanceInverse.isNaN() || distanceInverse.isInfinite()) {
                Log.w("refine_gauss_newton", "distanceInverse is abnormal: $distanceInverse at anchor $i")
            }

            jacobian[i][0] = -dx * distanceInverse
            jacobian[i][1] = -dy * distanceInverse
            jacobian[i][2] = -dz * distanceInverse

            // 각 anchor별 로그
            Log.d(
                "refine_gauss_newton",
                "anchor $i | dx=$dx, dy=$dy, dz=$dz, predDist=$predictedDistance, " +
                        "residual=$residual, invDist=$distanceInverse"
            )
        }

        // 자코비안 전치 계산
        val jacobianT = transposeMatrix(jacobian)

        // H = J^T * J 계산
        val hessian = multiplyMatrixMatrix(jacobianT, jacobian)

        // 레귤러라이제이션 적용
        val lambda = 1e-3f
        val identityMatrix = Array(3) { FloatArray(3) { 0f } }
        for (i in 0..2) {
            identityMatrix[i][i] = 1f
        }
        val regularizedHessian = addMatrices(hessian, scalarMultiplyMatrix(identityMatrix, lambda))

        // Hessian 값 로그 (간단히 한 줄로 표시)
        Log.d("refine_gauss_newton", "Regularized Hessian: ${regularizedHessian.joinToString { it.joinToString() }}")

        // 역행렬 계산
        val hessianInverse = invertMatrix(regularizedHessian)
        if (hessianInverse == null) {
            Log.e("refine_gauss_newton", "Hessian is singular, cannot invert. Break at iteration $iteration")
            break
        }

        // g = J^T * residuals 계산
        val gradient = multiplyMatrixVector(jacobianT, residuals)
        Log.d("refine_gauss_newton", "Gradient: ${gradient.joinToString()}")

        // Δx = H^-1 * g 계산
        val delta = multiplyMatrixVector(hessianInverse, gradient)
        Log.d("refine_gauss_newton", "Delta: ${delta.joinToString()}")

        // 위치 업데이트
        currentPosition = Point(
            currentPosition.x + delta[0],
            currentPosition.y + delta[1],
            currentPosition.z + delta[2]
        )

        // 변화량 확인
        val deltaNorm = sqrt(delta[0].pow(2) + delta[1].pow(2) + delta[2].pow(2))
        Log.d("refine_gauss_newton", "Delta norm: $deltaNorm")

        // NaN 체크
        if (currentPosition.x.isNaN() || currentPosition.y.isNaN() || currentPosition.z.isNaN()) {
            Log.e("refine_gauss_newton", "Current position turned NaN at iteration $iteration")
            break
        }

        if (deltaNorm < tolerance) {
            Log.d("refine_gauss_newton", "Converged at iteration $iteration")
            break
        }

        iteration++
    }

    Log.d("refine_gauss_newton", "Final refined position: $currentPosition")
    Log.d("refine_gauss_newton", "===== End refinePositionByGaussNewton =====")

    return currentPosition
}


fun calcBy3Side(pointAndDistanceList: List<PandD>): Point {
    val distances = pointAndDistanceList.map{it.distance}
    val anchorPosition = pointAndDistanceList.map{it.point}
    val x = anchorPosition.map{it.x}
    val y = anchorPosition.map{it.y}
    val z = anchorPosition.map{it.z}
    val d = distances.map{it}

    val A = arrayOf(
        floatArrayOf(2 * (x[1] - x[0]), 2 * (y[1] - y[0])),
        floatArrayOf(2 * (x[2] - x[0]), 2 * (y[2] - y[0]))
    )
    val B = floatArrayOf(
        generateRight(x[1],y[1],d[1]) - generateRight(x[0],y[0],d[0]),
        generateRight(x[2],y[2],d[2]) - generateRight(x[0],y[0],d[0])
    )
    val Ainv = invertMatrix(A)
    val result = multiplyMatrixVector(Ainv!!, B)


    return Point(result[0],0f , result[1]) // y is Height in AR world
}
fun generateRight(x:Float, y:Float, d:Float, z: Float= 0f): Float{
    return x*x + y*y + z*z - d*d
}