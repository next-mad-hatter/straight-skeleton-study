package at.tugraz.igi.util;

import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolygonMeasureData {
	public List<MeasuringData> subPolygonData;
	public double mean_area;
	public double variance_area;
	public double standardDeviation_area;
	public double runningTime;
	@Getter private int numberOfFlip;
	@Getter private int numberOfEdge;
	@Getter private int numberOfSplit;
 
	

	public PolygonMeasureData() {
		this.subPolygonData = new ArrayList<MeasuringData>();
	}
	
	public Line getLineWithMinLength(){
		Collections.sort(subPolygonData, new DataComparator());
		return subPolygonData.get(0).getPolyLine();
		
	}
	
	public Line getLineWithMaxLength(){
		Collections.sort(subPolygonData, new DataComparator());
		return subPolygonData.get(subPolygonData.size()-1).getPolyLine();
		
	}
	
	public Line getLineWithMeanLength(){
		Collections.sort(subPolygonData, new DataComparator());
		double length = 0;
		for(MeasuringData data: subPolygonData){
			length += data.length();
		}
		double mean_length = length/subPolygonData.size();
		Map<Double, Line> diffLineMapping = new HashMap<Double, Line>();
		for(MeasuringData data: subPolygonData){
			double d_len = data.length();
			diffLineMapping.put(Math.abs(d_len -mean_length), data.getPolyLine());
		}
		List<Double> keyList = new ArrayList<Double>(diffLineMapping.keySet());
		Collections.sort(keyList);
		return diffLineMapping.get(keyList.get(0));
	}
	
	public StringBuffer print(){
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("%s;%s;%s\n", "Mean value", "Variance", "Standard Deviation"));
		sb.append(String.format("%s;%s;%s;%s\n", mean_area, variance_area, standardDeviation_area, Integer.toString(subPolygonData.size())));
		sb.append(String.format("%d;%d;%d;\n", numberOfEdge, numberOfSplit, numberOfFlip));
		for (MeasuringData data: subPolygonData){
			sb.append(data.getPolyLine());
		}
		sb.append(String.format("\nWeight;Area;Length;Number of Lines\n"));
		sb.append("[");
		for (MeasuringData data: subPolygonData){
			sb.append(data.print());
		}
		sb.append("];");
		return sb;
	}
	public double[] create_array(){
		double[] data = new double[subPolygonData.size()*4];
		int i =0;
		for (MeasuringData mData: subPolygonData){
			data[i] = mData.getPolyLine().getWeight();
			data[i+1] = mData.getArea();
			data[i+2] = mData.length();
			data[i+3] = mData.getPolygon().size();
			i+=4;
		}
		return data;
	}
	
	public void addNumberOfEvents(int flip, int edge, int split){
		numberOfFlip = flip;
		numberOfEdge = edge;
		numberOfSplit = split;
	}
}
class DataComparator implements Comparator<MeasuringData> {
    @Override
    public int compare(MeasuringData a, MeasuringData b) {
        return a.length() < b.length() ? -1 : a.length() == b.length() ? 0 : 1;
    }
}