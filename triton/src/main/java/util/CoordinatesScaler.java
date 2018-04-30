package at.tugraz.igi.util;

import lombok.*;
import org.apache.commons.lang3.tuple.*;
import org.apache.commons.math3.exception.MathParseException;
import org.apache.commons.math3.fraction.*;
import org.apache.commons.math3.exception.*;

import java.math.*;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

/*
 * For reasons of numerical stability, we might want to scale our input to some
 * (nonnegative, for safety's sake :)) range.  This is where said functionality
 * lives.
 */
public class CoordinatesScaler {

    @NoArgsConstructor @Data public class ScalingData {
        private BigFraction
                scale = null,
                minX = null, minY = null,
                maxX = null, maxY = null;
    }


    /**
     * BigFractionFormat cannot read floating point format, hence this.
     */
    private static BigFraction str2bf(String str) throws NumberFormatException {
        BigDecimal val = new BigDecimal(str);
        int scale = val.scale();
        return scale >= 0 ? new BigFraction(val.unscaledValue(), BigInteger.TEN.pow(scale)) :
                            new BigFraction(val.unscaledValue().multiply(BigInteger.TEN.pow(-scale)));
    }


    /**
     *  Scales a list of coordinates to range of [0..scaleFactor].
     */
    public <T> Pair<ScalingData, List<Pair<BigFraction, BigFraction>>>
    scalePoints(int scaleFactor, List<Pair<T, T>> coors) throws ParseException {
        val scalingData = new ScalingData();

        List<Pair<BigFraction, BigFraction>> points = new ArrayList<>();
        val fmt = new BigFractionFormat();
        for (val pt: coors) {
            try {
                val x = str2bf(String.valueOf(pt.getLeft()));
                val y = str2bf(String.valueOf(pt.getRight()));
                val v = new ImmutablePair<>(x, y);
                points.add(v);
                if (scalingData.getMinX() == null || scalingData.getMinX().compareTo(x) > 0) scalingData.setMinX(x);
                if (scalingData.getMinY() == null || scalingData.getMinY().compareTo(y) > 0) scalingData.setMinY(y);
                if (scalingData.getMaxX() == null || scalingData.getMaxX().compareTo(x) < 0) scalingData.setMaxX(x);
                if (scalingData.getMaxY() == null || scalingData.getMaxY().compareTo(y) < 0) scalingData.setMaxY(y);
            } catch (NumberFormatException e) {
                throw new ParseException(e.getMessage());
            }
        }

        BigFraction sx = scalingData.getMaxX().subtract(scalingData.getMinX());
        BigFraction sy = scalingData.getMaxY().subtract(scalingData.getMinY());
        if (sx.compareTo(sy) < 0)
            scalingData.setScale(sx);
        else
            scalingData.setScale(sy);

        List<Pair<BigFraction, BigFraction>> res = points.stream().map((p) -> new ImmutablePair<>(
                p.getLeft().subtract(scalingData.getMinX()).divide(scalingData.getScale()).multiply(scaleFactor),
                p.getRight().subtract(scalingData.getMinY()).divide(scalingData.getScale()).multiply(scaleFactor)
        )).collect(Collectors.toList());

        scalingData.setScale(scalingData.getScale().divide(scaleFactor));
        return new ImmutablePair<>(scalingData, res);
    }


    /**
     * The inverse transform.
     */
    static public Function<Pair<Double, Double>, Pair<BigFraction, BigFraction>> inverseTransform(ScalingData scalingData) {
        return (pt) -> new ImmutablePair<>(
                new BigFraction(pt.getLeft()).multiply(scalingData.getScale()).add(scalingData.getMinX()),
                new BigFraction(pt.getRight()).multiply(scalingData.getScale()).add(scalingData.getMinY())
        );
    }


}
