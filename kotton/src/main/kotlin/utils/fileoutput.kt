package madhat.kotton.utils

import madhat.kotton.callers.*

import javax.vecmath.*
import org.jfree.graphics2d.svg.*
import java.io.*
import org.tukaani.xz.*
import java.util.zip.GZIPOutputStream


/**
 * Rolling our own json generation seems silly but easiest
 * considering kotlinx serialization is still very experimental
 * as of now and things like moshi or gson seem like an overkill.
 */
fun traceToJSON(trace: SkeletonTrace): String {

    fun snapshotToJSON(snapshot: SkeletonSnapshot): String {
        return (if (snapshot.location != null) "\"location\": { \"x\": ${snapshot.location.x}, \"y\": ${snapshot.location.y} },\n" else "") +
               (if (snapshot.eventType != null) "\"event type\": { \"${snapshot.eventType}\" },\n" else "") +
               "\"edges\": [\n" +
               (snapshot.edges.joinToString(",\n") {
                   "{\n" +
                   (if (it.id == null) "" else "  \"id\": ${it.id},\n") +
                   (if (it.type == null) "" else "  \"type\": \"${it.type}\",\n") +
                   "  \"start\": { \"x\": ${it.start.x}, \"y\": ${it.start.y} }\n" +
                   "  \"end\": { \"x\": ${it.end.x}, \"y\": ${it.end.y} }\n" +
                   "}"
               }).prependIndent("  ") + "\n]"
    }

    return "[\n" + trace.map {
        (time, tr) ->
            "{\n  \"time\": $time,\n" +
            "${snapshotToJSON(tr).prependIndent("  ")}\n}"
    }.joinToString(",\n").prependIndent("  ") + "\n]\n"

}


/**
 * This creates new ids for edges if needed.
 */
fun edgesToText(indices: Map<Point2d, Int>,
                edges: Set<Pair<Point2d, Point2d>>,
                createJSON: Boolean = true): String {
    if (indices.isEmpty()) return ""

    var res = ""
    var maxInd = indices.values.max()!!
    var newIndices = indices.toMutableMap()
    for (e in edges) {
        val p = e.first
        val q = e.second
        for (pt in listOf(p, q)) {
            if (newIndices[pt] == null) {
                maxInd = maxInd.inc()
                newIndices[pt] = maxInd
            }
        }
        res += if (createJSON) {
            (if (res == "") "" else ",\n") +
            """ |  {
                |    "start": { "id": ${newIndices[p]}, "x": ${p.x}, "y": ${p.y} },
                |    "end":   { "id": ${newIndices[q]}, "x": ${q.x}, "y": ${q.y} }
                |  }
            """.trimMargin()
        } else {
            (if (res == "") "" else "\n") +
                    "${newIndices[p]};${p.x};${p.y}" +
                    "-" +
                    "${newIndices[q]};${q.x};${q.y}" +
                    "-1"
        }
    }
    res = if (createJSON)
        """ |{
            |  "skeleton edges": [
            |
        """.trimMargin() +
        res.prependIndent("  ") +
        """ |
            |  ]
            |}
        """.trimMargin()
    else
        "$res\n"

    return res
}


fun writeTextToFile(payload: String, filename: String) {
    val file = File(filename)

    if (file.parentFile != null) file.parentFile.mkdirs()
    file.createNewFile()

    if (filename.endsWith(".xz")) {
        File(filename).outputStream().use {
            XZOutputStream(it, LZMA2Options()).use {
                it.writer().use {
                    it.write(payload)
                    it.flush()
                    it.close()
                }
            }
        }
        return
    }
    if (filename.endsWith(".gz")) {
        File(filename).outputStream().use {
            GZIPOutputStream(it, 1024).use {
                it.writer().use {
                    it.write(payload)
                    it.flush()
                    it.close()
                }
            }
        }
        return
    }
    if (filename == "-") System.out.writer().use {
        it.write(payload)
        it.flush()
        return
    }
    File(filename).outputStream().use {
        it.writer().use {
            it.write(payload)
            it.flush()
            it.close()
        }
    }

}


fun writeSVGToFile(svg: SVGGraphics2D, filename: String) {
    if (filename.endsWith(".xz")) {
        File(filename).outputStream().use {
            XZOutputStream(it, LZMA2Options()).use {
                it.writer().use {
                    it.write(svg.svgDocument)
                    it.flush()
                    it.close()
                }
            }
        }
        return
    }
    if (filename == "-") System.out.writer().use {
        it.write(svg.svgDocument)
        it.flush()
    }
    else SVGUtils.writeToSVG(
            File(filename),
            svg.getSVGElement(),
            filename.endsWith(".gz"))
}

