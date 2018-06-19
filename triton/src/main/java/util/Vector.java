package at.tugraz.igi.util;

import at.tugraz.igi.main.Controller;

public class Vector {
	private double x;
	private double y;

	public Vector(Vector other) {
		this.x = other.x;
		this.y = other.y;
	}

	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void multiplyInPlace(double constant) {
		x = x * constant;
		y = y * constant;
	}

	public double length() {
		return Math.sqrt(x * x + y * y);
	}

	public Vector normalize() {
		double length = length();
		return this.multiply(1 / length);
	}

	public Vector multiply(double constant) {
		return new Vector(x * constant, y * constant);
	}

	public Vector addVector(Vector other) {
		return new Vector(x + other.x, y + other.y);
	}

	public double dot(Vector v2) {
		return x * v2.x + y * v2.getY();
	}

	public double det(Vector v2) {
		return v2.getX() * this.getY() - v2.getY() * this.getX();
	}

	public Vector getRotatedVector() {
		if (Controller.isCounterClockwise) {
			return new Vector(this.y, -1.0 * this.x);
		}
		return new Vector(-1.0 * this.y, this.x);

	}

	@Override
	public String toString() {
		return "Vector [x=" + x + ", y=" + y + "]";
	}

}
