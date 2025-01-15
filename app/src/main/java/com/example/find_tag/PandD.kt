package com.example.find_tag

// point and Distance
data class PandD(val point: Point, val distance: Float)

fun List<PandD>.getPointList():List<Point>{
    return this.map{it.point}
}
fun List<PandD>.getDistanceList():List<Float>{
    return this.map{it.distance}
}