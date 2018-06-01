package at.tugraz.igi.ui;

import lombok.*;

import org.apache.commons.lang3.tuple.*;
import javafx.scene.transform.Affine;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import at.tugraz.igi.main.Controller;
import at.tugraz.igi.util.*;
import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Point;

public class GraphicPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private Controller controller;

	private JLabel currentEvent;
	private boolean finished;

	private Image image;
	private Graphics2D graphics;
	@Getter @Setter private Double min_x, min_y, max_x, max_y; //coors_scale;
	// private JPopupMenu popup;

	@Getter private List<Triangle> screenTriangles;
	private List<Line> lines;
	// public List<Line> polyLines;
	private Set<Point> points;

	public java.awt.Point p1;
	public java.awt.Point p2;
	public Point point1;

	// Style
	private final static AlphaComposite OVER_HALF = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
	private final static BasicStroke basic = new BasicStroke(2);
	private final static float dash1[] = { 10.0f };
	private final static BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
			10.0f, dash1, 0.0f);

	public GraphicPanel(final Controller controller) {
		addMouseListener(controller.createMouseListener());
		addMouseMotionListener(controller.createMMListener());
		this.controller = controller;
		this.setLayout(null);
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		this.setFont(new Font(this.getFont().getFontName(), Font.ITALIC, 14));
		this.setBackground(Color.white);
	}

	public void init(List<Line> lines, Set<Point> points) {
		this.lines = lines;
		this.points = points;
		// this.polyLines = polyLines;

	}

	// public void showPopUp(MouseEvent e) { popup.show(this, e.getX(), e.getY()); }

	public void update(Graphics g) {
		if (image == null) {
			image = createImage(this.getWidth(), this.getHeight());
			graphics = (Graphics2D) image.getGraphics();
		}
		graphics.setColor(getBackground());

		graphics.fillRect(this.getX(), this.getY(), this.getWidth(), this.getHeight());
		graphics.setColor(getForeground());

		paint(graphics);
		g.drawImage(image, this.getX(), this.getY(), this);
	}

	public void setCoordinatesBounds(Double min_x, Double min_y,
									 Double max_x, Double max_y) {
		this.min_x = min_x;
		this.min_y = min_y;
		this.max_x = max_x;
		this.max_y = max_y;
	}

	private double getScale() {
		if (min_x == null)
			return 1.0;
		return Math.min((getWidth()-40)/ (max_x - min_x),
				        (getHeight()-40)/ (max_y - min_y));
	}

	public Pair<AffineTransform, AffineTransform> getCoordinatesTransform() {
		if (min_x == null)
			return null;
		val scale = getScale();
		val tx = new AffineTransform(
				scale, 0, 0, scale,
				20 + getX() - min_x * scale,
				20 + getY() - min_y * scale
		);
		AffineTransform inv;
		try {
			inv = tx.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
			return null;
		}
		return new ImmutablePair<>(tx, inv);
	}

	public Point2D transformCoordinates(double x, double y, boolean toScreen) {
		val loc = new Point2D.Double(x, y);
		val zooms = getCoordinatesTransform();
		if (zooms == null)
			return loc;
		AffineTransform zoom;
		if (toScreen)
			zoom = zooms.getLeft();
		else
			zoom = zooms.getRight();
		if (zoom != null)
			zoom.transform(loc, loc);
		return loc;
	}

	public void paintSVG(SVGGraphics2D g2) {
		// super.paintComponent(g);
		// Graphics2D g2 = (Graphics2D) g;
		// g2.setColor(Color.WHITE);
		// g2.fillRect(this.getX(), this.getY(), getWidth(), getHeight());
		// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);
		// g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		// RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		// g2.setStroke(basic);
		if (controller.getPolygons() != null) {
			g2.setStroke(new BasicStroke(1));
			g2.setColor(Color.GREEN);

			paintTriangles(g2);

			paintMovedPoints(g2);
		}

		paintStraightSkeletons(g2);

		// for (Point mp : movedPoints.keySet()) {
		// drawPoint(g2, mp, Color.LIGHT_GRAY);
		// }

		g2.setStroke(basic);
		g2.setColor(Color.BLACK);

		if (p1 != null && p2 != null) {
			g2.drawLine(p1.x, p1.y, p2.x, p2.y);
		}

		g2.setColor(Color.BLACK);
		g2.setFont(new Font(this.getFont().getName(), Font.ITALIC, 14));

		if (controller.getPolyLines().size() == 0 && point1 != null) {
			g2.setStroke(new BasicStroke(1));
			drawPoint(g2, point1, Color.BLACK);
		}

		g2.setFont(new Font(this.getFont().getName(), Font.ITALIC, 14));
		g2.drawString("Drawing area", 20, 20);

		paintPolygon(g2);

		if (currentEvent != null) {
			g2.setFont(new Font("default", Font.BOLD, 14));
			g2.drawString(currentEvent.getText(), currentEvent.getLocation().x, currentEvent.getLocation().y);
		}
		for (int j = 0; j < this.getComponentCount(); j++) {
			CustomTextField field = ((CustomTextField) this.getComponents()[j]);
			String text = field.getText();
			Line line = field.getLine();
//			g2.drawRect(field.getBounds().x, field.getBounds().y, field.getBounds().width, field.getBounds().height);
//			int x = (field.getBounds().x+ field.getBounds().width);
			int x = (field.getBounds().x);
			int y = (field.getBounds().y+ field.getBounds().height);
			g2.drawString(text, x, y);
		}
		g2.setColor(Color.BLACK);
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		super.paintComponent(g);
		g2.setColor(Color.WHITE);
		g2.fillRect(this.getX(), this.getY(), getWidth(), getHeight());
		AffineTransform zoomIn = null;
		AffineTransform zoomOut = null;
		val ts = getCoordinatesTransform();
		if (ts != null) {
			zoomIn = ts.getLeft();
			zoomOut = ts.getRight();
		}
		if (zoomIn != null) g2.transform(zoomIn);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setStroke(basic);

		if (controller.getPolygons() != null) {
			g2.setStroke(new BasicStroke(1));
			g2.setColor(Color.GREEN);

			paintTriangles(g2);

			paintMovedPoints(g2);
		}

		boolean show = paintStraightSkeletons(g2);

		// for (Point mp : movedPoints.keySet()) {
		// drawPoint(g2, mp, Color.LIGHT_GRAY);
		// }

		g2.setStroke(basic);
		g2.setColor(Color.BLACK);

		if (p1 != null && p2 != null) {
			g2.drawLine(p1.x, p1.y, p2.x, p2.y);
		}

		g2.setColor(Color.BLACK);
		g2.setFont(new Font(this.getFont().getName(), Font.ITALIC, 14));

		if (controller.getPolyLines().size() == 0 && point1 != null) {
			g2.setStroke(new BasicStroke(1));
			drawPoint(g2, point1, Color.BLACK);
		}

		g2.setFont(new Font(this.getFont().getName(), Font.ITALIC, 14));
		g2.drawString("Drawing area", 20, 20);

		if(show){
			paintPolygon(g2);
		}
		if (currentEvent != null) {
			g2.setFont(new Font("default", Font.BOLD, 14));
			g2.setFont(new Font("default", Font.BOLD, (int) Math.ceil(14 / getScale())));
			g2.drawString(currentEvent.getText(), currentEvent.getLocation().x, currentEvent.getLocation().y);
		}
		g2.setColor(Color.BLACK);

		for (val c: getComponents())
			if (c instanceof CustomTextField)
				((CustomTextField) c).updatePosition(
						controller.isClosed() ? Util.isCounterClockwise(new ArrayList<Point>(points)) : false);

		if (zoomOut != null) g2.transform(zoomOut);

	}

	private void paintMovedPoints(Graphics2D g2) {
		if (controller.isAnimation() || controller.isBrowsingHistory()) {
			for (Set<Point> points : controller.getPolygons()) {
				for (Point p : new ArrayList<Point>(points)) {
					Line l = null;
					for (Line lin : p.adjacentLines) {
						if (lin.getP2().equals(p)) {
							l = lin;
							break;
						}
					}
					if (l == null) {
						return;
					}
					StraightSkeleton straightSkeleton = controller.getStraightSkeleton();
					if (straightSkeleton != null && !straightSkeleton.contains(l)) {
						Point p1 = l.getP1();
						Point p2 = l.getP2();

						g2.setColor(straightSkeleton.getColor());
						g2.drawLine((int) p1.getOriginalX(), (int) p1.getOriginalY(), (int) p1.current_x,
								(int) p1.current_y);

						g2.setColor(Color.BLACK);

						g2.drawLine((int) p1.current_x, (int) p1.current_y, (int) p2.current_x, (int) p2.current_y);

					}
				}
			}
		}
	}

	private void paintTriangles(Graphics2D g2) {
		if (screenTriangles != null) {
			for (Triangle t : new ArrayList<Triangle>(screenTriangles)) {
				for (Line s : t.strokes) {
					g2.drawLine((int) s.getP1().current_x, (int) s.getP1().current_y, (int) s.getP2().current_x,
							(int) s.getP2().current_y);

				}
			}
			g2.setStroke(basic);
			g2.setColor(Color.BLACK);
		}
	}

	private void paintPolygon(Graphics2D g2) {
		for (int i = 0; i < controller.getPolyLines().size(); i++) {
			// if (editMode) {
			// g2.setStroke(dashed);
			// g2.setColor(Color.LIGHT_GRAY);
			// } else {
			g2.setColor(Color.BLACK);
			// }

			Line line = controller.getPolyLines().get(i);
			Point p1 = line.getP1();
			Point p2 = line.getP2();

			if (finished) {
				g2.setColor(Color.LIGHT_GRAY);
			}
			g2.drawLine((int) p1.getOriginalX(), (int) p1.getOriginalY(), (int) p2.getOriginalX(),
					(int) p2.getOriginalY());

			g2.setStroke(new BasicStroke(1));

			Color color;
			if (finished) {
				color = Color.LIGHT_GRAY;
			} else {
				color = Color.BLACK;
			}
			drawPoint(g2, p1, color);

			drawPoint(g2, p2, color);

			g2.setStroke(basic);

			for (int j = 0; j < this.getComponentCount(); j++) {
				if (finished) {
					this.getComponents()[j].setForeground(Color.LIGHT_GRAY);

				} else {
					this.getComponents()[j].setForeground(Color.BLACK);
				}
			}

		}
	}

	private boolean paintStraightSkeletons(Graphics2D g2) {
		List<StraightSkeleton> skeletons = new ArrayList<StraightSkeleton>(controller.getStraightSkeletons());
		if (!skeletons.isEmpty() && controller.getStraightSkeleton() != null) {
			skeletons.remove(controller.getStraightSkeleton());
			skeletons.add(controller.getStraightSkeleton());
		}
		for (StraightSkeleton skeleton : skeletons) {
			if (!skeleton.isVisible()) {
				continue;
			}
			
			Color[] colors = { Color.DARK_GRAY, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN, Color.MAGENTA,
					Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW };
			if (skeleton.polygon != null && skeleton.polygon.size() > 0) {
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
				
				int i=0;
				for(List<Line> poly: skeleton.polygon.keySet()){
					List<Point> points = new ArrayList<Point>();
					for (Line l : poly) {
						if(!points.contains(l.getP1())){
							points.add(l.getP1());
						}
						if(!points.contains(l.getP2())){
							points.add(l.getP2());
						}
					}
						g2.setColor(colors[i%10]);
						int[]x = new int[points.size()];
						int[]y = new int[points.size()];
						for(int j=0; j<points.size(); j++){
							x[j] = (int) points.get(j).getOriginalX();
							y[j] = (int) points.get(j).getOriginalY();
						}
						g2.fillPolygon(x, y, points.size());
						
						
					i++;
				}
			
//				return false;
			}
				g2.setComposite(AlphaComposite.Src);
				if (!skeleton.equals(controller.getStraightSkeleton()) && !controller.finished) {
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
				}
				for (Line l : new ArrayList<Line>(skeleton.getLines())) {
					g2.setColor(skeleton.getColor());
					g2.setStroke(basic);

					drawLine(g2, l);
				}

				g2.setComposite(AlphaComposite.Src);
				for (Line l : skeleton.getPolyLines()) {
					if (l.polyLine) {
						g2.setStroke(dashed);
						g2.setColor(Color.LIGHT_GRAY);
					} else {
						g2.setColor(Color.BLACK);
						g2.setStroke(basic);
					}
					drawLine(g2, l);
				}
				g2.setStroke(new BasicStroke(1));
				for (Line l : skeleton.getPolyLines()) {
					drawPoint(g2, l.getP2(), Color.LIGHT_GRAY);
				}
			}
		

		g2.setStroke(new BasicStroke(1));
		return true;
	}

	private void drawZoomedLine(Graphics2D g2, double x1, double y1, double x2, double y2) {
		val p1 = transformCoordinates(x1, y1, true);
		val p2 = transformCoordinates(x2, y2, true);

		val zooms = getCoordinatesTransform();
		val zoomIn = zooms.getLeft();
		val zoomOut = zooms.getRight();

		if (zoomIn != null)
			g2.transform(zoomOut);
		g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
		if (zoomOut != null)
			g2.transform(zoomOut);

	}

	private void drawLine(Graphics2D g2, Line l) {
	    val p1 = l.getP1();
		val p2 = l.getP2();

		// drawZoomedLine(g2, p1.getOriginalX(), p1.getOriginalY(), p2.getOriginalX(), p2.getOriginalY());
		g2.drawLine((int) p1.getOriginalX(), (int) p1.getOriginalY(), (int) p2.getOriginalX(), (int) p2.getOriginalY());
	}

	private void drawPoint(Graphics2D g2, Point p, Color color) {
	    val loc = new Point2D.Double(p.getOriginalX(), p.getOriginalY());
		Pair<AffineTransform, AffineTransform> zooms = getCoordinatesTransform();

		if (zooms == null) {
			setCoordinatesBounds(getX()+20.0, getY()+20.0, getX() + getWidth() - 20.0, getY() + getHeight() - 20.0);
			zooms = getCoordinatesTransform();
		}

		val zoomIn = zooms.getLeft();
		val zoomOut = zooms.getRight();
		if (zoomIn != null)
			zoomIn.transform(loc, loc);

		g2.transform(zoomOut);

		g2.setColor(Color.WHITE);
		g2.fillOval((int) loc.getX() - 7, (int) loc.getY() - 7, 14, 14);
		g2.setColor(color);
		g2.drawOval((int) loc.getX() - 7, (int) loc.getY() - 7, 14, 14);
		int width = g2.getFontMetrics().stringWidth(p.getNumberAsString());
		g2.drawString(p.getNumberAsString(), (int) loc.getX() - width / 2, (int) loc.getY() + 6);

		g2.transform(zoomIn);

	    /*
		g2.setColor(Color.WHITE);
		g2.fillOval((int) p.getOriginalX() - (int) (7.0/getScale()), (int) p.getOriginalY() - (int) (7.0/getScale()), (int) (14.0/getScale()), (int) (14.0/getScale()));
		g2.setColor(color);
		g2.drawOval((int) p.getOriginalX() - (int) (7.0/getScale()), (int) p.getOriginalY() - (int) (7.0/getScale()), (int) (14.0/getScale()), (int) (14.0/getScale()));
		int width = g2.getFontMetrics().stringWidth(p.getNumberAsString());
		g2.drawString(p.getNumberAsString(), (int) p.getOriginalX() - width / 2, (int) p.getOriginalY() + (int) (6.0/getScale()));
		*/
	    /*
		g2.setColor(Color.WHITE);
		g2.fillOval((int) p.getOriginalX() - 7, (int) p.getOriginalY() - 7, 14, 14);
		g2.setColor(color);
		g2.drawOval((int) p.getOriginalX() - 7, (int) p.getOriginalY() - 7, 14, 14);
		int width = g2.getFontMetrics().stringWidth(p.getNumberAsString());
		g2.drawString(p.getNumberAsString(), (int) p.getOriginalX() - width / 2, (int) p.getOriginalY() + 6);
		*/
	}

	public void reset() {
		this.removeAll();
		point1 = null;
		p1 = null;
		p2 = null;

		min_x = null;
		min_y = null;
		max_x = null;
		max_y = null;
		// coors_scale = 1.0;

		EventCalculation.vertex_counter = 1;
		screenTriangles = new ArrayList<Triangle>();
		finished = false;
		currentEvent = null;
		// revalidate();
	}

	public Set<Point> getPoints() {
		return points;
	}

	// public List<Line> getScreenLines() {
	// return polyLines;
	// }

	public List<Line> getLines() {
		return lines;
	}

	// public void restart(List<Line> polyLines) {
	// finished = false;
	// points.clear();
	// lines.clear();
	// // alreadyMovedPts.clear();
	// Point p1;
	// Point p2;
	// if (polyLines == null) {
	// polyLines = this.polyLines;
	// }
	//
	// Line l1 = polyLines.get(0);
	// p1 = new Point(l1.getP1().getNumber(), l1.getP1().getOriginalX(),
	// l1.getP1().getOriginalY());
	// points.add(p1);
	// for (Line l : polyLines) {
	// if (l.getP2().getNumber() != 1) {
	// p2 = new Point(l.getP2().getNumber(), l.getP2().getOriginalX(),
	// l.getP2().getOriginalY());
	// } else {
	// p2 = (Point) points.toArray()[0];
	// }
	// Line line = new Line(p1, p2, l.getWeight());
	// p1.adjacentLines.add(line);
	// p2.adjacentLines.add(line);
	// // p1.addLine(line);
	// // p2.addLine(line);
	// points.add(p2);
	// lines.add(line);
	// p1 = p2;
	// }
	//
	// for (int j = 0; j < this.getComponentCount(); j++) {
	// this.getComponents()[j].setForeground(Color.BLACK);
	// }
	// }

	public void repositionTextfields() {
		for (int j = 0; j < this.getComponentCount(); j++) {
			((CustomTextField) this.getComponents()[j]).reposition();
		}
		repaint();
	}

	// public void createTestData() {
	// // 1;542.0;85.0-2;113.0;83.0-5
	// // 2;113.0;83.0-3;236.0;159.0-1
	// // 3;236.0;159.0-4;133.0;247.0-8
	// // 4;133.0;247.0-5;231.0;372.0-7
	// // 5;231.0;372.0-6;341.0;294.0-9
	// // 6;341.0;294.0-7;426.0;336.0-2
	// // 7;426.0;336.0-8;461.0;213.0-4
	// // 8;461.0;213.0-9;551.0;232.0-3
	// // 9;551.0;232.0-10;580.0;106.0-3
	// // 10;580.0;106.0-1;542.0;85.0-5
	//
	// Point p1 = new Point(1, 542.0, 85.0);
	// points.add(p1);
	// Point p2 = new Point(2, 113.0, 83.0);
	// points.add(p2);
	// Line line = new Line(p1, p2, 5);
	//
	// Point screenPoint1 = new Point(1, 542.0, 85.0);
	// points.add(p1);
	// Point screenPoint2 = new Point(2, 113.0, 83.0);
	// Line screenLine = new Line(screenPoint1, screenPoint2, 5);
	// screenLines.add(screenLine);
	// lines.add(line);
	// CustomTextField field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// p2 = new Point(3, 236.0, 159.0);
	// screenPoint2 = new Point(3, 236.0, 159.0);
	// points.add(p2);
	// line = new Line(p1, p2, 1);
	// screenLine = new Line(screenPoint1, screenPoint2, 1);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// p2 = new Point(4, 133.0, 247.0);
	// screenPoint2 = new Point(4, 133.0, 247.0);
	// points.add(p2);
	// line = new Line(p1, p2, 8);
	// screenLine = new Line(screenPoint1, screenPoint2, 8);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// p2 = new Point(5, 231.0, 372.0);
	// screenPoint2 = new Point(5, 231.0, 372.0);
	// points.add(p2);
	// line = new Line(p1, p2, 7);
	// screenLine = new Line(screenPoint1, screenPoint2, 7);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// p2 = new Point(6, 341.0, 294.0);
	// screenPoint2 = new Point(6, 341.0, 294.0);
	// points.add(p2);
	// line = new Line(p1, p2, 9);
	// screenLine = new Line(screenPoint1, screenPoint2, 9);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// p2 = new Point(7, 426.0, 336.0);
	// screenPoint2 = new Point(7, 426.0, 336.0);
	// points.add(p2);
	// line = new Line(p1, p2, 2);
	// screenLine = new Line(screenPoint1, screenPoint2, 2);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// p2 = new Point(8, 461.0, 213.0);
	// screenPoint2 = new Point(8, 461.0, 213.0);
	// points.add(p2);
	// line = new Line(p1, p2, 4);
	// screenLine = new Line(screenPoint1, screenPoint2, 4);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	// // 10;580.0;106.0-1;542.0;85.0-5
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// p2 = new Point(9, 551.0, 232.0);
	// screenPoint2 = new Point(9, 551.0, 232.0);
	// points.add(p2);
	// line = new Line(p1, p2, 3);
	// screenLine = new Line(screenPoint1, screenPoint2, 3);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// p2 = new Point(10, 580.0, 106.0);
	// screenPoint2 = new Point(10, 580.0, 106.0);
	// points.add(p2);
	// line = new Line(p1, p2, 3);
	// screenLine = new Line(screenPoint1, screenPoint2, 3);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// p1 = p2;
	// screenPoint1 = screenPoint2;
	// line = new Line(p1, lines.get(0).getP1(), 5);
	// screenLine = new Line(screenPoint1, screenLines.get(0).getP1(), 5);
	// screenLines.add(screenLine);
	// lines.add(line);
	// field = new CustomTextField(line, screenLine, this);
	//
	// this.add(field);
	// this.repaint();
	// this.revalidate();
	//
	// }

	public void setCurrentData(List<StraightSkeleton> straightSkeletons, StraightSkeleton straightSkeleton,
			List<Triangle> screenTriangles) {
		// this.straightSkeletons = straightSkeletons;
		// this.straightSkeleton = straightSkeleton;
		this.screenTriangles = screenTriangles;

	}

	// public void setPolygons(List<Set<Point>> polygons) {
	// this.polygons = polygons;
	// }

	// public void setEnabled(boolean enabled) {
	// this.enabled = enabled;
	// }
	//
	// public void setClosed(boolean closed) {
	// this.closed = closed;
	// }

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public void setCurrentEvent(JLabel currentEvent) {
		this.currentEvent = currentEvent;
	}

}
