package at.tugraz.igi.main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.SwingWorker.StateValue;

import at.tugraz.igi.algorithm.SimpleAlgorithm;
import at.tugraz.igi.algorithm.SimpleAlgorithmNoSwingWorker;
import at.tugraz.igi.ui.ConfigurationTable;
import at.tugraz.igi.ui.CustomTextField;
import at.tugraz.igi.ui.GraphicPanel;
import at.tugraz.igi.util.EventCalculation;
import at.tugraz.igi.util.FileHandler;
import at.tugraz.igi.util.Line;
import at.tugraz.igi.util.Point;
import at.tugraz.igi.util.PolygonMeasureData;
import at.tugraz.igi.util.StraightSkeleton;
import at.tugraz.igi.util.Triangle;
import at.tugraz.igi.util.Util;
import data.Graph;

public class Controller {
	public static enum TYPES {
		OPEN, SAVE, SAVE_AS, PLAY, STEP, RESET, OPEN_POLY, SVG
	}

	public static boolean isCounterClockwise;
	public static ImageIcon delete_icon;
	public static ImageIcon play_icon;
	public static ImageIcon color_icon;
	public static ImageIcon step_icon;
	public static ImageIcon reset_icon;
	public static ImageIcon edit_icon;
	public static ImageIcon visible_icon;
	public static ImageIcon not_visible_icon;
	private boolean isRunning;
	private boolean restart;
	private boolean move;

	private boolean nextStep = true;
	private boolean step;
	private boolean animation;
	private boolean enabled = true;
	private boolean closed;
	private boolean editMode;
	public boolean randomWeights;
	public boolean finished = true;
	public PolygonMeasureData polyMeasureData;

	public GraphicPanel view;
	private SimpleAlgorithm algo;
	private ConfigurationTable table;

	private List<StraightSkeleton> straightSkeletons;
	private StraightSkeleton straightSkeleton;

	private Point point1;
	private Point point2;
	private Point screenPoint1;
	private boolean initialize;
	private boolean init_drag;

	private Point sdragPoint;
	private Point dragPoint;

	private Random randomGenerator = new Random();

	private Map<Point, List<StraightSkeleton>> movedPoints;
	private List<Line> lines;
	public List<Line> polyLines;
	private Set<Point> points;
	private List<Set<Point>> polygons;

	public Controller() {
		this.lines = new ArrayList<Line>();
		this.points = new LinkedHashSet<Point>();
		this.polyLines = new ArrayList<Line>();
		this.movedPoints = new HashMap<Point, List<StraightSkeleton>>();
		delete_icon = getImage("delete");
		play_icon = getImage("play");
		color_icon = getImage("color");
		step_icon = getImage("step");
		reset_icon = getImage("reset");
		edit_icon = getImage("edit");
		visible_icon = getImage("eye");
		not_visible_icon = getImage("eye_blocked");
	}

	public CommonListener createActionListener(TYPES type) {
		return new CommonListener(type, this);
	}

	public CommonMouseMotionListener createMMListener() {
		return new CommonMouseMotionListener();
	}

	public CommonMouseListener createMouseListener() {
		return new CommonMouseListener(this);
	}

