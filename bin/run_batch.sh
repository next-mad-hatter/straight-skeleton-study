#!/bin/sh

trap "exit 1" INT QUIT TERM

PROJ_DIR=`realpath ${0%/*}/../`
DATA_DIR="${PROJ_DIR}"/data
SRC_DIR="${PROJ_DIR}"/triton
BATCH_NAME=${BATCH_NAME:-simple-batch.txt}

# We'll go for $@ instead for now
# RUNTIME_OPTIONS=${RUNTIME_OPTIONS:--s}

# cd "${SRC_DIR}" || exit 1
# gradle build || exit 2

cd "${DATA_DIR}" || exit 1
exec java \
  -Xmx4096M -XX:+HeapDumpOnOutOfMemoryError \
  -cp "${SRC_DIR}"/build/libs/triton-all.jar \
  at.tugraz.igi.main.BatchRun \
  "$@" ${DATA_DIR}/batches/${BATCH_NAME}

