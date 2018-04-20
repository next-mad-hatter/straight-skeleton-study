package madhat.kotton

import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*

import java.io.*

import org.twak.camp.*
import org.twak.utils.collections.*
import org.twak.camp.Output.*

import javax.vecmath.Point3d
import org.twak.utils.collections.Loop
import java.time.chrono.JapaneseEra.values
import java.time.chrono.JapaneseEra.values





fun main(args: Array<String>) {

    for (filename in args) {
        println("\nReading $filename\n")

        val file = File(filename)
        val poly: ParsedPolygon<Double> = try {
            parseFile(file)
        } catch (e: IOException) {
            System.err.println("Error reading file $filename : ${e.localizedMessage}")
            continue
        }

        println("  Vertices & Weights:\n")
        for ((ind, wt) in poly.sortedIndices zip poly.weights) {
            println("  $ind : ${poly.coordinates[ind]} -- $wt --> ")
        }

        val triangles = triangulate(poly.sortedIndices.map { x -> poly.coordinates[x]!! })
        println("\n  Triangulation:\n")
        for (t in triangles) {
            println("  ${t.points.map{ poly.indices[Pair(it.x, it.y)] }}")
        }

        /*
        ParsedPolygon<BigDecimal>(
        poly.sortedIndices,
        poly.weights,
        poly.indices.mapKeys({ x -> Pair(x.key.first.toBigDecimal(), x.key.second.toBigDecimal()) }),
        poly.coordinates.mapValues({ x -> Pair(x.value.first.toBigDecimal(), x.value.second.toBigDecimal()) })
        */

        // TODO: remove consequtive collinear edges for campskeleton

        println("\n  Campskeleton:\n")

        var loop: Loop<Edge> = Loop()
        val corners = poly.sortedIndices.map {
            Corner( poly.coordinates[it]!!.first, poly.coordinates[it]!!.second)
        }

        for (ind in 0 until corners.count()) {
            val p = corners[ind]
            val q = corners[(ind + 1) % poly.sortedIndices.count()]
            val e = Edge(p, q)
            loop.append(e)
            e.machine = Machine(Math.PI/4 * poly.weights[ind]) // TODO: set weight properly
        }
        val skeleton = Skeleton(loop.singleton(), true)
        skeleton.skeleton()

        for (face in skeleton.output.faces.values) {
            println("  face:")
            for (lp3 in face.points)
                for (pt in lp3)
                    println("  $pt")
        }
    }

}
