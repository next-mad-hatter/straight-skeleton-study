package at.tugraz.igi.main;

import lombok.*;

import org.apache.commons.lang3.tuple.*;
import java.util.function.*;
import java.util.stream.*;
import java.util.*;

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

import javax.imageio.*;
import javax.swing.*;
import javax.swing.SwingWorker.StateValue;

import at.tugraz.igi.algorithm.*;
import at.tugraz.igi.ui.ConfigurationTable;
import at.tugraz.igi.ui.CustomTextField;
import at.tugraz.igi.ui.GraphicPanel;
import at.tugraz.igi.util.*;
import at.tugraz.igi.events.*;
import data.Graph;

public class Controller {

	public static enum TYPES {
		OPEN, SAVE, SAVE_AS, PLAY, STEP, BACK, RESET, OPEN_POLY, SVG
	}

	public static boolean isCounterClockwise;
	public static ImageIcon delete_icon;
	public static ImageIcon copy_icon;
	public static ImageIcon play_icon;
	public static ImageIcon color_icon;
	public static ImageIcon step_icon;
    public static ImageIcon back_icon;
    public static ImageIcon reset_icon;
	public static ImageIcon edit_icon;
	public static ImageIcon visible_icon;
	public static ImageIcon not_visible_icon;

	public boolean randomWeights = false;

	public PolygonMeasureData polyMeasureData;

	public GraphicPanel view;
	private ConfigurationTable table;

	private Point point1;
	private Point point2;
	private Point screenPoint1;
	private boolean init_drag;

	private Point sdragPoint;

	private Random randomGenerator = new Random();

	public Controller() {
		delete_icon = getImage("edit-delete");
		copy_icon = getImage("edit-copy");
		play_icon = getImage("play");
		color_icon = getImage("color");
		step_icon = getImage("go-next");
		back_icon = getImage("go-previous");
		reset_icon = getImage("view-refresh");
		edit_icon = getImage("edit");
		visible_icon = getImage("eye");
		not_visible_icon = getImage("eye_blocked");
	}

	/**
	 * To be called for kotton trace export.
	 */
	@Setter @Getter Consumer<Pair<Event, List<Triangle>>> tracer;

	/**
	 * For small differences between coordinates, affine transform offered by the swing
	 * canvas seems to break.  This is an attempt to help with that at input stage.
	 * The scaling we do here is for now implemented for one input format and is lossless
	 * (operates on rationals).
	 */
	public static CoordinatesScaler.ScalingData inputScalingData;

	/**
	 * We have to globally change point comparison logic while making history snapshots.
	 * See buildSnapshot() and Point.equals().
	 */
	public static boolean HISTORY_MODE = false;

	/**
	 * As the design mixes computations, rendering and state quite intricately,
	 * for purpose of providing means of navigating back through events we'll
	 * try and keep a history of corresponding states, i.e. (cloned) data relevant to
	 * rendering those.
	 */
	@Data public class Snapshot {
	    private final StraightSkeleton skeleton;
		private final Set<Point> points;
		private final List<Set<Point>> polygons;
		private final List<Line> polyLines;
		private final List<Line> lines;
		private final List<Triangle> triangles;
		private final JLabel currentEvent;
	}

