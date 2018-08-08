# Kotton -- Weighted Straight Skeleton Algorithms

Here you'll find source code for kotton -- a wrapper to run algorithms computing
weighted straight skeletons.  Currently it can call two particular implementations --
campskeleton and triton (latter is currently also contained in this repository).

## Building

To build kotton, you'll need to build (patched) campskeleton and triton first.

### Campskeleton

Clone

  * https://github.com/twak/jutils
  * https://github.com/next-mad-hatter/campskeleton

and run `mvn compile install` in each to install to local maven repo.
For some theory on these, see
  http://www.twak.co.uk/2009/05/engineering-weighted-straight-skeleton.html
and
  http://www.twak.co.uk/2011/01/degeneracy-in-weighted-straight.html .


### Triton (a triangulation keeping algorithm)

The code found under `triton` should be buildable with gradle.
It requires manual download of two libraries -- see `triton/lib/EXTRA_LIBS`.
Run `gradle publishToMavenLocal` to install it.


### Kotton

Once you've installed the dependencies, kotton can be built via `gradle build`
inside `kotton` directory.  You can then run
`java -jar build/libs/kotton-all.jar --help` to see available runtime options.
For better performance, consider setting -Xmx4096M or more :).


# Triton standalone runners

You still can run triton standalone:

  * Main class starts the interactive swing applet (default in the built
    jar file).

  * SingleRun class runs the algorithm for a given data file and writes
    results to given output file(s) -- but consider using kotton instead.

  * BatchRun expects a file where each (non-empty) line contains a list of
    input and output parameters, and runs the algorithm for every
    such line (see `bin/run_batch.sh` and `data/batches/simple-batch.txt`).
    It is advisable to split very large batches into chunks of under 10k items
    each, as the speed seems to deteriorate steadily here (due to garbage
    collection and swing issues?).  Again, consider using kotton instead.

From `triton` directory, those can be invoked via `gradle run` (see `build.gradle` file for details),
or (after a successful `gradle build`) from the built jar (`build/libs/triton-all.jar`) directly.

