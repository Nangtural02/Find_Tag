package com.example.find_tag


// k-means 알고리즘
fun kMeans(points: List<PandD>, k: Int, maxIterations: Int = 100): List<List<PandD>> {
    if (points.size < k) throw IllegalArgumentException("Number of points must be at least $k")

    val centroids = points.shuffled().take(k).map { it.point }.toMutableList()
    var clusters: List<List<PandD>> = List(k) { emptyList<PandD>() }

    repeat(maxIterations) {
        // 새로운 클러스터 초기화
        val newClusters = List(k) { mutableListOf<PandD>() }

        // 각 점을 가장 가까운 중심으로 할당
        points.forEach { pandD ->
            val nearestCentroidIndex = centroids.indices.minByOrNull { centroids[it].getDistance(pandD.point) }!!
            newClusters[nearestCentroidIndex].add(pandD)
        }

        // 새로운 중심 계산
        val newCentroids = newClusters.mapIndexed { index, cluster ->
            if (cluster.isNotEmpty()) {
                cluster.map { it.point }.reduce { acc, point -> acc.add(point) }.div(cluster.size.toFloat())
            } else {
                centroids[index] // 빈 클러스터인 경우 이전 중심 유지
            }
        }

        // 중심이 변경되지 않으면 반복 종료
        if (newCentroids == centroids) return newClusters

        // 중심 업데이트
        centroids.clear()
        centroids.addAll(newCentroids)

        // 클러스터 업데이트
        clusters = newClusters
    }

    return clusters
}
// 클러스터에서 각 중심과 가장 가까운 PandD 객체 선택
fun selectRepresentativePoints(clusters: List<List<PandD>>, centroids: List<Point>): List<PandD> {
    return clusters.mapIndexed { index, cluster ->
        cluster.minByOrNull { it.point.getDistance(centroids[index]) }!!
    }
}

// 최종 함수: k-means로 3 개의 PandD 선택
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