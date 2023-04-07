package com.bric.math;

import java.math.BigDecimal;

public class StringConverter {
    public static String toString(double[][] d) {
        String s = "";
        for (double[] doubles : d) {
            s = s + toString(doubles) + "\n";
        }
        return s.trim();
    }

    public static String toString(double[] d) {
        String s = "[";
        for (int a = 0; a < d.length; a++) {
            if (a == 0) {
                s = s + " " + d[a];
            } else {
                s = s + ", " + d[a];
            }
        }
        return s + " ]";
    }

    public static String toString(BigDecimal[][] d) {
        String s = "";
        for (BigDecimal[] bigDecimals : d) {
            s = s + toString(bigDecimals) + "\n";
        }
        return s.trim();
    }

    public static String toString(BigDecimal[] d) {
        String s = "[";
        for (int a = 0; a < d.length; a++) {
            if (a == 0) {
                s = s + " " + d[a];
            } else {
                s = s + ", " + d[a];
            }
        }
        return s + " ]";
    }
}