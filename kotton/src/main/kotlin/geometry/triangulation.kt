package madhat.kotton.geometry

import org.poly2tri.*
import org.poly2tri.geometry.polygon.*
import org.poly2tri.triangulation.delaunay.*


/**
 * Given list of vertices
 * Triangulates a polygon
 */
fun triangulate(edges: List<Pair<Double, Double>>): List<DelaunayTriangle> {
    // We must not have points with same coordinates here.
    // Also, only simple polygons are supported.
    var polygon = Polygon(edges.map { PolygonPoint(it.first, it.second) })
    Poly2Tri.triangulate(polygon)
    return polygon.triangles
}
