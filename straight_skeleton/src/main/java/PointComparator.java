package at.tugraz.igi.util;

import java.util.Comparator;

public class PointComparator implements Comparator<Point> {

	@Override
	public int compare(Point p1, Point p2) {
		if (p1 != null && p2 != null) {
			if (p1.current_x < p2.current_x)
				return -1;
			else if (p1.current_x > p2.current_x) {
				return 1;
			}
		}
		return 0;
	}

}
