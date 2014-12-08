/*
 * @(#)ResourcePool.java
 *
 * $Date: 2014-06-06 20:04:49 +0200 (P, 06 jún. 2014) $
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
package com.bric.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** This object manages a pool of resources (arrays and images)
 * for frequent repetitive reuse.
 * <p>Sometimes constantly reallocating
 * resources in tight loops can be an expensive drain, so this
 * should help relieve that performance expense.
 * <p>This pool also has limitations, though, so it does not
 * introduce a memory leak. Strong references to resources are
 * purged after a fixed time interval (usually 5 seconds). Also
 * there are limits to the number of resources of any given
 * type that will be stored. There are two such limits:
 * <ul><li>A limit of the exact resource type. For example: by default
 * there will only be 5 images of the same width, height and type.</li>
 * <li>A limit of all resources of a given type. For example: by
 * default there will only be 20 images total.</li></ul>
 * <p>(Arrays have a higher tolerance.)
 * <p>It is still possible to abuse this model and introduce
 * memory problems, in the same way that is possible to run
 * out of memory just by constructing a few 10,000x10,000 images.
 * But when used responsibly, this should be a valuable tool to
 * easy the cost of constructing thousands of similar objects.
 * <p>Every time you retrieve an object from this pool, you should
 * wrap the following code in a try/finally block and return
 * the object back to this pool when finished.
 * @see ResourcePoolDemo
 */
public class ResourcePool {
	
	private enum Type { IMAGE, FLOAT, INT, DOUBLE, SHORT, LONG, BYTE };
	
	/** The limit for each type of resource this pool manages. */
	public static class Limit {
		int totalLimit, groupLimit;
		double timeLimit;
		/**
		 * 
		 * @param groupLimit the number of identical instances this pool stores.
		 * @param totalLimit the total number of objects of a given type this stores.
		 * @param timeLimit the number of seconds this stores a resource.
		 */
		public Limit(int groupLimit,int totalLimit,double timeLimit) {
			this.totalLimit = totalLimit;
			this.groupLimit = groupLimit;
			this.timeLimit = timeLimit;
		}
	}
	
	private static Set<WeakReference<ResourcePool>> allPools = new HashSet<WeakReference<ResourcePool>>();
	static {
		new Thread("ResourcePool Cleanup") {
			public void run() {
				while(true) {
					synchronized(allPools) {
						Iterator<WeakReference<ResourcePool>> iter = allPools.iterator();
						while(iter.hasNext()) {
							WeakReference<ResourcePool> ref = iter.next();
							ResourcePool pool = ref.get();
							if(pool==null) {
								iter.remove();
							} else {
								pool.clean();
							}
						}
					}
					try {
						Thread.sleep(5000);
					} catch(Exception e) {}
				}
			}
		}.start();
	}
	
	private static long recordIDCtr = 0;
	
	private static class Record {
		Object resource;
		long timestamp = System.currentTimeMillis();
		long id = recordIDCtr++;
		
		public Record(Object resource) {
			this.resource = resource;
		}
	}
	
	private static Comparator<Record> recordComparator = new Comparator<Record>() {

		public int compare(Record o1, Record o2) {
			if(o1.timestamp<o2.timestamp)
				return -1;
			if(o1.timestamp>o2.timestamp)
				return 1;
			if(o1.id<o2.id)
				return -1;
			if(o1.id>o2.id)
				return 1;
			return 0;
		}
	};

	private static ResourcePool globalPool = new ResourcePool();
	
	/** Return the default <code>ResourcePool</code>.
	 */
	public static ResourcePool get() {
		return globalPool;
	}

	Map<Type, Limit> limits = new HashMap<Type, Limit>();
	Map<Type, Map<Long, SortedSet<Record>>> resources = new HashMap<Type, Map<Long, SortedSet<Record>>>();
	
	/** Create a new <code>ResourcePool</code>.
	 */
	public ResourcePool() {
		synchronized(allPools) {
			allPools.add(new WeakReference<ResourcePool>(this));
		}
		
		limits.put( Type.IMAGE, new Limit(5, 20, 5.0) );
	}
	
