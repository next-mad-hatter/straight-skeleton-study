package at.tugraz.igi.util;

public class Line implements Cloneable{
	public boolean polyLine;
	private Point p1;
	private Point p2;
	private Vector line_vector = null;
	private Vector line_normal_vector  = null;
	private Vector line_weighted_movement_vector = null;
	
	private int weight;

	public Line(Point p1, Point p2, int weight) {
		this.p1 = p1;
		this.p2 = p2;
		this.weight = weight;
	}

	public Point getP1() {
		return p1;
	}

	public Point getP2() {
		return p2;
	}

	public int getWeight() {
		return weight;
	}

	public String getWeightAsString() {
		return Integer.toString(weight);
	}

	public void setWeight(int weight) {
		this.resetCache();
		this.weight = weight;
	}

	public void setP1(Point p1) {
		this.resetCache();
		this.p1 = p1;
	}

	public void setP2(Point p2) {
		this.resetCache();
		this.p2 = p2;
	}

	public Vector getLineVector() {
//		if (line_vector == null) {
			line_vector = new Vector(p2.getCurrentX() - p1.getCurrentX(), p2.getCurrentY() - p1.getCurrentY());
//		}
		return line_vector;
	}
	
	public Vector getLineNormalVector() {
		if (line_normal_vector == null) {
			line_normal_vector = this.getLineVector().getRotatedVector().normalize();
		}
		return line_normal_vector;
	}

	public Vector getLineWeightedMovementVector(){
		if (line_weighted_movement_vector == null){
			 line_weighted_movement_vector = this.getLineNormalVector().multiply(this.getWeight());
		}
		return line_weighted_movement_vector;
	}
	
	public boolean contains(Point p) {
		if (p1.equals(p) || p2.equals(p)) {
			return true;
		}
		return false;
	}

	public void replacePoint(Point r1, Point r2, Point i) {
		if (p1.equals(r1) || p1.equals(r2)) {
			p1 = i;			
		}
		if (p2.equals(r1) || p2.equals(r2)) {
			p2 = i;
		}
		resetCache();
	}

	private void resetCache(){
		line_vector = null;
		line_normal_vector = null;
		line_weighted_movement_vector = null;
	}
	
	@Override
	public String toString() {
		return "(" + p1.getNumberAsString() + "," + p2.getNumberAsString() + " ," + +weight + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((p1 == null) ? 0 : p1.hashCode());
		result = prime * result + ((p2 == null) ? 0 : p2.hashCode());
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
		Line other = (Line) obj;
		if (p1 == null) {
			if (other.p1 != null)
				return false;
		}

		if (p2 == null) {
			if (other.p2 != null) {
				return false;
			}
		}
		if (!p1.equals(other.p1) && !p1.equals(other.p2)) {
			return false;
		}
		if (!p2.equals(other.p1) && !p2.equals(other.p2)) {
			return false;
		}
		if (p1.equals(other.p1)) {
			if (!p2.equals(other.p2)) {
				return false;
			}
		} else if (p1.equals(other.p2)) {
			if (!p2.equals(other.p1)) {
				return false;
			}
		}

		return true;
	}
	@Override
	public Line clone() throws CloneNotSupportedException {
		return new Line(p1.clone(), p2.clone(), weight);
	}
}
