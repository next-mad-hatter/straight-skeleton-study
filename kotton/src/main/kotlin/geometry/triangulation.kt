package madhat.kotton.geometry

import org.poly2tri.*
import org.poly2tri.geometry.polygon.*
import org.poly2tri.triangulation.delaunay.*
import javax.vecmath.*


/**
 * Given list of vertices
 * Triangulates a polygon
 */
fun triangulate(edges: List<Point2d>): List<DelaunayTriangle> {
    // We must not have points with same coordinates here.
    // Also, only simple polygons are supported.
    var polygon = Polygon(edges.map { PolygonPoint(it.x, it.y) })
    Poly2Tri.triangulate(polygon)
    return polygon.triangles
}