	/**
	 * As the classes' clone logic seems to vary or involve changing global counters,
	 * we'll resort to making a deep copy of what we want manually.
	 */
	private Snapshot buildSnapshot(
			StraightSkeleton straightSkeleton,
			Set<Point> points,
			List<Set<Point>> polygons,
			List<Line> polyLines,
			List<Line> lines,
			List<Triangle> triangles,
			JLabel currentEvent) {

		// A hack to make point comparison work with maps herein.  See Point.equals().
		HISTORY_MODE = true;

		val clonedPointsMap = new LinkedHashMap<Point, Point>();
		Function<Point, Point> clonePoint = (pt) -> {
			if (clonedPointsMap.containsKey(pt))
				return clonedPointsMap.get(pt);
			val np = new Point(pt.getNumber(), pt.getOriginalX(), pt.getOriginalY());
			np.current_x = pt.current_x;
			np.current_y = pt.current_y;
			np.adjacentLines = pt.adjacentLines;
			// np.setStrictComparison(true);
			clonedPointsMap.put(pt, np);
			return np;
		};

		val clonedLinesMap = new LinkedHashMap<Line, Line>();
		Function<Line, Line> cloneLine = (line) -> {
			if (clonedLinesMap.containsKey(line))
				return clonedLinesMap.get(line);
			val nl = new Line(
					clonePoint.apply(line.getP1()),
					clonePoint.apply(line.getP2()),
					line.getWeight());
			nl.polyLine = line.polyLine;
			clonedLinesMap.put(line, nl);
			return nl;
		};

		val clonedLineListsMap = new LinkedHashMap<List<Line>, List<Line>>();
		Function<List<Line>, List<Line>> cloneLineList = (ls) -> {
			if (!clonedLineListsMap.containsKey(ls))
				clonedLineListsMap.put(ls, ls.stream().map(cloneLine).collect(Collectors.toList()));
			return clonedLineListsMap.get(ls);
		};

		StraightSkeleton snapSkeleton;
		if (straightSkeleton == null)
			snapSkeleton = null;
		else {
			snapSkeleton = new StraightSkeleton();
			snapSkeleton.setColor(straightSkeleton.getColor());
			snapSkeleton.setVisible(straightSkeleton.isVisible());
			snapSkeleton.setLines(cloneLineList.apply(straightSkeleton.getLines()));
			snapSkeleton.setPolyLines(cloneLineList.apply(straightSkeleton.getPolyLines()));
			if (straightSkeleton.polygon != null) {
				snapSkeleton.polygon = new HashMap<>();
				for (val e : straightSkeleton.polygon.entrySet()) {
					val d = new MeasuringData(
							cloneLine.apply(e.getValue().getPolyLine()),
							cloneLineList.apply(e.getValue().getPolygon()),
							e.getValue().getArea()
					);
					snapSkeleton.polygon.put(cloneLineList.apply(e.getKey()), d);
				}
			}
		}

		val snapPolyLines = cloneLineList.apply(polyLines);
		val snapLines = cloneLineList.apply(lines);

		val snapPolygons = new ArrayList<Set<Point>>();
        for (val pts : polygons) {
            val newPoly = new LinkedHashSet<Point>();
            for (val p : pts) {
                newPoly.add(clonePoint.apply(p));
            }
            snapPolygons.add(newPoly);
        }

		val snapTriangles = new ArrayList<Triangle>();
        if (triangles != null)
            for (val tri: triangles) {
                val nt = new Triangle(
                        clonePoint.apply(tri.p1),
                        clonePoint.apply(tri.p2),
                        clonePoint.apply(tri.p3));

                nt.polyLines = cloneLineList.apply(tri.polyLines);
                nt.strokes = cloneLineList.apply(tri.strokes);
                snapTriangles.add(nt);
            }

		// What lines did we forget here?
		boolean rescan = false;
		do {
			for (val pt : clonedPointsMap.values()) {
				for (val line : pt.adjacentLines) {
					if (clonedLinesMap.get(line) == null) {
						rescan = !clonedPointsMap.containsKey(line.getP1()) ||
								!clonedPointsMap.containsKey(line.getP2());
						cloneLine.apply(line);
					}
				}
			}
		} while (rescan);

		Set<Point> snapPoints = new HashSet<>();
		for (val pt: points) {
			snapPoints.add(clonePoint.apply(pt));
		}

		for (val pt: clonedPointsMap.values()) {
			pt.adjacentLines = pt.adjacentLines.stream().map((x) -> clonedLinesMap.get(x)).collect(Collectors.toList());
			if (pt.adjacentLines.contains(null))
				System.err.println("History forgot a line");
		}

		HISTORY_MODE = false;

		return new Snapshot(snapSkeleton, snapPoints, snapPolygons, snapPolyLines, snapLines, snapTriangles, currentEvent);
	}

