package at.tugraz.igi.algorithm;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import at.tugraz.igi.events.ConcaveEvent;
import at.tugraz.igi.events.EdgeEvent;
import at.tugraz.igi.events.Event;
import at.tugraz.igi.events.EventComparator;
import at.tugraz.igi.events.FlipEvent;
import at.tugraz.igi.events.SplitEvent;
import at.tugraz.igi.main.Controller;
import at.tugraz.igi.util.EventCalculation;
import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Point;
import at.tugraz.igi.util.PolygonMeasureData;
import at.tugraz.igi.util.StraightSkeleton;
import at.tugraz.igi.util.Triangle;
import at.tugraz.igi.util.Util;
import at.tugraz.igi.util.Vector;

public class SimpleAlgorithmNoSwingWorker {
	private Color[] colors = { Color.BLUE, new Color(135, 206, 255), new Color(50, 205, 50), new Color(0, 100, 0),
			new Color(255, 0, 0), new Color(255, 127, 80), new Color(205, 51, 51) };
	private PriorityQueue<Event> events = new PriorityQueue<Event>(1, new EventComparator());
	private List<Triangle> triangles = new ArrayList<Triangle>();
	private StraightSkeleton straightSkeleton;

	private Event event;
	public Point i;
	private Triangle t;
	private Line line;
	private Point p1;
	private Point p2;
	public Point reflexP;

	private List<List<Line>> lines;
	private List<Point> points;
	private Controller controller;
	private int numberOfFlip;
	private int numberOfEdge;
	private int numberOfSplit;
	private double maxFlips;

	public SimpleAlgorithmNoSwingWorker(List<Point> pts, List<Line> l, Controller c) throws CloneNotSupportedException {
		numberOfFlip = 0;
		numberOfEdge = 0;
		numberOfSplit= 0;
		lines = new ArrayList<List<Line>>();
		lines.add(l);
		points = pts;
		maxFlips = Math.pow(points.size(), 3);
		controller = c;
		if (controller.getStraightSkeletons().size() == 0 || EventCalculation.skeleton_counter >= colors.length) {
			EventCalculation.skeleton_counter = 0;
		}
		straightSkeleton = controller.getStraightSkeleton();
		if (straightSkeleton == null) {
			straightSkeleton = new StraightSkeleton();
			controller.addStraightSkeleton(straightSkeleton);
			straightSkeleton.setColor(colors[EventCalculation.skeleton_counter]);
			EventCalculation.skeleton_counter++;
		}

		straightSkeleton.setPolyLines(c.createPolyLines());

	}

	public Boolean execute() throws Exception {
//		ThreadMXBean thread = ManagementFactory.getThreadMXBean();
//		long cpu = thread.getCurrentThreadCpuTime();
		long time = System.currentTimeMillis();
		Map<Line, Event> simultaneousEvents = new HashMap<Line, Event>();
        // FIXME: Is this what we want?!
		// EventCalculation.vertex_counter = points.get(points.size() - 1).getNumber() + 1;
		EventCalculation.vertex_counter = points.size() + 1;
		boolean convex = true;
		controller.addPolygon(new HashSet<Point>(points));
		for (Point p : points) {
			p.resetToOriginalPosition();
			p.calculateMovementInfo();
			if (!p.isConvex()) {
				convex = false;
			}
		}

		ArrayList<Point> copyPoints = new ArrayList<Point>(points);
		if (convex) {
      triangles = Util.triangulate2(new ArrayList<Point>(copyPoints), lines);
		} else {

      ArrayList<Point> pts = new ArrayList<Point>(copyPoints);
			if (!Controller.isCounterClockwise) {
				Collections.reverse(pts);
			}
			triangles = Util.triangulate(pts, lines);
		}
		calculateEvents();

		boolean eventExists = true;

		while (eventExists) {
			if (events.size() == 0) {
				eventExists = false;
				break;
			}

			event = events.poll();

			Event e = events.peek();
			if (e != null) {
				if (e.getCollapsingTime() - event.getCollapsingTime() <= 1e-12) {
					// FIXME: Shouldn't we check where two events happen before throwing out one of them?
					//        And what about involved entities?  Should those be processed somehow?
					if (event instanceof ConcaveEvent && e instanceof EdgeEvent) {
						event = e;
					} else if (event instanceof FlipEvent) {
						event = e;
					} else {
						// FIXME: Really?
						e.setCollapsingTime(e.getCollapsingTime() - event.getCollapsingTime());
						simultaneousEvents.put(e.getLine().clone(), e);
					}
				}
			}
			i = event.getIntersection();
			t = event.getTriangle();
			line = event.getLine();
			p1 = line.getP1();
			p2 = line.getP2();

			controller.setCurrentEvent(null);
			movePoints(event);

			if (!(event instanceof FlipEvent)) {
				triangles.remove(t);
			}
			if (event instanceof SplitEvent) {
				numberOfSplit++;
				splitEdge();

			} else if (event instanceof EdgeEvent) {
				numberOfEdge++;
				collapseEdge();

			} else if (event instanceof FlipEvent) {
				numberOfFlip++;
				if (numberOfFlip > maxFlips){
					throw new Exception();
				}
				flipEdge();

			}

			calculateEvents();

			for (Set<Point> points : controller.getPolygons()) {
				if (points.size() == 2) {
					Point point = points.iterator().next();
					Line last = point.adjacentLines.get(0);
					straightSkeleton.add(new Line(last.getP1().clone(), last.getP2().clone(), last.getWeight()));
					controller.removePolygon(points);
					break;
				}
			}
			eventExists = true;
			event = null;
		}
		controller.finished = true;
		controller.polyMeasureData = Util.calculatePolygArea(points, straightSkeleton);
		controller.polyMeasureData.addNumberOfEvents(numberOfFlip, numberOfEdge, numberOfSplit);
	
//		cpu = thread.getCurrentThreadCpuTime() - cpu;
		time = System.currentTimeMillis() - time;
//		System.out.println(TimeUnit.MILLISECONDS.convert(cpu, TimeUnit.NANOSECONDS));
		controller.polyMeasureData.runningTime = time;
		return true;
	}

