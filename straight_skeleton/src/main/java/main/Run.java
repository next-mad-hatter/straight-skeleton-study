package at.tugraz.igi.main;

import lombok.*;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;
import java.nio.file.*;
import javax.swing.*;
import java.util.stream.*;

import at.tugraz.igi.ui.*;
import at.tugraz.igi.util.*;
import at.tugraz.igi.util.Point;
import java.util.concurrent.*;

import org.jfree.graphics2d.svg.*;

public class Run {

    /**
     * Given a data file name and output file(s)' name(s), computes the straight
     * skeleton.  img_file can be null.
     */
    public static void run(String in_file, String out_file, String stats_file, String img_file, boolean scaleInput, Integer seconds, boolean runGC) throws Exception {

        Controller controller = new Controller();
        GraphicPanel panel = new GraphicPanel(controller);
        controller.setView(panel);
        controller.setTable(new ConfigurationTable(controller));

        val scalingData = FileHandler.open(panel, controller, new File(in_file), scaleInput);
        // FIXME: add autozoom
        panel.setSize(new Dimension(2000, 2000));

        class Call implements Callable<Boolean> {
            @Override
            public Boolean call() throws Exception {
                controller.runAlgorithmNoSwingWorker();
                return true;
            }
        }
        val executor = Executors.newSingleThreadExecutor();
        val future = executor.submit(new Call());
        executor.shutdown();
        try {
            if(seconds == null)
                future.get();
            else
                future.get(seconds, TimeUnit.SECONDS);
        }
        catch (ExecutionException ee) {
            throw new Exception(ee.getCause());
        }
        catch (TimeoutException te) {
            throw new Exception("Algorithm timed out");
        }
        if (!executor.isTerminated())
            executor.shutdownNow();

        if (!controller.finished) {
            throw new Exception("Algorithm didn't finish");
        }

        // FIXME: currently the algorithm includes some skeleton arcs/edges more than once in the result.
        //        We'll try and repair those cases here for now.
        List<Line> skeleton = controller.getStraightSkeleton().getLines()
            .stream()
            .map((l) -> l.getP1().getNumber() < l.getP2().getNumber() ? l : new Line(l.getP2(), l.getP1(), l.getWeight()))
            //.filter((l) -> l.getP1() != l.getP2())
            .distinct().collect(Collectors.toList());

        FileHandler.file = new File(out_file);
        if (FileHandler.file.getParentFile() != null) FileHandler.file.getParentFile().mkdirs();
        FileHandler.save(skeleton, false, scalingData);

        if (stats_file != null && controller.polyMeasureData != null) {
            String stats = "# Flip events:  " + String.valueOf(controller.polyMeasureData.getNumberOfFlip()) +
                    "\n# Edge events:  " + String.valueOf(controller.polyMeasureData.getNumberOfEdge()) +
                    "\n# Split events: " + String.valueOf(controller.polyMeasureData.getNumberOfSplit());
            Files.write(Paths.get(stats_file), stats.getBytes());
        }

        if (img_file != null) {
            File outputfile = new File(img_file);
            if (outputfile.getParentFile() != null)
                outputfile.getParentFile().mkdirs();
            outputfile.createNewFile();
            if (img_file.endsWith(".png")) {
                ImageIO.write(createImage(controller.view), "png", outputfile);
            } else if (img_file.endsWith(".xz")) {
                SVGGraphics2D g = new SVGGraphics2D(panel.getWidth(), panel.getHeight());
                panel.paintSVG(g);
                try {
                    if (img_file.endsWith(".xz")) {
                        FileHandler.writeXZFile(outputfile, g.getSVGDocument());
                    } else {
                        // NOTE: apparently invokeLater here fills up the awt event queue
                        //       which makes our vm crash with oom error.
                        //       All my attempts to fix it had been fruitless,
                        //       so we won't be calling it.
                        // FileHandler.svgfile = outputfile;
                        // FileHandler.saveSVG(controller.view, false);
                        SVGUtils.writeToSVG(outputfile, g.getSVGElement(), img_file.endsWith(".gz"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        TreeCheck.checkTree(new ArrayList<>(controller.getPoints()), skeleton);

        if(runGC) {
            // Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue());
            /*
            for (val w : Window.getWindows()) {
            // for (val w : Frame.getFrames()) {
                // w.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                w.setVisible(false);
                w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
                w.dispose();
            }
            */
            Runtime.getRuntime().gc();
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
