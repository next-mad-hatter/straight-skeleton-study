package at.tugraz.igi.main;

import lombok.*;

/*
import org.apache.batik.util.*;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.*;
import org.apache.batik.transcoder.image.ImageTranscoder;
*/

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

import at.tugraz.igi.algorithm.SimpleAlgorithm;
import at.tugraz.igi.algorithm.SimpleAlgorithmNoSwingWorker;
import at.tugraz.igi.ui.ConfigurationTable;
import at.tugraz.igi.ui.CustomTextField;
import at.tugraz.igi.ui.GraphicPanel;
import at.tugraz.igi.util.*;
import at.tugraz.igi.events.*;
import data.Graph;

public class Controller {

	/**
	 * We want to globally change weird point comparison logic while making history snapshots.
	 * See saveSnapshot() and Point.equals().
	 */
	public static boolean HISTORY_MODE = false;

	public static enum TYPES {
		OPEN, SAVE, SAVE_AS, PLAY, STEP, BACK, RESET, OPEN_POLY, SVG
	}

	public static CoordinatesScaler.ScalingData inputScalingData;

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
	private boolean isRunning;
	private boolean restart;
	private boolean move;

	private boolean nextStep = true;
	private boolean step;
	private boolean animation;
	private boolean enabled = true;
	@Getter private boolean closed;
	private boolean editMode;
	public boolean randomWeights;
	public boolean finished = true;
	public PolygonMeasureData polyMeasureData;

	public GraphicPanel view;
	private SimpleAlgorithm algo;
	private ConfigurationTable table;

	/**
	 * As the design mixes computations, rendering and state quite intricately,
	 * for purpose of providing means of navigating back through events we'll
	 * try and keep a history of corresponding states, i.e. data relevant to
	 * rendering those, plus one more snapshot (lastState below) for whenever
	 * we want to go back from non-event point or switch between skeletons
	 * (is this conflation wise?)
	 */
	@Data public class Snapshot {
	    private final StraightSkeleton straightSkeleton;
		private final Set<Point> points;
		private final List<Set<Point>> polygons;
		private final List<Line> polyLines;
		private final List<Line> lines;
		private final List<Triangle> triangles;
		private final JLabel label;
	}

	/**
	 * We want to keep the history for each skeleton.
	 * Below, historyPtr should be either zero or 1-based index of currently
	 * shown (for given skeleton) history event.
	 */
	@Data private class HistoryState {
		private List<Snapshot> snapshots = new ArrayList<>();
		private Snapshot lastState = null;
		private int historyPtr = 0;
	}
	private Map<StraightSkeleton, HistoryState> history = new HashMap<>();

	/**
	 * To be called for kotton trace export.
	 */
	@Setter @Getter Consumer<Pair<Event, List<Triangle>>> tracer;

	private List<StraightSkeleton> straightSkeletons;
	private StraightSkeleton straightSkeleton;

	private Point point1;
	private Point point2;
	private Point screenPoint1;
	private boolean initialize;
	private boolean init_drag;

	private Point sdragPoint;

	private Random randomGenerator = new Random();

	private List<Line> lines;
	private List<Line> polyLines;
	@Getter private Set<Point> points;
	private List<Set<Point>> polygons;

	public Controller() {
		this.lines = new ArrayList<Line>();
		this.points = new LinkedHashSet<Point>();
		this.polyLines = new ArrayList<Line>();
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
		saveSnapshot(null, true);
		getHistory().historyPtr = getHistory().snapshots.size();
		nextStep = true;
		table.setValueAt(new Boolean(true), straightSkeletons.indexOf(straightSkeleton), 0);
	}

	public void publish(final List<Pair<String, Boolean>> chunks, Point i, List<Triangle> triangles) {
		JLabel label = null;
		if (!chunks.isEmpty() && chunks.get(0).getLeft() != "Triangulated") {
			Graphics2D g2 = (Graphics2D) view.getGraphics();
			int width = g2.getFontMetrics().stringWidth(chunks.get(0).getLeft());
			label = new JLabel(chunks.get(0).getLeft());
			label.setLocation((int) (i.current_x - width / 2), (int) (i.current_y + 20));
			if (!nextStep)
                setCurrentEvent(label);
		}
		view.setCurrentData(straightSkeletons, straightSkeleton, triangles);
		if (!chunks.isEmpty() && chunks.get(0).getRight()) {
			saveSnapshot(null, true);
		}
		view.repaint();
	}

	/**
	 * For now, we'll only need history of currently selected skeleton.
	 */
	private HistoryState getHistory() {
		if (history.get(straightSkeleton) == null) {
			history.put(straightSkeleton, new HistoryState());
		}
		return history.get(straightSkeleton);
	}

