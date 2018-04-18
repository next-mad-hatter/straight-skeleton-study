package madhat.kotton

import  madhat.kotton.utils.*

import org.poly2tri.*
import org.poly2tri.geometry.polygon.*
//import org.poly2tri.triangulation.delaunay.*

import java.util.*

import java.io.*
import org.tukaani.xz.*


fun main(args: Array<String>) {

    /*
     * We must not have points with same coordinates here.
     * Also, only simple polygons are supported.
     */
    var polygon = Polygon(Arrays.asList(
            PolygonPoint(0.0, 0.0, 0.0),
            PolygonPoint(10.0, 0.0, 1.0),
            PolygonPoint(10.0, 10.0, 2.0),
            PolygonPoint(0.0, 10.0, 3.0)))
    Poly2Tri.triangulate(polygon)
    for (t in polygon.getTriangles()) {
        prn2(t)
    }

    /*
     * XZ File reading
     */
    for (filename in args) {
        val file = File(filename)
        try {
            try {
                val a = XZInputStream(FileInputStream(file)).reader()
                val b = BufferedReader(a)
                b.useLines {
                    it.forEach { println(it) }
                }
            } catch (e: XZFormatException) {
                FileInputStream(file).bufferedReader().useLines {
                    it.forEach { println(it) }
                }
            }
        } catch (e: IOException) {
            System.err.println("Error reading file $filename : ${e.localizedMessage}")
        }
    }

    val sandbox = Sandbox()
    sandbox.run()

}
