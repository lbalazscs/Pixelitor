/*
 * @(#)GeneralPathWriter.java
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

import java.awt.geom.GeneralPath;

/**
 * This writes path data to an underlying <code>GeneralPath</code>.
 * <P>This also omits redundant path information, such as two consecutive
 * calls to lineTo() that go to the same point.
 * <P>Also this is safe to make several consecutive calls to <code>closePath()</code>
 * (the GeneralPath will only be closed once, unless data has been written in the meantime.)
 */
public class GeneralPathWriter extends PathWriter {
    private final GeneralPath path;
    private float lastX;
    private float lastY;
    private float moveX;
    private float moveY;
    private boolean dataWritten = false;
    private boolean eliminateRedundantLines = true;

    public GeneralPathWriter(GeneralPath path) {
        this.path = path;
    }

    @Override
    public void flush() {
    }

    @Override
    public void curveTo(float cx1, float cy1, float cx2, float cy2, float x,
                        float y) {
        path.curveTo(cx1, cy1, cx2, cy2, x, y);
        lastX = x;
        lastY = y;
        dataWritten = true;
    }

    @Override
    public void lineTo(float x, float y) {
        if (eliminateRedundantLines && approxEqual(lastX, x) && approxEqual(lastY, y)) {
            return;
        }
        path.lineTo(x, y);
        lastX = x;
        lastY = y;
        dataWritten = true;
    }

    @Override
    public void moveTo(float x, float y) {
        path.moveTo(x, y);
        moveX = lastX = x;
        moveY = lastY = y;
        dataWritten = true;
    }

    @Override
    public void quadTo(float cx, float cy, float x, float y) {
        path.quadTo(cx, cy, x, y);
        lastX = x;
        lastY = y;
        dataWritten = true;
    }

    @Override
    public void closePath() {
        if (dataWritten) {
            path.closePath();
            lastX = moveX;
            lastY = moveY;
            dataWritten = false;
        }
    }

    private static boolean approxEqual(float z1, float z2) {
        float d = z2 - z1;
        if (d < 0) {
            d = -d;
        }
        return d < 0.001f;
    }

    @Override
    public String toString() {
        return "GeneralPathWriter[ data = " + ShapeStringUtils.toString(path) + " ]";
    }
}
