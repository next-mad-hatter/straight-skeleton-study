package madhat.kotton

import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*

import java.io.*

import kotlin.math.*

import org.twak.camp.*
import org.twak.utils.collections.*
import org.twak.camp.Output.*

import javax.vecmath.Point3d

import org.jfree.graphics2d.svg.*
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import java.nio.file.*

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

        /*
        val triangles = triangulate(poly.sortedIndices.map { x -> poly.coordinates[x]!! })
        println("\n  Triangulation:\n")
        for (t in triangles) {
            println("  ${t.points.map{ poly.indices[Pair(it.x, it.y)] }}")
        }
        */

        /*
        ParsedPolygon<BigDecimal>(
        poly.sortedIndices,
        poly.weights,
        poly.indices.mapKeys({ x -> Pair(x.key.first.toBigDecimal(), x.key.second.toBigDecimal()) }),
        poly.coordinates.mapValues({ x -> Pair(x.value.first.toBigDecimal(), x.value.second.toBigDecimal()) })
        */

        // TODO: remove consequtive collinear edges for campskeleton?

        // For campskeleton events: liveEdges + output edges ?  Ids from objects ?

        println("\n  Campskeleton:\n")

        var loop: Loop<Edge> = Loop()
        val corners = poly.sortedIndices.map {
            Corner( poly.coordinates[it]!!.first, poly.coordinates[it]!!.second)
        }

        for (ind in 0 until corners.count()) {
            val p = corners[ind]
            val q = corners[(ind + 1) % corners.count()]
            val e = Edge(p, q)
            loop.append(e)
            e.machine = Machine(Math.PI/4 * poly.weights[ind]) // TODO: set weight properly
        }
        val skeleton = Skeleton(loop.singleton(), true)
        skeleton.skeleton()

        /*
        for (face in skeleton.output.faces.values) {
            println("  face:")
            for (lp3 in face.points)
                for (pt in lp3)
                    println("  $pt")
        }
        */

        if (skeleton.output.edges.map.isEmpty()) return

        val edges = skeleton.output.edges.map.values
        val points = edges.flatMap{ listOf(it.start, it.end) }
        val scaler = IntScaler(points)
        var svg = SVGGraphics2D(scaler.maxX!!, scaler.maxY!!)

        for (edge in skeleton.output.edges.map.values) {
            val p = scaler[edge.start]!!
            val q = scaler[edge.end]!!
            println("  $edge : $p $q")
            svg.drawLine(p.first, p.second, q.first, q.second)
        }
        // SVGUtils.writeToSVG(File("test.svg.gz"), svg.getSVGElement(), true)
        val outfilename = Paths.get(filename).fileName.toString() + ".svg.xz"
        File(outfilename).outputStream().use {
            XZOutputStream(it, LZMA2Options()).use {
                it.writer().use {
                    it.write(svg.svgDocument)
                    it.flush()
                    it.close()
                }
            }
        }

    }

}
