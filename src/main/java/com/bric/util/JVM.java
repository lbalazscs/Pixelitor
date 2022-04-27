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

/**
 * Static methods relating to the JVM environment.
 * <P>Instead of burying a constant like "isQuartz" in its most
 * relevant class (such as OptimizedGraphics2D), it should be
 * stored here so if other classes need to access it they don't
 * necessary have to
 */
public class JVM {

    /**
     * Prints basic information about this session's JVM:
     * the OS name &amp; version, the Java version, and (on Mac) whether Quartz is being used.
     */
    public static void printProfile() {
        System.out.println(getProfile());
    }

    /**
     * Gets basic information about this session's JVM:
     * the OS name &amp; version, the Java version, and (on Mac) whether Quartz is being used.
     */
    public static String getProfile() {
        String sb = "OS = " + System.getProperty("os.name")
                    + " (" + System.getProperty("os.version")
                    + "), " + System.getProperty("os.arch") + "\n"
                    + "Java Version = " + System.getProperty("java.version")
                    + "\n";
        return sb;
    }

    private static final String osName = System.getProperty("os.name").toLowerCase();

    /**
     * Whether this session is on a Mac.
     */
    public static final boolean isMac = osName.contains("mac");

    public static final boolean isLinux = osName.toLowerCase().contains("linux");

    /**
     * Whether this session is on Windows.
     */
    public static final boolean isWindows = osName.contains("windows");

    /**
     * Whether this session is on Vista.
     */
    public static final boolean isVista = osName.contains("vista");

    /**
     * If on a Mac: whether Quartz is the rendering pipeline.
     * In applets this may throw a security exception; if this
     * cannot be ascertained we assume it is false.
     */
    public static final boolean usingQuartz = isUsingQuartz();

    private static boolean isUsingQuartz() {
        return isMac && "true".equals(System.getProperty("apple.awt.graphics.UseQuartz"));
    }
}