	public Snapshot buildSnapshot(Context context) {
		context.makingSnapshot = true;
		val res = buildSnapshot(
				context.getSkeleton(false),
				context.getPoints(false),
				context.getPolygons(false),
				context.getPolyLines(false),
				context.getLines(false),
				context.getTriangles(false),
				context.getCurrentEvent(false));
		context.makingSnapshot = false;
		return res;
	}

	/**
	 * We want to keep the history for each skeleton.
	 * Below, historyPtr should be either zero or 1-based index of currently
	 * shown (for given skeleton) history event.
	 */
	@RequiredArgsConstructor
	private class History {
		@Getter private final List<Snapshot> snapshots = new ArrayList<>();
		@Getter private int historyPtr = 0;

		public Snapshot getCurrentSnapshot() {
		    if (historyPtr == 0 || historyPtr > snapshots.size()) return null;
		    return snapshots.get(historyPtr-1);
		}

		public void back() {
		    if (historyPtr < 2) return;
		    historyPtr--;
		}

		public void forward() {
			historyPtr++;
			if (historyPtr > snapshots.size()) historyPtr = 0;
		}

		public boolean isEmpty() {
			return snapshots.isEmpty();
		}

		public boolean atFirst() {
		    return historyPtr == 1;
		}

		public boolean atLast() {
			return historyPtr == snapshots.size();
		}

		public boolean isBrowsing() {
			return historyPtr > 0;
		}

		public void setToLast() {
		    historyPtr = snapshots.size();
		}

		public void unbrowse() {
			historyPtr = 0;
		}

	}

	/**
	 * We want to keep a number of polygons (with differing geometry and/or
	 * edge weighting) for comparison purposes.  For this, we'll try and stick
	 * things which are dependent on the polygon into a list of contexts
	 * and switch between those.
	 */
	@RequiredArgsConstructor
	public class Context {
        // All these flags determine the system behaviour in ways I'll never comprehend :).
		public boolean isRunning = false;
		public boolean restart = false;
		public boolean move = false;
		public boolean stepMode = false;
		public boolean paused = false;
		public boolean animation = true;
		public boolean enabled = true;
		public boolean closed = false;
		public boolean editMode = false;
		public boolean finished = false;
		public boolean initialize = false;
		public boolean makingSnapshot = false;

		@Setter private StraightSkeleton skeleton = new StraightSkeleton();
		@Setter private List<Line> lines = new ArrayList<>();
		@Setter private List<Line> polyLines = new ArrayList<>();
		@Setter private Set<Point> points = new LinkedHashSet<>();
		@Setter private List<Set<Point>> polygons = new ArrayList<>();
		@Setter private JLabel currentEvent = null;
		@Getter private History history = new History();
		@Getter @Setter private SimpleAlgorithm algorithm = null;

		public StraightSkeleton getSkeleton(Boolean throughHistory) {
		    if (!throughHistory || history.historyPtr == 0) return skeleton;
		    return history.getCurrentSnapshot().getSkeleton();
		}

		public List<Line> getLines(Boolean throughHistory) {
			if (!throughHistory || history.historyPtr == 0) return lines;
			return history.getCurrentSnapshot().getLines();
		}

		public List<Line> getPolyLines(Boolean throughHistory) {
			if (!throughHistory || history.historyPtr == 0) return polyLines;
			return history.getCurrentSnapshot().getPolyLines();
		}

		public Set<Point> getPoints(Boolean throughHistory) {
			if (!throughHistory || history.historyPtr == 0) return points;
			return history.getCurrentSnapshot().getPoints();
		}

		public List<Set<Point>> getPolygons(Boolean throughHistory) {
			if (!throughHistory || history.historyPtr == 0) return polygons;
			return history.getCurrentSnapshot().getPolygons();
		}

