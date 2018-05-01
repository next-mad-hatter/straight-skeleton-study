# Kotton -- Weighted Straight Skeleton Algorithms

Note: this probably will be refactored soon
(triton to its own repo, dependencies to submodules).

## Building

To build kotton, you'll need to build campskeleton and triton first.

### Campskeleton

Clone

  * https://github.com/twak/jutils
  * https://github.com/next-mad-hatter/campskeleton

and run `mvn compile install` in each to install to local maven repo.

### Triton (a triangulation keeping algorithm)

The code found under `triton` should be buildable with gradle.
Run `gradle publishToMavenLocal` to install it.

### Kotton

Once you've installed the dependencies, kotton can be built via `gradle build`
inside `kotton` directory.  You can then run
`java -jar build/libs/kotton-all.jar --help` to see available runtime options.


# Triton standalone runners

You still can run triton standalone:

  * Main class starts the interactive swing applet (default in the built
    jar file)

  * SingleRun class runs the algorithm for a given data file and writes
    results to given output file(s) -- use kotton instead.

  * BatchRun expects a file where each (non-empty) line contains a list of
    input and output parameters, and runs the algorithm for every
    such line (see `bin/run_batch.sh` and `data/batches/simple-batch.txt`)
    -- use kotton instead.

From `triton` directory, those can be invoked via `gradle run` (see `build.gradle` file for details),
or (after a successful `gradle build`) from the built jar (`build/libs/triton-all.jar`) directly.

