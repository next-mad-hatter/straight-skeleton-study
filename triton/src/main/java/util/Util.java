package at.tugraz.igi.util;

import at.tugraz.igi.main.Controller;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import poly2Tri.Triangulation;

//import org.apache.commons.lang3.builder.ToStringBuilder;

public class Util {

	public static List<Triangle> triangulate(final ArrayList<Point> points, List<List<Line>> lines) {
		Map<Integer, Point> numberToPoint = new HashMap<Integer, Point>();
		List<Triangle> triangulation = new ArrayList<Triangle>();
		int numContours = 1;
		int[] contours = new int[numContours];
		contours[0] = points.size();
		double[][] vertices = new double[points.size()][2];
		if (Util.isCounterClockwise(points)) {
			int k = 0;
			for (int j = (points.size() - 1); j >= 0; j--) {
				Point p = points.get(j);
				numberToPoint.put(k, p);
				vertices[k++] = new double[] { p.getOriginalX(), p.getCurrentY() };
			}

		} else {
			for (int j = 0; j < points.size(); j++) {
				Point p = points.get(j);
				vertices[j] = new double[] { p.getOriginalX(), p.getCurrentY() };
			}
		}
		/*
		 * Poly2Tri Copyright (c) 2009-2010, Poly2Tri Contributors
		 * http://code.google.com/p/poly2tri/
		 */
		ArrayList triangles = Triangulation.triangulate(numContours, contours, vertices);
		for (int i = 0; i < triangles.size(); ++i) {
      //System.out.println(ToStringBuilder.reflectionToString(triangles.get(i)));
			ArrayList t = (ArrayList) triangles.get(i);
			Point p1 = numberToPoint.get(t.get(0));
			Point p2 = numberToPoint.get(t.get(1));
			Point p3 = numberToPoint.get(t.get(2));
			triangulation.add(createTriangle(p1, p2, p3, lines));
		}
		return triangulation;
	}

	// /**
	// * Ear clipping algorithm
	// *
	// * @param points
	// * @return list of triangles
	// */
	// public static List<Triangle> triangulate(final ArrayList<Point> points,
	// List<List<Line>> lines) {
	//
	// List<Triangle> triangles = new ArrayList<Triangle>();
	// int size = points.size();
	// int previousIndex = size - 1;
	// int nextIndex = 1;
	// while (points.size() > 3) {
	// for (Point p : points) {
	// Point previousPoint = points.get(previousIndex);
	// Point nextPoint = points.get(nextIndex);
	//
	// if (!isAnyVertexInTriangle(points, previousPoint, p, nextPoint)) {
	// Triangle t = createTriangle(p, previousPoint, nextPoint, lines);
	// triangles.add(t);
	// points.remove(p);
	// size = points.size();
	// previousIndex = size - 1;
	// nextIndex = 1;
	// break;
	// }
	// previousIndex = points.indexOf(p);
	// nextIndex += 1;
	// if (nextIndex == size) {
	// nextIndex = 0;
	// }
	//
	// }
	// }
	// Triangle t = createTriangle(points.get(1), points.get(0), points.get(2),
	// lines);
	// triangles.add(t);
	// return triangles;
	// }
	//
	public static List<Triangle> triangulate2(final List<Point> points, List<List<Line>> lines) {
		List<Triangle> triangles = new ArrayList<Triangle>();
		int size = points.size();
		Triangle t = createTriangle(points.get(0), points.get(size - 1), points.get(1), lines);
		triangles.add(t);
		for (int index = size - 1; index > 2; index--) {
			t = createTriangle(points.get(index - 1), points.get(index), points.get(1), lines);
			triangles.add(t);
		}
		return triangles;
	}

	public static boolean isConvex(Point a, Point b, Point c) {
		return (b.current_x - a.current_x) * (c.current_y - a.current_y) - (b.current_y - a.current_y)
				* (c.current_x - a.current_x) > 0;
	}

	public static boolean isCounterClockwise(List<Point> points) {
		float area = 0;
		for (Point p : points) {
			int index = points.indexOf(p);
			if (index == points.size() - 1) {
				index = -1;
			}
			Point p2 = points.get(index + 1);
			area += p.getOriginalX() * p2.getOriginalY() - p2.getOriginalX() * p.getOriginalY();
		}
		return area < 0;
	}

