package at.tugraz.igi.main;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import at.tugraz.igi.ui.ConfigurationTable;
import at.tugraz.igi.ui.GraphicPanel;
import at.tugraz.igi.util.FileHandler;
import at.tugraz.igi.util.Line;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

import data.Graph;
import frontend.MainFrame;

public class TestScript {

	public static void main(String[] args) throws Exception {

          /*
          kotlindemo.AdderKt.split("Three");
          kotlindemo.AdderKt.add(3, 5);
          */

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();
		//String path = String.format("D:\\Uni\\SVN\\PR_Algorithmen\\TestResults\\%s", dateFormat.format(date));
		String path = String.format("./test-%s/", dateFormat.format(date));
		int iteration = 1;
		int numPoints = 50;
		Dimension bounds = new Dimension(1264, 689);
		// create random point set
		MainFrame gui = new MainFrame();
		Graph graph = gui.getGraph();
		for (int i = 0; i < iteration; i++) {
			// try {
			String dirPath = String.format("%s/%d/", path, i);
			String dataPath = String.format("%s/%s/", dirPath, "MeasureData");
			new File(dirPath).mkdirs();
			new File(dataPath).mkdirs();
			graph.generateRandomPointSet(numPoints, bounds);

			// choose/execute algorithm
			String algo = "";
			for (int j = 0; j < 4; j++) {
				Collection<MLArray> matMapping = new ArrayList<MLArray>();
				switch (j) {
				case 0:
					graph.ExecuteConvexLayersB();
					algo = "convex";
					break;
				case 1:
					graph.ExecuteRandomTwoPeasants();
					algo = "twoPeasants";
					break;
				case 2:
					graph.ExecuteSteadyGrowth();
					algo = "SteadyGrowth";
					break;
				case 3:
					graph.ExecuteTwoOptMoves();
					algo = "TwoOptMoves";
					break;
				}
				Controller controller = new Controller();
				GraphicPanel panel = new GraphicPanel(controller);
				panel.setSize(new Dimension(1264, 689));
				controller.setView(panel);
				controller.setTable(new ConfigurationTable(controller));
				controller.createPolygon(graph);
				String algoPath = String.format("%s/%s/", dirPath, algo);
				new File(algoPath).mkdirs();
				int count_dirs = 0;
				for (int l = 0; l < 5; l++) {
					// create straight skeleton

					if (l == 1 || l == 2 || l == 3) {
						Line line = null;
						switch (l) {
						case (1):
							line = controller.polyMeasureData.getLineWithMinLength();
							break;
						case (2):
							line = controller.polyMeasureData.getLineWithMaxLength();
							break;
						case (3):
							line = controller.polyMeasureData.getLineWithMeanLength();
							break;
						}
						for (Line li : controller.getStraightSkeleton().getPolyLines()) {
							if (li.equals(line)) {
								li.setWeight(20);
								continue;
							}
							li.setWeight(1);
						}
						controller.restart(controller.getStraightSkeleton().getPolyLines());

					} else if (l == 4) {
						for (Line line : controller.getStraightSkeleton().getPolyLines()) {
							Random randomGenerator = new Random();
							int weight = randomGenerator.nextInt(10);
							line.setWeight(weight == 0 ? 1 : weight);
						}
						controller.restart(controller.getStraightSkeleton().getPolyLines());
					}
					try {
						controller.runAlgorithmNoSwingWorker();
					} catch (Exception e) {
            e.printStackTrace();
            break;
					}
					while (!controller.finished) {
              throw new Exception("I am not supposed to happen and probably am an artefact");
              // Otherwise FIXME: wait for worker rather than hogging CPU?
					}

					String weighting = null;
					switch (l) {
					case (0):
						weighting = "NoWeights";
						break;
					case (1):
						weighting = "OneWeightedMin";
						break;
					case (2):
						weighting = "OneWeightedMax";
						break;
					case (3):
						weighting = "OneWeightedMean";
						break;
					case (4):
						weighting = "RandomWeights";
						break;
					}
					String varName = algo + weighting;
					FileHandler.file = new File(String.format("%s%s%s", algoPath, weighting, ".txt"));
					FileHandler.save(controller.polyLines, false);

					File outputfile = new File(String.format("%s%s%s", algoPath, weighting, ".jpg"));
					try {
						ImageIO.write(createImage(controller.view), "jpg", outputfile);
					} catch (IOException e) {
						e.printStackTrace();
					}
					FileHandler.svgfile = new File(String.format("%s%s%s", algoPath, weighting, ".svg"));
					FileHandler.saveSVG(controller.view, false);

					try {
						BufferedWriter out = new BufferedWriter(new FileWriter(new File(String.format("%s/%s-%s",
								dataPath, algo, weighting))));
						out.write(controller.polyMeasureData.print().toString());
						out.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (controller.polyMeasureData.subPolygonData.size() == numPoints) {
						matMapping.add(new MLDouble(varName, controller.polyMeasureData.create_array(), numPoints));
					}
					count_dirs++;
				}
				new MatFileWriter(String.format("%s%s.mat", dataPath, algo), matMapping);
        /*
				if (count_dirs != 5) {
					new File(dirPath).delete();
				}
        */

			}
			// } catch (Exception e) {
			// continue;
			// }
		}

	}

	public static BufferedImage createImage(JPanel panel) {
		int w = panel.getWidth();
		int h = panel.getHeight();
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		panel.print(g);

		return bi;
	}
}
