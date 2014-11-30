/*
 * @(#)FocusArrowListener.java
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
package com.bric.plaf;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** This listens for arrow keys and shifts
 * the keyboard focus accordingly.
 * So if you press the left arrow key, the component
 * to the left of the source component requests the focus.
 * <P>This scans for the first available component whose
 * <code>isFocusable()</code> method returns <code>true</code>.
 * If no such component is found: nothing happens.
 */
public class FocusArrowListener extends KeyAdapter {

	@Override
	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		int dx = 0;
		int dy = 0;
		if(code==KeyEvent.VK_LEFT) {
			dx = -1;
		} else if(code==KeyEvent.VK_UP) {
			dy = -1;
		} else if(code==KeyEvent.VK_RIGHT) {
			dx = 1;
		} else if(code==KeyEvent.VK_DOWN) {
			dy = 1;
		}

		if( (dx==0 && dy==0)==false && shiftFocus(dx,dy,(Component)e.getSource()))
			e.consume();
	}
	
	/** Shifts the focus in a certain direction.
	 * 
	 * @param dx the amount to increment x.
	 * @param dy the amount to increment y.
	 * @param src the source to traverse from.
	 * @return true if another component requested the focus
	 * as a result of this method.  This may return false if
	 * no suitable component was found to shift focus to.
	 * (If you press the right arrow key on the right-most
	 * component, for example.)
	 */
	public static boolean shiftFocus(int dx,int dy,Component src) {
		if(dx==0 && dy==0) //this would result in an infinite loop
			throw new IllegalArgumentException("dx ("+dx+") and ("+dy+") cannot both be zero");
		
		Set<Component> focusableComponents = getFocusableComponents(src);
		
		int x = src.getWidth()/2;
		int y = src.getHeight()/2;
		Window window = SwingUtilities.getWindowAncestor(src);
		if(window==null) return false;
		Point p = SwingUtilities.convertPoint(src, x, y, window);
		
		Component comp = null;
		int windowWidth = window.getWidth();
		int windowHeight = window.getHeight();
		
		while(p.x>0 && p.x<windowWidth && p.y>0 && p.y<windowHeight && (comp==null || comp==src || (comp instanceof JPanel))) {
			p.x += dx;
			p.y += dy;
			comp = SwingUtilities.getDeepestComponentAt(window, p.x, p.y);
			boolean canAcceptFocus = focusableComponents.contains( comp );
			if(comp!=null && canAcceptFocus==false)
				comp = null;
		}
		
		//TODO: implement a more robust searching mechanism instead of the above
		//If a component is below the src, but to the left or right of the center:
		//it should still be detected when you press the down arrow key.

		if(comp!=null && comp!=src && comp!=window && (!(comp instanceof JPanel))) {
			comp.requestFocus();
			return true;
		}
		return false;
	}
	
	/** Returns a set of all the components that
	 * can have the keyboard focus.
	 * <P>My first implementation involved of this concept
	 * simply involved asking JCompnonents if they were
	 * focusable, but in the <code>FilledButtonTest</code> this
	 * resulted in shifting focus to the ContentPane.  Although
	 * it is technically focusable: if I used the tab key
	 * I did <i>not</i> get this result.  So I studied
	 * the inner workings for Component.transferFocus()
	 * and ended up with a method that involved
	 * calls to <code>getFocusCycleRootAncestor()</code>,
	 * and <code>getFocusTraversalPolicy()</code>.
	 * <P>(Also credit goes to Werner for originally tipping me off
	 * towards looking at FocusTraversalPolicies.)
	 * @param currentFocusOwner the current focus owner.
	 * @return all the JComponents that can receive the focus.
	 */
	public static Set<Component> getFocusableComponents(Component currentFocusOwner) {
		HashSet<Component> set = new HashSet<Component>();
		set.add(currentFocusOwner);

        Container rootAncestor = currentFocusOwner.getFocusCycleRootAncestor();
        Component comp = currentFocusOwner;
        while (rootAncestor != null && 
               !(rootAncestor.isShowing() && 
                 rootAncestor.isFocusable() && 
                 rootAncestor.isEnabled())) 
        {
            comp = rootAncestor;
            rootAncestor = comp.getFocusCycleRootAncestor();
        }
        if (rootAncestor != null) {
            FocusTraversalPolicy policy =
                rootAncestor.getFocusTraversalPolicy();
            Component toFocus = policy.getComponentAfter(rootAncestor, comp);
            
            while(toFocus!=null && set.contains(toFocus)==false) {
            	set.add(toFocus);
                toFocus = policy.getComponentAfter(rootAncestor, toFocus);
            }
            
            toFocus = policy.getComponentBefore(rootAncestor, comp);
            
            while(toFocus!=null && set.contains(toFocus)==false) {
            	set.add(toFocus);
                toFocus = policy.getComponentBefore(rootAncestor, toFocus);
            }
        }
		return set;
	}
}
