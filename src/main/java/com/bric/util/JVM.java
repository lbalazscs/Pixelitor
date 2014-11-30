/*
 * @(#)JVM.java
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
package com.bric.util;

import java.security.AccessControlException;

/** Static methods relating to the JVM environment.
 * <P>Instead of burying a constant like "isQuartz" in its most
 * relevant class (such as OptimizedGraphics2D), it should be
 * stored here so if other classes need to access it they don't
 * necessary have to 
 */
public class JVM {

	/** Prints basic information about this session's JVM:
	 * the OS name &amp; version, the Java version, and (on Mac) whether Quartz is being used.
	 */
	public static void printProfile() {
		System.out.println(getProfile());
	}
	
	/** Gets basic information about this session's JVM:
	 * the OS name &amp; version, the Java version, and (on Mac) whether Quartz is being used.
	 */
	public static String getProfile() {
		StringBuffer sb = new StringBuffer();
		sb.append("OS = "+System.getProperty("os.name")+" ("+System.getProperty("os.version")+"), "+System.getProperty("os.arch")+"\n");
		sb.append("Java Version = "+System.getProperty("java.version")+"\n");
		return sb.toString();
	}
	
	/** The major Java version being used (1.4, 1.5, 1.6, etc.), or
	 * -1 if this value couldn't be correctly determined.
	 */
	public static final float javaVersion = JVM.getMajorJavaVersion(true);

	private static final String osName = (System.getProperty("os.name").toLowerCase());
	
	/** Whether this session is on a Mac. */
	public static final boolean isMac = (osName.indexOf("mac")!=-1);

	public static final boolean isLinux = (osName.toLowerCase().indexOf("linux")!=-1);

	/** Whether this session is on Windows. */
	public static final boolean isWindows = (osName.indexOf("windows")!=-1);

	/** Whether this session is on Vista. */
    public static final boolean isVista = (osName.indexOf("vista")!=-1);
    
	/** Whether this session is on Windows 7. */
    public static final boolean isWindows7 = isWindows && (osName.indexOf("7")!=-1);
    
	/** Whether this session is on Windows XP. */
    public static final boolean isWindowsXP = isWindows && (osName.indexOf("xp")!=-1);
    
	/** Whether this session is on Windows Vista or Windows 7. */
    public static final boolean isVistaOrWindows7 = isVista || isWindows7;
	
	/** If on a Mac: whether Quartz is the rendering pipeline.
	 * In applets this may throw a security exception; if this
	 * cannot be ascertained we assume it is false.
	 */
	public static final boolean usingQuartz = isUsingQuartz();
	
	private static boolean isUsingQuartz() {
		try {
			return isMac && ((javaVersion>0 && javaVersion<1.4f) || (System.getProperty("apple.awt.graphics.UseQuartz")!=null && System.getProperty("apple.awt.graphics.UseQuartz").toString().equals("true")));
		} catch(AccessControlException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/** This converts the system property "java.version" to a float value.
	 * This drops rightmost digits until a legitimate float can be parsed.
	 * <BR>For example, this converts "1.6.0_05" to "1.6".
	 * <BR>This value is cached as the system property "java.major.version".  Although
	 * technically this value is a String, it will always be parseable as a float.
	 * @throws AccessControlException this may be thrown in unsigned applets!  Beware!
	 */
	public static float getMajorJavaVersion() throws AccessControlException {
		String majorVersion = null;
		try {
			System.getProperty("java.major.version");
		} catch(java.security.AccessControlException e) {
			return -1;
		}
		if(majorVersion==null) {
			String s = System.getProperty("java.version");
			float f = -1;
			int i = s.length();
			while(f<0 && i>0) {
				try {
					f = Float.parseFloat(s.substring(0,i));
				} catch(Exception e) {}
				i--;
			}
			majorVersion = Float.toString(f);
			System.setProperty("java.major.version",majorVersion);
		}
		return Float.parseFloat(majorVersion);
	}

	/** 
	 * 
	 * @param catchSecurityException if true and an exception occurs,
	 * then -1 is returned.
	 * @return the major java version, or -1 if this can't be determined/
	 */
	public static float getMajorJavaVersion(boolean catchSecurityException) {
		try {
			return getMajorJavaVersion();
		} catch(RuntimeException t) {
			if(catchSecurityException) {
				System.err.println("this exception was ignored without incident, but it means we can't determine the major java version:");
				t.printStackTrace();
				return -1;
			}
			throw t;
		}
	}
}
