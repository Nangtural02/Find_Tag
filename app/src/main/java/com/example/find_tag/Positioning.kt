package com.example.find_tag

import com.example.find_tag.ui.theme.PandD

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
    val result = multiplyMatrixVector(Ainv, B)


    return Point(result[0],0f , result[1]) // y is Height in AR world
}
fun generateRight(x:Float, y:Float, d:Float, z: Float= 0f): Float{
    return x*x + y*y + z*z - d*d
}