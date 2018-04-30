package madhat.kotton

import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*
import  madhat.kotton.callers.*

import java.io.*
import java.nio.file.*

fun main(args: Array<String>) {

    for (filename in args) {
        println("\nReading $filename\n")

        val file = File(filename)
        val input: ParsedPolygon = try {
            parseFile(file)
        } catch (e: IOException) {
            System.err.println("Error reading file $filename : ${e.localizedMessage}")
            continue
        }

        /*
        println("  Vertices & Weights:\n")
        for ((ind, wt) in input.sortedIndices zip input.weights) {
            println("  $ind : ${input.coordinates[ind]} -- $wt --> ")
        }
        */

        /*
        val triangles = triangulate(input.sortedIndices.map { x -> input.coordinates[x]!! })
        println("\n  Triangulation:\n")
        for (t in triangles) {
            println("  ${t.points.map{ input.indices[Pair(it.x, it.y)] }}")
        }
        */

        // TODO: remove consecutive collinear edges from input?

        try {
            println("Running Campskeleton\n")
            val result = CampSkeleton().computeSkeleton(input, null, true, true)

            println("Result:")
            for (e in result.edges) {
                println("  Skeleton edge: ${e.first} -- ${e.second}")
            }

            try {
                checkTree(input, result.edges)
            } catch (err: Exception) {
                System.err.println(err)
                System.exit(1)
            }

            for ((h, es) in result.trace!!.events) {
                println("At height $h:")
                for (e in es.edgesSet!!) {
                    println("  ${e.first.x} ${e.first.y} -- ${e.second.x} ${e.second.y}")
                }
            }

            val skelname = Paths.get(filename).fileName.toString() + ".skel.gz"
            writeText(edgesToText(input, result.edges), skelname)

            val svgname = Paths.get(filename).fileName.toString() + ".svg.gz"
            writeSVG(result.svg!!, svgname)

            if (result.misc != null) {
                val miscname = Paths.get(filename).fileName.toString() + ".stat.gz"
                writeText(result.misc, miscname)
            }

        } catch (err: Exception) {
            System.err.println(err)
            System.exit(1)
        }

    }

}
