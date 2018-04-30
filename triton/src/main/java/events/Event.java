package at.tugraz.igi.events;

import java.util.List;

import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Point;
import at.tugraz.igi.util.Triangle;

public class Event {
	private double collapsingTime;
	private Triangle triangle;
	private Point intersection;
	private Line line;

	public Event(double collapsingTime, Triangle t, Point i) {
		setCollapsingTime(collapsingTime);
		this.triangle = t;
		this.intersection = i;
	}

	public Event(double collapsingTime, Triangle t, Point i, Line line) {
		this.collapsingTime = collapsingTime;
		this.triangle = t;
		this.intersection = i;
		this.line = line;

	}

	public double getCollapsingTime() {
		return collapsingTime;
	}

	public void setCollapsingTime(double collapsingTime) {
		this.collapsingTime = collapsingTime;

	}

	public Triangle getTriangle() {
		return triangle;
	}

	public void setTriangle(Triangle triangle) {
		this.triangle = triangle;
	}

	public void setIntersection(Point intersection) {
		this.intersection = intersection;
	}

	public Point getIntersection() {
		return intersection;
	}

	public void setLine(Line line) {
		this.line = line;
	}

	public Line getLine() {
		return line;
	}

	public String getName() {
		return "";
	}

	@Override
	public String toString() {
		return "Event [collapsingTime=" + collapsingTime + ", intersection=" + intersection + "]";
	}

	public boolean updateTriangle(List<Triangle> triangles, Line newLine) {
		for (Triangle tr : triangles) {
			if (tr.contains(newLine)) {
				this.setTriangle(tr);
				return true;
			}
		}
		return false;
	}

	public void updateTriangles(List<Triangle> triangles) {
		for (Triangle tr : triangles) {
			tr.replacePoint(this.getLine().getP1(), this.getLine().getP2(), this.getIntersection());
		}
	}


}
