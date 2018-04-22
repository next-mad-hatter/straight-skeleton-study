package at.tugraz.igi.util;

import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;

public class TreeCheck {


    /**
     * Checks if the graph defined by given set of points and lines is a tree.
     *
     * Points can be null.
     *
     * TODO: how far do we need to relax this for polygons with holes?
     */
    public static boolean isTree(List<Point> points, List<Line> edges) throws Exception {
        Pseudograph<Point, DefaultEdge> gr = new Pseudograph<>(DefaultEdge.class);
        if (points != null)
          for (Point p: points)
              gr.addVertex(p);
        for (Line e: edges) {
            if (!gr.containsVertex(e.getP1()))
              gr.addVertex(e.getP1());
            if (!gr.containsVertex(e.getP2()))
              gr.addVertex(e.getP2());
            gr.addEdge(e.getP1(), e.getP2());
        }
        for (Point p: points) {
            if(gr.degreeOf(p) != 1)
                throw new Exception("Bad skeleton: a vertex is not a leaf in skeleton");
        }
        return GraphTests.isTree(gr);
    }

}
