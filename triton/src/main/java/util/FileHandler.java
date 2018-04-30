/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package at.tugraz.igi.util;

import lombok.*;
import org.apache.commons.lang3.tuple.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.function.*;
import java.util.List;
import java.io.*;
import org.tukaani.xz.*;

import com.codepoetics.protonpack.*;
import static java.lang.Math.toIntExact;

import java.util.stream.*;
import at.tugraz.igi.util.ParseException;
import java.nio.*;
import java.nio.file.*;
import javax.swing.*;
import javax.xml.parsers.*;
import org.jfree.graphics2d.svg.*;
import org.w3c.dom.*;
import at.tugraz.igi.main.*;
import at.tugraz.igi.ui.*;
import at.tugraz.igi.ui.*;
import at.tugraz.igi.util.Point;
import data.*;

public class FileHandler {

	public static File file = null;
	public static File svgfile = null;
	public static String parent = (new File(System.getProperty("user.dir"))).getPath();
	final static JFrame fcFrame = new JFrame("Choose new file name");

    /**
     * Reads contents of a stream into a string.
     */
	private static String fetchStreamContents(InputStream in, String encoding) throws IOException {
		byte[] buffer = new byte[8192];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int size;
		while ((size = in.read(buffer)) != -1)
			out.write(buffer, 0, size);
		String res = out.toString(encoding);
		out.close();
		return res;
	}


    /**
     * Reads contents of a UTF-8-encoded file into a String object.  Tries to interpret
     * contents of the file as xz-compressed stream first, and if that fails, as plain text.
     */
	private static String fetchFileContents(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		String result;
		try {
			try {
				result = fetchStreamContents(new XZInputStream(in), "UTF-8");
			} catch (XZFormatException e) {
				in.close();
				in = new FileInputStream(file);
				result = fetchStreamContents(in, "UTF-8");
			}
		} catch (Exception e) {
			in.close();
			throw e;
		}
		in.close();
		return result;
	}


    /**
     * As far as I can see right now, we should be able to read three file formats:
     *
     *  - the csv-based weighted segments format as produced by the main app
     *  - the xml-based graph format as produced by the random generator app found here
     *  - the simple two-floats-per-line format used by generated polygons dataset
     *
     * While #open() below attempts to take care of former two, this reads the latter.
     * We'll also try reading input as xz stream first.
     *
     * For now we expect the input to fit in memory easily, so we read it in all at once.
     *
     * Maybe contrary to app design, we'd rather separate this logic from the controller.
     */
    public static Pair<CoordinatesScaler.ScalingData, List<Point>>
	readCoordinatesFile(File file, boolean scaleInput) throws IOException {
        String input = fetchFileContents(file);
        return parseCoordinates(input, scaleInput);
    }


    /**
     * Parses whitespace-separated list of float coordinates, returning a list of points
     * (numbered, as is being expected in other application parts).
     */
    private static Pair<CoordinatesScaler.ScalingData, List<Point>>
	parseCoordinates(String input, boolean scaleInput) throws ParseException {
        try {
            input = input.replaceAll("#.*(\n|$)", "").trim();
            val strs = Stream.of(input.split("\\s+")).collect(Collectors.toList());
            if ((strs.size() % 2) != 0)
                throw new ParseException("Uneven number of fields in coordinates file");

            List<Indexed<String>> zi = StreamUtils.zipWithIndex(strs.stream()).collect(Collectors.toList());
            Stream<Indexed<String>> xs = zi.stream().filter(t -> t.getIndex() % 2 == 0);
            Stream<Indexed<String>> ys = zi.stream().filter(t -> t.getIndex() % 2 != 0);
			List<Pair<String, String>> coors = StreamUtils.zip(
					xs, ys, (x, y) -> new ImmutablePair<>(x.getValue(), y.getValue())
			).collect(Collectors.toList());

			if(scaleInput) {
			    val scaled = (new CoordinatesScaler()).<String>scalePoints(1000, coors);
			    val scaling = scaled.getLeft();
				val points = StreamUtils.zipWithIndex(scaled.getRight().stream())
						.map((v) -> new Point(toIntExact(v.getIndex()) + 1,
								              v.getValue().getLeft().doubleValue(),
								              v.getValue().getRight().doubleValue()))
						.collect(Collectors.toList());
				return new ImmutablePair<>(scaling, points);
			} else {
				val points = StreamUtils.zipWithIndex(coors.stream())
						.map((v) -> new Point(toIntExact(v.getIndex()) + 1,
								new Double(v.getValue().getLeft()),
								new Double(v.getValue().getRight())))
						.collect(Collectors.toList());
				return new ImmutablePair<>(null, points);
			}
        } catch (NumberFormatException e) {
            throw new ParseException("Bad float format encountered in coordinates file");
        }
    }


