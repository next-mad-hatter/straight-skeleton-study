package at.tugraz.igi.main;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.List;

import at.tugraz.igi.ui.ConfigurationTable;
import at.tugraz.igi.ui.GraphicPanel;
import at.tugraz.igi.util.*;
import at.tugraz.igi.util.Point;

public class Run {

    public static void run(String in_file, String out_file, String img_file) throws Exception {

        Controller controller = new Controller();
        GraphicPanel panel = new GraphicPanel(controller);
        controller.setView(panel);
        controller.setTable(new ConfigurationTable(controller));

        FileHandler.open(panel, controller, new File(in_file));
        // FIXME: add autozoom
        panel.setSize(new Dimension(2000, 2000));

        controller.runAlgorithmNoSwingWorker();
        if (!controller.finished) {
            throw new Exception("Algorithm didn't finish");
        }

        // FIXME: get event counts

        FileHandler.file = new File(out_file);
        if (FileHandler.file.getParentFile() != null) FileHandler.file.getParentFile().mkdirs();
        FileHandler.save(controller.polyLines, false);
        if (img_file != null) {
            File outputfile = new File(img_file);
            if (outputfile.getParentFile() != null) outputfile.getParentFile().mkdirs();
            ImageIO.write(createImage(controller.view), "png", outputfile);
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
