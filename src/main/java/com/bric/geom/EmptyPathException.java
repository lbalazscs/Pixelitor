/*
 * @(#)EmptyPathException.java
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

/** This indicates that a path had no shape data.
 * <P>This means it had no lines, quadratic or cubic
 * segments in it (although it may have had a MOVE_TO
 * and a CLOSE segment).
 *
 */
public class EmptyPathException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EmptyPathException() {
	}

	public EmptyPathException(String message) {
		super(message);
	}

	public EmptyPathException(Throwable cause) {
		super(cause);
	}

	public EmptyPathException(String message, Throwable cause) {
		super(message, cause);
	}

}
