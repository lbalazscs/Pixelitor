/*
 * @(#)MathG.java
 *
 * $Date: 2014-11-08 19:55:43 +0100 (Szo, 08 nov. 2014) $
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
package com.bric.math;


/**
 * This provides some alternative implementations of a few methods from
 * the Math class.
 * <P>This class may use approximations with various levels of error.  The "G"
 * in the name stands for "Graphics", because it was originally conceived
 * as a tool to speed up graphics.  When I iterate over every pixel in an image
 * to perform some operation: I don't really need the precision that the Math
 * class offers.
 * <P>Many thanks to Oleg E. for some insights regarding machine error and
 * design.
 * <P>See MathGDemo.java for a set of tests comparing the speed/accuracy
 * of java.lang.Math and com.bric.math.MathG.
 */
public abstract class MathG {

    /**
     * Finds the closest integer that is less than or equal to the argument as a double.
     * <BR>Warning: do not use an argument greater than 1e10, or less than 1e-10.
     *
     * @param d the value to calculate the floor of.
     * @return the closest integer that is less than the argument as a double.
     */
    public static final double floorDouble(double d) {
        int id = (int) d;
        return d == id || d > 0 ? id : id - 1;
    }

    /**
     * Finds the closest integer that is less than or equal to the argument as an int.
     * <BR>Warning: do not use an argument greater than 1e10, or less than 1e-10.
     *
     * @param d the value to calculate the floor of.
     * @return the closest integer that is less than the argument as an int.
     */
    public static final int floorInt(double d) {
        int id = (int) d;
        return d == id || d > 0 ? id : id - 1;
    }

    /**
     * Rounds a double to the nearest integer value.
     * <BR>Warning: do not use an argument greater than 1e10, or less than 1e-10.
     *
     * @param d the value to round.
     * @return the closest integer that is less than the argument.
     */
    public static final int roundInt(double d) {
        int i;
        if (d >= 0) {
            i = (int) (d + .5);
        } else {
            i = (int) (d - .5);
        }
        return i;
    }

    /**
     * Rounds a double to the nearest integer value.
     * <BR>Warning: do not use an argument greater than 1e10, or less than 1e-10.
     *
     * @param d the value to round.
     * @return the closest integer that is less than the argument as a double.
     */
    public static final double roundDouble(double d) {
        int i;
        if (d >= 0) {
            i = (int) (d + .5);
        } else {
            i = (int) (d - .5);
        }
        return i;
    }

    /**
     * Finds the closest integer that is greater than or equal to the argument as an int.
     * <BR>Warning: do not use an argument greater than 1e10, or less than 1e-10.
     *
     * @param d the value to calculate the ceil of.
     * @return the closest integer that is greater than the argument as an int.
     */
    public static final int ceilInt(double d) {
        int id = (int) d;
        return d == id || d < 0 ? id : -((int) (-d)) + 1;
    }

    /**
     * Finds the closest integer that is greater than or equal to the argument as a double.
     * <BR>Warning: do not use an argument greater than 1e10, or less than 1e-10.
     *
     * @param d the value to calculate the ceil of.
     * @return the closest integer that is greater than the argument as a double.
     */
    public static final double ceilDouble(double d) {
        int id = (int) d;
        return d == id || d < 0 ? id : -((int) (-d)) + 1;
    }

    // Laszlo: cut out the rest to avoid importing additional classes
}
