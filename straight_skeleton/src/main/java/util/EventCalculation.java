package at.tugraz.igi.util;


public class EventCalculation {
	public static int vertex_counter = 1;
	public static int skeleton_counter = 0;
	
	public static double findEvent(Line line, Triangle t) {

		Point p1 = line.getP1();
		Point p2 = line.getP2();
		Vector v1 = p1.getMovementVector();
		Vector v2 = p2.getMovementVector();

		double dx, dy;
		dx = (p2.current_x - p1.current_x);
		dy = (p2.current_y - p1.current_y);
		if (v1 != null && v2 != null) {
			double det = v1.det(v2);
			if (det != 0) {
				double u = (dy * v2.getX() - dx * v2.getY()) / det;
				double v = (dy * v1.getX() - dx * v1.getY()) / det;
				double x, y;
				if (u >= 0 && v >= 0 && (t != null || (t == null && v <= 1))) {
					x = p1.current_x + v1.getX() * u;
					y = p1.current_y + v1.getY() * u;
					return u;
				}
			}
		}
		return Double.POSITIVE_INFINITY;

	}
}