	/** Iterate over all resources and make sure nothing has expired
	 * based on the current time limits and time stamps.
	 * 
	 */
	protected synchronized void clean() {
		long currentTime = System.currentTimeMillis();
		for(Type type : resources.keySet()) {
			Limit limit = getLimit(type);
			long timelimit = (long)(limit.timeLimit*1000);
			Map<Long, SortedSet<Record>> group = resources.get(type);
			for(Long id : group.keySet()) {
				SortedSet<Record> r = group.get(id);
				Iterator<Record> i = r.iterator();
				while(i.hasNext()) {
					Record record = i.next();
					if(record.timestamp+timelimit<currentTime) {
						//System.out.println("cleaning "+type+", "+id+", "+record.resource);
						i.remove();
					}
				}
			}
		}
	}

	private static final Limit genericLimit = new Limit(1000, 2000, 5.0);
	/** Return the Limit for a given resource type. */
	protected Limit getLimit(Type type) {
		Limit l = limits.get(type);
		if(l==null) return genericLimit;
		return l;
	}
	
	/** Return a BufferedImage from this pool. If no cached
	 * images exists matching these parameters: then a new image
	 * is created.
	 * @param width the width of the image.
	 * @param height the height of the image.
	 * @param type the type of the image.
	 * @param clear if true and a cached image is identified: then
	 * that image is first cleared (using an AlphaComposite.Clear).
	 * @return an image matching the arguments provided. If possible this
	 * will recycle a previously cached image.
	 */
	public synchronized BufferedImage getImage(int width,int height,int type,boolean clear) {
		long id = (width << 16) + (height << 8) + type;
		BufferedImage bi = (BufferedImage)get( Type.IMAGE, id);
		if(bi!=null) {
			if(clear) {
				Graphics2D g = bi.createGraphics();
				g.setComposite(AlphaComposite.Clear);
				g.fillRect(0,0,width,height);
				g.dispose();
			}
			return bi;
		}
		return new BufferedImage(width, height, type);
	}
	
	/** Return a cached instanceof a resource, or null if no cached instance
	 * exists.
	 */
	private Object get(Type type,long id) {
		Map<Long, SortedSet<Record>> group = resources.get(type);
		SortedSet<Record> records = group==null ? null : group.get(id);
		if(records!=null && records.size()>0) {
			Iterator<Record> iter = records.iterator();
			Record r = iter.next();
			iter.remove();
			//System.out.println("getting "+type+", "+id+", "+r.resource);
			return r.resource;
		}
		//System.out.println("missing "+type+", "+id);
		return null;
	}
	
	protected synchronized void finalize() {
		resources.clear();
	}

	/** Return an int array from this pool. If no cached
	 * int array exists matching the requested length: then a new array
	 * is created.
	 * @param length the array length
	 */
	public synchronized int[] getIntArray(int length) {
		int[] cachedValue = (int[])get( Type.INT, length);
		if(cachedValue!=null) return cachedValue;
		return new int[length];
	}

	/** Return a float array from this pool. If no cached
	 * float array exists matching the requested length: then a new array
	 * is created.
	 * @param length the array length
	 */
	public synchronized float[] getFloatArray(int length) {
		float[] cachedValue = (float[])get( Type.FLOAT, length);
		if(cachedValue!=null) return cachedValue;
		return new float[length];
	}

	/** Return a double array from this pool. If no cached
	 * double array exists matching the requested length: then a new array
	 * is created.
	 * @param length the array length
	 */
	public synchronized double[] getDoubleArray(int length) {
		double[] cachedValue = (double[])get( Type.DOUBLE, length);
		if(cachedValue!=null) return cachedValue;
		return new double[length];
	}

	/** Return a short array from this pool. If no cached
	 * short array exists matching the requested length: then a new array
	 * is created.
	 * @param length the array length
	 */
	public synchronized short[] getShortArray(int length) {
		short[] cachedValue = (short[])get( Type.SHORT, length);
		if(cachedValue!=null) return cachedValue;
		return new short[length];
	}

