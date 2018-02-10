package at.tugraz.igi.util;

import java.util.List;

public class MeasuringData {
	private List<Line> polygon;
	private double area;
	private Line polyLine;

	public MeasuringData(Line line, List<Line> poly, double area) {
		this.polygon = poly;
		this.polyLine = line;
		this.area = area;
	}

	public List<Line> getPolygon() {
		return polygon;
	}

	public double getArea() {
		return area;
	}

	public Line getPolyLine() {
		return polyLine;
	}
	
	public double length(){
		int DPI = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		double length = polyLine.getLineVector().length();
//		return length;
		return ((length * 2.54)/DPI);
	}
	public StringBuffer print(){
//		StringBuffer sb = new StringBuffer();
//		sb.append(String.format("Polygon %s\n",polyLine));
//		sb.append(String.format("Line;Weight;Area;Length;Number of Lines\n"));
//		sb.append(String.format("%s,%s,%s,%s,%s;\n", polyLine,Integer.toString(polyLine.getWeight()), Double.toString(area), length(), polygon.size()));
//		for(Line l:polygon){
//			sb.append(l);
//		}
//		sb.append("\n");
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("%s,%s,%s,%s;\n",Integer.toString(polyLine.getWeight()), Double.toString(area), length(), polygon.size()));
//		for(Line l:polygon){
//			sb.append(l);
//		}
//		sb.append("\n");
		return sb;
	}
}