	private void movePoints(Event event) throws InterruptedException {

		double time = event.getCollapsingTime();
		for (Set<Point> points : controller.getPolygons()) {
			for (Point p : points) {
				p.move(time);
			}
		}
//		float roundedTime = (float) Math.floor(time);
//		for (int i = 1; i <= roundedTime; i++) {
//
//			for (Set<Point> points : controller.getPolygons()) {
//				for (Point p : points) {
//					p.move(1);
//				}
//			}
//
//		}
//
//		float remainingTime = (float) (time - roundedTime);
//		for (Set<Point> points : controller.getPolygons()) {
//			for (Point p : points) {
//				p.move(remainingTime);
//			}
//		}

	}

	private void calculateEvents() {
		createEvents();

	}

	private void createEvents() {
		for (Triangle triangle : triangles) {
			try {
				Point p1 = triangle.p1;
				Point p2 = triangle.p2;
				Point p3 = triangle.p3;

				List<Point> reflexPoints = new ArrayList<Point>();
				if (!p1.isConvex()) {
					reflexPoints.add(p1);
				}
				if (!p2.isConvex()) {
					reflexPoints.add(p2);
				}
				if (!p3.isConvex()) {
					reflexPoints.add(p3);
				}

				Vector v1 = p1.getMovementVector();
				Vector v2 = p2.getMovementVector();
				Vector v3 = p3.getMovementVector();

				double p1_x = p1.current_x;
				double p1_y = p1.current_y;
				double p2_x = p2.current_x;
				double p2_y = p2.current_y;
				double p3_x = p3.current_x;
				double p3_y = p3.current_y;

				double bx_ax_c = p2_x - p1_x;
				double cy_ay_c = p3_y - p1_y;
				double cx_ax_c = p3_x - p1_x;
				double by_ay_c = p2_y - p1_y;

				double bx_ax_l = v2.getX() - v1.getX();
				double cy_ay_l = v3.getY() - v1.getY();
				double cx_ax_l = v3.getX() - v1.getX();
				double by_ay_l = v2.getY() - v1.getY();

				double u_c = bx_ax_c * cy_ay_c;
				double u1_l = bx_ax_l * cy_ay_c;
				double u2_l = bx_ax_c * cy_ay_l;
				double u_q = bx_ax_l * cy_ay_l;

				double v_c = cx_ax_c * by_ay_c;
				double v1_l = cx_ax_l * by_ay_c;
				double v2_l = cx_ax_c * by_ay_l;
				double v_q = cx_ax_l * by_ay_l;

				double b = u1_l + u2_l - v1_l - v2_l;
				double c = u_c - v_c;
				double a = u_q - v_q;

				double t1 = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
				double t2 = (-b - (Math.sqrt(b * b - 4 * a * c))) / (2 * a);

				boolean valid1 = !Double.isNaN(t1) && t1 >= 0;
				boolean valid2 = !Double.isNaN(t2) && t2 >= 0;
				double t = 0.0;

				if (valid1 && valid2) {
					t = Math.min(t1, t2);
				} else if (valid1) {
					t = t1;
				} else if (valid2) {
					t = t2;
				} else {
					t = Double.POSITIVE_INFINITY;
					if (triangle.strokes.size() == 0) {
						for (Line l : triangle.polyLines) {
							double collapsingTime = EventCalculation.findEvent(l, triangle);
							if (collapsingTime < t) {
								t = collapsingTime;
							}
						}
					}
					if (t == Double.POSITIVE_INFINITY) {
						continue;
					}
				}
				double epsilon = 1e-5;

				if (reflexPoints != null) {
					// is Split Event?
					for (Line line : triangle.polyLines) {
						for (Point reflex : reflexPoints) {
							if (!line.contains(reflex)) {
								Point i = validEvent(reflex, t, epsilon, line);
								if (i != null) {
									events.add(new SplitEvent(t, triangle, i, reflex, line));
								}
							}
						}
					}
					// is Flip Event?
					for (Line line : triangle.strokes) {
						for (Point reflex : reflexPoints) {
							if (!line.contains(reflex)) {
								Point i = validEvent(reflex, t, epsilon, line);
								if (i != null) {
									events.add(new FlipEvent(t, triangle, i, reflex, line));
								}
							}
						}
					}
				}
				// is Edge Event?

				for (Line line : triangle.polyLines) {
					Point p_1 = line.getP1();
					Vector v_1 = p_1.getMovementVector();
					Point p_2 = line.getP2();
					Vector v_2 = p_2.getMovementVector();

					double x_1 = p_1.current_x + v_1.getX() * t;
					double x_2 = p_2.current_x + v_2.getX() * t;
					double y_1 = p_1.current_y + v_1.getY() * t;
					double y_2 = p_2.current_y + v_2.getY() * t;

					if (Math.abs(x_1 - x_2) <= epsilon && Math.abs(y_1 - y_2) <= epsilon) {
						events.add(new EdgeEvent(t, triangle, new Point(EventCalculation.vertex_counter, x_1, y_1),
								line));
						EventCalculation.vertex_counter++;
						break;
					}

				}
			} catch (Exception e) {
          throw e;
			}
		}

	}

