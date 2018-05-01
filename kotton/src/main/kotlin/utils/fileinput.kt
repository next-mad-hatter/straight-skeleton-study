package madhat.kotton.utils

import org.tukaani.xz.*
import java.io.*
import org.jgrapht.*
import org.jgrapht.graph.*
import javax.vecmath.*
//import org.jgrapht.traverse.*


class ParseException(override var message:String, override var cause: Throwable): Exception(message, cause)


/**
 * Given input polygon, this is the data we want to collect in very first step.
 * perimeterIndices should correspond to ordering along edges (not closed, i.e. unique),
 * while weights should apply to edges corresponding to said ordering
 * (starting with edge {perimeterIndices[0], perimeterIndices[1]}).
 */
data class ParsedPolygon(
        val perimeterIndices: List<Int>,
        val weights: List<Double>,
        val indices: Map<Point2d, Int>,
        val coordinates: Map<Int, Point2d>
)


/**
 * Attempts to parse given input.
 */
fun parseFile(file: File): ParsedPolygon {

    val lines = try {
        val a = XZInputStream(FileInputStream(file)).reader()
        BufferedReader(a).readLines()
    } catch (e: XZFormatException) {
        FileInputStream(file).bufferedReader().readLines()
    }

    return try {
        parseEdgesFormat(lines)
    } catch (e: IOException) {
        if (e.message == "Parse error")
            try {
                parseVertexFormat(lines)
            } catch (e: IOException) {
                throw ParseException("Failed parsing file", e)
            }
        else
            throw e
    }
}


/**
 * Parses the one-edge-per-line format.
 */
fun parseEdgesFormat(lines: List<String>): ParsedPolygon {

    var graph: SimpleWeightedGraph<Int, Set<Int>> = SimpleWeightedGraph({ x, y -> setOf(x, y) })
    var coors: HashMap<Int, Point2d> = HashMap()
    var inds: HashMap<Point2d, Int> = HashMap()

    for ((lineno, line) in (1..lines.count()) zip lines) {
        val tokens = line.trim().split("-", ";").map{ it.trim() }
        if (tokens.count() != 7) throw IOException("Parse error")

        val (v1, v2, wt) =
        try {
            val v1 = Triple(tokens[0].toInt(), tokens[1].toDouble(), tokens[2].toDouble())
            val v2 = Triple(tokens[3].toInt(), tokens[4].toDouble(), tokens[5].toDouble())
            val wt = tokens[6].toDouble()
            Triple(v1, v2, wt)
        } catch (e: Exception) {
            throw IOException("While parsing file: ${e.message} in line $lineno")
        }

        for (vertex in listOf(v1, v2)) {
            val (ind, x, y) = vertex
            if (coors.containsKey(ind)) {
                if (coors[ind] != Point2d(x, y))
                    throw IOException("Conflicting definition for point $ind in line $lineno")
            }
            else
                coors[ind] = Point2d(x, y)
            if (inds.containsKey(Point2d(x, y))) {
                if (inds[Point2d(x, y)] != ind)
                    throw IOException("Multiple points with coordinates $x , $y")
            }
            else
                inds[Point2d(x, y)] = ind
            if (!graph.containsVertex(ind))
                graph.addVertex(ind)
        }
        val e = graph.addEdge(v1.first, v2.first)
        graph.setEdgeWeight(e, wt)

    }

    if (coors.keys.count() < 3)
        throw IOException("Polygon contains less than three vertices")

    if (!GraphTests.isSimple(graph) || !GraphTests.isConnected(graph))
        throw IOException("Given polygon does not yield a simple connected graph")

    for (v in graph.vertexSet()) {
        if (graph.degreeOf(v) != 2)
            throw IOException("Vertex $v has degree ${graph.degreeOf(v)}")
    }

    val first = coors.keys.min()!!
    var prev = first
    var current = Graphs.neighborListOf(graph, prev).min()!!
    var sortedInds: MutableList<Int> = ArrayList()
    var weights: MutableList<Double> = ArrayList()
    do {
        sortedInds.add(prev)
        weights.add(graph.getEdgeWeight(setOf(prev, current)))
        var next = Graphs.neighborListOf(graph, current)
        next.remove(prev)
        prev = current
        current = next[0]
    } while (prev != first)

    return ParsedPolygon(sortedInds, weights, inds, coors)

}


/**
 * Parses one vertex per line format.
 */
fun parseVertexFormat(rawLines: List<String>): ParsedPolygon {
    var coors: HashMap<Int, Point2d> = HashMap()
    var inds: HashMap<Point2d, Int> = HashMap()

    // Our big dataset contains lots of repeating points.
    val lines = rawLines.distinct()

    for ((lineno, line) in (1..lines.count()) zip lines) {
        val xy = try {
            line.replace(Regex("#.*(\n|$)"), "")
                    .trim()
                    .split(Regex("\\s+"))
                    .map { it.trim().toDouble() }
        } catch (e: Exception) {
            throw IOException("Error encountered in line $lineno : $e")
        }
        if (xy.count() != 2)
            throw IOException("Bad input line $lineno")
        val x = xy[0]
        val y = xy[1]
        if (inds.containsKey(Point2d(x, y))) {
            // we don't care for closed polygon input
            if (lineno == lines.count()) continue
            throw IOException("Multiple points with coordinates $x , $y")
        }
        inds[Point2d(x, y)] = lineno
        coors[lineno] = Point2d(x, y)

    }
    if (coors.keys.count() < 3)
        throw IOException("Polygon contains less than three vertices")

    val its = coors.keys.toList()
    return ParsedPolygon(
            its,
            its.map { 1.0 },
            inds,
            coors
    )
}