    /**
     * This loads a list of vertex coordinates into the controller, akin to readFromFile().
     */
    public static void loadPoints(List<Point> pointsList, GraphicPanel panel, Controller controller) throws Exception {
        controller.reset();
        panel.revalidate();

        if(pointsList.size() > 1) {
            Point p1 = pointsList.get(0);
            Point p2 = pointsList.get(pointsList.size() - 1);
            if (p1.getOriginalX() == p2.getOriginalX() && p1.getOriginalY() == p2.getOriginalY())
                pointsList.remove(pointsList.size() - 1);
        }
        if(pointsList.size() < 3) {
            throw new Exception("Too few points given");
        }

        Set<Point> points = new LinkedHashSet<Point>(pointsList);
        Double max_x = null;
        Double max_y = null;
		Double min_x = null;
		Double min_y = null;
        List<Line> lines = new ArrayList<Line>();
        List<Line> screenLines = new ArrayList<Line>();

        if(pointsList.size() > 1) {
            Point p1 = pointsList.get(pointsList.size() - 1);
            Point sp1;
            try {
                sp1 = p1.clone();
            } catch (CloneNotSupportedException e) {
                throw new Exception("Implementation error");
            }

            for (Point p2: pointsList) {
                if (max_x == null || p2.getOriginalX() > max_x) max_x = new Double(p2.getOriginalX());
				if (min_x == null || p2.getOriginalX() < min_x) min_x = new Double(p2.getOriginalX());
                if (max_y == null || p2.getOriginalY() > max_y) max_y = new Double(p2.getOriginalY());
				if (min_y == null || p2.getOriginalY() < min_y) min_y = new Double(p2.getOriginalY());

                Point sp2;
                try {
                    sp2 = p2.clone();
                } catch (CloneNotSupportedException e) {
                    throw new Exception("Implementation error");
                }
                Line screenLine = new Line(sp1, sp2, 1);
                screenLines.add(screenLine);

                Line l = new Line(p1, p2, 1);
                p1.adjacentLines.add(l);
                p2.adjacentLines.add(l);
                lines.add(l);

                CustomTextField field = new CustomTextField(l, screenLine, controller);
                panel.add(field);
                panel.repaint();
                panel.revalidate();

                p1 = p2;
                sp1 = sp2;
            }
        }

        controller.setLoadedData(lines, screenLines, points);
        panel.setSize(new Dimension( (int) Math.ceil(max_x) + 20,  (int) Math.ceil(max_y) + 20));
        // panel.setPreferredSize(new Dimension( (int) Math.ceil(max_x) + 20, (int) Math.ceil(max_y) + 20));
		panel.setCoordinatesBounds(min_x, min_y, max_x, max_y);
        panel.repaint();
        if (Util.isCounterClockwise(pointsList)) {
            panel.repositionTextfields();
        }
    }


