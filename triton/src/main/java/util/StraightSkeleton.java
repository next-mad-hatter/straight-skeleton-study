package at.tugraz.igi.util;

import lombok.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class StraightSkeleton {
	public Map<List<Line>, MeasuringData> polygon;
	@Getter @Setter private List<Line>lines;
	@Getter @Setter private List<Line> polyLines;
	@Getter @Setter private Color color;

	public StraightSkeleton(){
		lines = new ArrayList<Line>();
		polyLines = new ArrayList<Line>();
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
	public boolean contains(Line l){
		return lines.contains(l);
	}

}

