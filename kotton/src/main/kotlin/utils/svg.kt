package madhat.kotton.utils

import madhat.kotton.geometry.*

import org.jfree.graphics2d.svg.*
import java.awt.*
import java.awt.font.*
import javax.vecmath.*
import kotlin.math.*


/**
 * Produces an SVG document from given set of polygon vertices,
 * map of all vertices to their labels and a set of edges.
 * Each edge's vertices must be present in given map.
 *
 * TODO: set stroke zoom-appropriately and/or draw faces.
 *       Also: how can we set z-index to bring fonts to the front?
 */
fun skeletonToSVG(polygonVertices: Collection<Point2d>,
            completedIndices: Map<Point2d, Int>,
            edges: Collection<Pair<Point2d, Point2d>>): SVGGraphics2D {
    var rad = 24
    // TODO: scale using only polygon vertices?
    val scaler = CoorsIntScaler(completedIndices.keys.toList(), (rad * 1.6).pow(2.0), rad)
    var svg = SVGGraphics2D(scaler.maxX!!, scaler.maxY!!)
    svg.stroke = BasicStroke(max(
            2.0, max(scaler.maxX!!, scaler.maxY!!).toDouble() / 666.0
    ).toFloat())
    rad = max(rad.toDouble(), max(scaler.maxX!!, scaler.maxY!!).toDouble() / 333.0 ).roundToInt()
    val half = rad / 2
    for (edge in edges) {
        val p = scaler[edge.first]!!
        val q = scaler[edge.second]!!
        if (polygonVertices.contains(edge.first) and polygonVertices.contains(edge.second))
            svg.paint = Color.BLACK
        else
            svg.paint = Color.DARK_GRAY
        svg.drawLine(p.first, p.second, q.first, q.second)
    }
    for ((pt, ind) in completedIndices) {
        val (x, y) = scaler[pt]!!
        svg.paint = Color.WHITE
        svg.fillOval(x - half, y - half, rad, rad)
        if (polygonVertices.contains(pt))
            svg.paint = Color.BLACK
        else
            svg.paint = Color.LIGHT_GRAY
        svg.drawOval(x - half, y - half, rad, rad)

        svg.font = svg.font.deriveFont(half / 3)
        val rect = svg.font.getStringBounds(ind.toString(), FontRenderContext(null, true, true))
        val rx = rect.x.roundToInt()
        val ry = rect.y.roundToInt()
        val w = rect.width.roundToInt()
        val h = rect.height.roundToInt()
        svg.drawString(ind.toString(), x - w / 2 - rx, y - h / 2 - ry)
    }
    return svg
}