		public List<Triangle> getTriangles(Boolean throughHistory) {
			if (!throughHistory || history.historyPtr == 0) {
				if (algorithm == null) return null;
				return algorithm.getTriangles();
			}
			return history.getCurrentSnapshot().getTriangles();
		}

		public JLabel getCurrentEvent(Boolean throughHistory) {
			if (!throughHistory || history.historyPtr == 0) return currentEvent;
			return history.getCurrentSnapshot().getCurrentEvent();
		}

		@Synchronized
		public void saveSnapshot() {
			Snapshot state = buildSnapshot(this);
            history.snapshots.add(state);
		}

		public boolean isBrowsingHistory() {
			return history.isBrowsing();
		}

	}
	private List<Context> contexts;
	private int contextPtr;
	// TODO: synchronize view methods where necessary
	@Getter private final Object contextLock = new Object();

	/**
	 * We cannot do this inside our constructor since we need table here,
	 * and table constructor needs controller.
	 */
	public void initContexts(ConfigurationTable table) {
		this.contexts = new ArrayList<Context>();
		this.contexts.add(new Context());
		this.contextPtr = 0;
		this.table = table;
		table.addRow();
		table.setRowSelectionInterval(table.getRowCount()-1, table.getRowCount()-1);
	}

	public Context getContext(int ptr) {
		return contexts.get(ptr);
	}

	public Context getContext() {
		return getContext(contextPtr);
	}

	@Synchronized("contextLock")
	public void switchContext(int contextPtr) {
		this.contextPtr = contextPtr;
		val context = getContext();
		view.init(context.getLines(true), context.getPoints(true));
		view.setTriangles(context.getTriangles(true));
		if (context.isBrowsingHistory() || !context.finished && context.paused) {
			// System.err.println("EVENT");
			view.setCurrentEvent(context.getCurrentEvent(true));
		}
        else
        	view.setCurrentEvent(null);
		view.repaint();
	}

	@Synchronized("contextLock")
	public void refreshContext() {
		switchContext(contextPtr);
	}

	public void setView(GraphicPanel view) {
		this.view = view;
		refreshContext();
	}

	@Synchronized("contextLock")
	public void setLoadedData(List<Line> lines, List<Line> screenLines, Set<Point> points) {
		val context = getContext();
		context.setLines(lines);
		context.setPolyLines(screenLines);
		context.setPoints(points);
	}

	public void addContext(Snapshot snapshot) {
		val context = new Context();
		context.setSkeleton(snapshot.getSkeleton());
		context.setPoints(snapshot.getPoints());
		context.setPolygons(snapshot.getPolygons());
		context.setPolyLines(snapshot.getPolyLines());
		context.setLines(snapshot.getLines());
		context.setCurrentEvent(snapshot.getCurrentEvent());
		contexts.add(context);
		table.addRow();
		table.setRowSelectionInterval(table.getRowCount()-1, table.getRowCount()-1);
	}

	@Synchronized("contextLock")
	public void cloneContext(int ptr) {
		val snapshot = buildSnapshot(contexts.get(ptr));
		addContext(snapshot);
		switchContext(contexts.size()-1);
	}

	@Synchronized("contextLock")
	public void removeContext(int ptr) {
		val algo = contexts.get(ptr).getAlgorithm();
		if (algo != null) algo.cancel(true);
		table.removeRow(ptr);
		contexts.remove(ptr);
		if (contexts.isEmpty()) reset();
		if (contextPtr + 1 > contexts.size()) contextPtr = contexts.size() - 1;
		refreshContext();
	}

