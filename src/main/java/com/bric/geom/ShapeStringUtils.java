/*
 * @(#)ShapeStringUtils.java
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

import static java.awt.geom.PathIterator.*;

/**
 * This is a small set of static methods that translate shape data into
 * <code>java.lang.Strings</code> and vice versa.
 */
public class ShapeStringUtils {
    /**
     * This describes a shape in a <code>String</code>.  The model is used
     * is based on how SVG encodes shape data.
     * <P>The call:
     * <BR><code>ShapeStringUtils.createPathIterator(ShapeStringUtils.toString(shape));</code>
     * <BR>should result in an identical shape.
     * <P>The shape data is formatted as a single letter (m, l, q, c, z)
     * followed by the appropriate number of points (2, 2, 4, 6, 0 respectively).
     *
     * @param s the shape to describe
     * @return textual representation of that shape.
     */
    public static String toString(Shape s) {
        PathIterator i = s.getPathIterator(null);
        return toString(i);
    }

    public static String toString(PathIterator it) {
        float[] f = new float[6];
        StringBuilder sb = new StringBuilder();
        for (; !it.isDone(); it.next()) {
            int type = it.currentSegment(f);
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            switch (type) {
                case SEG_MOVETO -> appendCommand(sb, 'm', f, 2);
                case SEG_LINETO -> appendCommand(sb, 'l', f, 2);
                case SEG_QUADTO -> appendCommand(sb, 'q', f, 4);
                case SEG_CUBICTO -> appendCommand(sb, 'c', f, 6);
                case SEG_CLOSE -> sb.append('z');
                default -> throw new IllegalArgumentException("Unknown segment type: " + type);
            }
        }
        return sb.toString();
    }

    private static void appendCommand(StringBuilder sb, char cmd, float[] f, int n) {
        sb.append(cmd);
        for (int i = 0; i < n; i++) {
            sb.append(' ').append(f[i]);
        }
    }

    /**
     * This creates a <code>PathIterator</code> that iterates
     * over the text in <code>s</code>.
     * <P>The shape returned uses winding rule WIND_EVEN_ODD.
     *
     * @param s textual representation of a path.
     *          <P>This should be the output of <code>ShapeStringUtils.toString()</code>,
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
     *          <P>This should be the output of <code>ShapeStringUtils.toString()</code>,
     *          resembling: "m 1 2 l 3 4 q 5 6 7 8 c 9 10 11 12 13 14 z"
     * @return a <code>GeneralPath</code> that represents this shape.
     */
    public static GeneralPath createGeneralPath(String s) {
        GeneralPath p = new GeneralPath(WIND_EVEN_ODD);
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
     *                    <P>This should be the output of <code>ShapeStringUtils.toString()</code>,
     *                    resembling: "m 1 2 l 3 4 q 5 6 7 8 c 9 10 11 12 13 14 z"
     * @return a <code>PathIterator</code> that will iterate over the data in s.
     */
    public static PathIterator createPathIterator(String s, int windingRule) {
        /* This little bit is added so if the string passed resembles:
          "Data[ m 34 20 l 20 10 z ]"
          Then we focus only on the part in brackets.
         */
        int open = s.indexOf('[');
        int close = s.indexOf(']');
        if (open != -1 && close != -1 && open < close) {
            s = s.substring(open + 1, close);
        }
        return new SerializedPathIterator(s, windingRule);
    }
}
