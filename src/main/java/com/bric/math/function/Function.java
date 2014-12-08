/*
 * @(#)Function.java
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
package com.bric.math.function;

/** This is a simple mathematical function: f(x) */
public interface Function {
	/** Evaluates f(x).
	 * @param x the input for this function.
	 * @return the output of this function.
	 * 
	 */
	public double evaluate(double x);
	
	/** Returns all the x-values for the equation f(x) = y.
	 * @param y a possible output of this function.
	 * @return all the possible inputs that would map to the argument.
	 * 
	 */
	public double[] evaluateInverse(double y);
}
