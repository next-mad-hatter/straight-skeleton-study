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

import at.tugraz.igi.events.*
import at.tugraz.igi.events.Event

import org.apache.commons.lang3.tuple.Pair as APair


class AlgorithmException(override var message:String, override var cause: Throwable): Exception(message, cause)


/**
 * The "caller" classes below will run given skeleton computing algorithms,
 * returning results in common format described below.
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
 * A sequence of time/height of encountered events and corresponding computation states
 */
typealias SkeletonTrace = List<Pair<Double, SkeletonSnapshot>>


/**
 * A state of skeleton at any time of computation.
 */
data class SkeletonSnapshot(
        val edges: Set<TraceEdge>,
        val location: Point2d? = null,
        val eventType: EventType? = null
)


/**
 * While campskeleton won't yield unique edge ids or edge types, triton
 * should.  Both should keep edge orientation between snapshots.
 */
data class TraceEdge(
        val id: List<Int>?,
        val type: EdgeType?,
        val start: Point2d,
        val end: Point2d
)


enum class EventType {
    FLIP, SPLIT, EDGE
}


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

        var trace: MutableList<Pair<Double, SkeletonSnapshot>> = mutableListOf()
        if (rawTrace != null && createTrace) {
            val polyEdges: Set<Pair<Point2d, Point2d>>? = if (rawTrace.firstKey() != null)
                rawTrace[rawTrace.firstKey()]!!.map{
                    Pair(Point2d(it[0][0], it[0][1]),
                         Point2d(it[1][0], it[1][1]))
                }.toHashSet()
            else null
            for ((h, es) in rawTrace) {
                var edgesSet: MutableSet<TraceEdge> = HashSet()
                for (e in es) {
                    val p = Point2d(e[0][0], e[0][1])
                    val q = Point2d(e[1][0], e[1][1])
                    edgesSet.add(TraceEdge(
                            null,
                            if (polyEdges!!.contains(Pair(p, q)) or polyEdges.contains(Pair(q, p))) EdgeType.POLYGON else EdgeType.SKELETON,
                            p, q))
                }
                trace.add(Pair(h, SkeletonSnapshot(edges = edgesSet)))
            }
        }

        if (error != null) return SkeletonResult(
                error,
                null,
                trace,
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
                trace,
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

        var timeline: MutableList<Pair<Double, SkeletonSnapshot>>? =
                if (createTrace) mutableListOf() else null
        if (createTrace) {
            var lastTime = 0.0
            controller.tracer = Consumer<APair<Event?, List<Triangle>>> {
                lastTime += it.left?.collapsingTime ?: 0.0

                // the whole extraction thing would probably fit better as part of triton, but java is just a bit tideous

                val triEdges = it.right.flatMap {
                    it.strokes.map {
                        val (p, q) = if (it.p1.number <= it.p2.number) Pair(it.p1, it.p2) else Pair(it.p2, it.p1)
                        TraceEdge(
                                id = listOf(p.number, q.number),
                                type = EdgeType.TRIANGULATION,
                                start = Point2d(p.currentX, p.currentY),
                                end = Point2d(q.currentX, q.currentY))

                } }.toHashSet()

                val skelEdges = controller.straightSkeleton.lines.map {
                    TraceEdge(
                            id = listOf(it.p1.number, it.p2.number),
                            type = EdgeType.SKELETON,
                            start = Point2d(it.p1.originalX, it.p1.originalY),
                            end = Point2d(it.p2.originalX, it.p2.originalY))
                }.toHashSet()

                val polyEdges = controller.straightSkeleton.polyLines.map {
                    val (p, q) = if (it.p1.number <= it.p2.number) Pair(it.p1, it.p2) else Pair(it.p2, it.p1)
                    TraceEdge(
                            id = listOf(p.number, q.number),
                            type = EdgeType.POLYGON,
                            start = Point2d(p.originalX, p.originalY),
                            end = Point2d(q.originalX, q.originalY))
                }.toHashSet()

                // wavefront and not yet complete skeleton arcs -- see GraphicPanel::paintMovedPoints() in triton
                val auxEdges = controller.polygons.flatMap {
                    it.flatMap {
                        val pt = it
                        // is this equivalent to finding a line where p1.number == p2.number?
                        val l = it.adjacentLines.find { it.p2.equals(pt) }
                        if (l == null) listOf<TraceEdge>() else listOf(
                                TraceEdge(
                                        id = listOf(l.p1.number),
                                        type = EdgeType.SKELETON,
                                        start = Point2d(l.p1.originalX, l.p1.originalY),
                                        end = Point2d(l.p1.currentX, l.p1.currentY)),
                                TraceEdge(
                                        id = listOf(l.p1.number, l.p2.number),
                                        type = EdgeType.WAVEFRONT,
                                        start = Point2d(l.p1.currentX, l.p1.currentY),
                                        end = Point2d(l.p2.currentX, l.p2.currentY))
                        )
                    }
                }.toSet()

                timeline!!.add(Pair(lastTime, SkeletonSnapshot(
                        location = if (it.left != null) Point2d(it.left!!.intersection.currentX, it.left!!.intersection.currentY) else null,
                        eventType = if (it.left != null) {
                           when (it.left) {
                               is FlipEvent -> EventType.FLIP
                               is SplitEvent -> EventType.SPLIT
                               is EdgeEvent -> EventType.EDGE
                               else -> null
                           }
                        } else null,
                        edges = listOf(triEdges, skelEdges, polyEdges, auxEdges).reduce { x,y -> x.union(y) }.toSet())))

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

        if (error != null) return SkeletonResult(
                    error,
                    null,
                    timeline,
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
                timeline,
                svg,
                null,
                completedIndices)
    }

}