	// displays open file dialog and reads selected file using FileOpenService
	// TODO: add input scaling option to applet
	public static void open(GraphicPanel panel, Controller controller) {
		JFileChooser fc = new JFileChooser(parent);
		int retVal = fc.showOpenDialog(panel);
		if (retVal == 0) {
			file = fc.getSelectedFile();
			parent = file.getParent();
			try {
				Controller.inputScalingData = open(panel, controller, file, true);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}

    public static CoordinatesScaler.ScalingData
	open(GraphicPanel panel, Controller controller, File newfile, boolean scaleInput) throws Exception {
		//JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel);
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(panel);
        file = newfile;
		Path curpath = Paths.get((new File(System.getProperty("user.dir"))).getCanonicalPath());
		Path filepath = Paths.get(file.getCanonicalPath());
		String newpath = curpath.relativize(filepath).toString();
        try {
            try {
                val res = readCoordinatesFile(newfile, scaleInput);
                loadPoints(res.getRight(), panel, controller);
                if(frame != null)
					frame.setTitle("Weighted Straight Skeleton - " + newpath);
                return res.getLeft();
            } catch (ParseException ee) {
				// TODO: implement input scaling
                readFromFile(file, panel, controller);
				if(frame != null)
                    frame.setTitle("Weighted Straight Skeleton - " + newpath);
                return null;
            }
        } catch (ParseException e) {
			if(frame != null)
                frame.setTitle("Weighted Straight Skeleton");
            throw e;
        } catch (NumberFormatException | IOException e) {
			// TODO: implement input scaling
            try {
				openPoly(file, panel, controller);
				if(frame != null)
                    frame.setTitle("Weighted Straight Skeleton - " + newpath);
			} catch (Exception err) {
				if(frame != null)
                    frame.setTitle("Weighted Straight Skeleton");
            	err.printStackTrace();
			}
            return null;
        }
    }

  public static void createPoly(GraphicPanel panel, Controller controller, Graph graph) {
	  Double max_x = null;
	  Double max_y = null;
	  Double min_x = null;
	  Double min_y = null;
		List<Point> loadedVertices = new ArrayList<Point>();
		List<Point> screenVertices = new ArrayList<Point>();
		ArrayList<Line> loadedEdges = new ArrayList<>();
		ArrayList<Line> screenEdges = new ArrayList<>();

		if (graph.isEmpty()) {
			return;
		}
		if (panel != null) {
			controller.reset();
			panel.revalidate();
		}

		for (int i = 0; i < graph.getVertices().size(); i++) {
			Vertex v = graph.getVertices().get(i);
			if (max_x == null || v.getX() > max_x) max_x = new Double(v.getX());
			if (min_x == null || v.getX() < min_x) min_x = new Double(v.getX());
			if (max_y == null || v.getY() > max_y) max_y = new Double(v.getY());
			if (min_y == null || v.getY() < min_y) min_y = new Double(v.getY());
			loadedVertices.add(new Point(i + 1, v.getX(), v.getY()));
			screenVertices.add(new Point(i + 1, v.getX(), v.getY()));
		}
		for (int i = 0; i < graph.getEdges().size(); i++) {
			Edge edge = graph.getEdges().get(i);
			int indexV1 = edge.getV1().getIndex();
			int indexV2 = edge.getV2().getIndex();

			Point p1 = loadedVertices.get(indexV1 - 1);
			Point p2 = loadedVertices.get(indexV2 - 1);

			Line l;
			Line screenLine;
			if (p1.adjacentLines.size() == 0 || p1.adjacentLines.get(0).getP1().equals(p1)) {
				l = new Line(p2, p1, controller.randomWeights?controller.generateWeight():1);
				screenLine = new Line(screenVertices.get(indexV2 - 1), screenVertices.get(indexV1 - 1),
						l.getWeight());
			} else {
				l = new Line(p1, p2, 1);
				screenLine = new Line(screenVertices.get(indexV1 - 1), screenVertices.get(indexV2 - 1),
						l.getWeight());
			}

			loadedEdges.add(l);
			screenEdges.add(screenLine);
			p1.adjacentLines.add(l);
			p2.adjacentLines.add(l);

			if (panel != null) {
				CustomTextField field = new CustomTextField(l, screenLine, controller);
				panel.add(field);
				panel.repaint();
				panel.revalidate();
			}
		}
		loadedEdges = reorder(loadedEdges);
		Set<Point> points = new LinkedHashSet<Point>();
		for (Line l : loadedEdges) {
            points.add(l.getP1());
            // FIXME: we'll try and keep points' numbers in order for now --
            //        there seems to be at least some implicit reliance on the
            //        structure of those numbers (grep for use of
            //        vertex_counter, clonePoint & closePolygon for examples)
            l.getP1().setNumber(points.size());
        }

		if (panel != null) {
			controller.setLoadedData(loadedEdges, reorder(screenEdges), points);
			// panel.setPreferredSize(new Dimension((int) Math.ceil(max_x) + 20, (int) Math.ceil(max_y) + 20));
			panel.setCoordinatesBounds(min_x, min_y, max_x, max_y);
			panel.repaint();
			if (Util.isCounterClockwise(new ArrayList<Point>(loadedVertices))) {
				panel.repositionTextfields();
			}
		}
	}

	public static void openPoly(File file, GraphicPanel panel, Controller controller) throws Exception {
		Double max_x = null;
		Double max_y = null;
		Double min_x = null;
		Double min_y = null;
		// JFileChooser fc = new JFileChooser();
		// int retVal = fc.showOpenDialog(panel);
		// if (retVal == JFileChooser.APPROVE_OPTION) {
		// File file = fc.getSelectedFile();

		controller.reset();
		panel.revalidate();

		ArrayList<Point> loadedVertices = new ArrayList<>();
		ArrayList<Point> screenVertices = new ArrayList<>();
		ArrayList<Line> loadedEdges = new ArrayList<>();
		ArrayList<Line> screenEdges = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        NodeList vList = doc.getElementsByTagName("vertex");
        for (int i = 0; i < vList.getLength(); i++) {
            Node vNode = vList.item(i);
            if (vNode.getNodeType() == 1) {
                Element vElement = (Element) vNode;
                int x = Integer.parseInt(vElement.getElementsByTagName("x").item(0).getTextContent());
                int y = Integer.parseInt(vElement.getElementsByTagName("y").item(0).getTextContent());
                if (max_x == null || x > max_x) max_x = new Double(x);
                if (min_x == null || x < min_x) min_x = new Double(x);
                if (max_y == null || y > max_y) max_y = new Double(y);
                if (min_y == null || y < min_y) min_y = new Double(y);
                loadedVertices.add(new Point(i + 1, x, y));
                screenVertices.add(new Point(i + 1, x, y));
            }
        }
        NodeList eList = doc.getElementsByTagName("edge");
        for (int i = 0; i < eList.getLength(); i++) {
            Node eNode = eList.item(i);
            if (eNode.getNodeType() == 1) {
                Element eElement = (Element) eNode;
                int indexV1 = Integer.parseInt(eElement.getElementsByTagName("idV1").item(0).getTextContent());
                int indexV2 = Integer.parseInt(eElement.getElementsByTagName("idV2").item(0).getTextContent());

                Point p1 = loadedVertices.get(indexV1 - 1);
                Point p2 = loadedVertices.get(indexV2 - 1);
                Line l;
                Line screenLine;
                l = new Line(p2, p1, controller.randomWeights?controller.generateWeight():1);
                screenLine = new Line(screenVertices.get(indexV2 - 1),
                        screenVertices.get(indexV1 - 1), l.getWeight());

                loadedEdges.add(l);
                screenEdges.add(screenLine);
                p1.adjacentLines.add(l);
                p2.adjacentLines.add(l);

                CustomTextField field = new CustomTextField(l, screenLine, controller);
                panel.add(field);
                panel.repaint();
                panel.revalidate();
            }
        }
        loadedEdges = reorder(loadedEdges);
        Set<Point> points = new LinkedHashSet<Point>();
        for (Line l : loadedEdges) {
            points.add(l.getP1());
            // FIXME: we'll try and keep points' numbers in order for now --
            //        there seems to be at least some implicit reliance on the
            //        structure of those numbers (grep for use of
            //        vertex_counter, clonePoint & closePolygon for examples)
            l.getP1().setNumber(points.size());
        }

        controller.setLoadedData(loadedEdges, reorder(screenEdges), points);

        // panel.setPreferredSize(new Dimension((int) Math.ceil(max_x) + 20, (int) Math.ceil(max_y) + 20));
        panel.setCoordinatesBounds(min_x, min_y, max_x, max_y);
        panel.repaint();

        if (Util.isCounterClockwise(new ArrayList<Point>(loadedVertices))) {
            panel.repositionTextfields();
        }

        // g.load(loadedVertices, loadedEdges);
	}

	public static ArrayList<Line> reorder(List<Line> poly) {
		ArrayList<Line> reorderedEdges = new ArrayList<Line>();
		reorderedEdges.add(poly.get(0));
		Point p2 = poly.get(0).getP2();
		int size = poly.size();
		poly.remove(0);
		while (reorderedEdges.size() != size) {
			for (Line l : poly) {
				if (l.contains(p2)) {
					if (l.getP2().equals(p2)) {
						Point tmp = l.getP1();
						l.setP1(p2);
						l.setP2(tmp);
					}
					reorderedEdges.add(l);
					p2 = l.getP2();
					poly.remove(l);
					break;
				}
			}

		}
		return reorderedEdges;
	}

	// displays saveFileDialog and saves file using FileSaveService
	public static void save(List<Line> lines, boolean saveAs, CoordinatesScaler.ScalingData scalingData) {
		// initialize();
		// try {
		StringBuilder sb = new StringBuilder();
		val transform = scalingData == null ? null : CoordinatesScaler.inverseTransform(scalingData);
		for (Line l : lines) {
			Pair<Double, Double> p1 = new ImmutablePair<>(l.getP1().getOriginalX(), l.getP1().getOriginalY());
			Pair<Double, Double> p2 = new ImmutablePair<>(l.getP2().getOriginalX(), l.getP2().getOriginalY());
			if (scalingData != null) {
                val tp1 = transform.apply(p1);
				val tp2 = transform.apply(p2);
				p1 = new ImmutablePair<>(tp1.getLeft().doubleValue(), tp1.getRight().doubleValue());
				p2 = new ImmutablePair<>(tp2.getLeft().doubleValue(), tp2.getRight().doubleValue());
			}
			sb.append(l.getP1().getNumber() + ";" + p1.getLeft() + ";" + p1.getRight() + "-");
			sb.append(l.getP2().getNumber() + ";" + p2.getLeft() + ";" + p2.getRight() + "-");
			sb.append(l.getWeight() + "\n");
		}
		// Show save dialog if no name is already given
		if (file == null || saveAs) {
			JFileChooser fc = new JFileChooser(parent);
			if (file != null) {
				fc.setCurrentDirectory(file.getParentFile());
			}

			int returnVal = fc.showSaveDialog(fcFrame);

			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}
			file = fc.getSelectedFile();
		}

		if (file.getName().endsWith(".xz")) {
			try {
				writeXZFile(file, sb.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(file));
			out.write(sb.toString());
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void readFromFile(File file, GraphicPanel panel, Controller controller)
			throws IOException, NumberFormatException {
		if (file != null) {
			Double max_x = null;
			Double max_y = null;
			Double min_x = null;
			Double min_y = null;
			controller.reset();
			panel.revalidate();
			InputStream in = Files.newInputStream(file.toPath(), StandardOpenOption.READ);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();

			List<Line> lines = new ArrayList<>();
			List<Line> screenLines = new ArrayList<>();
			ArrayList<Point> points = new ArrayList<>();
			Point p1 = null;
			Point screenP1 = null;
			Point p2 = null;
			Point screenP2 = null;

            // FIXME: given implementation seems to rely on points' "numbers" being
            //        consequtive ordinals starting at 1 (grep for usage of
            //        vertex_counter or clonePoint) -- apparently not necessarily in
            //        order (apart from vertex_counter initialization, which
            //        definitely looks like a bug) though, if we look at use of
            //        FileHandler.reorder().
            //
            //        We'll try and ignore given numbers here (not for screen lines
            //        though -- if it works).
            ArrayList<Integer> pointIDs = new ArrayList<>();
            // backward lookup map -- in case we wanted to implement non-consecutive lines input:
            // HashMap<Integer, Integer> pointIDsLU = new HashMap<>();

			while (line != null) {
				String[] pts = line.split("-");

				String[] p_data1 = pts[0].split(";");

                Integer id = new Integer(Integer.parseInt(p_data1[0]));
                double x = Double.parseDouble(p_data1[1]);
                double y = Double.parseDouble(p_data1[2]);
                if (p1 == null) {
                    p1 = new Point(points.size()+1, x, y);
                    points.add(p1);
                    pointIDs.add(id);
                    // pointIDsLU.put(id, points.size());
					if (max_x == null || x > max_x) max_x = new Double(x);
					if (min_x == null || x < min_x) min_x = new Double(x);
					if (max_y == null || y > max_y) max_y = new Double(y);
					if (min_y == null || y < min_y) min_y = new Double(y);
                    // screenP1 = new Point(points.size(), x, y);
                    screenP1 = new Point(id.intValue(), x, y);
                } else {
                    if (pointIDs.get(pointIDs.size()-1).intValue() != id.intValue()) {
                        throw new ParseException("Not yet supported input feature.");
                    }
                    if (p2.getOriginalX() != x || p2.getOriginalY() != y )
                        throw new ParseException("Inconsistent input");
                }

                String[] p_data2 = pts[1].split(";");
                id = new Integer(Integer.parseInt(p_data2[0]));
                x = Double.parseDouble(p_data2[1]);
                y = Double.parseDouble(p_data2[2]);

                p2 = new Point(points.size()+1, x, y);
                points.add(p2);
                pointIDs.add(id);
                // pointIDsLU.put(id, points.size());
				if (max_x == null || x > max_x) max_x = new Double(x);
				if (min_x == null || x < min_x) min_x = new Double(x);
				if (max_y == null || y > max_y) max_y = new Double(y);
				if (min_y == null || y < min_y) min_y = new Double(y);
                // screenP2 = new Point(points.size(), x, y);
                screenP2 = new Point(id.intValue(), x, y);

                if (lines.size() > 0 && pointIDs.get(0).intValue() == pointIDs.get(pointIDs.size()-1).intValue()) {
                    p2 = points.get(0);
                    if (x != p2.getOriginalX() || y != p2.getOriginalY())
                        throw new ParseException("Inconsistent input for point " + String.valueOf(p2.getNumber()));
                    screenP2 = screenLines.get(0).getP1();
                }

                Line l = new Line(p1, p2, Integer.parseInt(pts[2]));
                p1.adjacentLines.add(l);
                p2.adjacentLines.add(l);
                Line screenLine = new Line(screenP1, screenP2, Integer.parseInt(pts[2]));
                screenLines.add(screenLine);
                lines.add(l);

                CustomTextField field = new CustomTextField(l, screenLine, controller);
                panel.add(field);
                panel.repaint();
                panel.revalidate();

                p1 = p2;
                screenP1 = screenP2;

                line = br.readLine();
            }
            br.close();
			in.close();

            if(points.size() == 0) {
                return;
            }
            if (pointIDs.get(pointIDs.size()-1).intValue() != pointIDs.get(0).intValue()) {
                throw new ParseException("Polygon not closed explicitly by last edge");
            }
            pointIDs.remove(pointIDs.size()-1);
            if(pointIDs.stream().map((x) -> x.intValue()).distinct().count() != pointIDs.size()) throw new ParseException("Bad polygon");
            points.get(points.size()-1).setNumber(1);

            // controller.reset();
            // FIXME: do we want to otherwise enforce uniqueness of points?
            controller.setLoadedData(lines, screenLines, new LinkedHashSet<Point>(points));
			// panel.setPreferredSize(new Dimension((int) Math.ceil(max_x) + 20, (int) Math.ceil(max_y) + 20));
			panel.setCoordinatesBounds(min_x, min_y, max_x, max_y);
			panel.repaint();
			if (Util.isCounterClockwise(new ArrayList<Point>(points))) {
				panel.repositionTextfields();
			}

		}
	}

	public static void saveSVG(final GraphicPanel panel, boolean saveAs) {
		JFileChooser fc = new JFileChooser(parent);
		if (svgfile != null) {
			fc.setCurrentDirectory(svgfile.getParentFile());
		}
		if (saveAs) {

			int returnVal = fc.showSaveDialog(fcFrame);

			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}
			svgfile = fc.getSelectedFile();
			// FIXME: Really?
			// if (!svgfile.getPath().toLowerCase().endsWith(".svg.gz")) {
			// 	svgfile = new File(svgfile.getPath() + ".svg.gz");
			// }
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				SVGGraphics2D g = new SVGGraphics2D(panel.getWidth(), panel.getHeight());
				panel.paintSVG(g);

				try {
					SVGUtils.writeToSVG(svgfile, g.getSVGElement(), true);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		});

	}


	public static void writeXZFile(String filename, String payload) throws IOException {
		File file = new File(filename);
		writeXZFile(file, payload);
	}


	public static void writeXZFile(File file, String payload) throws IOException {
		if (file.getParentFile() != null) file.getParentFile().mkdirs();
		file.createNewFile();
		FileOutputStream fos = new FileOutputStream(file);
		XZOutputStream xzos = new XZOutputStream(fos, new LZMA2Options());
		PrintWriter pw = new PrintWriter(xzos);
		try {
			pw.write(payload);
			xzos.finish();
		} finally {
			xzos.close();
			fos.close();
		}
	}

}
