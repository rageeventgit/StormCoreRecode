package net.nethersmp.storm.utilities;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@UtilityClass
public class Numbers {

    private static final NavigableMap<Double, String> SUFFIXES = new TreeMap<>();

    static {
        SUFFIXES.put(1_000D, "K");
        SUFFIXES.put(1_000_000D, "M");
        SUFFIXES.put(1_000_000_000D, "B");
        SUFFIXES.put(1_000_000_000_000D, "T");
    }

    public static String format(double value) {
        if (value == Double.MIN_VALUE) return format(Double.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Double.toString(value); //deal with easy case

        Map.Entry<Double, String> e = SUFFIXES.floorEntry(value);
        double divideBy = e.getKey();
        String suffix = e.getValue();

        double truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    public static String format(String value) {
        try {
            double a = Double.parseDouble(value);
            return format(a);
        } catch (NumberFormatException e) {
            return value;
        }
    }

}