	public static void closePolygon(List<Point> points, List<Line> lines) {
		Random randomGenerator = new Random();
		Point p = points.get(0);
		if (p.getNumber() == 1) {
			Line last = lines.get(lines.size() - 1);
			if (!last.contains(p)) {
				Point lastPoint = points.get(points.size() - 1);
				int weight = randomGenerator.nextInt(20);
				weight = weight == 0 ? 1 : weight;
				Line line = new Line(lastPoint, p, weight);
				lines.add(line);

			}
		}
	}

	public static Line getAdjacentLine(Point p1, Point p2, List<List<Line>> lines) {
		for (Line l : p1.adjacentLines) {
			if (l.contains(p1) && l.contains(p2)) {
				return l;
			}
		}
		return null;
	}

	public static Point getOtherPointOfLine(Point p, Line l) {

		if (l.getP1().getNumber() == p.getNumber()) {
			return l.getP2();
		}
		if (l.getP2().getNumber() == p.getNumber()) {
			return l.getP1();
		}
		
		return new Point(11111, 0.0, 0.0);

	}

	private static Triangle createTriangle(Point p, Point previousPoint, Point nextPoint, List<List<Line>> lines) {

		Triangle t = new Triangle(previousPoint, p, nextPoint);
		Line l1 = getAdjacentLine(previousPoint, p, lines);
		Line l3 = getAdjacentLine(previousPoint, nextPoint, lines);
		Line l2 = getAdjacentLine(p, nextPoint, lines);

		addStroke(previousPoint, p, nextPoint, t, l1);
		addStroke(p, nextPoint, previousPoint, t, l2);
		addStroke(previousPoint, nextPoint, p, t, l3);
		return t;

	}

	private static void addStroke(Point p1, Point p2, Point p3, Triangle triangle, Line line) {
		if (line == null) {
			if (p1.getNumber() < p2.getNumber()) {
				line = new Line(p1, p2, 1);
			} else {
				line = new Line(p2, p1, 1);
			}
			triangle.strokes.add(line);
		} else {
			triangle.polyLines.add(line);
		}
		// return line;
	}

	// Taken from
	// https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/EarClippingTriangulator.java
	public static boolean isAnyVertexInTriangle(final List<Point> pVertices, Point p1, Point p2, Point p3) {
		double pX1 = p1.getOriginalX();
		double pX2 = p2.getOriginalX();
		double pX3 = p3.getOriginalX();
		double pY1 = p1.getOriginalY();
		double pY2 = p2.getOriginalY();
		double pY3 = p3.getOriginalY();

		for (Point p : pVertices) {
			if (!p.isConvex()) {
				final double currentVertexX = p.getOriginalX();
				final double currentVertexY = p.getOriginalY();

				final int areaSign1 = computeSpannedAreaSign(pX1, pY1, pX2, pY2, currentVertexX, currentVertexY);
				final int areaSign2 = computeSpannedAreaSign(pX2, pY2, pX3, pY3, currentVertexX, currentVertexY);
				final int areaSign3 = computeSpannedAreaSign(pX3, pY3, pX1, pY1, currentVertexX, currentVertexY);

				if (areaSign1 > 0 && areaSign2 > 0 && areaSign3 > 0) {
					return true;
				} else if (areaSign1 <= 0 && areaSign2 <= 0 && areaSign3 <= 0) {
					return true;
				}
			}
		}
		return false;
	}

	// Taken from
	// https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/EarClippingTriangulator.java
	private static int computeSpannedAreaSign(final double pX1, final double pY1, final double pX2, final double pY2,
			final double pX3, final double pY3) {

		double area = 0;

		area += pX1 * (pY3 - pY2);
		area += pX2 * (pY1 - pY3);
		area += pX3 * (pY2 - pY1);

		return (int) Math.signum(area);
	}

	public static Line getOtherAdjacentLine(Line adjLine, Point p) {
		for (Line l : p.adjacentLines) {
			if (!l.equals(adjLine)) {
				return l;
			}
		}
		return null;
	}

	public static Point clonePoint(Controller.Context context, Point p, int vertex_counter) {
        Point copy = new Point(vertex_counter, p.getOriginalX(), p.getOriginalY());
        EventCalculation.incVertexCounter(context);
		return copy;
	}

	public static boolean inSamePolygon(Map<Point, Set<Point>> polygons, Triangle triangle) {
		for (Set<Point> points : polygons.values()) {
			boolean same = points.contains(triangle.p1) && points.contains(triangle.p2) && points.contains(triangle.p3);
			if (same) {
				return true;
			}
		}
		return false;
	}

