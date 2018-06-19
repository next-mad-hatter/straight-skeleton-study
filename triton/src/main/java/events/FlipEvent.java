package at.tugraz.igi.events;

import java.util.List;

import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Point;
import at.tugraz.igi.util.Triangle;
import at.tugraz.igi.util.Util;

public class FlipEvent extends ConcaveEvent {

	public FlipEvent(double collapsingTime, Triangle t, Point i, Point p, Line l) {
		super(collapsingTime, t, i, p, l);
	}
	
	@Override
	public void updateTriangles(List<Triangle> triangles) {
		Triangle t1 = null;
		Triangle t2 = null;
		for (Triangle t : triangles) {
			if (t.contains(this.getLine())) {
				if (t.contains(this.getIntersection())) {
					t1 = t;
					t1.strokes.clear();
				} else {
					t2 = t;
					t2.strokes.clear();
				}
			}

			t.replacePoint(this.getReflexVertex(), null, this.getIntersection());
		}
		if (t1 != null && t2 != null) {

			Point p3 = t2.getThirdPoint(this.getLine());
			t1.replacePoint(this.getLine().getP2(), null, p3);

			t2.replacePoint(this.getLine().getP1(), null, this.getIntersection());

			addStroke(t1, t1.p1, t1.p2);
			addStroke(t1, t1.p1, t1.p3);
			addStroke(t1, t1.p2, t1.p3);
			
			addStroke(t2, t2.p1, t2.p2);
			addStroke(t2, t2.p1, t2.p3);
			addStroke(t2, t2.p2, t2.p3);

		}
	}

	private void addStroke(Triangle t, Point p1, Point p2) {
		if (!Util.getOtherPointOfLine(p1, p1.adjacentLines.get(0)).equals(p2)
				&& !Util.getOtherPointOfLine(p1, p1.adjacentLines.get(1)).equals(p2)) {
			t.strokes.add(new Line(p1, p2, 1));
		}
	}

	@Override
	public String toString() {
		return "FlipEvent [getReflexVertex()=" + getReflexVertex() + ", getName()=" + getName() + ", getCollapsingTime()=" + getCollapsingTime() + ", getTriangle()="
				+ getTriangle() + ", getIntersection()=" + getIntersection() + ", getLine()=" + getLine() + "]";
	}

}
