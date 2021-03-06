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

import me.tongfei.progressbar.*;

import java.nio.file.*;
import java.util.stream.*;

public class BatchRun {

    /**
     * Computes a straight skeleton for each line of given input (can be a
     * file name or '-' for stdin), where said line is interpreted as
     * containing data file name and output file(s)' name(s), separated by
     * (unescaped) whitespace. Parts of lines starting with '#' are ignored.
     *
     * While requirements allow this, we slurp whole file into memory for
     * purpose of having a definite progress indication.
     */
    public static void main(String[] args) throws Exception {

        boolean scaleInput = ArrayUtils.contains(args, "-s");
        if (scaleInput) args = ArrayUtils.removeAllOccurences(args, "-s");

        boolean timeout = ArrayUtils.contains(args, "-t");
        if (timeout) args = ArrayUtils.removeAllOccurences(args, "-t");

        if (!(args.length == 1)) {
            StackTraceElement[] stackTraceElements= new Exception().getStackTrace();
            String className = stackTraceElements[0].getClassName();
            System.out.println("Usage: " + className + " [-s] batch_file");
            System.exit(1);
        }

        ArrayList<List<String>> lines = new ArrayList<>();
        Integer line_count = 0;
        try(BufferedReader br = (args[0] == "-") ?
            new BufferedReader(new InputStreamReader(System.in)) :
            new BufferedReader(new FileReader(args[0]))
            ) {
            for(String line; (line = br.readLine()) != null; ) {
                line_count += 1;
                line = line.replaceFirst("#.*$", "").trim();
                if (line.isEmpty()) continue;
                List<String> strs = Arrays.asList(line.split("(?<!\\\\)\\s+"));
                strs = strs.stream().map((x) -> x.replaceAll("\\\\ ", " ")).collect(Collectors.toList());
                if (strs.size() < 2 || strs.size() > 4) {
                    throw new Exception("Bad input file line " + line_count);
                }
                lines.add(strs);
            }
        }

        ProgressBar pb = new ProgressBar("Test", lines.size(), 400);
        Integer succeeded = 0;
        Integer failed = 0;
        pb.start();
        int counter = 1;
        for(List<String> strs: lines) {
            try {
                Run.run(strs.get(0),
                        strs.get(1),
                        strs.size() > 2 ? strs.get(2): null,
                        strs.size() > 3 ? strs.get(3): null,
                        scaleInput,
                        timeout ? 3 : null,
                        counter % 100 == 0);
                succeeded += 1;
            } catch (Exception ee) {
                Throwable e = ee.getCause();
                if (e == null) e = ee;
                System.err.println();
                System.out.println("Error encountered while processing data file " +
                                   strs.get(0) + " : " + e.toString());
                e.printStackTrace();
                failed += 1;
            } finally {
                counter += 1;
            }
            pb.step();
            pb.setExtraMessage("Done: " + String.valueOf(succeeded + failed) +
                               "; finished: " + succeeded.toString() +
                               ", failed: " + failed.toString() + ".");
        }
        pb.stop();
        System.out.println("Done: " + String.valueOf(succeeded + failed) +
                           "; finished: " + succeeded.toString() +
                           ", failed: " + failed.toString() + ".");

        System.exit(0);
    }

}
