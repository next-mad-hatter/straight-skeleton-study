package at.tugraz.igi.util;

import at.tugraz.igi.main.Controller;

import java.util.ArrayList;
import java.util.List;

public class PointAttributes {

    private Point point;

	public PointAttributes(Point point) {
		this.point = point;
	}

	@Override
	public int hashCode() {
	    return point.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			return false;
		}

		PointAttributes other = (PointAttributes) obj;
		if (point == other.point)
			return true;

		if (point.getNumber() != other.point.getNumber())
			return false;

		if (Math.abs(point.getOriginalX() - other.point.getOriginalX()) > 1e-7 || Math.abs(point.getOriginalY() - other.point.getOriginalY()) > 1e-7 ||
		    Math.abs(point.getCurrentX() - other.point.getCurrentX()) > 1e-7 || Math.abs(point.getCurrentY() - other.point.getCurrentY()) > 1e-7)
			return false;

		return true;
	}

}
