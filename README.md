# Weighted Straight Skeleton

### Triangulation keeping algorithm -- a Java Implementation

The code found under `straight_skeleton` should be buildable with gradle.

Right now, there are three ways to run it:

  * Main class starts the interactive swing applet (default in the built
    jar file)

  * SingleRun class runs the algorithm for a given data file and writes
    results to given output file(s).

  * BatchRun expects a file where each (non-empty) line contains a list of
    input and output parameters, and runs the algorithm for every
    such line (see `bin/run_batch.sh` and `data/batches/simple-batch.txt`).

From `straight_skeleton` directory, those can be invoked via `gradle run`
(see `build.gradle` file for details), or (after a successful `gradle
build`) from the built jar directly, e.g. as

```
java -cp build/libs/straight_skeleton.jar at.tugraz.igi.main.SingleRun [-s] in.file out.skel out.stat out.png
```
,
```
java -cp build/libs/straight_skeleton.jar at.tugraz.igi.main.BatchRun [-s] batch.txt
```
.
