/*
 * @(#)SerializedPathIterator.java
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

import java.awt.geom.PathIterator;

/** A PathIterator that parses serialized shape info.
 */
class SerializedPathIterator implements PathIterator {
    char[] c;
    int ctr = 0;
    double[] data = new double[6];
    int currentSegment = -1;
    int windingRule;
    
    public SerializedPathIterator(String s,int windingRule) {
    	if(!(windingRule==PathIterator.WIND_EVEN_ODD || windingRule==PathIterator.WIND_NON_ZERO))
    		throw new IllegalArgumentException("The winding rule must be PathIterator.WIND_NON_ZERO or PathIterator.WIND_EVEN_ODD");
    	
        c = s.toCharArray();
        this.windingRule = windingRule;
        next();
    }
    
    public int getWindingRule() {
        return windingRule;
    }
    
    protected void consumeWhiteSpace(boolean expectingWhiteSpace) {
    	if(ctr>=c.length) {
    		ctr = c.length+2;
    		return;
    	}
    	
    	char ch = c[ctr];
    	if(Character.isWhitespace(ch)==false) {
    		if(expectingWhiteSpace==false)
    			return;
    		throw new ParserException("expected whitespace", ctr, 1);
    	}
    	while(true) {
    		ctr++;
    		if(ctr>=c.length) {
    			ctr = c.length+2;
    			return;
    		}
    		
    		ch = c[ctr];
    		if(Character.isWhitespace(ch)==false) {
    			return;
    		}
    	}
    }
    
    public void next() {
    	consumeWhiteSpace(false);
    	
        if(ctr>=c.length) {
        	ctr = c.length+2;
            return;
        }
        int terms;
        char k = c[ctr];
        
        switch(k) {
	        case 'm':
	        case 'M':
	            currentSegment = PathIterator.SEG_MOVETO;
	            terms = 2;
	            break;
	        case 'l':
	        case 'L':
	            currentSegment = PathIterator.SEG_LINETO;
	            terms = 2;
	            break;
	        case 'q':
	        case 'Q':
	            currentSegment = PathIterator.SEG_QUADTO;
	            terms = 4;
	            break;
	        case 'c':
	        case 'C':
	            currentSegment = PathIterator.SEG_CUBICTO;
	            terms = 6;
	            break;
	        case 'z':
	        case 'Z':
	            currentSegment = PathIterator.SEG_CLOSE;
	            terms = 0;
	            break;
	        default:
	        	throw new ParserException("Unrecognized character in shape data: \'"+c[ctr]+"\'", ctr, 1);
	        	
        }
        ctr++;
        if(terms>0) {
            parseTerms(terms);
        } else {
        	if(ctr<c.length) {
        		if(Character.isWhitespace( c[ctr] )==false)
        			throw new ParserException("expected whitespace after z", ctr, 1);
        	}
        }
    }

    class ParserException extends RuntimeException {
    	private static final long serialVersionUID = 1L;

		ParserException(String msg,int ptr,int length) {
    		super(msg);
    		System.err.println("\""+(new String(c))+"\"");
    		StringBuffer sb = new StringBuffer();
    		for(int a = 0; a<ptr+1; a++) {
    			sb.append(' ');
    		}
    		for(int a = 0; a<length; a++) {
    			sb.append('^');
    		}
    		System.err.println(sb);
    	}
    }
    
    protected void parseTerms(int terms) {
        for(int a = 0; a<terms; a++) {
            data[a] = parseTerm();
        }
    }
    
    protected double parseTerm() {
        consumeWhiteSpace(true);
        int i = ctr;
        while(i<c.length && (Character.isWhitespace(c[i])==false) ) {
        	i++;
        }
        String string = new String(c, ctr, i-ctr);
        try {
        	return Double.parseDouble( string );
        } catch(RuntimeException e) {
        	//just constructing this prints data to System.err:
        	ParserException e2 = new ParserException(e.getMessage(), ctr, i-ctr);
        	throw e2;
        } finally {
	        ctr = i;
        }
    }
    
    public int currentSegment(double[] d) {
        d[0] = data[0];
        d[1] = data[1];
        d[2] = data[2];
        d[3] = data[3];
        d[4] = data[4];
        d[5] = data[5];
        return currentSegment;
    }
    
    public int currentSegment(float[] f) {
        f[0] = (float)data[0];
        f[1] = (float)data[1];
        f[2] = (float)data[2];
        f[3] = (float)data[3];
        f[4] = (float)data[4];
        f[5] = (float)data[5];
        return currentSegment;
    }
    
    public boolean isDone() {
        return ctr>c.length+1;
    }
}
