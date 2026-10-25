/*
 * @(#)SerializedPathIterator.java
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

import java.awt.geom.PathIterator;

/**
 * A {@link PathIterator} that parses serialized shape info.
 * <p>
 * The serialized format describes path segments as a sequence of whitespace-separated terms:
 * <ul>
 *   <li><code>m</code> or <code>M</code> (moveto) followed by 2 coordinates: <i>x</i>, <i>y</i></li>
 *   <li><code>l</code> or <code>L</code> (lineto) followed by 2 coordinates: <i>x</i>, <i>y</i></li>
 *   <li><code>q</code> or <code>Q</code> (quadto) followed by 4 coordinates: <i>cx</i>, <i>cy</i>, <i>x</i>, <i>y</i></li>
 *   <li><code>c</code> or <code>C</code> (cubicto) followed by 6 coordinates: <i>cx1</i>, <i>cy1</i>, <i>cx2</i>, <i>cy2</i>, <i>x</i>, <i>y</i></li>
 *   <li><code>z</code> or <code>Z</code> (close) followed by 0 coordinates</li>
 * </ul>
 * <p>
 * Both lowercase and uppercase command letters are accepted. All coordinates are absolute;
 * unlike SVG path data, lowercase letters do not designate relative coordinates.
 * All terms (command letters and coordinate numbers) must be separated by whitespace.
 */
class SerializedPathIterator implements PathIterator {
    private final String s;
    private int ctr = 0;
    private final double[] data = new double[6];
    private int currentSegment = -1;
    private final int windingRule;
    private boolean done = false;

    public SerializedPathIterator(String s, int windingRule) {
        if (!(windingRule == WIND_EVEN_ODD || windingRule == WIND_NON_ZERO)) {
            throw new IllegalArgumentException("The winding rule must be PathIterator.WIND_NON_ZERO or PathIterator.WIND_EVEN_ODD");
        }

        this.s = s;
        this.windingRule = windingRule;
        next();
    }

    @Override
    public int getWindingRule() {
        return windingRule;
    }

    private void skipWhitespace() {
        while (ctr < s.length() && Character.isWhitespace(s.charAt(ctr))) {
            ctr++;
        }
    }

    private void requireWhitespace() {
        if (ctr < s.length()) {
            if (!Character.isWhitespace(s.charAt(ctr))) {
                throw new ParserException("expected whitespace", s, ctr, 1);
            }
            skipWhitespace();
        }
    }

    @Override
    public void next() {
        if (done) {
            return;
        }

        skipWhitespace();

        if (ctr >= s.length()) {
            done = true;
            return;
        }
        int terms;
        char k = s.charAt(ctr);

        terms = switch (k) {
            case 'm', 'M' -> {
                currentSegment = SEG_MOVETO;
                yield 2;
            }
            case 'l', 'L' -> {
                currentSegment = SEG_LINETO;
                yield 2;
            }
            case 'q', 'Q' -> {
                currentSegment = SEG_QUADTO;
                yield 4;
            }
            case 'c', 'C' -> {
                currentSegment = SEG_CUBICTO;
                yield 6;
            }
            case 'z', 'Z' -> {
                currentSegment = SEG_CLOSE;
                yield 0;
            }
            default -> throw new ParserException("Unrecognized character in shape data: '" + k + "'", s, ctr, 1);
        };
        ctr++;
        if (terms > 0) {
            parseTerms(terms);
        } else {
            requireWhitespace();
        }
    }

    private void parseTerms(int terms) {
        for (int a = 0; a < terms; a++) {
            data[a] = parseTerm();
        }
    }

    private double parseTerm() {
        requireWhitespace();
        if (ctr >= s.length()) {
            throw new ParserException("missing coordinate", s, ctr, 1);
        }
        int i = ctr;
        while (i < s.length() && !Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        String string = s.substring(ctr, i);
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            String msg = e.getMessage();
            throw new ParserException(msg, s, ctr, i - ctr);
        } finally {
            ctr = i;
        }
    }

    // For SEG_CLOSE, the array contents are unspecified.
    @Override
    public int currentSegment(double[] d) {
        System.arraycopy(data, 0, d, 0, 6);
        return currentSegment;
    }

    // For SEG_CLOSE, the array contents are unspecified.
    @Override
    public int currentSegment(float[] f) {
        for (int i = 0; i < 6; i++) {
            f[i] = (float) data[i];
        }
        return currentSegment;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    static class ParserException extends IllegalArgumentException {
        ParserException(String msg, String s, int ptr, int length) {
            super(createMessage(msg, s, ptr, length));
        }

        private static String createMessage(String msg, String s, int ptr, int length) {
            StringBuilder sb = new StringBuilder();
            if (msg != null && !msg.isEmpty()) {
                sb.append(msg).append('\n');
            }
            if (s != null) {
                sb.append('"').append(s).append("\"\n");
                sb.repeat(" ", Math.max(0, ptr) + 1);
                sb.repeat("^", Math.max(1, length));
            }
            return sb.toString();
        }
    }
}
