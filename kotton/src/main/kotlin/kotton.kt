package madhat.kotton

import madhat.kotton.callers.AlgorithmException
import madhat.kotton.geometry.TreeCheckException
import  madhat.kotton.utils.*
import  madhat.kotton.geometry.*
import  madhat.kotton.callers.*

import picocli.*
import picocli.CommandLine.*
import me.tongfei.progressbar.*
import java.nio.file.*
import java.io.File
import java.util.concurrent.TimeoutException



enum class SkeletonMethod { TRITON, CAMP_SKELETON }
enum class CompressionMethod { NONE, XZ, GZ }


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
        System.err.println("WARNING: method failed to yield execution trace")
    if (result.trace != null && !result.trace.isEmpty())
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


@Command(name = "Kotton",
         version = arrayOf("0.0.1"),
         description = arrayOf("Computes weighted straight skeletons"))
class Kotton : Runnable {

    @Option(names = arrayOf("-h", "--help"), usageHelp = true,
            description = arrayOf("print this help and exit"))
    private var helpRequested: Boolean = false

    @Option(names = arrayOf("-V", "--version"), versionHelp = true,
            description = arrayOf("print version info and exit"))
    private var versionRequested: Boolean = false

    @Option(names = arrayOf("-m", "--method"),
            description = arrayOf("algorithm to be used (TRITON|CAMP_SKELETON)"))
    private var skeletonMethod: SkeletonMethod = SkeletonMethod.TRITON

    @Option(names = arrayOf("-t", "--timeout"),
            description = arrayOf("timeout in seconds"))
    private var timeout: Long? = null

    @Option(names = arrayOf("-e", "--events-trace"),
            description = arrayOf("create events trace"))
    private var createTrace: Boolean = false

    @Option(names = arrayOf("-s", "--svg"),
            description = arrayOf("save final result to svg"))
    private var createSVG: Boolean = false

    @Option(names = arrayOf("-o", "--out"),
            description = arrayOf("output directory (default: .)"))
    private var outputDirectory: String = "."

    @Option(names = arrayOf("-l", "--l"),
            description = arrayOf("log file (default: ./kotton.method.log)"))
    private var logFile: String? = null

    @Option(names = arrayOf("-z", "--compression"),
            description = arrayOf("compress output via given method (GZ|XZ|NONE)"))
    private var compressionMethod: CompressionMethod = CompressionMethod.GZ

    @Parameters(paramLabel = "FILE|DIR")
    private var filenames: MutableList<String> = mutableListOf()

    override fun run() {
        if (helpRequested) {
            CommandLine(this).usage(System.err)
            System.exit(0)
        }
        if (versionRequested) {
            CommandLine(this).printVersionHelp(System.err)
            System.exit(0)
        }

        filenames = filenames.flatMap {
            if (Files.isDirectory(File(it).toPath()))
                File(it).walkTopDown().filter{ it.isFile }
                        .map{ it.toString() }.toList()
            else
                listOf(it)
        }.toMutableList()

        var bar = ProgressBar("Kotton", filenames.count(), 400)
        var passed = 0
        var failed = 0
        logFile = logFile ?: "./kotton.${skeletonMethod.toString().toLowerCase()}.log"
        System.err.println()
        System.err.println("Logging to $logFile")
        System.err.println("Writing output to $outputDirectory")
        System.err.println()
        bar.start()
        File(logFile).printWriter().use { log ->
            for (filename in filenames) {
                try {
                    runSkeletonMethodOnFile(
                            filename,
                            skeletonMethod,
                            timeout = timeout,
                            outputDirectory = outputDirectory,
                            compressionMethod = compressionMethod,
                            createTrace = createTrace,
                            createSVG = createSVG
                    )
                    passed += 1
                } catch (e: Exception) {
                    when (e) {
                        is ParseException,
                        is TreeCheckException,
                        is AlgorithmException -> {
                            val err = e.cause ?: e
                            log.print("While running $skeletonMethod on $filename : ")
                            if (!(err is TreeCheckException || err is TimeoutException))
                                err.printStackTrace(log)
                            else
                                log.println(err)
                            log.println()
                            log.flush()
                            failed += 1
                        }
                        else -> throw e
                    }
                }
                bar.step()
                bar.setExtraMessage("Ran: ${passed + failed}; passed: $passed, failed: $failed.")
            }
            log.println("Ran: ${passed + failed}; passed: $passed, failed: $failed.")
            log.flush()
        }
        bar.stop()
        System.exit(0)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            CommandLine.run(Kotton(), System.err, *args)
        }
    }
}
