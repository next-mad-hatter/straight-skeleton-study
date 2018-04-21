package madhat.kotton.geometry

import javax.vecmath.Point3d

import kotlin.math.*

/**
 * Given a list of floating-point based points,
 * we'll want to scale them such that minimum distance
 * between two points is not less than given value;
 * we'll also want to transform coordinates to non-negative Ints.
 */
class IntScaler {

    private var map: MutableMap<Pair<Double, Double>, Pair<Int, Int>> = HashMap()
    var maxX: Int? = null
    var maxY: Int? = null

    constructor(points: List<Point3d>, minSquaredDist: Double = 5.0) {

        if (points.count() < 2) throw Exception("Not enough points to scale")

        var minDist: Double? = null
        for (p in points) {
            for (q in points) {
                p.z = 0.0
                q.z = 0.0
                val d = p.distanceSquared(q)
                if ((minDist == null || d < minDist) && d > 0) minDist = d
            }
        }
        if (minDist == null) throw Exception("Cannot scale data with no distinct points")
        val scale = max(1.0, minSquaredDist / minDist)

        val xs: List<Double> = points.map { it.x }
        val ys: List<Double> = points.map { it.y }
        val minX = xs.min()!!
        val minY = ys.min()!!

        for (pt in xs zip ys) {
            map[pt] = Pair(((pt.first - minX) * scale).toInt(), ((pt.second - minY) * scale).toInt())
        }

        val xis = map.values.map { it.first }
        val yis = map.values.map { it.second }
        maxX = xis.max()
        maxY = yis.max()
    }

    operator fun get(p: Point3d): Pair<Int, Int>? {
        return map[Pair(p.x, p.y)]
    }

    operator fun get(p: Pair<Double, Double>): Pair<Int, Int>? {
        return map[p]
    }

}