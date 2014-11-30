/*
 * @(#)IconSample.java
 *
 * $Date: 2014-11-27 07:50:51 +0100 (Cs, 27 nov. 2014) $
 *
 * Copyright (c) 2014 by Jeremy Wood.
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
package com.bric.blog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** This is used by the {@link CreateSamplesJob} to apply screenshots/animations
 * to javadocs.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceSample {
	
	/** An array of strings used to construct a particular resource.
	 * <p>For example: "new com.bric.swing.resources.TriangleIcon( 24, 24, new Color( 0 ) )"
	 * <p>This is something that {@link com.bric.reflect.Reflection#parse(String)} is capable of parsing.
	 */
	String[] sample();
	
	/** Return a list of the names of the samples provided. There should be a 1:1 correspondence
	 * for every sample provided. By default this returns an empty array, which indicates
	 * that nothing will be labeled.
	 */
	String[] names() default {};
	
	/** This is the number of columns to show if the samples are presented in a table.
	 * The default value is 4.
	 */
	int columnCount() default 4;
	
	/** An optional list of row names for table data. */
	String[] rowNames() default {};

	/** An optional list of column names for table data. */
	String[] columnNames() default {};

	/** The text that precedes the table of samples. The default value is "Here are some samples:" */
	String tableIntroduction() default "Here are some samples:";
}
