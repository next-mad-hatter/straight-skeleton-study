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

        println("\n  Campskeleton Test:\n")
        // This runs fine

        val c1 = Corner(0.0, 0.0)
	    val c2 = Corner(100.0, -100.0)
        val c3 = Corner(100.0, 0.0)
		val speed1 = Machine(Math.PI/4)
        val speed2 = Machine(Math.PI/3)
		val loop1: Loop<Edge> = Loop()
		val e1 = Edge(c1, c2)
        val e2 = Edge(c2, c3)
        val e3 = Edge(c3, c1)
		loop1.append(e1)
		loop1.append(e2)
		loop1.append(e3)
		e1.machine = speed1
		e2.machine = speed1
		e3.machine = speed2
		val skel = Skeleton( loop1.singleton(), true )
		skel.skeleton()
        for (face in skel.output.faces.values) {
            println("face:")
            for (lp3 in face.points)
                for (pt in lp3)
                    println(pt)
        }


        println("\n  Campskeleton:\n")

        /*
        // FIXME: crashes
        var loop: Loop<Edge> = Loop()
        for (ind in 0 until poly.sortedIndices.count()) {
            val v = poly.sortedIndices[ind]
            val w = poly.sortedIndices[(ind + 1) % poly.sortedIndices.count()]
            val p = Corner(poly.coordinates[v]!!.first, poly.coordinates[v]!!.second)
            val q = Corner(poly.coordinates[w]!!.first, poly.coordinates[w]!!.second)
            val e = Edge(p, q)
            println("Appending $e")
            loop.append(e)
            val machine = Machine(Math.PI/4) // TODO: set weight
            e.machine = machine
        }
        val skeleton = Skeleton(loop.singleton(), true)
        */

        // FIXME: also crashes
        var loop: Loop<Corner> = Loop()
        for (ind in poly.sortedIndices) {
            val c = Corner(poly.coordinates[ind]!!.first, poly.coordinates[ind]!!.second)
            println("Appending $c")
            loop.append(c)
        }
        val skeleton = Skeleton(loop.singleton())

        /*
        skeleton.skeleton()
        println(skeleton.output.edges)
        for (e in skeleton.output.faces.values) {
        }
        */
    }

}
