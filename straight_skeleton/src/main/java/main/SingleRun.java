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

public class SingleRun {

    public static void main(String[] args) throws Exception {

        if (args.length < 2 || args.length > 3) {
            StackTraceElement[] stackTraceElements= new Exception().getStackTrace();
            String className = stackTraceElements[0].getClassName();
            System.out.println("Usage: " + className + " in_file out_file [out_image]");
            System.exit(1);
        }

        Run.run(args[0], args[1], args.length > 2 ? args[2] : null);
    }

}
