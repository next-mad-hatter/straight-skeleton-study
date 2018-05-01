package madhat.kotton.callers

import madhat.kotton.utils.*

import at.tugraz.igi.util.*
import at.tugraz.igi.main.*
import at.tugraz.igi.ui.*
import at.tugraz.igi.util.Point

import org.jfree.graphics2d.svg.*
import java.util.*
import java.util.concurrent.*
import javax.vecmath.*
import org.twak.camp.*
import org.twak.utils.collections.*
import kotlin.math.*
import java.awt.*
import java.awt.event.*
import java.util.function.*


class AlgorithmException(override var message:String, override var cause: Throwable): Exception(message, cause)


/**
 * The "caller" classes below will run given skeleton computing algorithms,
 * returning results in common format described above.
 */
interface SkeletonComputation {
    fun computeSkeleton(input: ParsedPolygon,
                        timeout: Long?,
                        createTrace: Boolean = false,
                        createSVG: Boolean = false
    ): SkeletonResult
}


/**
 * Final skeleton computation result.
 */
data class SkeletonResult(
        val error: Exception?, // we might want to return trace even if computation failed
        val edges: Set<Pair<Point2d, Point2d>>?,
        val trace: SkeletonTrace?,
        val svg: SVGGraphics2D?, // triton already builds svg, hence inclusion here
        val misc: String?, // for e.g. event statistics
        val completedIndices: Map<Point2d, Int>? // in case we want to give skeleton nodes indices
)


/**
 * A computation trace shall map time/height of encountered events
 * to corresponding computation state.
 */
data class SkeletonTrace(
        val timeline: SortedMap<Double, SkeletonSnapshot>
)


/**
 * A state of skeleton at any time of computation.
 *
 * If we won't carry more data here (event locations/types?),
 * we should consider removing it.
 */
data class SkeletonSnapshot(
        val edges: Set<TraceEdge>
)


/**
 * While campskeleton won't yield unique edge ids or edge types, triton
 * should.  Both should keep edge orientation between snapshots.
 */
data class TraceEdge(
        val id: Int?,
        val type: EdgeType?,
        val start: Point2d,
        val end: Point2d
)


enum class EdgeType {
    POLYGON, SKELETON, WAVEFRONT, TRIANGULATION
}


/**
 * In case we have, after a computation, added edges with
 * vertices (i.e. skeleton nodes) we have not yet assigned numbers to,
 * and want to do so in a persistent manner, we can call this and
 * put the result into completedIndices above.
 */
fun indexNewVertices(
        indices: Map<Point2d, Int>,
        edges: Set<Pair<Point2d, Point2d>>
): Map<Point2d, Int> {
    if (indices.isEmpty()) return indices
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
    }
    return newIndices
}


/**
 * Attempts to perform a computation under a timeout constraint.
 * This still requires some cooperation: subjects are expected to
 * perform something along the lines of
 *     if (Thread.currentThread().isInterrupted()) break;
 * from time to time.
 */
class TimeoutComputation <R> (private val call: Callable<R>) {
    fun run(timeout: Long): R {
        val executor = Executors.newSingleThreadExecutor()
        val task = FutureTask<R>(call)
        executor.submit(task)
        executor.shutdown()
        @Suppress("UNCHECKED_CAST")
        try {
            return task.get(timeout, TimeUnit.SECONDS) as R
        } catch (ee: ExecutionException) {
            throw ee.cause ?: ee
        } catch (te: TimeoutException) {
            task.cancel(true)
            throw te
        } finally {
            if (!executor.isTerminated()) {
                executor.shutdownNow()
            }
        }
    }
}


class CampSkeleton : SkeletonComputation {

