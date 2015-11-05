/*
 * @(#)FilteredStroke.java
 *
 * $Date: 2014-06-06 20:04:49 +0200 (P, 06 jún. 2014) $
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
package com.bric.awt;

import java.awt.Stroke;

/**
 * This is a <code>Stroke</code> that modifies or sits on top of
 * another <code>Stroke</code>.
 * <p>
 * This model is especially convenient when you design a GUI to
 * manipulate the properties of your <code>Stroke</code>.
 */
public interface FilteredStroke extends Stroke {

    /**
     * @return the underlying stroke being filtered.
     */
    public Stroke getStroke();

    /**
     * Similar to <code>Font.deriveFont()</code>, this makes
     * a stroke similar to this object, except the underlying
     * <code>Stroke</code> this stroke filters is replaced.
     *
     * @param s the new underlying stroke to use.
     * @return a new stroke that is built on top of <code>s</code>
     */
    public FilteredStroke deriveStroke(Stroke s);
}
