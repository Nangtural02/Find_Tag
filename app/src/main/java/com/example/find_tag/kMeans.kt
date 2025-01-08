package com.example.find_tag

import com.example.find_tag.ui.theme.PandD

// k-means 알고리즘
fun kMeans(points: List<PandD>, k: Int, maxIterations: Int = 100): List<List<PandD>> {
    if (points.size < k) throw IllegalArgumentException("Number of points must be at least $k")

    val centroids = points.shuffled().take(k).map { it.point }.toMutableList()
    var clusters = List(k) { mutableListOf<PandD>() }

    repeat(maxIterations) {
        clusters = List(k) { mutableListOf<PandD>() }

        points.forEach { pandD ->
            val nearestCentroidIndex = centroids.indices.minByOrNull { centroids[it].getDistance(pandD.point) }!!
            clusters[nearestCentroidIndex].add(pandD)
        }

        val newCentroids = clusters.mapIndexed { index, cluster ->
            if (cluster.isNotEmpty()) {
                cluster.map { it.point }.reduce { acc, point -> acc.add(point) }.div(cluster.size.toFloat())
            } else {
                centroids[index] // 빈 클러스터인 경우 이전 중심 유지
            }
        }

        if (newCentroids == centroids) return@repeat
        centroids.clear()
        centroids.addAll(newCentroids)
    }
    return clusters
}

// 클러스터에서 각 중심과 가장 가까운 PandD 객체 선택
fun selectRepresentativePoints(clusters: List<List<PandD>>, centroids: List<Point>): List<PandD> {
    return clusters.mapIndexed { index, cluster ->
        cluster.minByOrNull { it.point.getDistance(centroids[index]) }!!
    }
}

// 최종 함수: k-means로 세 개의 PandD 선택
fun topThreePandDByKMeans(rangingList: List<PandD>): List<PandD> {
    val clusters = kMeans(rangingList, 3)
    val centroids = clusters.map { cluster ->
        cluster.map { it.point }.reduce { acc, point -> acc.add(point) }.div(cluster.size.toFloat())
    }
    return selectRepresentativePoints(clusters, centroids)
}


// 평균 위치와 평균 거리를 계산하여 PandD 리스트 반환
fun topThreeAveragePandDByKMeans(rangingList: List<PandD>): List<PandD> {
    val clusters = kMeans(rangingList, 3)

    // 각 클러스터에 대해 평균 위치와 평균 거리 계산
    return clusters.map { cluster ->
        val avgPoint = cluster.map { it.point }.reduce { acc, point -> acc.add(point) }.div(cluster.size.toFloat())
        val avgDistance = cluster.map { it.distance }.average().toFloat()
        PandD(avgPoint, avgDistance)
    }
}