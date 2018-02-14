#!/bin/sh

trap "exit 1" INT QUIT TERM

PROJ_DIR=`realpath ${0%/*}/../`
DATA_DIR="${PROJ_DIR}"/data
SRC_DIR="${PROJ_DIR}"/straight_skeleton

# cd "${SRC_DIR}" || exit 1
# gradle build || exit 2

cd "${DATA_DIR}" || exit 1
exec java -cp "${SRC_DIR}"/build/libs/straight_skeleton.jar \
  at.tugraz.igi.main.BatchRun \
  ${DATA_DIR}/batches/simple-batch.txt

