package com.example.find_tag

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt


fun refinePositionByLevenbergMarquardt(
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

    // 최대 반복 횟수 및 기타 파라미터
    val maxIterations = 100
    val tolerance = 1e-6f

    // 레벤버그-마콰르트용 파라미터
    var lambda = 1e-2f             // 초기 감쇠 계수
    val lambdaUpFactor = 10f       // 잔차가 줄지 않으면 lambda를 키우는 배율
    val lambdaDownFactor = 0.1f    // 잔차가 줄면 lambda를 줄이는 배율

    val epsilon = 1e-8f

    var iteration = 0
    while (iteration < maxIterations) {
        val numAnchors = distances.size
        val jacobian = Array(numAnchors) { FloatArray(3) } // 자코비안 행렬
        val residuals = FloatArray(numAnchors)

        // 자코비안, residual 계산
        for (i in distances.indices) {
            val dx = currentPosition.x - anchorPosition[i].x
            val dy = currentPosition.y - anchorPosition[i].y
            val dz = currentPosition.z - anchorPosition[i].z

            val distPred = sqrt(dx * dx + dy * dy + dz * dz) + epsilon
            residuals[i] = distances[i] - distPred

            val invDist = 1f / distPred
            jacobian[i][0] = -dx * invDist
            jacobian[i][1] = -dy * invDist
            jacobian[i][2] = -dz * invDist
        }

        // 현재 residual norm (오차) 계산
        val residualNorm = residuals.map { it * it }.sum().let { sqrt(it) }

        // 자코비안 전치 계산
        val jT = transposeMatrix(jacobian)
        // Gauss-Newton Hessian 근사: H = J^T * J
        val hessian = multiplyMatrixMatrix(jT, jacobian)
        // gradient = J^T * residual
        val gradient = multiplyMatrixVector(jT, residuals)

        // 레벤버그-마콰르트: H + lambda * 대각선(I or diag(H))
        // 간단히 "H + lambda * diag(H)" 대신 "H + lambda * I"로도 많이 씀
        val diag = diagOf(hessian)
        val lmHessian = addMatrices(
            hessian,
            diagToMatrix(diag.map { it * lambda })  // H + lambda * diag(H)
        )

        // H^-1
        val invHessian = invertMatrix(lmHessian)
            ?: run {
                // singular하면 break
                Log.d("LM","LM: Hessian is singular at iteration $iteration")
                return Point(-66f,-66f,-66f)
            }

        val delta = multiplyMatrixVector(invHessian, gradient)

        // 업데이트 후보
        val candidate = Point(
            currentPosition.x + delta[0],
            currentPosition.y + delta[1],
            currentPosition.z + delta[2]
        )

        // candidate의 residual norm 계산
        val newResiduals = FloatArray(numAnchors)
        for (i in distances.indices) {
            val dx = candidate.x - anchorPosition[i].x
            val dy = candidate.y - anchorPosition[i].y
            val dz = candidate.z - anchorPosition[i].z

            val distPred = sqrt(dx * dx + dy * dy + dz * dz) + epsilon
            newResiduals[i] = distances[i] - distPred
        }
        val newResidualNorm = newResiduals.map { it * it }.sum().let { sqrt(it) }

        // residual 비교에 따라 lambda 조정
        if (newResidualNorm < residualNorm) {
            // 잔차가 줄어들었으면 -> accept move, lambda 줄이기
            currentPosition = candidate
            lambda *= lambdaDownFactor

            // 수렴 체크
            if (delta.norm() < tolerance) {
                Log.d("LM","LM: Converged at iteration $iteration")
                break
            }
        } else {
            // 잔차가 좋아지지 않으면 -> move reject, lambda 키우고 다음 iteration
            lambda *= lambdaUpFactor
        }

        iteration++
    }

    Log.d("LM","LM: Final position at iteration $iteration: $currentPosition")
    return currentPosition
}

// ========== 아래는 행렬 연산 유틸 함수들 예시 ==========

// diag 를 받아서 대각선 요소만 곱해서 행렬로 만들어 준다 (LM 시 사용)
fun diagToMatrix(diagVals: List<Float>): Array<FloatArray> {
    val n = diagVals.size
    val m = Array(n) { FloatArray(n) { 0f } }
    for (i in 0 until n) {
        m[i][i] = diagVals[i]
    }
    return m
}

fun diagOf(matrix: Array<FloatArray>): List<Float> {
    val size = minOf(matrix.size, matrix[0].size)
    return List(size) { i -> matrix[i][i] }
}

