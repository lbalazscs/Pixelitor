/*
 * @(#)Equations.java
 *
 * $Date: 2014-03-27 08:50:51 +0100 (Cs, 27 márc. 2014) $
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;

/** This contains a static method to solve a system of simple linear equations.
 *
 */
public class Equations {
	/** Generating the text in certain exceptions is not a trivial task if you
	 * expect those exceptions to be called millions of times.
	 * 
	 */
	public static boolean VERBOSE_EXCEPTIONS = true;
	
	private static Comparator<double[]> coefficientComparator = new Comparator<double[]>() {
		public int compare(double[] d1, double[] d2) {
			int v1 = 0;
			int v2 = 0;
			int a;
			for( a = 0; a<d1.length; a++) {
				if(d1[a]==1) {
					v1 = a;
					a = d1.length;
				}
			}
			for( a = 0; a<d2.length; a++) {
				if(d2[a]==1) {
					v2 = a;
					a = d2.length;
				}
			}
			return v1-v2;
		}
	};
	
	private static Comparator<BigDecimal[]> bigCoefficientComparator = new Comparator<BigDecimal[]>() {
		public int compare(BigDecimal[] d1, BigDecimal[] d2) {
			int v1 = 0;
			int v2 = 0;
			int a;
			for( a = 0; a<d1.length; a++) {
				if(d1[a].equals(BigDecimal.ONE)) {
					v1 = a;
					a = d1.length;
				}
			}
			for( a = 0; a<d2.length; a++) {
				if(d2[a].equals(BigDecimal.ONE)) {
					v2 = a;
					a = d2.length;
				}
			}
			return v1-v2;
		}
	};
	
	public static String toString(double[][] d) {
		String s = "";
		for(int a = 0; a<d.length; a++) {
			s = s+toString(d[a])+"\n";
		}
		return s.trim();
	}
	
	public static String toString(double[] d) {
		String s = "[";
		for(int a = 0; a<d.length; a++) {
			if(a==0) {
				s = s+" "+d[a];
			} else {
				s = s+", "+d[a];
			}
		}
		return s+" ]";
	}
	
	public static String toString(BigDecimal[][] d) {
		String s = "";
		for(int a = 0; a<d.length; a++) {
			s = s+toString(d[a])+"\n";
		}
		return s.trim();
	}
	
	public static String toString(BigDecimal[] d) {
		String s = "[";
		for(int a = 0; a<d.length; a++) {
			if(a==0) {
				s = s+" "+d[a];
			} else {
				s = s+", "+d[a];
			}
		}
		return s+" ]";
	}

	/** Given a matrix of variable coefficients for a linear system of equations,
	* this will solve for each variable.
	* <P> For example, if you have the three equations:
	* <BR> 2x + y + z = 1
	* <BR> 6x + 2y + z = -1
	* <BR> -2x + 2y + z = 7
	* <P> You can call:
	* <code><BR>double[][] coeffs = { { 2, 1, 1, 1},</code>
	* <code><BR>                      { 6, 2, 1, -1},</code>
	* <code><BR>						{ -2, 2, 1, 7} };</code>
	* <code><BR>Equations.solve(coeffs,true);</code>
	* <P> Then this method will simplify the matrix to:
	* <code><BR>{ { 1, 0, 0, -1}, </code>
	* <code><BR>  { 0, 1, 0, 2}, </code>
	* <code><BR>  { 0, 0, 1, 1} }</code>
	* <P> This indicates that:
	* <BR> 1*x+0*y+0*z = -1
	* <BR> 0*x+1*y+0*z = 2
	* <BR> 0*x+0*y+1*z = 1
	* <P> This method uses Gaussian Elimination and back substitution.
	* <P> Note that due to the limits of double-precision, some safeguards were
	* built-in that may round values less than .00000001 to zero.  If you
	* need better precision, you may have to copy and paste the algorithm
	* in this code and use a more precise number format.
	* @param coefficients the matrix of coefficients.  It must be N x (N+1) units in size.
	* @throws IllegalArgumentException if it is impossible to solve for each variable.  In this case an
	* exception is thrown, and no guarantee is made regarding the final state of the
	* <code>coefficients</code> matrix.
	* <P> Also an exception may be thrown if the matrix is not correctly sized.
	*/
  public static void solve(double[][] coefficients) {
	  solve(coefficients,true);
  }

