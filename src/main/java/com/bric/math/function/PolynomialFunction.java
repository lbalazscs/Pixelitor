/*
 * @(#)PolynomialFunction.java
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

import java.util.Vector;

import com.bric.math.Equations;

/** This function evaluates a polynomial expression.
 * 
 */
public class PolynomialFunction implements Function {
	
	/** Creates a linear <code>PolynomialFunction</code> that passes through
	 * the two points provided.
	 * 
	 * @param x1 the x-coordinate of the first point.
	 * @param y1 the y-coordinate of the first point.
	 * @param x2 the x-coordinate of the second point.
	 * @param y2 the y-coordinate of the second point.
	 * @return a <code>PolynomialFunction</code> that passes through the points provided.
	 */
	public static PolynomialFunction createFit(double x1,double y1,double x2,double y2) {
		return new PolynomialFunction(new double[] {(y2-y1)/(-x1+x2), (y1*x2-y2*x1)/(-x1+x2) });
	}
	
	/** Creates a <code>PolynomialFunction</code> that uses the coordinates
	 * provided.
	 * 
	 * @param xs an array of x-coordinates.
	 * @param ys an array of y-coordinates.  Each element in this array
	 * corresponds to an element of the x coordinates.
	 * @return a function that matches the coordinates provided.
	 */
	public static PolynomialFunction createFit(double[] xs,double[] ys) {
		if(ys.length!=xs.length)
			throw new IllegalArgumentException("xs.length ("+xs.length+") != ys.length ("+ys.length+")");
	
		double[][] coefficientsMatrix = new double[ys.length][ys.length+1];
		for(int row = 0; row<coefficientsMatrix.length; row++) {
			//make one row focusing on the value of ys(x),
			for(int column = 0; column<coefficientsMatrix[row].length-1; column++) {
				int power = ys.length-column-1;
				coefficientsMatrix[row][column] = Math.pow(xs[row], power);
			}
			coefficientsMatrix[row][coefficientsMatrix[row].length-1] = ys[row];
		}
		
		Equations.solve(coefficientsMatrix, true);
		double[] coeffs = new double[coefficientsMatrix.length];
		for(int a = 0; a<coeffs.length; a++) {
			coeffs[a] = coefficientsMatrix[a][coefficientsMatrix[a].length-1];
		}
		return new PolynomialFunction(coeffs);
	}
	
	/** Creates a <code>PolynomialFunction</code> that uses the coordinates
	 * provided.
	 * <br>The function returned will pass through all the points provided, with
	 * the dy/dx values provided.
	 * 
	 * @param xs an array of x-coordinates.
	 * @param ys an array of y-coordinates.  Each element in this array
	 * corresponds to an element of the x coordinates.
	 * @param yDerivatives an array of dy/dx values.  Each element in this array
	 * corresponds to an element of the x coordinates.
	 * @return a function that matches the coordinates provided.
	 */
	public static PolynomialFunction createFit(double[] xs,double[] ys,double[] yDerivatives) {
		if(ys.length!=yDerivatives.length)
			throw new IllegalArgumentException("ys.length ("+ys.length+") != yDerivatives.length ("+yDerivatives.length+")");
		if(ys.length!=xs.length)
			throw new IllegalArgumentException("xs.length ("+xs.length+") != ys.length ("+ys.length+")");
	
		double[][] coefficientsMatrix = new double[ys.length*2][ys.length*2+1];
		for(int row = 0; row<coefficientsMatrix.length; row+=2) {
			//make one row focusing on the value of ys(x),
			//and the next row focusing on the value of yDerivs(x)
			for(int column = 0; column<coefficientsMatrix[row].length-1; column++) {
				int power = ys.length*2-column-1;
				coefficientsMatrix[row][column] = Math.pow(xs[row/2], power);
				if(power==0) { //no derivative for this one
					coefficientsMatrix[row+1][column] = 0;
				} else {
					coefficientsMatrix[row+1][column] = power*Math.pow(xs[row/2], power-1);
				}
			}
			coefficientsMatrix[row][coefficientsMatrix[row].length-1] = ys[row/2];
			coefficientsMatrix[row+1][coefficientsMatrix[row].length-1] = yDerivatives[row/2];
		}
		
		Equations.solve(coefficientsMatrix, true);
		double[] coeffs = new double[coefficientsMatrix.length];
		for(int a = 0; a<coeffs.length; a++) {
			coeffs[a] = coefficientsMatrix[a][coefficientsMatrix[a].length-1];
		}
		return new PolynomialFunction(coeffs);
	}
	
	double[] coeffs;
	
	/** Create a new <code>PolynomialFunction</code>.
	 * 
	 * @param coeffs the coefficients of this polynomial.  The first
	 * coefficient corresponds to the highest power of x.  So
	 * if coeffs is [2, 3, 4] then this function will
	 * evaluate as (2*t*t+3*t+4).
	 */
	public PolynomialFunction(double[] coeffs) {
		this.coeffs = new double[coeffs.length];
		System.arraycopy(coeffs, 0, this.coeffs, 0, coeffs.length);
	}
	
