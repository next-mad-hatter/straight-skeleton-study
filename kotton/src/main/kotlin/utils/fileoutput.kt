package madhat.kotton.utils

import javax.vecmath.*
import org.jfree.graphics2d.svg.*
import java.io.*
import org.tukaani.xz.*
import java.util.zip.GZIPOutputStream


fun edgesToText(input: ParsedPolygon,
                edges: Set<Pair<Point2d, Point2d>>,
                oldFormat: Boolean = false): String {
    if (input.coordinates.keys.isEmpty()) return ""

    var res = ""
    var maxInd = input.coordinates.keys.max()!!
    var newIndices = input.indices.toMutableMap()
    for (e in edges) {
        val p = e.first
        val q = e.second
        for (pt in listOf(p, q)) {
            if (newIndices[pt] == null) {
                maxInd = maxInd.inc()
                newIndices[pt] = maxInd
            }
        }
        res += if (oldFormat) {
            "${newIndices[p]};${p.x};${p.y}" +
                    "-" +
                    "${newIndices[q]};${q.x};${q.y}" +
                    "-1\n"
        } else {
            "${newIndices[p]};${p.x};${p.y}" +
                    " -- " +
                    "${newIndices[q]};${q.x};${q.y}" +
                    "\n"
        }
    }

    return res
}


fun writeText(payload: String, filename: String) {
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
    File(filename).outputStream().use {
        it.writer().use {
            it.write(payload)
            it.flush()
            it.close()
        }
    }

}


fun writeSVG(svg: SVGGraphics2D, filename: String) {
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