	public static PolygonMeasureData calculatePolygArea(List<Point> points, StraightSkeleton straightSkeleton) throws Exception {
		Map<Integer, List<Line>> pointToLine = new HashMap<Integer, List<Line>>();
		for (Line line : straightSkeleton.getLines()) {
			Point p1 = line.getP1();
			Point p2 = line.getP2();
			if (pointToLine.get(p1.getNumber()) == null) {
				List<Line> tmpList = new ArrayList<Line>();
				tmpList.add(line);
				pointToLine.put(p1.getNumber(), tmpList);
			} else {
				pointToLine.get(p1.getNumber()).add(line);
			}

			if (pointToLine.get(p2.getNumber()) == null) {
				List<Line> tmpList = new ArrayList<Line>();
				tmpList.add(line);
				pointToLine.put(p2.getNumber(), tmpList);
			} else {
				pointToLine.get(p2.getNumber()).add(line);
			}
		}
		PolygonMeasureData polyData = new PolygonMeasureData();
		straightSkeleton.polygon = new HashMap<List<Line>, MeasuringData>();
		for (Line line : straightSkeleton.getPolyLines()) {
			try {
				List<Line> poly = new ArrayList<Line>();
				List<Line> visitedLines = new ArrayList<Line>();
				Point start = line.getP1();
				Point end = line.getP2();
				poly.add(line);
				poly.addAll(findPath(start, end, points, visitedLines, pointToLine));
				poly = FileHandler.reorder(poly);
				double area = calculateArea(poly);
				MeasuringData data = new MeasuringData(line, poly, area);
				straightSkeleton.polygon.put(poly, data);
				polyData.subPolygonData.add(data);
			} catch (NullPointerException e) {
				continue;
			}
		}
		double[] values = new double[straightSkeleton.polygon.size()];
		int i = 0;
		for (MeasuringData v : straightSkeleton.polygon.values()) {
			values[i] = v.getArea();
			i++;
		}
		polyData.mean_area = mean(values);
		polyData.variance_area = variance(values, mean(values));
		polyData.standardDeviation_area = standardDeviation(variance(values, mean(values)));

		return polyData;
	}

	public static double mean(double[] values) {
		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		return sum / values.length;
	}

	public static double variance(double[] values, double mean) {
		double varianz = 0;
		for (int i = 0; i < values.length; i++) {
			varianz += (values[i] - mean) * (values[i] - mean);
		}
		return varianz / values.length;
	}

	public static double standardDeviation(double variance) {
		return Math.sqrt(variance);
	}

	private static List<Line> findPath(Point start, Point end, List<Point> points, List<Line> visitedLines,
			Map<Integer, List<Line>> pointToLine) throws Exception {
		List<Line> poly = new ArrayList<Line>();
		List<Line> check = new ArrayList<Line>();
		for (Line li : pointToLine.get(start.getNumber())) {
			Point other = Util.getOtherPointOfLine(start, li);
			if (visitedLines.contains(li)) {
				continue;
			}
			if (li.contains(end)) {
				poly.add(li);
				return poly;
			}
			if (points.contains(other)) {
				continue;
			}
			check.add(li);

		}
		for (Line li : check) {
			visitedLines.add(li);
			Point other = Util.getOtherPointOfLine(start, li);
			List<Line> path = findPath(other, end, points, visitedLines, pointToLine);
			if (path.size() != 0) {
				poly.add(li);
				poly.addAll(path);
				return poly;
			}

		}
		return poly;
	}

	private static double calculateArea(List<Line> polygon) {
		List<Point> points = new ArrayList<Point>();
		double area = 0.0;
		int DPI = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		for (Line l : polygon) {
			if (!points.contains(l.getP1())) {
				points.add(l.getP1());
			}
			if (!points.contains(l.getP2())) {
				points.add(l.getP2());
			}
		}
		double[] x = new double[points.size()];
		double[] y = new double[points.size()];
		for (int j = 0; j < points.size(); j++) {
			x[j] = ((points.get(j).getOriginalX() * 2.54) / DPI);
			y[j] = ((points.get(j).getOriginalY() * 2.54) / DPI);
//			x[j] = (points.get(j).getOriginalX());
//			y[j] = (points.get(j).getOriginalY());
		}

		for (int i = 0; i < points.size(); i++) {
			area = area + (y[i] + y[(i + 1) % points.size()]) * (x[i] - x[(i + 1) % points.size()]);
		}
		area = Math.abs(area);
		return area == 0.0 ? area : area / 2;
	}
}