	/** Given a matrix of variable coefficients for a linear system of equations,
	* this will solve for each variable.
	* <P> For example, if you have the three equations:
	* <BR> 2x + y + z = 1
	* <BR> 6x + 2y + z = -1
	* <BR> -2x + 2y + z = 7
	* <P> You can call:
	* <code><BR>double[][] coeffs = { { 2, 1, 1, 1},</code>
	* <code><BR>                      { 6, 2, 1, -1},</code>
	* <code><BR>						{ -2, 2, 1, 7} };</code>
	* <code><BR>Equations.solve(coeffs,true);</code>
	* <P> Then this method will simplify the matrix to:
	* <code><BR>{ { 1, 0, 0, -1}, </code>
	* <code><BR>  { 0, 1, 0, 2}, </code>
	* <code><BR>  { 0, 0, 1, 1} }</code>
	* <P> This indicates that:
	* <BR> 1*x+0*y+0*z = -1
	* <BR> 0*x+1*y+0*z = 2
	* <BR> 0*x+0*y+1*z = 1
	* <P> This method uses Gaussian Elimination and back substitution.
	* <P> Note that due to the limits of double-precision, some safeguards were
	* built-in that may round values less than .00000001 to zero.  If you
	* need better precision, you may have to copy and paste the algorithm
	* in this code and use a more precise number format.
	* @param coefficients the matrix of coefficients.  It must be N x (N+1) units in size.
	* @throws IllegalArgumentException if it is impossible to solve for each variable.  In this case an
	* exception is thrown, and no guarantee is made regarding the final state of the
	* <code>coefficients</code> matrix.
	* <P> Also an exception may be thrown if the matrix is not correctly sized.
	*/
	public static void solve(BigDecimal[][] coefficients) {
		  solve(coefficients,true);
	}
  
