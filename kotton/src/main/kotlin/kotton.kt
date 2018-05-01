package madhat.kotton

import madhat.kotton.geometry.TreeCheckException
import  madhat.kotton.runners.*

import picocli.*
import picocli.CommandLine.*
import me.tongfei.progressbar.*
import java.nio.file.*
import java.io.File
import java.util.concurrent.TimeoutException


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
            description = arrayOf("log file (default: ./kotton.log)"))
    private var logFile: String = "./kotton.log"

    @Option(names = arrayOf("-z", "--compression"),
            description = arrayOf("compress output via given method (GZ|XZ|NONE)"))
    private var compressionMethod: CompressionMethod = CompressionMethod.GZ

    @Parameters(paramLabel = "FILE")
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
                    val err = e.cause ?: e
                    log.print("While running $skeletonMethod on $filename : ")
                    if (!(err is TreeCheckException || err is TimeoutException))
                        err.printStackTrace(log)
                    else
                        log.println(err)
                    log.println()
                    failed += 1
                }
                bar.step()
                bar.setExtraMessage("Ran: ${passed + failed}; passed: $passed, failed: $failed.")
            }
            log.println("Ran: ${passed + failed}; passed: $passed, failed: $failed.")
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
