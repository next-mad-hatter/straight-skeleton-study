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

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import at.tugraz.igi.main.Controller;
import at.tugraz.igi.ui.CustomTextField;
import at.tugraz.igi.ui.GraphicPanel;
import data.Edge;
import data.Graph;
import data.Vertex;

public class FileHandler {

	public static File file = null;
	public static File svgfile = null;
  public static String parent = (new File(System.getProperty("user.dir"))).getPath();

	// displays open file dialog and reads selected file using FileOpenService
    public static void open(GraphicPanel panel, Controller controller) {
		JFileChooser fc = new JFileChooser(parent);
		int retVal = fc.showOpenDialog(panel);
		if (retVal == 0) {
			file = fc.getSelectedFile();
			parent = file.getParent();
      open(panel, controller, file);
		}
	}

    public static void open(GraphicPanel panel, Controller controller, File newfile) {
        file = newfile;
        try {
            readFromFile(file, panel, controller);
        } catch (NumberFormatException | IOException e) {
            openPoly(file, panel, controller);
        }
    }

	public static void createPoly(GraphicPanel panel, Controller controller, Graph graph) {
		int max_x = 0, max_y = 0;
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
			if (v.getX() > max_x) {
				max_x = v.getX();
			}
			if (v.getY() > max_y) {
				max_y = v.getY();
			}
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
		}

		if (panel != null) {
			controller.setLoadedData(loadedEdges, reorder(screenEdges), points);
			panel.setPreferredSize(new Dimension(max_x + 20, max_y + 20));
			panel.repaint();
			if (Util.isCounterClockwise(new ArrayList<Point>(loadedVertices))) {
				panel.repositionTextfields();
			}
		}
	}

	public static void openPoly(File file, GraphicPanel panel, Controller controller) {
		int max_x = 0, max_y = 0;
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
		try {
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
					if (x > max_x) {
						max_x = x;
					}
					if (y > max_y) {
						max_y = y;
					}
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
			}

			controller.setLoadedData(loadedEdges, reorder(screenEdges), points);

			panel.setPreferredSize(new Dimension(max_x + 20, max_y + 20));
			panel.repaint();

			if (Util.isCounterClockwise(new ArrayList<Point>(loadedVertices))) {
				panel.repositionTextfields();
			}

			// g.load(loadedVertices, loadedEdges);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	public static void save(List<Line> lines, boolean saveAs) {
		// initialize();
		// try {
		StringBuilder sb = new StringBuilder();
		for (Line l : lines) {
			Point p1 = l.getP1();
			Point p2 = l.getP2();
			sb.append(p1.getNumber() + ";" + p1.getOriginalX() + ";" + p1.getOriginalY() + "-");
			sb.append(p2.getNumber() + ";" + p2.getOriginalX() + ";" + p2.getOriginalY() + "-");
			sb.append(l.getWeight() + "\n");
		}
		// Show save dialog if no name is already given
		if (file == null || saveAs) {
			JFileChooser fc = new JFileChooser(parent);
			if (file != null) {
				fc.setCurrentDirectory(file.getParentFile());
			}
			int returnVal = fc.showSaveDialog(null);
			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}
			file = fc.getSelectedFile();
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
			int max_x = 0, max_y = 0;
			controller.reset();
			panel.revalidate();
			InputStream in = Files.newInputStream(file.toPath(), StandardOpenOption.READ);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();

			List<Line> lines = new ArrayList<Line>();
			List<Line> screenLines = new ArrayList<Line>();
			Set<Point> points = new LinkedHashSet<Point>();
			Point p1 = null;
			Point screenP1 = null;
			while (line != null) {
				String[] pts = line.split("-");

				String[] p_data1 = pts[0].split(";");
				if (p1 == null) {
					p1 = new Point(Integer.parseInt(p_data1[0]), Double.parseDouble(p_data1[1]),
							Double.parseDouble(p_data1[2]));
					points.add(p1);
					screenP1 = new Point(p1.getNumber(), p1.getOriginalX(), p1.getOriginalY());

				}
				String[] p_data2 = pts[1].split(";");
				Point p2 = new Point(Integer.parseInt(p_data2[0]), Double.parseDouble(p_data2[1]),
						Double.parseDouble(p_data2[2]));
				if (p2.getOriginalX() > max_x) {
					max_x = (int) p2.getOriginalX();
				}
				if (p2.getOriginalY() > max_y) {
					max_y = (int) p2.getOriginalY();
				}
				Point screenP2 = new Point(p2.getNumber(), p2.getOriginalX(), p2.getOriginalY());
				if (lines.size() > 0 && p2.equals(lines.get(0).getP1())) {
					p2 = lines.get(0).getP1();
					screenP2 = screenLines.get(0).getP1();
				}

				points.add(p2);

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

			// controller.reset();
			controller.setLoadedData(lines, screenLines, points);
			panel.setPreferredSize(new Dimension(max_x + 20, max_y + 20));
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
			int returnVal = fc.showSaveDialog(null);
			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}
			svgfile = fc.getSelectedFile();
			if (!svgfile.getPath().toLowerCase().endsWith(".svg")) {
				svgfile = new File(svgfile.getPath() + ".svg");
			}
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				SVGGraphics2D g = new SVGGraphics2D(panel.getWidth(), panel.getHeight());
				panel.paintSVG(g);

				try {
					SVGUtils.writeToSVG(svgfile, g.getSVGElement());
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		});

	}

}
