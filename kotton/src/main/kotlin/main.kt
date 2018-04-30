package madhat.kotton

import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*
import  madhat.kotton.callers.*

import java.io.*
import java.nio.file.*

fun main(args: Array<String>) {

    for (filename in args) {
        println("\nReading $filename\n")

        val input: ParsedPolygon = try {
            File(filename).let {
                parseFile(it)
            }
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

            Paths.get(filename).fileName.toString() + ".skel.gz".let {
                writeText(edgesToText(input, result.edges), it)
            }

            Paths.get(filename).fileName.toString() + ".svg.gz".let {
                writeSVG(result.svg!!, it)
            }

            if (result.misc != null) {
                Paths.get(filename).fileName.toString() + ".stat.gz".let {
                    writeText(result.misc, it)
                }
            }

        } catch (err: Exception) {
            System.err.println(err)
            System.exit(1)
        }

    }

}