	public double evaluate(double x) {
		double result = coeffs[0];
		for(int a = 1, n = coeffs.length; a<n; a++) {
			result = result*x+coeffs[a];
		}
		return result;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("y = ");
		for(int a = 0; a<coeffs.length; a++) {
			int degree = coeffs.length-a-1;
			if(degree==0) {
				sb.append( coeffs[a] );
			} else if(degree==1) {
				sb.append(coeffs[a]+"*x");
			} else {
				sb.append( coeffs[a]+"*(x^"+(degree)+")" );
			}
			if(a!=coeffs.length-1)
				sb.append("+");
		}
		return sb.toString();
	}
	
	public PolynomialFunction getDerivative() {
		double[] newCoeffs = new double[coeffs.length-1];
		System.arraycopy(coeffs, 0, newCoeffs, 0, newCoeffs.length);
		for(int a = 0; a<newCoeffs.length; a++) {
			newCoeffs[a] *= (coeffs.length-a-1);
		}
		return new PolynomialFunction(newCoeffs);
	}

	/** Solve this polynomial function by recursive exploring all the derivatives
	 * and strategically applying Newton's Method. This is imperfect, but a decent
	 * analytical guess.
	 */
	public double[] evaluateInverse(double y) {
		if(coeffs.length==2) {
			double x = (y-coeffs[1])/coeffs[0];
			return new double[] {x};
		}
		PolynomialFunction f = this;
		if(y!=0) {
			double[] newCoeffs = new double[coeffs.length];
			System.arraycopy(coeffs, 0, newCoeffs, 0, coeffs.length);
			newCoeffs[newCoeffs.length-1] -= y;
			f = new PolynomialFunction(newCoeffs);
		}
		
		PolynomialFunction derivative = f.getDerivative();
		
		double[] extrema = derivative.solve();
		double[] extremaYs = new double[extrema.length];
		for(int a = 0; a<extrema.length; a++) {
			extremaYs[a] = f.evaluate(extrema[a]);
		}
		
		double[] interest = new double[extrema.length+2];
		double[] interestYs = new double[extrema.length+2];
		
		System.arraycopy(extrema, 0, interest, 1, extrema.length);
		System.arraycopy(extremaYs, 0, interestYs, 1, extremaYs.length);
		
		//seek the first interesting time:
		boolean seekPos;
		if(coeffs.length%2==0) {
			//an odd-degree polynomial
			if(coeffs[0]<0) {
				//with a negative leading coefficient
				
				//f(-infinity) = +infinity && f(+infinity) = -infinity
				seekPos = true;
			} else {
				seekPos = false;
			}
		} else {
			//an even-degree polynomial
			if(coeffs[0]<0) {
				seekPos = false;
			} else {
				seekPos = true;
			}
		}
		
		double initialValue = extrema.length==0 ? 0 : extrema[0];
		identifyBoundary : for(int power = 1; power<30; power++) {
			double x = initialValue-Math.pow(10, power);
			double v = f.evaluate(x);
			if(seekPos) {
				if(v>0) {
					interest[0] = x;
					interestYs[0] = v;
					break identifyBoundary;
				}
			} else { //we seek negative
				if(v<0) {
					interest[0] = x;
					interestYs[0] = v;
					break identifyBoundary;
				}
			}
		}
		
		//seek the last interesting time:

		if(coeffs.length%2==0) {
			//an odd-degree polynomial
			seekPos = !seekPos;
		} else {
			//leave seekPos as-is
		}

		initialValue = extrema.length==0 ? 0 : extrema[extrema.length-1];
		identifyBoundary : for(int power = 1; power<30; power++) {
			double x = initialValue+Math.pow(10, power);
			double v = f.evaluate(x);
			if(seekPos) {
				if(v>0) {
					interest[interest.length-1] = x;
					interestYs[interest.length-1] = v;
					break identifyBoundary;
				}
			} else { //we seek negative
				if(v<0) {
					interest[interest.length-1] = x;
					interestYs[interest.length-1] = v;
					break identifyBoundary;
				}
			}
		}
		
		Vector<Double> solutions = new Vector<Double>();
		
		for(int a = 0; a<interest.length-1; a++) {
			double y1 = interestYs[a];
			double y2 = interestYs[a+1];
			if( (y1>0 && y2<0) || (y1<0 && y2>0)) {
				applyNewtonsMethod(f, derivative, interest[a], interest[a+1], solutions);
			} else if(y1==0) {
				solutions.add(interest[a]);
			}
		}
		
		double[] returnArray = new double[solutions.size()];
		for(int a = 0; a<solutions.size(); a++) {
			returnArray[a] = solutions.get(a);
		}
		return returnArray;
	}
	
	private static void applyNewtonsMethod(PolynomialFunction function,PolynomialFunction derivative,double min,double max,Vector<Double> solutions) {
		
		double dt;
		double t = (max+min)/2;
		
		int k = 0;
		
		while(k<300) { //sometimes .00000000001 is too strict; 300 iterations may be our best shot
			dt = derivative.evaluate(t);
			if(dt==0) {
				k = 300; //abort!
			} else {
				double newT = t - function.evaluate(t)/dt;
				
				double delta = t-newT;
				if(delta<0) delta = -delta;
				if(delta<=.00000000001) {
					solutions.add(new Double(t));
					return;
				}
				
				t = newT;
			}
			k++;
		}
		return;
	}
	
	/** Calls <code>evaluateInverse(0)</code> 
	 * @return calls <code>evaluateInverse(0)</code>
	 */
	public double[] solve() {
		return evaluateInverse(0);
	}
}
