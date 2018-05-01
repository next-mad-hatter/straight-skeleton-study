package madhat.kotton

import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*
import  madhat.kotton.callers.*

import java.io.*
import java.nio.file.*

fun main(args: Array<String>) {

    val DEBUG = false
    val TIMEOUT: Long = 4

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

        if (DEBUG) {
            println("Vertices & Weights:\n")
            for ((ind, wt) in input.perimeterIndices zip input.weights) {
                println("  $ind : ${input.coordinates[ind]} -- $wt --> ")
            }
        }

        /*
        val triangles = triangulate(input.perimeterIndices.map { x -> input.coordinates[x]!! })
        println("\n  Triangulation:\n")
        for (t in triangles) {
            println("  ${t.points.map{ input.indices[Pair(it.x, it.y)] }}")
        }
        */

        // TODO: remove consecutive collinear edges from input?

        for ((methodName, method) in listOf(
                Pair("Triton", Triton(false)),
                Pair("Campskeleton", CampSkeleton())
        )) {
            try {
                println("\nRunning $methodName\n")
                val result = method.computeSkeleton(input, TIMEOUT, true, true)

                if (DEBUG) {
                    println("Result:\n")
                    for (e in result.edges) {
                        println("  Skeleton edge: ${e.first} -- ${e.second}")
                    }
                }

                try {
                    checkTree(input, result.edges)
                    println("\nTree check passed")
                } catch (err: Exception) {
                    println("\n! Tree check failed: $err")
                }

                if (DEBUG) {
                    if (result.trace != null) {
                        println("\nTrace:\n")
                        for ((h, es) in result.trace.events) {
                            println("  At height $h:")
                            for (e in es.edgesSet!!) {
                                println("    ${e.first.x} ${e.first.y} -- ${e.second.x} ${e.second.y}")
                            }
                        }
                    }
                }

                val completedIndices = result.completedIndices ?: input.indices

                (Paths.get(filename).fileName.toString() + ".$methodName.skel.gz").let {
                    writeText(edgesToText(completedIndices, result.edges), it)
                }

                if (result.svg != null) {
                    (Paths.get(filename).fileName.toString() + ".$methodName.svg.gz").let {
                        writeSVG(result.svg, it)
                    }
                }

                if (result.misc != null) {
                    (Paths.get(filename).fileName.toString() + ".$methodName.stat.gz").let {
                        writeText(result.misc, it)
                    }
                }

            } catch (err: Exception) {
                System.err.println("! ERROR : $err\n")
            }
        }

    }

}
