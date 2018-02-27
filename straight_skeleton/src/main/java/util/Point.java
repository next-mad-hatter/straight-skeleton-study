package at.tugraz.igi.util;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

import at.tugraz.igi.main.Controller;

public class Point implements Cloneable {
	protected int number;
	protected double original_x;
	protected double original_y;
	public double current_x;
	public double current_y;
	public List<Line> adjacentLines;

	private Vector movement_vector = null;

	private Boolean convex = null;

	public Point(int number, double x, double y) {
		this.adjacentLines = new ArrayList<Line>();
		this.number = number;
		this.original_x = x;
		this.original_y = y;
		this.current_x = x;
		this.current_y = y;
	}

	public int getNumber() {
		return number;
	}

	public String getNumberAsString() {
		return Integer.toString(number);
	}

	public double getOriginalX() {
		return original_x;
	}

	public double getOriginalY() {
		return original_y;
	}

	public double getCurrentX() {
		return current_x;
	}

	public double getCurrentY() {
		return current_y;
	}

	public void calculateMovementInfo() {
		this.movement_vector = null;
		this.convex = null;
		
		if (!this.adjacentLines.get(0).getP2().equals(this)) {
			Line tmp = this.adjacentLines.get(0);
			this.adjacentLines.remove(tmp);
			this.adjacentLines.add(tmp);
		}

		Line line1 = this.adjacentLines.get(0);
		Line line2 = this.adjacentLines.get(1);

		if (line1 == null || line2 == null) {
			return;
		}
		Point p1 = Util.getOtherPointOfLine(this, line1);
		Point p2 = Util.getOtherPointOfLine(this, line2);

		if (this.convex == null) {

			this.convex = new Boolean(Util.isConvex(p1, p2, this));
			if (!Controller.isCounterClockwise) {
				this.convex = !this.convex;
			}
		}

		Vector l1_mov_vec = line1.getLineWeightedMovementVector();
		Vector l2_mov_vec = line2.getLineWeightedMovementVector();

		Vector l1_vector = line1.getLineVector();
		Vector l2_vector = line2.getLineVector();

		Vector d_vec = l2_mov_vec.addVector(l1_mov_vec.multiply(-1.));

		double dx, dy;
		dx = d_vec.getX();
		dy = d_vec.getY();

		double det = l1_vector.det(l2_vector);
		if (det != 0.0) {
			double u = (dy * l2_vector.getX() - dx * l2_vector.getY()) / det;
			this.movement_vector = l1_mov_vec.addVector(l1_vector.multiply(u));
		}

//		else {
//			System.out.println("Cant calculate point movement info for this case");
//			System.out.println("det " + det);
//			System.out.println(this.adjacentLines);
//			System.out.println("l1 mov " + l1_mov_vec.toString());
//			System.out.println("l2 mov " + l2_mov_vec.toString());
//			System.out.println("l2 vec " + l1_vector.toString());
//			System.out.println("l2 vec " + l2_vector.toString());
//			System.out.println("d_vec  " + d_vec.toString());
//		}

	}

	public void move(double skalar) {
		Vector a_v = getMovementVector();
		if(a_v == null){
			return;
		}
		current_x += skalar * a_v.getX();
		current_y += skalar * a_v.getY();
	}

	public void updateAdjacentLines(Point oldPoint, Point newPoint) {
		for (Line l : adjacentLines) {
			l.replacePoint(oldPoint, null, newPoint);
		}
	}

	public void resetToOriginalPosition() {
		current_x = original_x;
		current_y = original_y;
	}

	public Vector getMovementVector() {
		return movement_vector;
	}

	public boolean isConvex() {
		return convex;
	}

	public void setConvex(boolean convex) {
		this.convex = convex;
	}

	public void setBothXCoordinates(double x) {
		this.original_x = x;
		this.current_x = x;
	}

	public void setBothYCoordinates(double y) {
		this.original_y = y;
		this.current_y = y;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	@Override
	public String toString() {
		return Integer.toString(number);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		return result;
	}

	/**
	 * We want to be able to differentiate between Points with same id ("number") and
	 * different coordinates (e.g. for keeping snapshots history).
	 *
	 * Apparently some code depends on conflating Points with same number values though,
	 * since simply adding this functionality to the Point class breaks some test cases
	 * (which then produce skeletons featuring unconnected components albeit not visibly
	 * so due to very closely placed points, i.e. "almost" connected).
     *
	 * Also, it seems we need to change the behaviour globally for snapshots to work --
	 * hence the HISTORY_MODE hack.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			return false;
		}
		Point other = (Point) obj;
		if (number != other.number)
			return false;

		if (!Controller.HISTORY_MODE && number == other.number)
			return true;
		if (number != other.number)
			return false;

		if (Controller.HISTORY_MODE && (
		    Math.abs(original_x - other.original_x) > 1e-5 || Math.abs(original_y - other.original_y) > 1e-5 ||
		    Math.abs(current_x - other.current_x) > 1e-5 || Math.abs(current_y - other.current_y) > 1e-5))
			return false;

		return true;
	}

	@Override
	public Point clone() throws CloneNotSupportedException {
		return (Point) super.clone();
	}

}
