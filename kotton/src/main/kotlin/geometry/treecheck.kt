package  madhat.kotton.geometry

import madhat.kotton.utils.*
import javax.vecmath.*
import org.jgrapht.*
import org.jgrapht.graph.*

class TreeCheckException(override var message:String): Exception(message)

/**
 * Checks if given set of edges is a tree (we only care for simple holeless polygons)
 * and if every vertex of the input is a leaf in it (we check only in this one direction).
 *
 * Throws exception describing the error if not.
 */
fun runTreeCheck(input: ParsedPolygon,
                 edges: Set<Pair<Point2d, Point2d>>) {

    var graph: Pseudograph<Point2d, DefaultEdge> = Pseudograph(DefaultEdge::class.java)
    for (e in edges) {
        val p = e.first
        val q = e.second
        for (pt in listOf(p, q)) {
            if (!graph.containsVertex(pt)) graph.addVertex(pt)
        }
        graph.addEdge(p, q)
    }

    for ((i, v) in input.coordinates)
        if (graph.degreeOf(v) != 1)
            throw TreeCheckException("Bad skeleton: a vertex ($i) is not a leaf (has degree ${graph.degreeOf(v)})")

    if(!GraphTests.isTree(graph))
        throw TreeCheckException("Bad skeleton: not a tree")

}