	public void runAlgorithm(Context context) {
		context.isRunning = true;
		ArrayList<Point> points = new ArrayList<>(context.getPoints(false));
		if (points.size() > 2) {
			Util.closePolygon(points, context.getLines(false));
			context.closed = true;
			isCounterClockwise = Util.isCounterClockwise(points);
			context.getSkeleton(false).clear();
			context.move = false;
			if (context.getAlgorithm() == null || context.finished)
                try {
                    val algo = new SimpleAlgorithm(points, context.getLines(false), context.animation, this, context);
                    context.setAlgorithm(algo);
                    context.setPolygons(new ArrayList<>());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            context.getAlgorithm().setAnimation(context.animation);
			context.getAlgorithm().execute();
		}
	}

	public void runAlgorithmNoSwingWorker(Context context) throws Exception {
		context.isRunning = true;
		ArrayList<Point> points = new ArrayList<>(context.getPoints(false));
		if (points.size() > 2) {
			Util.closePolygon(points, context.getLines(false));
			context.closed = true;
			isCounterClockwise = Util.isCounterClockwise(points);
			polyMeasureData = null;
			SimpleAlgorithmNoSwingWorker algoNoSwing = new SimpleAlgorithmNoSwingWorker(points, context.getLines(false), this, context);
			context.setPolygons(new ArrayList<>());
			algoNoSwing.execute();
		}
	}

	public void publish(Context context, final List<AlgoChunk> chunks) {
		if (chunks.isEmpty()) {
			refreshContext();
			return;
		}
		JLabel label;
		for (val ch: chunks) {
		    System.err.println("EVENT " + ch.eventName);
			if (ch.eventName != null && ch.eventName != "Triangulated") {
				Graphics2D g2 = (Graphics2D) view.getGraphics();
				int width = g2.getFontMetrics().stringWidth(ch.eventName);
				label = new JLabel(ch.eventName);
				label.setLocation((int) (ch.loc_x - width / 2), (int) (ch.loc_y + 20));
				context.setCurrentEvent(label);
			}
			if (ch.eventName == "Triangulated") context.setCurrentEvent(null);
			// do we need the triangles from algo here?
			if (ch.isEvent) {
				context.saveSnapshot();
			}
			refreshContext();
		}
	}

	public boolean wantsUpdates(Context context) {
		return !context.makingSnapshot && !context.paused && !context.isBrowsingHistory(); // && !context.finished;
	}

	/**
	 * Used to compute adjusted skeleton after weights / geometry change.
	 */
	public void quickRerun(Context context) {
	    if (context.getAlgorithm() == null) return;
		context.restart = true;
		context.animation = false;
        restart(context, null);
		runAlgorithm(context);
		context.animation = true;
	}

	public void reset() {
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(view);
		if(frame != null) frame.setTitle("Triton Applet");

		for (val c: contexts) {
			if (c.getAlgorithm() != null && c.getAlgorithm().getState().equals(StateValue.STARTED)) {
				c.getAlgorithm().cancel(true);
			}
		}

		table.removeAllRows();
		initContexts(table);
		view.reset();
		refreshContext();

		EventCalculation.vertex_counter.clear();
		EventCalculation.skeleton_counter = 0;

		FileHandler.file = null;
	}

	public void finished(Context context) {
		// straightSkeleton.add(lines);
		// polygons = null;
		context.finished = true;
		context.restart = true;
		context.isRunning = false;
		context.animation = true;
		context.paused = false;
		context.saveSnapshot();
		context.history.setToLast();
		table.setValueAt(new Boolean(true), contextPtr, 0);
	}

	public void restart(Context context, List<Line> polyLines) {
		context.history = new History();
		val points = context.getPoints(false);
		val lines = context.getLines(false);
		points.clear();
		if (!lines.equals(polyLines)) lines.clear();

		if (polyLines == null) {
			polyLines = context.getPolyLines(false);
		} else if (!polyLines.equals(context.getPolyLines(false))) {
			context.getPolyLines(false).clear();
			context.getPolyLines(false).addAll(polyLines);
		}

		Point p1;
		Point p2;
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

	@Synchronized("contextLock")
	private void roll(Context context, boolean restart) {
		if (restart) {
			this.restart(context, null);
			EventCalculation.setVertexCounter(context, context.getPoints(false).size());
			context.restart = false;
			context.getSkeleton(false).polygon = null;
		}
		refreshContext();
		if (!context.isRunning && !context.isBrowsingHistory()) {
			view.setEnabled(false);
			runAlgorithm(context);
		}
    }

	@Synchronized("contextLock")
	private void play() {
		val context = getContext();
		val history = context.getHistory();
		history.unbrowse();
		context.stepMode = false;
        context.paused = false;
        roll(context, context.restart);
	}

	@Synchronized("contextLock")
	private void step() {
		val context = getContext();
		val history = context.getHistory();
		context.stepMode = true;
		if (history.isBrowsing()) {
			if (history.atLast()) {
				history.unbrowse();
				context.paused = false;
			} else {
				history.forward();
			}
		} else context.paused = false;
		roll(context, context.restart && !context.isBrowsingHistory());
	}

	@Synchronized("contextLock")
	private void back() {
	    val context = getContext();
	    val history = context.getHistory();
		if (history.isEmpty() || history.atFirst()) return;
		if (!history.isBrowsing()) {
			history.setToLast();
		} else {
		    history.back();
		}
		refreshContext();
	}

	public void setCurrentEvent(Context context, JLabel l) {
		context.setCurrentEvent(l);
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

	private ImageIcon getImage(String name) {
		InputStream imgStream = getClass().getClassLoader().getResourceAsStream(name + ".png");
        if (imgStream == null)
            imgStream = getClass().getClassLoader().getResourceAsStream("tango/" + name + ".png");
        if (imgStream != null) {
            ImageIcon icon = null;
            try {
                icon = new ImageIcon(ImageIO.read(imgStream));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return icon;
        }
        return null;
    }

	public List<Line> createPolyLines(Context context) {
		Point p1;
		Point p2;
		List<Line> polyLines = new ArrayList<Line>();

		val lines = context.getLines(false);

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

	public double getAverage_weight(Context context) {
		int sum = 0;
		for (Line l : context.getPolyLines(false)) {
			sum += l.getWeight();
		}
		return sum / context.getPolyLines(false).size();
	}

	public void addPolygon(Context context, Set<Point> pts) {
		val p = context.getPolygons(false);
		if (p != null) p.add(pts);
	}

	public void removePolygon(Context context, Set<Point> pts) {
		val p = context.getPolygons(false);
		if (p != null) p.remove(pts);
	}

	public void showColorChooser(int index) {
		StraightSkeleton skeleton = contexts.get(index).getSkeleton(false);
		Color newColor = JColorChooser.showDialog(view, "Choose Background Color", skeleton.getColor());
		if (newColor != null) {
			skeleton.setColor(newColor);
		}
	}

	public void toggleVisibility(int index, boolean visible) {
		StraightSkeleton skeleton = contexts.get(index).getSkeleton(false);
		skeleton.setVisible(visible);
		view.repaint();
	}

	/*
	// disabled until safe
	public StraightSkeleton getStraightSkeleton() {
	    return getContext().getSkeleton(true);
	}
	*/

	public StraightSkeleton getStraightSkeleton(boolean fromHistory) {
		return getContext().getSkeleton(fromHistory);
	}

	public StraightSkeleton getStraightSkeleton(Context context, boolean fromHistory) {
		return context.getSkeleton(fromHistory);
	}

	/*
	// disabled until safe
	public List<StraightSkeleton> getStraightSkeletons() {
		return contexts.stream().map(c -> c.getSkeleton(true)).collect(Collectors.toList());
	}
	*/

	public List<StraightSkeleton> getStraightSkeletons(boolean fromHistory) {
		return contexts.stream().map(c -> c.getSkeleton(fromHistory)).collect(Collectors.toList());
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
				case BACK:
					back();
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
			getContext().closed = true;
		}

		// private void openPoly() {
		// FileHandler.openPoly(view, controller);

		// }
		private void save(boolean saveAs) {
			FileHandler.save(controller.getContext().getPolyLines(false), saveAs, inputScalingData);
		}

		private void saveSVG() {
			FileHandler.saveSVG(view, true);
		}

	}

	class CommonMouseMotionListener implements MouseMotionListener {
		@Override
		public void mouseDragged(MouseEvent e) {
			if (!getContext().closed || !getContext().enabled) {
				return;
			}
			getContext().move = true;
			// if (sdragPoint != null) {
            getContext().enabled = true;
			val loc = view.transformCoordinates(
					e.getPoint().getX(),
					e.getPoint().getY(), false);

			if (view.getMin_x() > loc.getX()) view.setMin_x(loc.getX());
			if (view.getMin_y() > loc.getY()) view.setMin_y(loc.getY());
			if (view.getMax_x() < loc.getX()) view.setMax_x(loc.getX());
			if (view.getMax_y() < loc.getY()) view.setMax_y(loc.getY());

			sdragPoint.setBothXCoordinates(loc.getX());
			sdragPoint.setBothYCoordinates(loc.getY());
			/*
			sdragPoint.setBothXCoordinates(e.getPoint().getX());
			sdragPoint.setBothYCoordinates(e.getPoint().getY());
			*/

			for (int j = 0; j < view.getComponentCount(); j++) {
				CustomTextField field = ((CustomTextField) view.getComponents()[j]);
				if (field.getScreenLine().contains(sdragPoint)) {
					field.updatePosition(getContext().closed ? Util.isCounterClockwise(new ArrayList<Point>(getContext().getPoints(false))) : false);
				}
			}
			if (!init_drag) {
				getContext().move = false;
				getContext().restart = true;
			}
			restart(getContext(), null);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (!getContext().enabled) {
				return;
			}
			view.p2 = e.getPoint();
			view.repaint();
		}

	}

	class CommonMouseListener implements MouseListener {
		Controller controller;

		public CommonMouseListener(Controller controller) {
			this.controller = controller;
		}

		public void mouseClicked(MouseEvent e) {
		    Context context = getContext();

			if (!context.enabled || context.closed) {
				return;
			}
			view.p1 = e.getPoint();
			if (!context.initialize) {
				point1 = new Point(EventCalculation.getVertexCounter(context), e.getPoint().getX(), e.getPoint().getY());
				EventCalculation.incVertexCounter(context);
				screenPoint1 = new Point(point1.getNumber(), point1.getOriginalX(), point1.getOriginalY());
				context.getPoints(false).add(point1);
				context.initialize = true;
				view.point1 = point1;
				view.revalidate();
				view.repaint();

			} else {
				double x = e.getPoint().getX();
				double y = e.getPoint().getY();
				point2 = checkIfPointExists(context, x, y);

				Point screenPoint2 = null;
				if (point2 == null) {
					point2 = new Point(EventCalculation.getVertexCounter(context), x, y);
					EventCalculation.incVertexCounter(context);
					screenPoint2 = new Point(point2.getNumber(), point2.getOriginalX(), point2.getOriginalY());
				} else {
					context.closed = true;
					screenPoint2 = context.getPolyLines(false).get(0).getP1();
					view.p1 = null;
					view.p2 = null;
				}
				
				int weight = 1;
				if (randomWeights) {
					weight = generateWeight();
				}

				val polyLines = context.getPolyLines(false);
				val lines = context.getLines(false);
				val points = context.getPoints(false);
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

		private Point checkIfPointExists(Context context, double x, double y) {
			for (Line line : context.getLines(false)) {
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
			if (!getContext().closed) {
				return;
			}

			java.awt.Point ep = e.getPoint();

			for (Line l : getContext().getPolyLines(false)) {
				Point p = l.getP1();
				val loc = view.transformCoordinates(
						p.getOriginalX(),
						p.getOriginalY(), true);
				// Ellipse2D c = new Ellipse2D.Double(p.getOriginalX() - 7, p.getOriginalY() - 7, 14, 14);
				Ellipse2D c = new Ellipse2D.Double(loc.getX() - 7, loc.getY() - 7, 14, 14);
				if (c.contains(ep)) {
					sdragPoint = p;
					init_drag = true;
					break;
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		    if (!init_drag) return;
            quickRerun(getContext());
			init_drag = false;
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