	/**
	 * As the classes' clone logic seems to vary or involve changing global counters,
	 * we'll resort to making a deep copy of what we want manually.
	 */
	@Synchronized
	private Snapshot buildSnapshot(
			StraightSkeleton straightSkeleton,
			Set<Point> points,
			List<Set<Point>> polygons,
			List<Line> polyLines,
			List<Line> lines,
			List<Triangle> triangles,
			JLabel label) {

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

		// FIXME: shouldn't there be a difference between lines and polylines?!
		System.err.println("SNAPSHOT BUILT: " + polyLines.size() + "/" + lines.size() + " -> " + snapPolyLines.size() + "/" + snapLines.size() );
		return new Snapshot(snapSkeleton, snapPoints, snapPolygons, snapPolyLines, snapLines, snapTriangles, label);
	}

	public Snapshot buildSnapshot(JLabel label) {
		return buildSnapshot(straightSkeleton, points, polygons, polyLines, lines, view.getScreenTriangles(), label);
	}

	/**
	 * Saves snapshot of currently seen data to history or lastState.
	 */
	public Snapshot saveSnapshot(JLabel label, boolean toHistory) {
		Snapshot state = buildSnapshot(label);
		if (toHistory)
			getHistory().snapshots.add(state);
		else
			getHistory().lastState = state;
		return state;
	}

	/**
	 * Init from a snapshot (i.e. copy of a skeleton).
	 */
	public void initFromSnapshot(Snapshot snapshot) {
		HISTORY_MODE = true;
		addStraightSkeleton(snapshot.getStraightSkeleton());
		setLoadedData(snapshot.getLines(), snapshot.getPolyLines(), snapshot.getPoints());
		setPolygons(snapshot.getPolygons());
		view.setCurrentData(null, null, snapshot.getTriangles());
		setCurrentEvent(null);
		// view.repaint();
		HISTORY_MODE = false;
	}

	/**
	 * Loads a snapshot from history without changing currently considered skeleton,
     * i.e. makes said snapshot currently viewed point in history.
	 */
	private Snapshot loadSnapshot(boolean fromHistory) {
		HISTORY_MODE = true;
		Snapshot state;
	    if (fromHistory)
	    	state = getHistory().snapshots.get(getHistory().historyPtr-1);
		else
		    state = getHistory().lastState;
		setLoadedData(state.getLines(), state.getPolyLines(), state.getPoints());
		setPolygons(state.getPolygons());
		view.setCurrentData(null, null, state.getTriangles());
		setCurrentEvent(state.getLabel());
		view.repaint();
		HISTORY_MODE = false;
		return state;
	}

	private void truncateHistory() {
	    if (getHistory().historyPtr == 0)
	    	return;
	    getHistory().snapshots.subList(getHistory().historyPtr, getHistory().snapshots.size()).clear();
	}

	public boolean isBrowsingHistory() {
	    return getHistory().historyPtr > 0;
	}

