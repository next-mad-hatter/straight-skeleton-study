package at.tugraz.igi.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StraightSkeleton {
	public Map<List<Line>, MeasuringData> polygon;
	private List<Line>lines;
	private List<Line> polyLines;
	private Color color;
	private boolean visible;
	
	public StraightSkeleton(){
		lines = new ArrayList<Line>();
		polyLines = new ArrayList<Line>();
		visible = true;
	}
	public void add(Line l){
		lines.add(l);
	}
	public void add(List<Line>lines){
		this.lines.addAll(lines);
	}
	public void clear(){
//		this.polyLines.clear();
		this.lines.clear();
	}
	public List<Line> getLines() {
		return lines;
	}
	
	public boolean contains(Line l){
		return lines.contains(l);
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	public List<Line> getPolyLines() {
		return polyLines;
	}
	public void setPolyLines(List<Line> polyLines) {
		this.polyLines = polyLines;
//		this.polyLines.clear();
//		this.polyLines.addAll(polyLines);
	}
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	
}

