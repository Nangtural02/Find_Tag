package com.example.find_tag

import kotlin.math.pow
import kotlin.math.sqrt

fun Point?.toString():String{
    if(this == null) return "null"
    else return this!!.toString()
}

data class Point(var x: Float = 0f, var y: Float = 0f, var z: Float =0f){
    fun add(other: Point): Point {
        return Point(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    fun sub(other: Point): Point {
        return Point(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    fun mul(realNum: Float): Point {
        return Point(this.x * realNum, this.y * realNum, this.z * realNum)
    }

    fun div(realNum: Float): Point {
        return Point(this.x / realNum, this.y / realNum, this.z / realNum)
    }
    fun getDistance(anotherPoint: Point): Float{
        return sqrt((this.x-anotherPoint.x).pow(2)+(this.y-anotherPoint.y).pow(2)+(this.z-anotherPoint.z).pow(2))
    }
    fun getMiddlePoint(anotherPoint: Point): Point{
        return Point((this.x+anotherPoint.x)/2, (this.y+anotherPoint.y)/2)
    }

    fun move(dx:Float = 0f, dy:Float = 0f, dz: Float = 0f): Point{
        return Point(x+dx,y+dy,z+dz)
    }
    override fun toString():String{
        return "(${String.format("%.2f",x)},${String.format("%.2f",y)},${String.format("%.2f",z)})"
    }
    fun changeYandZ():Point{
        return Point(x,z,y)
    }

}

fun getVarianceOfPointList(pointList: List<Point>):Float {
    if(pointList.isEmpty()) return 0f
    return maxOfPointList(pointList).getDistance(minOfPointList(pointList))
}

fun maxOfPointList(pointList: List<Point>): Point {
    if(pointList.isEmpty()) return Point()
    return pointList.reduce { maxPoint, point ->
        Point(
            x = maxOf(maxPoint.x, point.x),
            y = maxOf(maxPoint.y, point.y),
            z = maxOf(maxPoint.z, point.z)
        )
    }
}

fun minOfPointList(pointList: List<Point>): Point {
    if(pointList.isEmpty()) return Point()
    return pointList.reduce { minPoint, point ->
        Point(
            x = minOf(minPoint.x, point.x),
            y = minOf(minPoint.y, point.y),
            z = minOf(minPoint.z, point.z)
        )
    }
}

