package at.tugraz.igi.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Point;
import at.tugraz.igi.util.Triangle;
import at.tugraz.igi.util.Util;

public class SplitEvent extends ConcaveEvent {
	public SplitEvent(double collapsingTime, Triangle t, Point i, Point p, Line l) {
		super(collapsingTime, t, i, p, l);
	}

	@Override
	public String toString() {
		return "SplitEvent [getReflexVertex()=" + getReflexVertex() + ", getName()=" + getName() + ", getCollapsingTime()=" + getCollapsingTime() + ", getTriangle()="
				+ getTriangle() + ", getIntersection()=" + getIntersection() + ", getLine()=" + getLine() + "]";
	}

	public Map<Point, Set<Point>> splitPolygons(List<Set<Point>> polygons, Event event, Point copyVertex) throws Exception {
		Map<Point, Set<Point>> splitLines = new HashMap<Point, Set<Point>>();
		Point reflex = ((SplitEvent) event).getReflexVertex();
	
		Set<Point> points = null;
		for (Set<Point> pts : polygons) {
			if (pts.contains(reflex)) {
				points = pts;
			}
		}
	
		Point i = event.getIntersection();
//		int index = points.indexOf(reflex);
		points.remove(reflex);
		points.add(copyVertex);
		points.add(i);
	
		Point p1 = event.getLine().getP1();
		Point p2 = event.getLine().getP2();
	
		Line l1 = new Line(event.getLine().getP1(), event.getLine().getP2(), event.getLine().getWeight());
		l1.replacePoint(p2, null, i);
	
		Line l = reflex.adjacentLines.get(1);
		l.replacePoint(reflex, null, i);
		
		i.adjacentLines.add(l1);
		i.adjacentLines.add(l);
	
		p1.adjacentLines.remove(event.getLine());
		p1.adjacentLines.add(l1);
	
		Line l2 = event.getLine();
		l2.replacePoint(p1, null, copyVertex);
	
		l = reflex.adjacentLines.get(0);
		l.replacePoint(reflex, null, copyVertex);
		
		copyVertex.adjacentLines.add(l);
		copyVertex.adjacentLines.add(l2);
		
		Set<Point> polygon1 = createPolygon(i, 0, points.size());
	
		Set<Point> polygon2 = createPolygon(copyVertex, 1,points.size());
	
		polygons.remove(points);
		polygons.add(polygon1);
		polygons.add(polygon2);
	
		splitLines.put(i, polygon1);
		splitLines.put(copyVertex, polygon2);
	
		return splitLines;
	
	}


	private boolean updateReflexVertex(Map<Point, Set<Point>> polygons, Triangle t) {
		List<Point> others = t.getOtherPoints(this.getReflexVertex());
		if (others.size() == 2) {
			for (Point i : polygons.keySet()) {
				if (inSamePolygon(polygons.get(i), others.get(0), others.get(1))) {
					t.replacePoint(this.getReflexVertex(), null, i);
					return true;
				}
			}
		}
		if (t.contains(this.getLine().getP1()) || t.contains(this.getLine().getP2())) {

			if (t.contains(this.getLine().getP1())) {
				t.replacePoint(this.getLine().getP1(), null, this.getLine().getP2());
			} else {
				t.replacePoint(this.getLine().getP2(), null, this.getLine().getP1());
			}
			return true;
		}

		return false;
	}

	
	public void updateTriangles(Map<Point, Set<Point>> polygons, List<Triangle> triangles) {
		//Double For
		for (Triangle t : triangles) {
			if (t.contains(this.getReflexVertex())) {
				updateReflexVertex(polygons, t);
			}
		
			if (!Util.inSamePolygon(polygons, t)) {
				Point rPoint = null;
				for (Point i : polygons.keySet()) {
					if (inSamePolygon(polygons.get(i), t.p1, t.p2)) {
						rPoint = t.p3;
					} else if (inSamePolygon(polygons.get(i), t.p1, t.p3)) {
						rPoint = t.p2;
					} else if (inSamePolygon(polygons.get(i), t.p2, t.p3)) {
						rPoint = t.p1;
					}
					if (rPoint != null) {
						t.replacePoint(rPoint, null, i);
						break;
					}
				}
			}
		}
	}

	private boolean inSamePolygon(Set<Point> polygon, Point p1, Point p2) {
		return polygon.contains(p1) && polygon.contains(p2);
	}

	private Set<Point> createPolygon(Point startingPoint, int index, int size) throws Exception {
		int i = 0;
		Set<Point> polygon = new HashSet<Point>();
		polygon.add(startingPoint);
		Point p = Util.getOtherPointOfLine(startingPoint, startingPoint.adjacentLines.get(index));
		while (!p.equals(startingPoint)) {
			if(i==size){
				throw new Exception("FIXME: undocumented error 1");
			}
			polygon.add(p);
			
			
			p = Util.getOtherPointOfLine(p, p.adjacentLines.get(index));
			if(p.getNumber() == 11111){
				throw new Exception("FIXME: undocumented error 2");
			}
			i++;
		}
		return polygon;
	}

}