// 벡터 norm 계산을 위한 확장
fun FloatArray.norm(): Float {
    var sum = 0f
    for (v in this) {
        sum += v * v
    }
    return sqrt(sum)
}

fun refine2DPositionLevenbergMarquardt(
    distances: List<Float>,
    anchors2D: List<Point>,
    initPos: Point? = null
): Point {
    // 초기 위치 설정 (z는 0이든 무시)
    val initialX = initPos?.x ?: anchors2D.map { it.x }.average().toFloat()
    val initialY = initPos?.y ?: anchors2D.map { it.y }.average().toFloat()

    var currentX = initialX
    var currentY = initialY

    val maxIterations = 100
    val tolerance = 1e-6f

    // LM 파라미터
    var lambda = 1e-2f
    val lambdaUp = 10f
    val lambdaDown = 0.1f

    // 현재 (x, y)에서 residual Norm 계산
    fun residualNorm(x: Float, y: Float): Float {
        var sumSq = 0f
        for (i in distances.indices) {
            val dx = x - anchors2D[i].x
            val dy = y - anchors2D[i].y
            val predDist = sqrt(dx*dx + dy*dy) + 1e-8f
            val r = distances[i] - predDist
            sumSq += (r*r)
        }
        return sqrt(sumSq)
    }

    // Hessian(2x2) + Gradient(2) 계산
    fun computeHG(x: Float, y: Float): Pair<Array<FloatArray>, FloatArray> {
        // 2x2 행렬, 길이2 벡터
        val H = Array(2){ FloatArray(2){0f} }
        val g = FloatArray(2){0f}

        for (i in distances.indices) {
            val dx = x - anchors2D[i].x
            val dy = y - anchors2D[i].y
            val dist = sqrt(dx*dx + dy*dy) + 1e-8f

            val rx = -dx/dist
            val ry = -dy/dist
            val rVal = distances[i] - dist

            // gradient
            g[0] += rx * rVal
            g[1] += ry * rVal

            // Hessian (Gauss-Newton 근사 = sum(Jᵢᵀ Jᵢ))
            H[0][0] += rx*rx
            H[0][1] += rx*ry
            H[1][0] += ry*rx
            H[1][1] += ry*ry
        }
        return H to g
    }

    // 2x2 역행렬 (이미 유틸 함수가 있다면 그걸 사용)
    fun invert2x2(m: Array<FloatArray>): Array<FloatArray>? {
        val a = m[0][0]; val b = m[0][1]
        val c = m[1][0]; val d = m[1][1]
        val det = a*d - b*c
        if (abs(det) < 1e-12f) return null
        val invDet = 1f/det

        return arrayOf(
            floatArrayOf(d*invDet, -b*invDet),
            floatArrayOf(-c*invDet, a*invDet)
        )
    }

    var iteration = 0
    while (iteration < maxIterations) {
        val oldRes = residualNorm(currentX, currentY)
        // Hessian, Gradient 계산
        val (H, g) = computeHG(currentX, currentY)

        // damping: H' = H + lambda * diag(H)
        val dampedH = Array(2){ FloatArray(2){0f} }
        dampedH[0][0] = H[0][0] + lambda * H[0][0]
        dampedH[0][1] = H[0][1]
        dampedH[1][0] = H[1][0]
        dampedH[1][1] = H[1][1] + lambda * H[1][1]

        val invH = invert2x2(dampedH)
        if (invH == null) {
            Log.e("LM2D", "Hessian singular at iteration $iteration")
            break
        }

        // delta = invH * g
        val dx = invH[0][0]*g[0] + invH[0][1]*g[1]
        val dy = invH[1][0]*g[0] + invH[1][1]*g[1]

        val candX = currentX + dx
        val candY = currentY + dy
        val newRes = residualNorm(candX, candY)

        if (newRes < oldRes) {
            // accept
            currentX = candX
            currentY = candY
            lambda *= lambdaDown

            val stepSize = sqrt(dx*dx + dy*dy)
            if (stepSize < tolerance) {
                Log.d("LM2D", "Converged at iteration=$iteration stepSize=$stepSize")
                break
            }
        } else {
            // reject
            lambda *= lambdaUp
        }

        iteration++
    }

    Log.d("LM2D", "End iteration=$iteration, pos=($currentX, $currentY, z=0f)")
    // z=0f로 고정
    return Point(currentX, currentY, 0f)
}
