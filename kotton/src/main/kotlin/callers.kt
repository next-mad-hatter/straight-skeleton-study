package madhat.kotton.callers

import madhat.kotton.utils.*
import madhat.kotton.geometry.*

import at.tugraz.igi.util.*
import at.tugraz.igi.main.*
import at.tugraz.igi.ui.*

import java.util.concurrent.*

import org.jfree.graphics2d.svg.*
import java.util.*
import javax.vecmath.*
import org.twak.camp.*
import org.twak.utils.collections.*
import kotlin.math.*


enum class EdgeType {
    POLYGON, SKELETON, WAVEFRONT, TRIANGULATION
}

data class SkeletonSnapshot(
        // While campskeleton won't yield uniquely id'ed edges, triton will.
        // We won't bother implementing Either type just for this.
        // Both should keep edge orientation between snapshots.
        val edgesSet: Set<Pair<Point2d, Point2d>>?,
        val edgesMap: Map<Int, Triple<Point2d, Point2d, EdgeType>>?
)

/**
 * A computation trace shall map time/height of encountered events
 * to corresponding computation state.
 */
data class SkeletonTrace(
        val events: SortedMap<Double, SkeletonSnapshot>
)

data class SkeletonResult(
        val edges: Set<Pair<Point2d, Point2d>>,
        val trace: SkeletonTrace?,
        val svg: SVGGraphics2D?, // triton already builds svg, hence inclusion here
        val misc: String? // for e.g. event statistics
)


interface SkeletonComputation {
    fun computeSkeleton(input: ParsedPolygon,
                        timeout: Long?,
                        createTrace: Boolean = false,
                        createSVG: Boolean = false
    ): SkeletonResult
}

class TimeoutComputation <R> (val call: Callable<R>) {
    fun run(timeout: Long): R {
        val executor = Executors.newSingleThreadExecutor()
        val task = FutureTask<R>(call)
        val future = executor.submit(task)
        executor.shutdown()
        @Suppress("UNCHECKED_CAST")
        try {
            // FIXME: returning value does not work here
            return future.get(timeout, TimeUnit.SECONDS) as R
        } catch (ee: ExecutionException) {
            throw Exception(ee.cause)
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

        var loop: Loop<Edge> = Loop()
        val corners = input.sortedIndices.map {
            Corner(input.coordinates[it]!!.x,
                    input.coordinates[it]!!.y)
        }

        for (ind in 0 until corners.count()) {
            val p = corners[ind]
            val q = corners[(ind + 1) % corners.count()]
            val e = Edge(p, q)
            loop.append(e)
            e.machine = Machine(atan(input.weights[ind]))
        }

        val skeleton = Skeleton(loop.singleton(), true)

        val rawTrace =
                if (timeout == null)
                    skeleton.skeleton(true)
                else
                    TimeoutComputation(Callable({
                        skeleton.skeleton(createTrace)
                    })).run(timeout)

        /*
        for (face in skeleton.output.faces.values) {
            println("  face:")
            for (lp3 in face.points)
                for (pt in lp3)
                    println("  $pt")
        }
        */

        val polyEdges = HashSet((0 until input.sortedIndices.count()).map { HashSet(listOf(
                input.coordinates[input.sortedIndices[it]],
                input.coordinates[input.sortedIndices[(it + 1) % input.sortedIndices.count()]]
        )) })
        var skelEdges: MutableSet<Pair<Point2d, Point2d>> = HashSet()
        for (e in  skeleton.output.edges.map.values) {
            val edge = Pair(
                    Point2d(e.start.x, e.start.y),
                    Point2d(e.end.x, e.end.y)
            )
            if (!polyEdges.contains(HashSet(listOf(edge.first, edge.second)))) {
                skelEdges.add(edge)
            }
        }

        var traceMap: SortedMap<Double, SkeletonSnapshot> = TreeMap()
        if (createTrace) {
            for ((h, es) in rawTrace) {
                var edgesSet: MutableSet<Pair<Point2d, Point2d>> = HashSet()
                for (e in es) {
                    edgesSet.add(Pair(
                            Point2d(e[0][0], e[0][1]),
                            Point2d(e[1][0], e[1][1])
                            ))
                }
                traceMap[h] = SkeletonSnapshot(edgesSet, null)
            }
        }

        var svg: SVGGraphics2D? = null
        if (createSVG) {
            val edges = skeleton.output.edges.map.values
            val points = edges.flatMap { listOf(it.start, it.end) }
            val scaler = CoorsIntScaler(points.map { Point2d(it.x, it.y) })
            svg = SVGGraphics2D(scaler.maxX!!, scaler.maxY!!)
            for (edge in skeleton.output.edges.map.values) {
                val p = scaler[edge.start]!!
                val q = scaler[edge.end]!!
                svg.drawLine(p.first, p.second, q.first, q.second)
            }
        }

        return SkeletonResult(skelEdges, SkeletonTrace(traceMap), svg, null)
    }

}



class Triton : SkeletonComputation {

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
                    input.sortedIndices[it-1].let { Point(it,
                            input.coordinates[it]!!.x,
                            input.coordinates[it]!!.y
                        )
                    }
                },
                panel,
                controller)

        if (timeout == null)
            controller.runAlgorithmNoSwingWorker()
        else
            TimeoutComputation(Callable({
                controller.runAlgorithmNoSwingWorker()
            })).run(timeout)
        if (!controller.finished) throw Exception("Algorithm failed to finish")

        var edges: MutableSet<Pair<Point2d, Point2d>> = HashSet()
        for (line in controller.straightSkeleton.lines) {
            val p = Point2d(line.p1.originalX, line.p1.originalY)
            val q = Point2d(line.p2.originalX, line.p2.originalY)
            if (p != q) edges.add(
                    if (line.p1.number < line.p2.number)
                        Pair(p, q)
                    else
                        Pair(q, p))
        }

        var svg: SVGGraphics2D? = null
        if (createSVG) {
            svg = SVGGraphics2D(panel.width, panel.height)
            panel.paintSVG(svg)
        }

        // TODO: get trace from triton
        return SkeletonResult(edges, null, svg, null)
    }

}