	private Point validEvent(Point reflex, double t, double epsilon, Line line) {

		Point i = new Point(EventCalculation.vertex_counter, reflex.current_x + t * reflex.getMovementVector().getX(),
				reflex.current_y + t * reflex.getMovementVector().getY());
		EventCalculation.vertex_counter++;
		Point i2 = new Point(0, line.getP1().current_x + t * line.getP1().getMovementVector().getX(),
				line.getP1().current_y + t * line.getP1().getMovementVector().getY());
		Point i3 = new Point(0, line.getP2().current_x + t * line.getP2().getMovementVector().getX(),
				line.getP2().current_y + t * line.getP2().getMovementVector().getY());

		boolean diff_x = (Math.abs(i2.current_x - i.current_x) <= epsilon && (Math.abs(i3.current_x - i.current_x) <= epsilon));
		boolean diff_y = (Math.abs(i2.current_y - i.current_y) <= epsilon && (Math.abs(i3.current_y - i.current_y) <= epsilon));

		boolean x = Math.min(i2.current_x, i3.current_x) <= i.current_x
				&& i.current_x <= Math.max(i2.current_x, i3.current_x) + epsilon;
		boolean y = Math.min(i2.current_y, i3.current_y) <= i.current_y
				&& i.current_y <= Math.max(i2.current_y, i3.current_y) + epsilon;

		if ((x || diff_x) && (y || diff_y)) {
			return i;
		}
		return null;
	}

	private void flipEdge() {

		FlipEvent ev = (FlipEvent) event;
		Point reflex = ev.getReflexVertex();
		i = reflex;
		ev.setIntersection(i);

		updatePoints(reflex);
		ev.updateTriangles(triangles);
		events.clear();

	}

	private boolean collinear(Point p1, Point p2, Point p3) {
		double det = Math.abs((p2.current_x - p1.current_x) * (p3.current_y - p1.current_y)
				- (p3.current_x - p1.current_x) * (p2.current_y - p1.current_y));
		return det == 0.0;
	}