	public ItemListener createItemListener() {
		return new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					randomWeights = true;
				} else {
					randomWeights = false;
				}

			}
		};
	}

	public void repaint() {
		view.repaint();
	}

	public void setLoadedData(List<Line> lines, List<Line> screenLines, Set<Point> points) {
		this.lines = lines;
		this.polyLines = screenLines;
		this.points = points;
		view.init(lines, points);
	}

	public void finished() {
		// straightSkeleton.add(lines);
		// polygons = null;
		repaint();
		finished = true;
		restart = true;
		isRunning = false;
		animation = false;
		step = false;
		nextStep = true;
		table.setValueAt(new Boolean(true), straightSkeletons.indexOf(straightSkeleton), 0);
	}

	public void publish(final List<String> chunks, Point i, List<Triangle> triangles) {
		if (!chunks.isEmpty()) {
			Graphics2D g2 = (Graphics2D) view.getGraphics();
			int width = g2.getFontMetrics().stringWidth(chunks.get(0));
			JLabel label = new JLabel(chunks.get(0));
			label.setLocation((int) (i.current_x - width / 2), (int) (i.current_y + 20));
			setCurrentEvent(label);

		}
		view.setCurrentData(straightSkeletons, straightSkeleton, triangles);
		view.repaint();
	}

	private ImageIcon getImage(String name) {
		//InputStream imgStream = getClass().getClassLoader().getResourceAsStream("resources/" + name + ".png");
		InputStream imgStream = getClass().getClassLoader().getResourceAsStream(name + ".png");
		ImageIcon icon = null;
		try {
			icon = new ImageIcon(ImageIO.read(imgStream));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return icon;
	}

	public int getSelectedIndex() {
		for (int i = 0; i < table.getRowCount(); i++) {
			boolean value = (boolean) table.getModel().getValueAt(i, 0);
			if (value) {
				return i;
			}

		}
		return -1;
	}

	public void playSelected(int skeleton, boolean animate, boolean recreate) {
		if (skeleton == -1) {
			straightSkeleton = null;
		} else {
			straightSkeleton = straightSkeletons.get(skeleton);
		}
		restart = true;
		animation = animate;

		if (recreate) {
			restart(straightSkeleton.getPolyLines());
		}
		runAlgorithm();
	}

	public void playCurrent() {
		restart = true;
		animation = false;
		this.table.setValueAt(new Boolean(true), straightSkeletons.size() - 1, 0);
		editMode = true;
		restart(null);
		runAlgorithm();
	}

	public void updateSkeleton(int skeleton, boolean editMode) {
		if (editMode) {
			if (!straightSkeletons.get(skeleton).equals(straightSkeleton)) {
				straightSkeleton = straightSkeletons.get(skeleton);
				restart(straightSkeleton.getPolyLines());
			} else {
				restart(null);
			}
		}
		this.editMode = editMode;
	}

	public List<Line> createPolyLines() {
		Point p1;
		Point p2;
		List<Line> polyLines = new ArrayList<Line>();

		Line l1 = lines.get(0);
		Point first = l1.getP1();
		p1 = new Point(l1.getP1().getNumber(), l1.getP1().getOriginalX(), l1.getP1().getOriginalY());
		for (Line li : lines) {
			if (li.getP2().getNumber() != first.getNumber()) {
				p2 = new Point(li.getP2().getNumber(), li.getP2().getOriginalX(), li.getP2().getOriginalY());
			} else {
				p2 = polyLines.get(0).getP1();
			}
			Line line = new Line(p1, p2, li.getWeight());
			p1.adjacentLines.add(line);
			p2.adjacentLines.add(line);

			p1 = p2;

			line.polyLine = true;
			polyLines.add(line);
		}
		return polyLines;
	}

	public void createPolygon(Graph graph) {
		FileHandler.createPoly(view, this, graph);

	}

	public int generateWeight() {
		int weight = randomGenerator.nextInt(10);
		return weight == 0 ? 1 : weight;
	}

	public void runAlgorithm() {
		finished = false;
		isRunning = true;
		ArrayList<Point> points = (ArrayList<Point>) new ArrayList<Point>(this.points);
		if (points.size() > 3) {

			Util.closePolygon(points, lines);
			closed = true;
			isCounterClockwise = Util.isCounterClockwise(points);
			if (algo != null) {
				if (straightSkeleton != null) {
					if (straightSkeletons.size() > 0 && (restart || nextStep) && (!move || editMode)) {
						straightSkeleton.clear();
					} else {
						straightSkeleton = null;
					}
				}
			} else {
				straightSkeletons = new ArrayList<StraightSkeleton>();
			}
			move = false;
			try {
				algo = new SimpleAlgorithm(points, lines, animation, this);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			polygons = new ArrayList<Set<Point>>();
			algo.execute();
		}
	}

	public void runAlgorithmNoSwingWorker() throws Exception {
		finished = false;
		isRunning = true;
		ArrayList<Point> points = (ArrayList<Point>) new ArrayList<Point>(this.points);
		if (points.size() > 3) {

			Util.closePolygon(points, lines);
			closed = true;
			isCounterClockwise = Util.isCounterClockwise(points);
			straightSkeleton = null;
			straightSkeletons = new ArrayList<StraightSkeleton>();
			polyMeasureData = null;
//			try {
				SimpleAlgorithmNoSwingWorker algoNoSwing = new SimpleAlgorithmNoSwingWorker(points, lines, this);
				polygons = new ArrayList<Set<Point>>();
				algoNoSwing.execute();	
//			} catch (CloneNotSupportedException e) {
//				throw new Exception();
//			} catch (Exception e) {
//				throw new Exception();
//				
//			}

		}
	}

	public void restart(List<Line> polyLines) {
		// finished = false;
		points.clear();
		lines.clear();
		// alreadyMovedPts.clear();
		Point p1;
		Point p2;
		// boolean add = false;
		if (polyLines == null) {
			polyLines = this.polyLines;
		} else {
			this.polyLines.clear();
			this.polyLines.addAll(polyLines);
		}
		Line l1 = polyLines.get(0);
		Point first = l1.getP1();
		p1 = new Point(l1.getP1().getNumber(), l1.getP1().getOriginalX(), l1.getP1().getOriginalY());
		points.add(p1);
		int i = 0;
		for (Line l : polyLines) {
			if (l.getP2().getNumber() != first.getNumber()) {
				p2 = new Point(l.getP2().getNumber(), l.getP2().getOriginalX(), l.getP2().getOriginalY());
			} else {
				p2 = points.iterator().next();
			}
			Line line = new Line(p1, p2, l.getWeight());
			p1.adjacentLines.add(line);
			p2.adjacentLines.add(line);
			// p1.addLine(line);
			// p2.addLine(line);
			points.add(p2);
			lines.add(line);

			// CustomTextField field = new CustomTextField(line, l, this);
			// view.add(field);
			CustomTextField field = (CustomTextField) view.getComponents()[i];
			field.getDocument().removeDocumentListener(field);
			field.setLines(line, l);
			i++;
			p1 = p2;

		}

		if (Util.isCounterClockwise(new ArrayList<Point>(view.getPoints()))) {
			view.repositionTextfields();
		}
		for (int j = 0; j < view.getComponentCount(); j++) {
			CustomTextField field = (CustomTextField) view.getComponents()[j];
			field.getDocument().addDocumentListener(field);
		}

		view.init(lines, points);
		view.repaint();

	}

	public void setCurrentEvent(JLabel l) {
		view.setCurrentEvent(l);
	}

	public double getAverage_weight() {
		int sum = 0;
		for (Line l : this.polyLines) {
			sum += l.getWeight();
		}
		return sum / this.polyLines.size();
	}

	public void addStraightSkeleton(StraightSkeleton skeleton) {
		this.straightSkeleton = skeleton;
		straightSkeletons.add(skeleton);
		table.addRow();

	}

	public void removeStraightSkeleton(int skeleton) {
		StraightSkeleton selectedSk = straightSkeletons.get(skeleton);
		straightSkeletons.remove(skeleton);
		table.removeRow(skeleton);
		if (selectedSk.equals(straightSkeleton)) {
			int next = skeleton;
			if (straightSkeletons.size() == skeleton) {
				next = next - 1;
			}
			if (!straightSkeletons.isEmpty()) {
				StraightSkeleton nextSkeleton = straightSkeletons.get(next);
				if (nextSkeleton != null) {
					table.setValueAt(new Boolean(true), straightSkeletons.indexOf(nextSkeleton), 0);
				}
			}
		}
		view.repaint();
	}

	public void addPolygon(Set<Point> pts) {
		if (polygons != null) {
			polygons.add(pts);
		}
	}

	public void removePolygon(Set<Point> pts) {
		polygons.remove(pts);
	}

	public void showColorChooser(int index) {
		StraightSkeleton skeleton = straightSkeletons.get(index);
		Color newColor = JColorChooser.showDialog(view, "Choose Background Color", skeleton.getColor());
		if (newColor != null) {
			skeleton.setColor(newColor);
		}
	}

	public void toggleVisibility(int index, boolean visible) {
		StraightSkeleton skeleton = straightSkeletons.get(index);
		skeleton.setVisible(visible);
		view.repaint();

	}

	public void setTable(ConfigurationTable table) {
		this.table = table;
	}

	public void setView(GraphicPanel view) {
		this.view = view;
		view.init(lines, points);
	}

	public void reset() {
		if (algo != null && algo.getState().equals(StateValue.STARTED)) {
			algo.cancel(true);
		}
		straightSkeleton = null;
		if (straightSkeletons != null) {
			straightSkeletons.clear();
		}
		lines.clear();
		polyLines.clear();
		points.clear();
		view.reset();
		view.init(lines, points);
		view.repaint();
		table.removeAllRows();
		EventCalculation.vertex_counter = 1;
		EventCalculation.skeleton_counter = 0;
		isRunning = false;
		restart = false;
		move = false;
		nextStep = true;
		step = false;
		animation = false;
		enabled = true;
		closed = false;
		initialize = false;
		init_drag = false;
		editMode = false;
		FileHandler.file = null;

	}

	public List<StraightSkeleton> getStraightSkeletons() {
		if (straightSkeletons == null) {
			straightSkeletons = new ArrayList<StraightSkeleton>();
		}
		return straightSkeletons;
	}

	public StraightSkeleton getStraightSkeleton() {
		return straightSkeleton;
	}

	public boolean isMove() {
		return move;
	}

	public boolean isNextStep() {
		return nextStep;
	}

	public void setNextStep(boolean nextStep) {
		this.nextStep = nextStep;
	}

	public boolean isStep() {
		return step;
	}

	public void setPolygons(List<Set<Point>> polygons) {
		this.polygons = polygons;
	}

	public List<Set<Point>> getPolygons() {
		return polygons;
	}

	public boolean isAnimation() {
		return animation;
	}

	class CommonListener implements ActionListener {
		private TYPES type;
		private Controller controller;

		public CommonListener(TYPES t, Controller controller) {
			type = t;
			this.controller = controller;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (type) {
			case OPEN:
				open();
				break;
			case SAVE:
				save(false);
				break;
			case SAVE_AS:
				save(true);
				break;
			case PLAY:
				play();
				break;
			case STEP:
				step();
				break;
			case RESET:
				reset();
				break;
			case SVG:
				saveSVG();
				// case OPEN_POLY:
				// openPoly();
				// break;
			}

		}

		private void open() {
			FileHandler.open(view, controller);
			closed = true;

		}

		// private void openPoly() {
		// FileHandler.openPoly(view, controller);

		// }
		private void save(boolean saveAs) {
			FileHandler.save(controller.polyLines, saveAs);
		}

		private void saveSVG() {
			FileHandler.saveSVG(view, true);

		}

		private void play() {
			if (step) {
				step = false;
				nextStep = true;
			}
			if (restart) {
				step = false;
				nextStep = true;
				restart(null);
				EventCalculation.vertex_counter = view.getPoints().size();
				if(controller.getStraightSkeleton()!= null){
					controller.getStraightSkeleton().polygon = null;
				}
			}
			if (!isRunning) {
				animation = true;
				runAlgorithm();
				view.setEnabled(false);
			}
		}

		private void step() {

			if (restart) {
				EventCalculation.vertex_counter = view.getPoints().size();
				nextStep = true;
				restart(null);
				restart = false;
				EventCalculation.vertex_counter = view.getPoints().size();
				if(controller.getStraightSkeleton()!= null){
					controller.getStraightSkeleton().polygon = null;
				}
			}
			if (!isRunning) {
				animation = true;
				runAlgorithm();
				view.setEnabled(false);
			}
			step = true;
			nextStep = !nextStep;
		}

	}

	class CommonMouseMotionListener implements MouseMotionListener {
		@Override
		public void mouseDragged(MouseEvent e) {
			if (!closed) {
				return;
			}
			move = true;
			// if (dragPoint != null) {
			if (!enabled) {
				enabled = true;
			}
			dragPoint.setBothXCoordinates(e.getPoint().getX());
			dragPoint.setBothYCoordinates(e.getPoint().getY());
			sdragPoint.setBothXCoordinates(e.getPoint().getX());
			sdragPoint.setBothYCoordinates(e.getPoint().getY());

			for (int j = 0; j < view.getComponentCount(); j++) {
				CustomTextField field = ((CustomTextField) view.getComponents()[j]);
				if (field.getScreenLine().contains(sdragPoint)) {
					field.updatePosition(closed ? Util.isCounterClockwise(new ArrayList<Point>(points)) : false);
				}
			}
			if (!init_drag) {
				move = false;
				restart = true;
			}
			restart(null);
			if (getStraightSkeletons().size() != 0) {
				runAlgorithm();
			}
			init_drag = false;

		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (!enabled) {
				return;
			}
			view.p2 = e.getPoint();
			repaint();

		}

	}

	class CommonMouseListener implements MouseListener {
		Controller controller;

		public CommonMouseListener(Controller controller) {
			this.controller = controller;
		}

		public void mouseClicked(MouseEvent e) {
			if (!enabled || closed) {
				return;
			}
			view.p1 = e.getPoint();
			if (!initialize) {
				point1 = new Point(EventCalculation.vertex_counter++, e.getPoint().getX(), e.getPoint().getY());
				screenPoint1 = new Point(point1.getNumber(), point1.getOriginalX(), point1.getOriginalY());
				points.add(point1);
				initialize = true;
				view.point1 = point1;
				view.revalidate();
				view.repaint();

			} else {
				double x = e.getPoint().getX();
				double y = e.getPoint().getY();
				point2 = checkIfPointExists(x, y);

				Point screenPoint2 = null;
				if (point2 == null) {
					point2 = new Point(EventCalculation.vertex_counter++, x, y);
					screenPoint2 = new Point(point2.getNumber(), point2.getOriginalX(), point2.getOriginalY());
				} else {
					closed = true;
					screenPoint2 = polyLines.get(0).getP1();
					view.p1 = null;
					view.p2 = null;
				}
				
				int weight = 1;
				if (randomWeights) {
					weight = generateWeight();
				}
				

				if (point1.adjacentLines.size() > 0) {
					Point prevPoint = Util.getOtherPointOfLine(point1, point1.adjacentLines.get(0));
					if (collinear(prevPoint, point1, point2)) {
						Line sl = polyLines.get(polyLines.size() - 1);
						Point sprevPoint = Util.getOtherPointOfLine(screenPoint1, sl);
						polyLines.remove(sl);
						Line lastLine = lines.get(lines.size() - 1);
						lines.remove(lastLine);
						prevPoint.adjacentLines.remove(lastLine);
						points.remove(point1);
						view.remove(view.getComponentCount() - 1);
						point1 = prevPoint;
						screenPoint1 = sprevPoint;

					}
				}
				
				Line line = new Line(point1, point2, weight);
				point1.adjacentLines.add(line);
				point2.adjacentLines.add(line);

				Line screenLine = new Line(screenPoint1, screenPoint2, weight);
				polyLines.add(screenLine);

				points.add(point2);
				lines.add(line);

				CustomTextField field = new CustomTextField(line, screenLine, controller);

				view.add(field);
				view.repaint();
				view.revalidate();

				point1 = point2;
				point2 = null;
				screenPoint1 = screenPoint2;
				view.p1 = view.p2;
				if (view.p1 == null && view.p2 == null) {
					if (Util.isCounterClockwise(new ArrayList<Point>(view.getPoints()))) {
						view.repositionTextfields();
					}
				}

			}

		}

		private Point checkIfPointExists(double x, double y) {
			for (Line line : lines) {
				Point p1 = line.getP1();
				Point p2 = line.getP2();
				if (Point2D.distance(p1.getOriginalX(), p1.getOriginalY(), x, y) < 10) {
					return p1;

				}
				if (Point2D.distance(p2.getOriginalX(), p2.getOriginalY(), x, y) < 10) {
					return p2;
				}
			}
			return null;
		}

		private boolean collinear(Point p1, Point p2, Point p3) {
			double det = Math.abs((p2.current_x - p1.current_x) * (p3.current_y - p1.current_y)
					- (p3.current_x - p1.current_x) * (p2.current_y - p1.current_y));
			return det <= 1000;
		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

		@SuppressWarnings("unchecked")
		public void mousePressed(MouseEvent e) {
			if (!closed) {
				return;
			}

			java.awt.Point ep = e.getPoint();

			for (Line l : polyLines) {
				Point p = l.getP1();
				Ellipse2D c = new Ellipse2D.Double(p.getOriginalX() - 7, p.getOriginalY() - 7, 14, 14);
				if (c.contains(ep)) {
					sdragPoint = p;
					init_drag = true;
					for (Point po : points) {
						if (po.equals(p)) {
							dragPoint = po;

						}
					}
					break;
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

	}

	// class CommonChangeListener implements ChangeListener {
	//
	// @Override
	// public void stateChanged(ChangeEvent e) {
	// Color newColor = tcc.getColor();
	// straightSkeleton.setColor(newColor);
	//
	// }
	//
	// }
}