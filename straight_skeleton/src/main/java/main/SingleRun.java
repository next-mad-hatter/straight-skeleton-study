package at.tugraz.igi.main;

import org.apache.commons.lang3.*;

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

public class SingleRun {

    /**
     * Given a data file name and output file(s)' name(s), computes the straight
     * skeleton.
     */
    public static void main(String[] args) throws Exception {

        boolean scaleInput = ArrayUtils.contains(args, "-s");
        if (scaleInput) args = ArrayUtils.removeAllOccurences(args, "-s");

        if (args.length < 2 || args.length > 4) {
            StackTraceElement[] stackTraceElements= new Exception().getStackTrace();
            String className = stackTraceElements[0].getClassName();
            System.out.println("Usage: " + className + " [-s] in_file skeleton_file [stats_file] [image_file]");
            System.exit(1);
        }

        try {
          Run.run(args[0],
                  args[1],
                  args.length > 2 ? args[2] : null,
                  args.length > 3 ? args[3] : null,
                  scaleInput);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