	/** Given a matrix of variable coefficients for a linear system of equations,
	* this will solve for each variable.
	* <P> For example, if you have the three equations:
	* <BR> 2x + y + z = 1
	* <BR> 6x + 2y + z = -1
	* <BR> -2x + 2y + z = 7
	* <P> You can call:
	* <code><BR>double[][] coeffs = { { 2, 1, 1, 1},</code>
	* <code><BR>                      { 6, 2, 1, -1},</code>
	* <code>	<BR>						{ -2, 2, 1, 7} };</code>
	* <code><BR>Equations.solve(coeffs,true);</code>
	* <P> Then this method will simplify the matrix to:
	* <code><BR>{ { 1, 0, 0, -1}, </code>
	* <code><BR>  { 0, 1, 0, 2}, </code>
	* <code><BR>  { 0, 0, 1, 1} }</code>
	* <P> This indicates that:
	* <BR> 1*x+0*y+0*z = -1
	* <BR> 0*x+1*y+0*z = 2
	* <BR> 0*x+0*y+1*z = 1
	* <P> This method uses Gaussian Elimination and back substitution.
	* <P> Note that due to the limits of double-precision, some safeguards were
	* built-in that may round values less than .00000001 to zero.  If you
	* need better precision, you may have to copy and paste the algorithm
	* in this code and use a more precise number format.
	* @param coefficients the matrix of coefficients.  It must be N x (N+1) units in size.
	* @param sort If this is <code>true</code>, then you're guaranteed for your final solution
	* to be sorted such that the 1's form a diagonal line from [0,0] to [N,N].  If this
	* is <code>false</code>, then the solution <i>may</i> not form a diagonal line of 1's.
	* <P> (If there are no zeroes in the matrix, the diagonal line will be there by default,
	* but if zeroes are present then some rows may have to be skipped and the normal order
	* is disrupted.)
	* <P> The sort is performed by calling <code>Arrays.sort()</code>.
	* @throws IllegalArgumentException if it is impossible to solve for each variable.  In this case an
	* exception is thrown, and no guarantee is made regarding the final state of the
	* <code>coefficients</code> matrix.
	* <P> Also an exception may be thrown if the matrix is not correctly sized.
	*/
  public static void solve(double[][] coefficients,boolean sort) {
		if(coefficients==null) throw new NullPointerException("The coefficients matrix is null.");
		int size = coefficients.length;
		
		//this will keep track of which rows have been processed
		//in an ideal world, we would like to go sequentially down the list,
		//but if a coefficient somewhere is already zero, then we may have to skip around.
		boolean[] b = new boolean[coefficients.length];
		
		//this keeps track of which row we solved, from first to last
		//we use this to back substitute
		int[] order = new int[b.length];
		
		int ctr = 0; 
		int row = 0;
		int a, i;
		double t;
		int errorCounter = 0;
		//println(coefficients);
		while(ctr<b.length) {
			if(coefficients[row].length!=size+1) 
				throw new IllegalArgumentException("The matrix must be N x (N+1) units long.  The matrix provided is "+size+" x "+coefficients[row].length+" units.");
			if(b[row]==false && Math.abs(coefficients[row][ctr])>.0000000001) {
				
				errorCounter = 0;
				//we haven't considered this row yet
				
				//first we make the value at [row][ctr] a 1.0, by multiplying the entire row by a constant:
				t = 1/coefficients[row][ctr];
				for(a = 0; a<coefficients[row].length; a++) {
					coefficients[row][a] *= t;
				}
				coefficients[row][ctr] = 1.0; //to make sure, despite possible arithmetic rounding, that we get 1.0
				
				b[row] = true;
				for(a = 0; a<coefficients.length; a++) {
					if(b[a]==false) {
						//this is a row we need to adjust
						t = coefficients[a][ctr];
						for(i = 0; i<coefficients[a].length; i++) {
							coefficients[a][i] -= coefficients[row][i]*t;
						}
					}
				}
				//we've now considered this row
				order[ctr++] = row;
				
			}
			errorCounter++;
			row++;
			row = row%(coefficients.length);
			if(errorCounter>coefficients.length) {
				if(VERBOSE_EXCEPTIONS) {
					throw new IllegalArgumentException("The coefficient matrix cannot be solved.  Either it has infinitely many solutions, or zero solutions:\n"+toString(coefficients));
				} else {
					throw new IllegalArgumentException("The coefficient matrix cannot be solved.  Either it has infinitely many solutions, or zero solutions.");
				}
			}
			//System.out.println("\trow = "+row);
			//println(coefficients);
		}
		
		//System.out.println("\t\tmoving on...");
		
		//coefficients now contains an array that looks like this:
		//    1  a  b  c ...  x
		//    0  1  d  e ...  x
		//    0  0  1  f ...  x
		//	  0  0  0  1 ...  x
		//    ..........  1   x
		
		//so now we have to clear out all the other values by backtracking.
		//we stored the order we calculated everything in in the "order" array
		
		//the last element in the order array points to the row
		//in our matrix that reads "0 ... 0 1 z".  In other words,
		// it is the only solved row.
		//So we can skip it, and move to the row that reads: "0 ... 1 a b".
		
		ctr = 0;
		for(a = order.length-2; a>=0; a--) {
			row = order[a];
			for(i = coefficients[row].length-2; i>a; i--) {
				t = coefficients[row][i]*coefficients[order[i]][coefficients[row].length-1];
				coefficients[row][coefficients[row].length-1] -= t;
				coefficients[row][i] = 0;
			}
			//System.out.println("\trow = "+row);
			//println(coefficients);
		}
		if(sort) {
			Arrays.sort(coefficients,coefficientComparator);
		}
	}
  
