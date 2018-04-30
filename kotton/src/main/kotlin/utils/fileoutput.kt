package madhat.kotton.utils

import org.jfree.graphics2d.svg.*
import java.io.*
import org.tukaani.xz.*


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

