package at.tugraz.igi.events;

import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Point;
import at.tugraz.igi.util.Triangle;

public class EdgeEvent extends Event {

	public EdgeEvent(double collapsingTime, Triangle t, Point i, Line l) {
		super(collapsingTime, t, i, l);
	
	}
	
	@Override
	public String toString() {
		return "EdgeEvent [getCollapsingTime()=" + getCollapsingTime()
				+ ", getTriangle()=" + getTriangle() + ", getIntersection()="
				+ getIntersection() + ", getLine()=" + getLine() + "]";
	}

	@Override
	public String getName() {
		return "Edge event";
	}

}