	/** Given a matrix of variable coefficients for a linear system of equations,
	* this will solve for each variable.
	* <P> For example, if you have the three equations:
	* <BR> 2x + y + z = 1
	* <BR> 6x + 2y + z = -1
	* <BR> -2x + 2y + z = 7
	* <P> You can call:
	* <code><BR>double[][] coeffs = { { 2, 1, 1, 1},</code>
	* <code><BR>                      { 6, 2, 1, -1},</code>
	* <code>	<BR>						{ -2, 2, 1, 7} };</code>
	* <code><BR>Equations.solve(coeffs,true);</code>
	* <P> Then this method will simplify the matrix to:
	* <code><BR>{ { 1, 0, 0, -1}, </code>
	* <code><BR>  { 0, 1, 0, 2}, </code>
	* <code><BR>  { 0, 0, 1, 1} }</code>
	* <P> This indicates that:
	* <BR> 1*x+0*y+0*z = -1
	* <BR> 0*x+1*y+0*z = 2
	* <BR> 0*x+0*y+1*z = 1
	* <P> This method uses Gaussian Elimination and back substitution.
	* <P> Note that due to the limits of double-precision, some safeguards were
	* built-in that may round values less than .00000001 to zero.  If you
	* need better precision, you may have to copy and paste the algorithm
	* in this code and use a more precise number format.
	* @param coefficients the matrix of coefficients.  It must be N x (N+1) units in size.
	* @param sort If this is <code>true</code>, then you're guaranteed for your final solution
	* to be sorted such that the 1's form a diagonal line from [0,0] to [N,N].  If this
	* is <code>false</code>, then the solution <i>may</i> not form a diagonal line of 1's.
	* <P> (If there are no zeroes in the matrix, the diagonal line will be there by default,
	* but if zeroes are present then some rows may have to be skipped and the normal order
	* is disrupted.)
	* <P> The sort is performed by calling <code>Arrays.sort()</code>.
	* @throws IllegalArgumentException if it is impossible to solve for each variable.  In this case an
	* exception is thrown, and no guarantee is made regarding the final state of the
	* <code>coefficients</code> matrix.
	* <P> Also an exception may be thrown if the matrix is not correctly sized.
	*/
  public static void solve(BigDecimal[][] coefficients,boolean sort) {
		if(coefficients==null) throw new NullPointerException("The coefficients matrix is null.");
		int size = coefficients.length;
		
		//this will keep track of which rows have been processed
		//in an ideal world, we would like to go sequentially down the list,
		//but if a coefficient somewhere is already zero, then we may have to skip around.
		boolean[] b = new boolean[coefficients.length];
		
		//this keeps track of which row we solved, from first to last
		//we use this to back substitute
		int[] order = new int[b.length];
		
		int ctr = 0; 
		int row = 0;
		int a, i;
		BigDecimal t;
		BigDecimal tolerance = new BigDecimal(.0000000001);
		int errorCounter = 0;
		//println(coefficients);
		while(ctr<b.length) {
			if(coefficients[row].length!=size+1) 
				throw new IllegalArgumentException("The matrix must be N x (N+1) units long.  The matrix provided is "+size+" x "+coefficients[row].length+" units.");
			if(b[row]==false && coefficients[row][ctr].compareTo(tolerance)>0 ) {
				
				errorCounter = 0;
				//we haven't considered this row yet
				
				//first we make the value at [row][ctr] a 1.0, by multiplying the entire row by a constant:
				t = BigDecimal.ONE.divide(coefficients[row][ctr]);
				for(a = 0; a<coefficients[row].length; a++) {
					coefficients[row][a] = coefficients[row][a].multiply(t);
				}
				coefficients[row][ctr] = BigDecimal.ONE; //to make sure, despite possible arithmetic rounding, that we get 1.0
				
				b[row] = true;
				for(a = 0; a<coefficients.length; a++) {
					if(b[a]==false) {
						//this is a row we need to adjust
						t = coefficients[a][ctr];
						for(i = 0; i<coefficients[a].length; i++) {
							coefficients[a][i] = coefficients[a][i].subtract( coefficients[row][i].multiply(t) );
						}
					}
				}
				//we've now considered this row
				order[ctr++] = row;
				
			}
			errorCounter++;
			row++;
			row = row%(coefficients.length);
			if(errorCounter>coefficients.length) {
				if(VERBOSE_EXCEPTIONS) {
					throw new IllegalArgumentException("The coefficient matrix cannot be solved.  Either it has infinitely many solutions, or zero solutions:\n"+toString(coefficients));
				} else {
					throw new IllegalArgumentException("The coefficient matrix cannot be solved.  Either it has infinitely many solutions, or zero solutions.");
				}
			}
			//System.out.println("\trow = "+row);
			//println(coefficients);
		}
		
		//System.out.println("\t\tmoving on...");
		
		//coefficients now contains an array that looks like this:
		//    1  a  b  c ...  x
		//    0  1  d  e ...  x
		//    0  0  1  f ...  x
		//	  0  0  0  1 ...  x
		//    ..........  1   x
		
		//so now we have to clear out all the other values by backtracking.
		//we stored the order we calculated everything in in the "order" array
		
		//the last element in the order array points to the row
		//in our matrix that reads "0 ... 0 1 z".  In other words,
		// it is the only solved row.
		//So we can skip it, and move to the row that reads: "0 ... 1 a b".
		
		ctr = 0;
		for(a = order.length-2; a>=0; a--) {
			row = order[a];
			for(i = coefficients[row].length-2; i>a; i--) {
				t = coefficients[row][i].multiply( coefficients[order[i]][coefficients[row].length-1] );
				coefficients[row][coefficients[row].length-1] = coefficients[row][coefficients[row].length-1].subtract(t);
				coefficients[row][i] = BigDecimal.ZERO;
			}
			//System.out.println("\trow = "+row);
			//println(coefficients);
		}
		if(sort) {
			Arrays.sort(coefficients,bigCoefficientComparator);
		}
	}
}
