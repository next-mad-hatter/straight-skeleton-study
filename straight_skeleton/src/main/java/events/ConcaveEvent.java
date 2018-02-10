package at.tugraz.igi.events;

import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Point;
import at.tugraz.igi.util.Triangle;

public class ConcaveEvent extends Event{
	private Point reflexVertex;
	
	public ConcaveEvent(double collapsingTime, Triangle t, Point i, Point p, Line l) {
		super(collapsingTime, t, i,l);
		reflexVertex = p;
	}

	public Point getReflexVertex() {
		return reflexVertex;
	}

	public void setReflexVertex(Point reflexVertex) {
		this.reflexVertex = reflexVertex;
	}

	public void updateReflexVertex(Point oldP, Point newP){
		if(this.getReflexVertex().equals(oldP)){
			this.setReflexVertex(newP);
		}
	}
	
	
	@Override
	public String getName() {
		if(this instanceof FlipEvent){
			return "Flip event";
		}
		return "Split event";
	}

}
