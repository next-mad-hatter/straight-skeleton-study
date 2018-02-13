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


import java.nio.file.*;
import java.util.stream.*;

public class BatchRun {
    

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            StackTraceElement[] stackTraceElements= new Exception().getStackTrace();
            String className = stackTraceElements[0].getClassName();
            System.out.println("Usage: " + className + " batch_file");
            System.exit(1);
        }

        try(BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            for(String line; (line = br.readLine()) != null; ) {
                String[] strs = line.split("\\s+");
                if (strs.length < 2 || strs.length > 3) {
                    throw new Exception("Bad input file line: " + line);
                }
                Run.run(strs[0], strs[1], strs.length > 2 ? strs[2] : null);
            }
        }

        /*
        try (Stream<String> stream = Files.lines(Paths.get(args[0]))) {
            stream.forEach((x) -> System.out.println(x));
        }
        */
    }
}