	/** Return a long array from this pool. If no cached
	 * long array exists matching the requested length: then a new array
	 * is created.
	 * @param length the array length
	 */
	public synchronized long[] getLongArray(int length) {
		long[] cachedValue = (long[])get( Type.LONG, length);
		if(cachedValue!=null) return cachedValue;
		return new long[length];
	}

	/** Return a byte array from this pool. If no cached
	 * byte array exists matching the requested length: then a new array
	 * is created.
	 * @param length the array length
	 */
	public synchronized byte[] getByteArray(int length) {
		byte[] cachedValue = (byte[])get( Type.BYTE, length);
		if(cachedValue!=null) return cachedValue;
		return new byte[length];
	}
	
	/** Store an array in this pool for future reuse.
	 * @return true if the array was stored, false if
	 * this pool is not currently accepting any more
	 * arrays of this type/size.
	 */
	public synchronized boolean put(long[] array) {
		return store( Type.LONG, array.length, array);
	}

	/** Store an array in this pool for future reuse.
	 * @return true if the array was stored, false if
	 * this pool is not currently accepting any more
	 * arrays of this type/size.
	 */
	public synchronized boolean put(float[] array) {
		return store( Type.FLOAT, array.length, array);
	}

	/** Store an array in this pool for future reuse.
	 * @return true if the array was stored, false if
	 * this pool is not currently accepting any more
	 * arrays of this type/size.
	 */
	public synchronized boolean put(int[] array) {
		return store( Type.INT, array.length, array);
	}

	/** Store an array in this pool for future reuse.
	 * @return true if the array was stored, false if
	 * this pool is not currently accepting any more
	 * arrays of this type/size.
	 */
	public synchronized boolean put(byte[] array) {
		return store( Type.BYTE, array.length, array);
	}

	/** Store an array in this pool for future reuse.
	 * @return true if the array was stored, false if
	 * this pool is not currently accepting any more
	 * arrays of this type/size.
	 */
	public synchronized boolean put(short[] array) {
		return store( Type.SHORT, array.length, array);
	}

	/** Store an array in this pool for future reuse.
	 * @return true if the array was stored, false if
	 * this pool is not currently accepting any more
	 * arrays of this type/size.
	 */
	public synchronized boolean put(double[] array) {
		return store( Type.DOUBLE, array.length, array);
	}

	/** Store an image in this pool for future reuse.
	 * @return true if the image was stored, false if
	 * this pool is not currently accepting any more
	 * images of this type/size.
	 */
	public synchronized boolean put(BufferedImage bi) {
		long id = (bi.getWidth() << 16) + (bi.getHeight() << 8) + bi.getType();
		return store( Type.IMAGE, id, bi);
	}
	
	/** Return the total number of resources stored in this pool of this type. */
	private int getSize(Type type) {
		int totalSize = 0;
		Map<Long, SortedSet<Record>> group = resources.get(type);
		for(Long id : group.keySet()) {
			SortedSet<Record> cluster = group.get(id);
			totalSize += cluster.size();
		}
		return totalSize;
	}
	
	/** Store a resource in this pool.
	 * 
	 * @return true if the resource was stored, false if the current
	 * Limits do not allow storing more objects of this type/id
	 */
	private boolean store(Type type,long id,Object resource) {
		Map<Long, SortedSet<Record>> group = resources.get(type);
		Limit limit = getLimit(type);
		if(group==null) {
			group = new HashMap<Long, SortedSet<Record>>();
			resources.put(type, group);
		}
		SortedSet<Record> records = group.get(id);
		if(records==null) {
			records = new TreeSet<Record>(recordComparator);
			group.put(id, records);
		}

		if(records.size()+1>limit.groupLimit) {
			//System.out.println("abandoning "+type+", "+id+", "+resource);
			return false;
		}
		int totalSize = getSize(type);
		if(totalSize+1>limit.totalLimit) {
			//System.out.println("abandoning "+type+", "+id+", "+resource);
			return false;
		}
		//System.out.println("storing "+type+", "+id+", "+resource);
		records.add(new Record(resource));
		return true;
	}
}
