#!/bin/sh

SRC_DIR=${SRC_DIR:-`realpath ${0%/*}/../slides/`}

trap "exit 1" INT QUIT TERM
cd "${SRC_DIR}" || exit 1

rm -rf -- *.output \
          *.aux *.log *.toc *.out *.lol \
          *.bcf *.bbl *.blg *.run.xml \
          *.ind *.idx *.ilg *.ing \
          *.dvi *.ps *.bm \
          converted-images

