#!/bin/sh

trap "exit 1" INT QUIT TERM

PROJ_DIR=`realpath ${0%/*}/../`
BATCH_DIR="${PROJ_DIR}"/data/batches
cd "${BATCH_DIR}" || exit 1

if ! (ls large-batch-part.* 1> /dev/null 2>&1) ; then
  cat large-batch.txt | sort -R | split -l 8000 - large-batch-part. || exit 1
  rm -f ../../LARGE_BATCH_RESULTS
fi
for batch in large-batch-part.*; do (env BATCH_NAME=$batch ../../bin/run_batch.sh -s -t >> ../../LARGE_BATCH_RESULTS); done

