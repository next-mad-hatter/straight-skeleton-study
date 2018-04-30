package madhat.kotton

import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*
import  madhat.kotton.runners.*

import java.io.*

import kotlin.math.*

import org.twak.camp.*
import org.twak.utils.collections.*
import org.twak.camp.Output.*

import javax.vecmath.Point3d

import org.jfree.graphics2d.svg.*
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import org.twak.camp.debug.DebugDevice
import org.twak.camp.debug.DebugDevice.*
import java.nio.file.*
import javax.vecmath.*

fun main(args: Array<String>) {

    for (filename in args) {
        println("\nReading $filename\n")

        val file = File(filename)
        val poly: ParsedPolygon = try {
            parseFile(file)
        } catch (e: IOException) {
            System.err.println("Error reading file $filename : ${e.localizedMessage}")
            continue
        }

        /*
        println("  Vertices & Weights:\n")
        for ((ind, wt) in poly.sortedIndices zip poly.weights) {
            println("  $ind : ${poly.coordinates[ind]} -- $wt --> ")
        }
        */

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

        // TODO: remove consecutive collinear edges from input?

        println("\n  Running Campskeleton\n")

        val result = CampSkeleton().computeSkeleton(poly, null, true, true)
        println("Result:")
        for (e in result.edges) {
            println("  Skeleton edge: ${e.first} -- ${e.second}")
        }

        for ((h, es) in result.trace!!.events) {
            println("At height $h:")
            for (e in es.edgesSet!!) {
                println("  ${e.first.x} ${e.first.y} -- ${e.second.x} ${e.second.y}")
            }
        }
        val outfilename = Paths.get(filename).fileName.toString() + ".svg.gz"
        writeSVG(result.svg!!, outfilename)

    }

}
