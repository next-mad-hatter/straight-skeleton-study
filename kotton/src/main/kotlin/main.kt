package madhat.kotton

import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*

import java.io.*
import java.math.*


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

        ParsedPolygon<BigDecimal>(
        poly.sortedIndices,
        poly.weights,
        poly.indices.mapKeys({ x -> Pair(x.key.first.toBigDecimal(), x.key.second.toBigDecimal()) }),
        poly.coordinates.mapValues({ x -> Pair(x.value.first.toBigDecimal(), x.value.second.toBigDecimal()) })
        )
    }

}
