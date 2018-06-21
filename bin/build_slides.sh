#!/bin/sh

trap "exit 1" INT QUIT TERM

SRC_DIR=${SRC_DIR:-`realpath ${0%/*}/../slides/`}
SRC_FILE=${SRC_FILE:-slides}

cd "${SRC_DIR}" || exit 1

mkdir -p converted-images
#for file in images/*.png; do
#  convert "$file" -density 300 -units PixelsPerInch -transparent white -matte -colorspace RGB -channel RGBA converted-images/"`basename "$file" .png`.eps";
#done
#for file in images/*.jpg; do
#  convert "$file" -density 300 -units PixelsPerInch converted-images/"`basename "$file" .jpg`.eps";
#done
#gzip -f converted-images/*.eps
for file in images/*.eps.gz; do
  zgrep '%BoundingBox' "$file" > images/"`basename "$file" .gz`.bb";
done
#for file in converted-images/*.eps.gz; do
#  zgrep '%BoundingBox' "$file" > converted-images/"`basename "$file" .gz`.bb";
#done

ERR="$(latex -shell-escape -interaction=nonstopmode -file-line-error ${SRC_FILE}.tex)"
STATUS="$?"
echo "${ERR}" | egrep ".*:[0-9]*:.*|Warning:"
if [ $STATUS -ne 0 ]; then
  exit 1
fi
latex -shell-escape -interaction=batchmode -file-line-error ${SRC_FILE}.tex
texfot latex -shell-escape ${SRC_FILE}.tex
dvips ${SRC_FILE}.dvi
ps2pdf ${SRC_FILE}.ps
