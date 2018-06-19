package at.tugraz.igi.util;

import at.tugraz.igi.main.Controller;

import java.util.Map;
import java.util.HashMap;

public class EventCalculation {

	// FIXME: the original logic keeps skeletons count modulo number of colors in
	//        this global variable, while new colors get defined and assigned during
	//        algorithm start.  It would seem proper to factor out color management
	//        from both here and algorithm implementation.
	public static int skeleton_counter = 0;

	public static Map<Controller.Context, Integer> vertex_counter = new HashMap<>();

	public static Integer getVertexCounter(Controller.Context context) {
	    return vertex_counter.getOrDefault(context, new Integer(1));
	}

	public static void setVertexCounter(Controller.Context context, Integer val) {
		vertex_counter.put(context, val);
	}

	public static void incVertexCounter(Controller.Context context) {
		Integer vc = vertex_counter.getOrDefault(context, new Integer(1));
		vertex_counter.put(context, vc + 1);
	}

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