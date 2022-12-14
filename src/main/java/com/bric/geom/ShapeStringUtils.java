/*
 * @(#)ShapeStringUtils.java
 *
 * $Date: 2014-03-13 09:15:48 +0100 (Cs, 13 márc. 2014) $
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * https://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.bric.geom;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import static java.awt.geom.PathIterator.WIND_EVEN_ODD;

/**
 * This is a small set of static methods that translate shape data into
 * <code>java.lang.Strings</code> and vice versa.
 */
public class ShapeStringUtils {
    /**
     * This describes a shape in a <code>String</code>.  The model is used
     * is based on how SVG encodes shape data.
     * <P>The call:
     * <BR><code>ShapeUtils.createPathIterator(ShapeUtils.toString(shape));</code>
     * <BR>should result in an identical shape.
     * <P>The shape data is formatted as a single letter (m, l, q, c, z)
     * followed by the appropriate number of points (2, 2, 4, 6, 0 respectively).
     * This uses floats, not doubles, so it will not contain strings with an exponent
     * (i.e. "1.3e-4").
     *
     * @param s the shape to describe
     * @return textual representation of that shape.
     */
    public static String toString(Shape s) {
        PathIterator i = s.getPathIterator(null);
        return toString(i);
    }

    public static String toString(PathIterator i) {
        float[] f = new float[6];
        StringBuilder sb = new StringBuilder();
        int k;
        int j = 0;
        while (!i.isDone()) {
            k = i.currentSegment(f);

            if (k == SEG_MOVETO) {
                sb.append('m');
                j = 2;
            } else if (k == SEG_LINETO) {
                sb.append('l');
                j = 2;
            } else if (k == SEG_QUADTO) {
                sb.append('q');
                j = 4;
            } else if (k == SEG_CUBICTO) {
                sb.append('c');
                j = 6;
            } else if (k == SEG_CLOSE) {
                sb.append('z');
                j = 0;
            }
            if (j != 0) {
                sb.append(' ');
                for (int a = 0; a < j; a++) {
                    sb.append(f[a]);
                    if (a < j - 1) {
                        sb.append(' ');
                    }
                }
            }

            i.next();
            if (!i.isDone()) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    /**
     * This creates a <code>PathIterator</code> that iterates
     * over the text in <code>s</code>.
     * <P>The shape returned uses winding rule WIND_EVEN_ODD.
     *
     * @param s textual representation of a path.
     *          <P>This should be the output of <code>ShapeUtils.toString()</code>,
     *          resembling: "m 1 2 l 3 4 q 5 6 7 8 c 9 10 11 12 13 14 z"
     * @return a <code>PathIterator</code> that will iterate over the data in s.
     */
    public static PathIterator createPathIterator(String s) {
        return createPathIterator(s, WIND_EVEN_ODD);
    }

    /**
     * This creates a <code>GeneralPath</code> of rule WIND_EVEN_ODD
     * that represents this shape data.
     * <P>This method simply calls:
     * <BR><code>GeneralPath p = new GeneralPath();</code>
     * <BR><code>p.append(createPathIterator(s),true);</code>
     * <BR><code>return p;</code>
     *
     * @param s textual representation of a path.
     *          <P>This should be the output of <code>ShapeUtils.toString()</code>,
     *          resembling: "m 1 2 l 3 4 q 5 6 7 8 c 9 10 11 12 13 14 z"
     * @return a <code>GeneralPath</code> that represents this shape.
     */
    public static GeneralPath createGeneralPath(String s) {
        GeneralPath p = new GeneralPath();
        p.append(createPathIterator(s), true);
        return p;
    }

    /**
     * This creates a <code>PathIterator</code> that iterates
     * over the text in <code>s</code>.
     *
     * @param s           textual representation of a path.
     * @param windingRule the winding rule to use.
     *                    (This should be PathIterator.WIND_NON_ZERO or PathIterator.WIND_EVEN_ODD.)
     *                    <P>This should be the output of <code>ShapeUtils.toString()</code>,
     *                    resembling: "m 1 2 l 3 4 q 5 6 7 8 c 9 10 11 12 13 14 z"
     * @return a <code>PathIterator</code> that will iterate over the data in s.
     */
    public static PathIterator createPathIterator(String s, int windingRule) {
        /* This little bit is added so if the string passed resembles:
          "Data[ m 34 20 l 20 10 z ]"
          Then we focus only on the part in brackets.
         */
        int i1 = s.indexOf('[');
        int i2 = s.indexOf(']');
        if (i1 != -1 && i2 != -2 && i1 < i2) {
            s = s.substring(i1 + 1, i2);
        }
        return new SerializedPathIterator(s, WIND_EVEN_ODD);
    }
}
