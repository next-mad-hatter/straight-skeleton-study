package at.tugraz.igi.util;

import lombok.*;
import org.apache.commons.math3.fraction.*;
import org.apache.commons.lang3.tuple.*;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

public class CoordinatesScaler {

    /**
     * Sometimes we'll want to scale our input to some (nonnegative, for safety :)) range.
     * This is where we could keep data for inverse transformation.
     */
    @NoArgsConstructor @Data public class ScalingData {
        private BigFraction
                scale = null,
                minX = null, minY = null,
                maxX = null, maxY = null;
    }


    /**
     *  Scales a list of coordinates to range of [0..scaleFactor].
     */
    public <T> Pair<ScalingData, List<Pair<BigFraction, BigFraction>>>
    scalePoints(int scaleFactor, List<Pair<T, T>> coors) throws ParseException {
        val scaling = new ScalingData();

        List<Pair<BigFraction, BigFraction>> points = new ArrayList<>();
        val fmt = new BigFractionFormat();
        for (val pt: coors) {
            val x = fmt.parse(String.valueOf(pt.getLeft()));
            val y = fmt.parse(String.valueOf(pt.getRight()));
            val v = new ImmutablePair<>(x, y);
            points.add(v);
            if (scaling.getMinX() == null || scaling.getMinX().compareTo(x) > 0) scaling.setMinX(x);
            if (scaling.getMinY() == null || scaling.getMinY().compareTo(y) > 0) scaling.setMinY(y);
            if (scaling.getMaxX() == null || scaling.getMaxX().compareTo(x) < 0) scaling.setMaxX(x);
            if (scaling.getMaxY() == null || scaling.getMaxY().compareTo(y) < 0) scaling.setMaxY(y);
        }

        BigFraction sx = scaling.getMaxX().subtract(scaling.getMinX());
        BigFraction sy = scaling.getMaxY().subtract(scaling.getMinY());
        if (sx.compareTo(sy) < 0)
            scaling.setScale(sx);
        else
            scaling.setScale(sy);

        List<Pair<BigFraction, BigFraction>> res = points.stream().map((p) -> new ImmutablePair<>(
                p.getLeft().subtract(scaling.getMinX()).divide(scaling.getScale()).multiply(scaleFactor),
                p.getRight().subtract(scaling.getMinY()).divide(scaling.getScale()).multiply(scaleFactor)
        )).collect(Collectors.toList());

        scaling.setScale(scaling.getScale().divide(scaleFactor));
        return new ImmutablePair<>(scaling, res);
    }


    /**
     * The inverse transform.
     */
    public Function<Pair<Double, Double>, Pair<BigFraction, BigFraction>> backScaler(ScalingData scalingData) {
        return (pt) -> new ImmutablePair<>(
                new BigFraction(pt.getLeft()).multiply(scalingData.getScale()).add(scalingData.getMinX()),
                new BigFraction(pt.getRight()).multiply(scalingData.getScale()).add(scalingData.getMinY())
        );
    }


}
