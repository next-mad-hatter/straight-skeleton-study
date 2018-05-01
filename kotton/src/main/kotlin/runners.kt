package madhat.kotton.runners

import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*
import  madhat.kotton.callers.*

import java.io.*
import java.nio.file.*


enum class SkeletonMethod { TRITON, CAMP_SKELETON }
enum class CompressionMethod { NONE, XZ, GZ }


/**
 * The single runner.
 */
fun runSkeletonMethodOnFile(
        filename: String,
        method: SkeletonMethod,
        timeout: Long? = null,
        outputDirectory: String = ".",
        compressionMethod: CompressionMethod = CompressionMethod.GZ,
        createTrace: Boolean = false,
        createSVG: Boolean = false,
        useTritonSVG: Boolean = false,
        checkTree: Boolean = true
)
{
    val input: ParsedPolygon = parseFile(File(filename))
    // TODO: remove consecutive collinear edges from input?

    val result = when (method) {
        SkeletonMethod.TRITON -> Triton(useTritonSVG)
        SkeletonMethod.CAMP_SKELETON -> CampSkeleton()
    }.computeSkeleton(input, timeout, createTrace, createSVG)

    val completedIndices = result.completedIndices ?: input.indices

    val methodName = method.toString().toLowerCase()
    val ext = when (compressionMethod) {
        CompressionMethod.NONE -> ""
        CompressionMethod.GZ -> ".gz"
        CompressionMethod.XZ -> ".xz"
    }
    File(outputDirectory).let { if (!it.exists()) it.mkdirs() }

    fun out(kind: String): String = Paths.get(
            outputDirectory,
            Paths.get(filename).fileName.toString() + ".$methodName.$kind$ext"
    ).toString()

    if (result.trace == null && createTrace)
        System.err.println("WARNING: method failed to yield trace")
    if (result.trace != null && !result.trace.timeline.isEmpty())
        out("trace.json").let { writeTextToFile(traceToJSON(result.trace), it) }

    if (result.error != null) throw result.error

    out("skel.json").let {
        writeTextToFile(edgesToText(completedIndices, result.edges!!), it)
    }

    if (result.svg == null && createSVG)
        System.err.println("WARNING: method failed to yield SVG object")
    if (result.svg != null)
        out("svg").let { writeSVGToFile(result.svg, it) }

    if (result.misc != null)
        out("misc").let { writeTextToFile(result.misc, it) }

    if(checkTree) runTreeCheck(input, result.edges!!)

}
