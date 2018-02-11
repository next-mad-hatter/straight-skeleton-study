package at.tugraz.igi.main;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import at.tugraz.igi.ui.ConfigurationTable;
import at.tugraz.igi.ui.GraphicPanel;
import at.tugraz.igi.util.FileHandler;
import at.tugraz.igi.util.Line;

public class SingleRun {

    public static void main(String[] args) throws Exception {

        if (args.length != 3) {
            StackTraceElement[] stackTraceElements= new Exception().getStackTrace();
            String className = stackTraceElements[0].getClassName();
            System.out.println("Usage: " + className + " in_file out_file out_image");
            System.exit(1);
        }

        Controller controller = new Controller();
        GraphicPanel panel = new GraphicPanel(controller);
        controller.setView(panel);
        controller.setTable(new ConfigurationTable(controller));

        FileHandler.open(panel, controller, new File(args[0]));
        // FIXME: add autozoom
        panel.setSize(new Dimension(2000, 2000));

        try {
            controller.runAlgorithmNoSwingWorker();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        while (!controller.finished) {
            System.out.println("Implementation error");
            System.exit(1);
        }

        // FIXME: get event counts

        FileHandler.file = new File(args[1]);
        FileHandler.save(controller.polyLines, false);

        File outputfile = new File(args[2]);
        try {
            ImageIO.write(createImage(controller.view), "png", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
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
