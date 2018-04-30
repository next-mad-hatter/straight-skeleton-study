package at.tugraz.igi.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Triangle implements Cloneable {
	public Point p1;
	public Point p2;
	public Point p3;
	public List<Line> strokes;
	public List<Line> polyLines;

	public Triangle(Point p1, Point p2, Point p3) {

		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.strokes = new ArrayList<Line>();
		this.polyLines = new ArrayList<Line>();
	}

	public boolean contains(Point p) {
		Set<Point> points = new HashSet<Point>();
		points.add(p1);
		points.add(p2);
		points.add(p3);
		return points.contains(p);
		// if (p1.equals(p)) {
		// return true;
		// } else if (p2.equals(p)) {
		// return true;
		// } else if (p3.equals(p)) {
		// return true;
		// }
		// return false;
	}

	public boolean contains(Line l) {
		Point p_1 = l.getP1();
		Point p_2 = l.getP2();
		return contains(p_1) && contains(p_2);
		// boolean containsP1 = false;
		// if (p_1.equals(p1) || p_1.equals(p2) || p_1.equals(p3)) {
		// containsP1 = true;
		// }
		// boolean containsP2 = false;
		// if (p_2.equals(p1) || p_2.equals(p2) || p_2.equals(p3)) {
		// containsP2 = true;
		// }
		// return containsP1 && containsP2;
	}

	public Point getThirdPoint(Line l) {
		if (!p1.equals(l.getP1()) && !p1.equals(l.getP2())) {
			return p1;
		} else if (!p2.equals(l.getP1()) && !p2.equals(l.getP2())) {
			return p2;
		} else if (!p3.equals(l.getP1()) && !p3.equals(l.getP2())) {
			return p3;
		}
		return null;
	}

	public List<Point> getOtherPoints(Point p) {
		List<Point> others = new ArrayList<Point>();
		if (!p1.equals(p)) {
			others.add(p1);
		}
		if (!p2.equals(p)) {
			others.add(p2);
		}
		if (!p3.equals(p)) {
			others.add(p3);
		}
		return others;
	}

	public void replacePoint(Point r1, Point r2, Point i) {
		if (p1.equals(r1) || p1.equals(r2)) {
			p1 = i;
			
		}
		if (p2.equals(r1) || p2.equals(r2)) {
			p2 = i;
			
		}
		if (p3.equals(r1) || p3.equals(r2)) {
			p3 = i;
			
		}
		List<Line> wrongStrokes = new ArrayList<Line>();

		for (Line s : strokes) {
			s.replacePoint(r1, r2, i);
			if (getLine(s.getP1(), s.getP2()) != null) {
				wrongStrokes.add(s);
			}
		}
		strokes.removeAll(wrongStrokes);
		polyLines.removeAll(polyLines);
		Line l = getLine(p1, p2);
		if (l != null) {
			polyLines.add(l);
		}
		l = getLine(p1, p3);
		if (l != null) {
			polyLines.add(l);
		}
		
		l=getLine(p2, p3);
		if (l != null) {
			polyLines.add(l);
		}	
		
	}

	private Line getLine(Point p1, Point p2) {
		if (Util.getOtherPointOfLine(p1, p1.adjacentLines.get(0)).equals(p2)) {
			return p1.adjacentLines.get(0);
		}
		if (Util.getOtherPointOfLine(p1, p1.adjacentLines.get(1)).equals(p2)) {
			return p1.adjacentLines.get(1);
		}
		return null;
	}

	public boolean isTriangle() {
		return (!p1.equals(p2) && !p1.equals(p3) && !p2.equals(p3));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((p1 == null) ? 0 : p1.hashCode());
		result = prime * result + ((p2 == null) ? 0 : p2.hashCode());
		result = prime * result + ((p3 == null) ? 0 : p3.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Triangle other = (Triangle) obj;
		if (!(p1.equals(other.p1) || p1.equals(other.p2) || p1.equals(other.p3))) {
			return false;
		} else if (!(p2.equals(other.p1) || p2.equals(other.p2) || p2.equals(other.p3))) {
			return false;
		} else if (!(p3.equals(other.p1) || p3.equals(other.p2) || p3.equals(other.p3))) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Triangle [p1=" + p1 + ", p2=" + p2 + ", p3=" + p3 + "]";
	}

}