    override fun computeSkeleton(input: ParsedPolygon,
                                 timeout: Long?,
                                 createTrace: Boolean,
                                 createSVG: Boolean
    ): SkeletonResult {

        val corners = input.perimeterIndices.map {
            Corner(input.coordinates[it]!!.x,
                   input.coordinates[it]!!.y)
        }

        var loop: Loop<Edge> = Loop()
        for (ind in 0 until corners.count()) {
            val p = corners[ind]
            val q = corners[(ind + 1) % corners.count()]
            var e = Edge(p, q)
            e.machine = Machine(atan(input.weights[ind]))
            loop.append(e)
        }

        val skeleton = Skeleton(loop.singleton(), true)

        val (rawTrace, error) = try {
            if (timeout == null)
                Pair(skeleton.skeleton(createTrace), null)
            else
                Pair(TimeoutComputation(Callable({
                    skeleton.skeleton(createTrace)
                })).run(timeout),
                        null)
        } catch (e: Exception) {
            Pair(null, AlgorithmException("Algorithm Error", e.cause ?: e))
        }

        var traceMap: SortedMap<Double, SkeletonSnapshot> = TreeMap()
        if (rawTrace != null && createTrace) {
            for ((h, es) in rawTrace) {
                var edgesSet: MutableSet<TraceEdge> = HashSet()
                for (e in es)
                    edgesSet.add(TraceEdge(
                            null, null,
                            Point2d(e[0][0], e[0][1]),
                            Point2d(e[1][0], e[1][1])))
                traceMap[h] = SkeletonSnapshot(edgesSet)
            }
        }

        if (error != null) return SkeletonResult(
                error,
                null,
                SkeletonTrace(traceMap),
                null,
                null,
                null)

        // For now we could only easily get polygon along with skeleton edges
        // from campskeleton, so we'll subtract polygon edges here.
        val polyEdges = HashSet((0 until input.perimeterIndices.count()).map { HashSet(listOf(
                    input.coordinates[input.perimeterIndices[it]],
                    input.coordinates[input.perimeterIndices[(it + 1) % input.perimeterIndices.count()]]
            )) })
        var skelEdges: MutableSet<Pair<Point2d, Point2d>> = HashSet()
        for (e in skeleton.output.edges.map.values) {
            val edge = Pair(
                    Point2d(e.start.x, e.start.y),
                    Point2d(e.end.x, e.end.y)
            )
            if (!polyEdges.contains(HashSet(listOf(edge.first, edge.second)))) {
                skelEdges.add(edge)
            }
        }
        // Is this any different from the above?
        /*
        for (face in skeleton.output.faces.values) {
            for (l in face.edges) {
                for (e in l) {
                    val edge = Pair(
                            Point2d(e.start.x, e.start.y),
                            Point2d(e.end.x, e.end.y)
                    )
                    if (!polyEdges.contains(HashSet(listOf(edge.first, edge.second)))) {
                        skelEdges.add(edge)
                    }
                }
            }
        }
        */

        val completedIndices = if (createSVG) indexNewVertices(input.indices, skelEdges) else null

        var svg = if (createSVG)
            skeletonToSVG(
                    input.indices.keys,
                    completedIndices!!,
                    skeleton.output.edges.map.values.map { Pair(
                                Point2d(it.start.x, it.start.y),
                                Point2d(it.end.x, it.end.y))})
        else null

        return SkeletonResult(
                null,
                skelEdges,
                SkeletonTrace(traceMap),
                svg,
                null,
                completedIndices)
    }

}


class Triton(
        private val useTritonSVG: Boolean = true,
        private val runGC: Boolean = true
) : SkeletonComputation {

    override fun computeSkeleton(input: ParsedPolygon,
                                 timeout: Long?,
                                 createTrace: Boolean,
                                 createSVG: Boolean
    ): SkeletonResult {
        var controller = Controller()
        var panel = GraphicPanel(controller)
        controller.setView(panel)
        controller.setTable(ConfigurationTable(controller))

        FileHandler.loadPoints(
                (1..input.indices.count()).map {
                    input.perimeterIndices[it-1].let { Point(it,
                            input.coordinates[it]!!.x,
                            input.coordinates[it]!!.y
                        )
                    }
                },
                panel,
                controller)

        // TODO: make timeline keys discrete values,
        //       copy edges from controller
        //       and event data from algo
        var timeline: SortedMap<Double, SkeletonSnapshot>? =
                if (createTrace) TreeMap() else null
        if (createTrace) {
            var lastTime = 0.0
            controller.tracer = Consumer {
                lastTime += it
                System.err.println("Event at $lastTime")
                timeline!![lastTime] = SkeletonSnapshot(HashSet())
            }
        }

        val error = try {
            if (timeout == null)
                controller.runAlgorithmNoSwingWorker()
            else
                TimeoutComputation(Callable({
                    controller.runAlgorithmNoSwingWorker()
                })).run(timeout)
            if (!controller.finished) throw Exception("Algorithm failed to finish")
            null
        } catch (e: Exception) { AlgorithmException("Algorithm Error", e.cause ?: e) }

        // Java's GC seems to struggle with lots of Swing applications being created and destroyed :)
        if (runGC) {
            for (w in Window.getWindows()) {
                w.setVisible(false)
                w.dispatchEvent(WindowEvent(w, WindowEvent.WINDOW_CLOSING))
                w.dispose()
            }
        }

        if (createTrace) System.err.println("Total: ${timeline!!.keys.count()}")
        if (createTrace) System.err.println("At: ${timeline!!.keys.joinToString(", ")}")

        if (error != null) return SkeletonResult(
                    error,
                    null,
                    if (timeline != null) SkeletonTrace(timeline) else null,
                    null,
                    null,
                    null)

        // Triton sometimes yields duplicate edges and loops.
        var skelEdges: MutableSet<Pair<Point2d, Point2d>> = HashSet()
        for (line in controller.straightSkeleton.lines) {
            val p = Point2d(line.p1.originalX, line.p1.originalY)
            val q = Point2d(line.p2.originalX, line.p2.originalY)
            if (p != q) skelEdges.add(
                    if (line.p1.number < line.p2.number)
                        Pair(p, q)
                    else
                        Pair(q, p))
        }

        val completedIndices = if (createSVG)
            indexNewVertices(input.indices, skelEdges)
        else null

        var svg: SVGGraphics2D? = null
        if (createSVG) {
            if (useTritonSVG) {
                svg = SVGGraphics2D(panel.width, panel.height)
                panel.paintSVG(svg)
            } else {
                val polyEdges = (0 until input.perimeterIndices.count()).map { Pair(
                        input.coordinates[input.perimeterIndices[it]]!!,
                        input.coordinates[input.perimeterIndices[(it + 1) % input.perimeterIndices.count()]]!!) }
                svg = skeletonToSVG(
                        input.indices.keys,
                        completedIndices!!,
                        skelEdges.union(polyEdges))
            }
        }

        return SkeletonResult(
                null,
                skelEdges,
                if (timeline != null) SkeletonTrace(timeline) else null,
                svg,
                null,
                completedIndices)
    }

}