	private void collapseEdge() throws CloneNotSupportedException {
		Line l1 = Util.getOtherAdjacentLine(line, p1);
		Line l2 = Util.getOtherAdjacentLine(line, p2);

		Point o1 = Util.getOtherPointOfLine(p1, l1);
		Point o2 = Util.getOtherPointOfLine(p2, l2);

		boolean equal = o1.equals(o2);
		if (collinear(o1, i, o2) && !equal) {
			Point keepPoint;
			Point replacePoint;
			Line keepLine;
			Line replaceLine;
			if (l1.getWeight() < l2.getWeight()) {
				keepPoint = l1.getP1();
				replacePoint = l2.getP1();
				keepLine = l2;
				replaceLine = l1;
			} else {
				keepPoint = l2.getP2();
				replacePoint = l1.getP2();
				keepLine = l1;
				replaceLine = l2;
			}

			Point oldP = keepPoint.clone();
			keepPoint.setBothXCoordinates(keepPoint.current_x);
			keepPoint.setBothYCoordinates(keepPoint.current_y);

			keepLine.replacePoint(replacePoint, null, keepPoint);
			int index = keepPoint.adjacentLines.indexOf(replaceLine);
			keepPoint.adjacentLines.remove(index);
			keepPoint.adjacentLines.add(index, keepLine);

			lines.remove(l1);

			updatePoints(false);

			List<Triangle> rT = new ArrayList<Triangle>();
			for (Triangle t : triangles) {
				if (t.contains(replaceLine)) {
					rT.add(t);
					continue;
				}
				t.replacePoint(p1, p2, keepPoint);

			}
			triangles.removeAll(rT);

			// keepLine.getP1().calculateMovementInfo();
			// keepLine.getP2().calculateMovementInfo();
			//
			// o1.calculateMovementInfo();
			// o2.calculateMovementInfo();

			straightSkeleton.add(new Line(keepPoint, i, 1));
			straightSkeleton.add(new Line(oldP, keepPoint, 1));
		} else

		{
			o1.updateAdjacentLines(p1, i);
			o2.updateAdjacentLines(p2, i);

			// o1.calculateMovementInfo();
			// o2.calculateMovementInfo();

			l1.replacePoint(p1, null, i);
			l2.replacePoint(p2, null, i);

			i.adjacentLines.add(l1);
			i.adjacentLines.add(l2);

			i.calculateMovementInfo();

			updatePoints(true);

			event.updateTriangles(triangles);

		}
		straightSkeleton.add(new Line(p1.clone(), i.clone(), 1));
		straightSkeleton.add(new Line(p2.clone(), i.clone(), 1));
		events.clear();
		// events.removeAll(events);

	}

	private void splitEdge() throws Exception {
		
		SplitEvent ev = (SplitEvent) event;
		reflexP = ev.getReflexVertex();
		Point copyi = Util.clonePoint(i, EventCalculation.vertex_counter++);

		Map<Point, Set<Point>> splitLines = ev.splitPolygons(controller.getPolygons(), event, copyi);

		copyi.calculateMovementInfo();
		// Util.getOtherPointOfLine(copyi,
		// copyi.adjacentLines.get(0)).calculateMovementInfo();
		// Util.getOtherPointOfLine(copyi,
		// copyi.adjacentLines.get(1)).calculateMovementInfo();

		i.calculateMovementInfo();
		// Util.getOtherPointOfLine(i,
		// i.adjacentLines.get(0)).calculateMovementInfo();
		// Util.getOtherPointOfLine(i,
		// i.adjacentLines.get(1)).calculateMovementInfo();

		ev.updateTriangles(splitLines, triangles);
		events.clear();
		Line l = new Line(i.clone(), reflexP.clone(), 1);
		straightSkeleton.add(l);
		straightSkeleton.add(new Line(i.clone(), copyi.clone(), 1));

		straightSkeleton.add(new Line(i.clone(), reflexP.clone(), 1));

	}

	private void updatePoints(boolean add) {

		for (Set<Point> points : controller.getPolygons()) {
			if (points.remove(p1)) {
				points.remove(p2);
				if (add)
					points.add(i);
				break;
			}
		}
	}

	private void updatePoints(Point rP) {
		for (Line l : rP.adjacentLines) {
			l.replacePoint(rP, null, i);
		}

	}

}