	public boolean wantsUpdates() {
		return isNextStep() && !isBrowsingHistory() && !finished;
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
        /*
        imgStream = getClass().getClassLoader().getResourceAsStream("tango/" + name + ".svg");
        if (imgStream == null) return null;

        SVGTranscoder transcoder = new SVGTranscoder();
        TranscodingHints hints = new TranscodingHints();
        hints.put(ImageTranscoder.KEY_WIDTH, new Float(64));
        hints.put(ImageTranscoder.KEY_HEIGHT, new Float(64));
        hints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
        hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
        hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, SVGConstants.SVG_SVG_TAG);
        hints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, false);
        transcoder.setTranscodingHints(hints);

        try {
            transcoder.transcode(new TranscoderInput(imgStream), null);
        } catch (TranscoderException e) {
            e.printStackTrace();
            return null;
        }
        // FIXME: why do we get null here?
        if (transcoder.getImage() == null) return null;
        return new ImageIcon(transcoder.getImage());
        */
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

	public void playSelected(boolean animate, boolean recreate) {
		restart = true;
		animation = animate;
		if (recreate) {
			restart(null);
		}
		runAlgorithm();
	}

	public void switchToSkeleton(int skeleton, boolean editMode) {
		System.err.println("LOADING SKELETON " + skeleton);
		if (!straightSkeletons.get(skeleton).equals(straightSkeleton)) {
			saveSnapshot(null, false);
			straightSkeleton = straightSkeletons.get(skeleton);
			val snapshot = loadSnapshot(false);
			restart(snapshot.getPolyLines());
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
		ArrayList<Point> points = new ArrayList<>(this.points);
		if (points.size() > 2) {
			Util.closePolygon(points, lines);
			closed = true;
			isCounterClockwise = Util.isCounterClockwise(points);
			if (algo == null) straightSkeletons = getStraightSkeletons();
            if (straightSkeleton != null) straightSkeleton.clear();
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
		ArrayList<Point> points = new ArrayList<>(this.points);
		if (points.size() > 2) {

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
	    if (!lines.equals(polyLines)) lines.clear();

		getHistory().snapshots.clear();
		getHistory().historyPtr = 0;


		// alreadyMovedPts.clear();
		Point p1;
		Point p2;
		// boolean add = false;
		if (polyLines == null) {
			polyLines = this.polyLines;
		} else if (!polyLines.equals(this.polyLines)) {
			this.polyLines.clear();
			this.polyLines.addAll(polyLines);
		}
		Line l1 = polyLines.get(0);
		Point first = l1.getP1();
		p1 = new Point(l1.getP1().getNumber(), l1.getP1().getOriginalX(), l1.getP1().getOriginalY());
		points.add(p1);
		int i = 0;
		// FIXME: why the concurrent modification exception?  is this because algo keeps working or
		// because snapshot conflates lines and polylines and we should make them separate clones here?
		// Seems to work if we let algo run in between the switches...
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
		table.setRowSelectionInterval(table.getRowCount()-1, table.getRowCount()-1);
	}

	public void removeStraightSkeleton(int skeleton) {
		StraightSkeleton selectedSk = straightSkeletons.get(skeleton);
		history.remove(selectedSk);
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
        // JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(view);
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(view);
		if(frame != null) frame.setTitle("Triton Applet");

		history = new HashMap<>();
		getHistory().historyPtr = 0;
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

	public StraightSkeleton getStraightSkeleton() {
		if (getHistory().historyPtr > 0 && getHistory().historyPtr <= getHistory().snapshots.size())
			return getHistory().snapshots.get(getHistory().historyPtr-1).getStraightSkeleton();
		return straightSkeleton;
	}

	public List<StraightSkeleton> getStraightSkeletons() {
		if (straightSkeletons == null) straightSkeletons = new ArrayList<StraightSkeleton>();
        if (isBrowsingHistory()) {
            List<StraightSkeleton> res = new ArrayList<>();
            for (val s : straightSkeletons) {
            	if (s == straightSkeleton)
            		res.add(getStraightSkeleton());
				else
					res.add(s);
			}
            return res;
		}
		return straightSkeletons;
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
	    if (getHistory().historyPtr > 0 && getHistory().historyPtr <= getHistory().snapshots.size())
	    	return getHistory().snapshots.get(getHistory().historyPtr-1).getPolygons();
		return polygons;
	}

	public List<Line> getPolyLines() {
		if (getHistory().historyPtr > 0 && getHistory().historyPtr <= getHistory().snapshots.size())
			return getHistory().snapshots.get(getHistory().historyPtr-1).getPolyLines();
		return polyLines;
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
			closed = true;

		}

		// private void openPoly() {
		// FileHandler.openPoly(view, controller);

		// }
		private void save(boolean saveAs) {
			FileHandler.save(controller.polyLines, saveAs, inputScalingData);
		}

		private void saveSVG() {
			FileHandler.saveSVG(view, true);

		}

		private void play() {
            getHistory().historyPtr = getHistory().snapshots.size();
			if (getHistory().historyPtr != 0) {
				// loadSnapshot(false);
                loadSnapshot(true);
                // truncateHistory();
				step = false;
				nextStep = true;
				getHistory().historyPtr = 0;
			}
			if (step) {
				step = false;
				nextStep = true;
			}
			if (restart) {
				getHistory().snapshots.clear();
				getHistory().historyPtr = 0;
				step = false;
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
		}

        private void back() {
		    if (!finished && (nextStep || getHistory().snapshots.isEmpty() || getHistory().historyPtr == 1)) {
                step = true;
		    	return;
            }
		    if (getHistory().historyPtr == 0) {
				getHistory().historyPtr = getHistory().snapshots.size();
				// saveSnapshot(false);
				if (!nextStep && getHistory().historyPtr > 1) {
					getHistory().historyPtr--;
                    loadSnapshot(true);
				}
				nextStep = false;
			} else {
				getHistory().historyPtr--;
				loadSnapshot(true);
			}
        }

		private void step() {
		    if (getHistory().historyPtr != 0 && !getHistory().snapshots.isEmpty()) {
		        getHistory().historyPtr++;
		        if (getHistory().historyPtr > getHistory().snapshots.size()) {
		            if (finished)
		            	getHistory().historyPtr = getHistory().snapshots.size();
		            else {
						// loadSnapshot(false);
						getHistory().historyPtr = 0;
					}
				} else {
					loadSnapshot(true);
					return;
				}
			}
			if (restart) {
		    	getHistory().snapshots.clear();
		    	getHistory().historyPtr = 0;
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
			// if (sdragPoint != null) {
			if (!enabled) {
				enabled = true;
			}
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
					field.updatePosition(closed ? Util.isCounterClockwise(new ArrayList<Point>(points)) : false);
				}
			}
			if (!init_drag) {
				move = false;
				restart = true;
			}
			restart(null);
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
			if (straightSkeleton != null) {
				runAlgorithm();
			}
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
