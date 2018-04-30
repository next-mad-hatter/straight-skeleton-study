package  madhat.kotton.geometry

import madhat.kotton.utils.*
import javax.vecmath.*
import org.jgrapht.*
import org.jgrapht.graph.*

fun checkTree(input: ParsedPolygon,
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
            throw Exception("Bad skeleton: a vertex ($i) is not a leaf (has degree ${graph.degreeOf(v)})")

    if(!GraphTests.isTree(graph))
        throw Exception("Bad skeleton: not a tree")

}
