/*
 * @(#)Transition.java
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
package com.bric.image.transition;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * This renders a transition between two images.
 */
public interface Transition {
    int RIGHT = 1;
    int LEFT = 2;
    int UP = 3;
    int DOWN = 4;
    int COUNTER_CLOCKWISE = 5;
    int CLOCKWISE = 6;
    int IN = 7;
    int OUT = 8;
    int HORIZONTAL = 9;
    int VERTICAL = 10;
    int BIG = 11;
    int MEDIUM = 12;
    int SMALL = 13;
    int TOP_LEFT = 14;
    int TOP_RIGHT = 15;
    int BOTTOM_LEFT = 16;
    int BOTTOM_RIGHT = 17;

    /**
     * @param g        the Graphics2D to render to.
     * @param frameA   the first frame
     * @param frameB   the second frame
     * @param progress a value between zero and one indicating how
     *                 progressed this transition is.
     *                 <P>At progress = 0, frameA should be shown.  At progress = 1, frameB should be shown.
     * @param invert
     */
    void paint(Graphics2D g, BufferedImage frameA, BufferedImage frameB, float progress, boolean invert);
}
