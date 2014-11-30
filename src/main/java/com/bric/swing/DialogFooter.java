/*
 * @(#)DialogFooter.java
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
package com.bric.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Method;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.bric.plaf.FocusArrowListener;
import com.bric.util.JVM;

/** This is a row of buttons, intended to be displayed at the
 * bottom of a dialog.  This class is strongly related to the
 * {@link com.bric.swing.QDialog} project, although the
 * <code>DialogFooter</code> can exist by itself.
 * <P>On the left of a footer are controls that should apply to the dialog itself,
 * such as "Help" button, or a "Reset Preferences" button.
 * On the far right are buttons that should dismiss this dialog.  They
 * may be presented in different orders on different platforms based
 * on the <code>reverseButtonOrder</code> boolean.
 * <P>Buttons are also generally normalized, so the widths of buttons
 * are equal.
 * <P>This object will "latch onto" the RootPane that contains it.  It is assumed
 * two DialogFooters will not be contained in the same RootPane. It is also
 * assumed the same DialogFooter will not be passed around to several
 * different RootPanes.
 * <h3>Preset Options</h3>
 * This class has several OPTION constants to create specific buttons.
 * <P>In each constant the first option is the default button unless
 * you specify otherwise.  The Apple Interface Guidelines advises:
 * "The default button should be the button that represents the
 * action that the user is most likely to perform if that action isn't
 * potentially dangerous."
 * <P>The YES_NO options should be approached with special reluctance.
 * Microsoft <A HREF="http://msdn.microsoft.com/en-us/library/aa511331.aspx">cautions</A>,
 * "Use Yes and No buttons only to respond to yes or no questions."  This seems
 * obvious enough, but Apple adds, "Button names should correspond to the action
 * the user performs when pressing the button-for example, Erase, Save, or Delete."
 * So instead of presenting a YES_NO dialog with the question "Do you want to continue?"
 * a better dialog might provide the options "Cancel" and "Continue".  In short: we
 * as developers might tend to lazily use this option and phrase dialogs in such
 * a way that yes/no options make sense, but in fact the commit buttons should be
 * more descriptive.
 * <P>Partly because of the need to avoid yes/no questions, <code>DialogFooter</code> introduces the
 * dialog type: SAVE_DONT_SAVE_CANCEL_OPTION.  This is mostly straightforward, but
 * there is one catch: on Mac the buttons
 * are reordered: "Save", "Cancel" and "Don't Save".  This is to conform with standard
 * Mac behavior.  (Or, more specifically: because the Apple guidelines
 * state that a button that can cause permanent data loss be as physically far
 * from a "safe" button as possible.)  On all other platforms the buttons are
 * listed in the order "Save", "Don't Save" and "Cancel".
 * <P>Also note the field {@link #reverseButtonOrder}
 * controls the order each option is presented in the dialog from left-to-right.
 * <h3>Platform Differences</h3>
 * These are based mostly on studying Apple and Vista interface guidelines.
 * <ul><LI> On Mac, command-period acts like the escape key in dialogs.</li>
 * <LI> On Mac the Help component is the standard Mac help icon.  On other platforms
 * the help component is a {@link com.bric.swing.JLink}.</li>
 * <LI> By default button order is reversed on Macs compared to other platforms.  See
 * the <code>DialogFooter.reverseButtonOrder</code> field for details.</li>
 * <LI> There is a static boolean to control whether button mnemonics should be
 * universally activated.  This was added because
 * when studying Windows XP there seemed to be no fixed rules for whether to
 * use mnemonics or not.  (Some dialogs show them, some dialogs don't.)  So I
 * leave it to your discretion to activate them.  I think this boolean should never be
 * activated on Vista or Mac, but on XP and Linux flavors: that's up to you.  (Remember
 * using the alt key usually activates the mnemonics in most Java look-and-feels, so just
 * because they aren't universally active doesn't mean you're hurting accessibility needs.)</LI></ul>
 */
public class DialogFooter extends JPanel {	
	private static final long serialVersionUID = 1L;
	
	public static enum EscapeKeyBehavior {
		/** This (the default behavior) does nothing when the escape key is pressed. */
		DOES_NOTHING, 
		
		/** This triggers the cancel button when the escape key is pressed.  If no 
		 * cancel button is present: this does nothing.
		 * (Also on Macs command+period acts the same as the escape key.)
		 * <p>This should only be used if the cancel button does not lead to data
		 * loss, because users may quickly press the escape key before reading
		 * the text in a dialog.
		 */
		TRIGGERS_CANCEL,
		
		/** This triggers the default button when the escape key is pressed.  If no 
		 * default button is defined: this does nothing.
		 * (Also on Macs command+period acts the same as the escape key.)
		 * <p>This should only be used if the default button does not lead to data
		 * loss, because users may quickly press the escape key before reading
		 * the text in a dialog.
		 */
		TRIGGERS_DEFAULT,
		/** This triggers the non-default button when the escape key is pressed.  If no 
		 * non-default button is defined: this does nothing.
		 * (Also on Macs command+period acts the same as the escape key.)
		 * <p>This should only be used if the non-default button does not lead to data
		 * loss, because users may quickly press the escape key before reading
		 * the text in a dialog.
		 */
		TRIGGERS_NONDEFAULT
	}
	
	private static KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static KeyStroke commandPeriodKey = KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

	/** The localized strings used in dialogs. */
	public static ResourceBundle strings = ResourceBundle.getBundle("com.bric.swing.resources.DialogFooter");
	
	/** This is the client property of buttons created in static methods by this class. */
	public static String PROPERTY_OPTION = "DialogFooter.propertyOption";
	
	private static int uniqueCtr = 0;
	
	/** Used to indicate the user selected "Cancel" in a dialog.
	 * <BR>Also this can be used as a dialog type, to indicate that "Cancel" should
	 * be the only option presented to the user.
	 * <P>Note the usage is similar to JOptionPane's, but the numerical value is
	 * different, so you cannot substitute JOptionPane.CANCEL_OPTION for DialogFooter.CANCEL_OPTION.
	 */
	public static final int CANCEL_OPTION = uniqueCtr++;
	
	/** Used to indicate the user selected "OK" in a dialog.
	 * <BR>Also this can be used as a dialog type, to indicate that "OK" should
	 * be the only option presented to the user.
	 * <P>Note the usage is similar to JOptionPane's, but the numerical value is
	 * different, so you cannot substitute JOptionPane.OK_OPTION for DialogFooter.OK_OPTION.
	 */
	public static final int OK_OPTION = uniqueCtr++;
	
	/** Used to indicate the user selected "No" in a dialog.
	 * <BR>Also this can be used as a dialog type, to indicate that "No" should
	 * be the only option presented to the user.
	 * <P>Note the usage is similar to JOptionPane's, but the numerical value is
	 * different, so you cannot substitute JOptionPane.NO_OPTION for DialogFooter.NO_OPTION.
	 */
	public static final int NO_OPTION = uniqueCtr++;
	
	/** Used to indicate the user selected "Yes" in a dialog.
	 * <BR>Also this can be used as a dialog type, to indicate that "Yes" should
	 * be the only option presented to the user.
	 * <P>Note the usage is similar to JOptionPane's, but the numerical value is
	 * different, so you cannot substitute JOptionPane.YES_OPTION for DialogFooter.YES_OPTION.
	 */
	public static final int YES_OPTION = uniqueCtr++;
	
	/** Used to indicate a dialog should present a "Yes" and "No" option.
	 * <P>Note the usage is similar to JOptionPane's, but the numerical value is
	 * different, so you cannot substitute JOptionPane.YES_NO_OPTION for DialogFooter.YES_NO_OPTION.
	 */
	public static final int YES_NO_OPTION = uniqueCtr++;

	/** Used to indicate a dialog should present a "Yes", "No", and "Cancel" option.
	 * <P>Note the usage is similar to JOptionPane's, but the numerical value is
	 * different, so you cannot substitute JOptionPane.YES_NO_CANCEL_OPTION for DialogFooter.YES_NO_CANCEL_OPTION.
	 */
	public static final int YES_NO_CANCEL_OPTION = uniqueCtr++;

	/** Used to indicate a dialog should present a "OK" and "Cancel" option.
	 * <P>Note the usage is similar to JOptionPane's, but the numerical value is
	 * different, so you cannot substitute JOptionPane.OK_CANCEL_OPTION for DialogFooter.OK_CANCEL_OPTION.
	 */
	public static final int OK_CANCEL_OPTION = uniqueCtr++;

	/** Used to indicate a dialog should present a "Save", "Don't Save", and "Cancel" option.
	 */
	public static final int SAVE_DONT_SAVE_CANCEL_OPTION = uniqueCtr++;

	/** Used to indicate a dialog should present a "Don't Save" and "Save" option.
	 * This will be used for QOptionPaneCommon.FILE_EXTERNAL_CHANGES.
	 */
	public static final int DONT_SAVE_SAVE_OPTION = uniqueCtr++;
	
	/** Used to indicate the user selected "Save" in a dialog.
	 * <BR>Also this can be used as a dialog type, to indicate that "Save"
	 * should be the only option presented to the user.
	 */
	public static final int SAVE_OPTION = uniqueCtr++;

	/** Used to indicate the user selected "Don't Save" in a dialog.
	 * <BR>Also this can be used as a dialog type, to indicate that "Don't Save"
	 * should be the only option presented to the user.
	 */
	public static final int DONT_SAVE_OPTION = uniqueCtr++;

	/** Used to indicate the user selected an option not otherwise
	 * specified in this set of constants.  It may be possible
	 * that the user closed this dialog with the close decoration,
	 * or else another agent dismissed this dialog.
	 * <p>If you use a safely predesigned set of options this
	 * will not be used.
	 */
	public static final int UNDEFINED_OPTION = uniqueCtr++;

    private static AncestorListener escapeTriggerListener = new AncestorListener() {

		public void ancestorAdded(AncestorEvent event) {
			JButton button = (JButton)event.getComponent();
			Window w = SwingUtilities.getWindowAncestor(button);
			if(w instanceof RootPaneContainer) {
				setRootPaneContainer(button, (RootPaneContainer)w);
			} else {
				setRootPaneContainer(button, null);
			}
		}
		
		private void setRootPaneContainer(JButton button,RootPaneContainer c) {
			RootPaneContainer lastContainer = (RootPaneContainer)button.getClientProperty("bric.footer.rpc");
			if(lastContainer==c) return;
			
			if(lastContainer!=null) {
				lastContainer.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(escapeKey);
				lastContainer.getRootPane().getActionMap().remove(escapeKey);
				
				if(JVM.isMac)
					lastContainer.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(commandPeriodKey);

			}

			if(c!=null) {
				c.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escapeKey, escapeKey);
				c.getRootPane().getActionMap().put(escapeKey, new ClickAction(button));
				
				if(JVM.isMac)
					c.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(commandPeriodKey, escapeKey);
			}
			button.putClientProperty("bric.footer.rpc", c);
		}

		public void ancestorMoved(AncestorEvent event) {
			ancestorAdded(event);
		}

		public void ancestorRemoved(AncestorEvent event) {
			ancestorAdded(event);
			
		}
    	
    };
	
    /** Creates a new "Cancel" button.
     * 
     * @param escapeKeyIsTrigger if true then pressing the escape
     * key will trigger this button.  (Also on Macs command-period will act
     * like the escape key.)  This should be <code>false</code> if this button
     * can lead to permanent data loss.
     */
    public static JButton createCancelButton(boolean escapeKeyIsTrigger) {
    	JButton button = new JButton(strings.getString("dialogCancelButton"));
    	button.setMnemonic( strings.getString("dialogCancelMnemonic").charAt(0) );
    	button.putClientProperty(PROPERTY_OPTION, new Integer(CANCEL_OPTION));
    	if(escapeKeyIsTrigger)
    		makeEscapeKeyActivate(button);
    	return button;
    }
    
    /** This guarantees that when the escape key is pressed
     * (if its parent window has the keyboard focus) this button
     * is clicked.
     * <p>It is assumed that no two buttons will try to consume
     * escape keys in the same window.
     * 
     * @param button the button to trigger when the escape key is pressed.
     */
    public static void makeEscapeKeyActivate(AbstractButton button) {
		button.addAncestorListener(escapeTriggerListener);
    }
    
    /** Creates a new "OK" button that is not triggered by the escape key.
     */
    public static JButton createOKButton() {
    	return createOKButton(false);
    }

    /** Creates a new "OK" button.
     * 
     * @param escapeKeyIsTrigger if true then pressing the escape
     * key will trigger this button.  (Also on Macs command-period will act
     * like the escape key.)  This should be <code>false</code> if this button
     * can lead to permanent data loss.
     */
    public static JButton createOKButton(boolean escapeKeyIsTrigger) {
    	JButton button = new JButton(strings.getString("dialogOKButton"));
    	button.setMnemonic( strings.getString("dialogOKMnemonic").charAt(0) );
    	button.putClientProperty(PROPERTY_OPTION, new Integer(OK_OPTION));
    	if(escapeKeyIsTrigger)
    		makeEscapeKeyActivate(button);
    	return button;
    }
    
    /** Creates a new "Yes" button that is not triggered by the escape key.
     */
    public static JButton createYesButton() {
    	return createYesButton(false);
    }
    
    /** Creates a new "Yes" button.
     * 
     * @param escapeKeyIsTrigger if true then pressing the escape
     * key will trigger this button.  (Also on Macs command-period will act
     * like the escape key.)  This should be <code>false</code> if this button
     * can lead to permanent data loss.
     */
    public static JButton createYesButton(boolean escapeKeyIsTrigger) {
    	JButton button = new JButton(strings.getString("dialogYesButton"));
    	button.setMnemonic( strings.getString("dialogYesMnemonic").charAt(0) );
    	button.putClientProperty(PROPERTY_OPTION, new Integer(YES_OPTION));
    	if(escapeKeyIsTrigger)
    		makeEscapeKeyActivate(button);
    	return button;
    }

    /** Creates a new "No" button that is not triggered by the escape key.
     * 
     */
    public static JButton createNoButton() {
    	return createNoButton(false);
    }

    /** Creates a new "No" button.
     * 
     * @param escapeKeyIsTrigger if true then pressing the escape
     * key will trigger this button.  (Also on Macs command-period will act
     * like the escape key.)  This should be <code>false</code> if this button
     * can lead to permanent data loss.
     */
    public static JButton createNoButton(boolean escapeKeyIsTrigger) {
    	JButton button = new JButton(strings.getString("dialogNoButton"));
    	button.setMnemonic( strings.getString("dialogNoMnemonic").charAt(0) );
    	button.putClientProperty(PROPERTY_OPTION, new Integer(NO_OPTION));
    	if(escapeKeyIsTrigger)
    		makeEscapeKeyActivate(button);
    	return button;
    }
    
    /** Creates a new "Save" button that is not triggered by the escape key.
     */
    public static JButton createSaveButton() {
    	return createSaveButton(false);
    }

    /** Creates a new "Save" button.
     * 
     * @param escapeKeyIsTrigger if true then pressing the escape
     * key will trigger this button.  (Also on Macs command-period will act
     * like the escape key.)  This should be <code>false</code> if this button
     * can lead to permanent data loss.
     */
    public static JButton createSaveButton(boolean escapeKeyIsTrigger) {
    	JButton button = new JButton(strings.getString("dialogSaveButton"));
    	button.setMnemonic( strings.getString("dialogSaveMnemonic").charAt(0) );
    	button.putClientProperty(PROPERTY_OPTION, new Integer(SAVE_OPTION));
    	if(escapeKeyIsTrigger)
    		makeEscapeKeyActivate(button);
    	return button;
    }
    
    /** Creates a new "Don't Save" button that is not triggered by the escape key.
     */
    public static JButton createDontSaveButton() {
    	return createDontSaveButton(false);
    }

    /** Creates a new "Don't Save" button.
     * 
     * @param escapeKeyIsTrigger if true then pressing the escape
     * key will trigger this button.  (Also on Macs command-period will act
     * like the escape key.)  This should be <code>false</code> if this button
     * can lead to permanent data loss.
     */
    public static JButton createDontSaveButton(boolean escapeKeyIsTrigger) {
    	String text = strings.getString("dialogDontSaveButton");
    	JButton button = new JButton(text);
    	button.setMnemonic( strings.getString("dialogDontSaveMnemonic").charAt(0) );
    	button.putClientProperty(PROPERTY_OPTION, new Integer(DONT_SAVE_OPTION));
    	//Don't know if this documented by Apple, but command-D usually triggers "Don't Save" buttons:
    	button.putClientProperty(DialogFooter.PROPERTY_META_SHORTCUT,new Character(text.charAt(0)));
    	if(escapeKeyIsTrigger)
    		makeEscapeKeyActivate(button);
    	return button;
    }
    

    /** Creates a <code>DialogFooter</code> and assigns a default button.
     * The default button is the first button listed in the button type.  For example,
     * a YES_NO_CANCEL_OPTION dialog will make the YES_OPTION the default button.
     * 
     * @param options one of the OPTIONS fields in this class, such as YES_NO_OPTION or CANCEL_OPTION.
     * @param escapeKeyBehavior one of the EscapeKeyBehavior options in this class.
     * @return a <code>DialogFooter</code>
     */
    public static DialogFooter createDialogFooter(int options,EscapeKeyBehavior escapeKeyBehavior) {
    	return createDialogFooter(new JComponent[] {}, options, escapeKeyBehavior);
    }
    
    /** Creates a <code>DialogFooter</code> and assigns a default button.
     * The default button is the first button listed in the button type.  For example,
     * a YES_NO_CANCEL_OPTION dialog will make the YES_OPTION the default button.
     * <P>To use a different default button, use the other <code>createDialogFooter()</code> method.
     * 
     * @param leftComponents the components to put on the left side of the footer.
     * <P>The Apple guidelines state that this area is reserved for
     * "button[s] that affect the contents of the dialog itself, such as Reset [or Help]".
     * @param options one of the OPTIONS fields in this class, such as YES_NO_OPTION or CANCEL_OPTION.
     * @param escapeKeyBehavior one of the EscapeKeyBehavior options in this class.
     * @return a <code>DialogFooter</code>
     */
    public static DialogFooter createDialogFooter(JComponent[] leftComponents,int options,EscapeKeyBehavior escapeKeyBehavior) {
    	if(options==CANCEL_OPTION) {
    		return createDialogFooter(leftComponents,options,CANCEL_OPTION,escapeKeyBehavior);
    	} else if(options==DONT_SAVE_OPTION) {
    		return createDialogFooter(leftComponents,options,DONT_SAVE_OPTION,escapeKeyBehavior);
    	} else if(options==NO_OPTION) {
    		return createDialogFooter(leftComponents,options,NO_OPTION,escapeKeyBehavior);
    	} else if(options==OK_CANCEL_OPTION) {
    		return createDialogFooter(leftComponents,options,OK_OPTION,escapeKeyBehavior);
    	} else if(options==OK_OPTION) {
    		return createDialogFooter(leftComponents,options,OK_OPTION,escapeKeyBehavior);
    	} else if(options==SAVE_DONT_SAVE_CANCEL_OPTION) {
    		return createDialogFooter(leftComponents,options,SAVE_OPTION,escapeKeyBehavior);
    	} else if(options==DONT_SAVE_SAVE_OPTION) {
    		return createDialogFooter(leftComponents,options,DONT_SAVE_OPTION,escapeKeyBehavior);
    	} else if(options==SAVE_OPTION) {
    		return createDialogFooter(leftComponents,options,SAVE_OPTION,escapeKeyBehavior);
    	} else if(options==YES_NO_CANCEL_OPTION) {
    		return createDialogFooter(leftComponents,options,YES_OPTION,escapeKeyBehavior);
    	} else if(options==YES_NO_OPTION) {
    		return createDialogFooter(leftComponents,options,YES_OPTION,escapeKeyBehavior);
    	} else if(options==YES_OPTION) {
    		return createDialogFooter(leftComponents,options,YES_OPTION,escapeKeyBehavior);
    	}
    	throw new IllegalArgumentException("unrecognized option type ("+options+")");
    }
    

    /** Creates a <code>DialogFooter</code>.
     * @param leftComponents the components to put on the left side of the footer.
     * <P>The Apple guidelines state that this area is reserved for
     * "button[s] that affect the contents of the dialog itself, such as Reset [or Help]".
     * @param options one of the OPTIONS fields in this class, such as YES_NO_OPTION or CANCEL_OPTION.
     * @param defaultButton the OPTION field corresponding to the button that
     * should be the default button, or -1 if there should be no default button.
     * @param escapeKeyBehavior one of the EscapeKeyBehavior options in this class.
     * @return a <code>DialogFooter</code>
     */
    public static DialogFooter createDialogFooter(JComponent[] leftComponents,int options,int defaultButton,EscapeKeyBehavior escapeKeyBehavior) {
    	JButton[] dismissControls;
    	JButton cancelButton = null;
    	JButton dontSaveButton = null;
    	JButton noButton = null;
    	JButton okButton = null;
    	JButton saveButton = null;
    	JButton yesButton = null;
    	
    	if(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_NONDEFAULT) {
    		int buttonCount = 1;
    		if(options==OK_CANCEL_OPTION || options==YES_NO_OPTION || options==DONT_SAVE_SAVE_OPTION) {
    			buttonCount = 2;
    		} else if(options==SAVE_DONT_SAVE_CANCEL_OPTION || options==YES_NO_CANCEL_OPTION) {
    			buttonCount = 3;
    		}
    		if(defaultButton!=-1) {
    			buttonCount--;
    		}
    		if(buttonCount>1) {
    			throw new IllegalArgumentException("request for escape key to map to "+buttonCount+" buttons.");
    		}
    	}
    	
    	if(options==CANCEL_OPTION || 
    			options==OK_CANCEL_OPTION || 
    			options==SAVE_DONT_SAVE_CANCEL_OPTION ||
    			options==YES_NO_CANCEL_OPTION) {
    		cancelButton = createCancelButton( escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_CANCEL ||
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_NONDEFAULT && defaultButton!=CANCEL_OPTION) ||
					(defaultButton==CANCEL_OPTION && 
							escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_DEFAULT) );
    	}
    	if(options==DONT_SAVE_OPTION || options==SAVE_DONT_SAVE_CANCEL_OPTION || options==DONT_SAVE_SAVE_OPTION) {
    		dontSaveButton = createDontSaveButton(
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_NONDEFAULT && defaultButton!=DONT_SAVE_OPTION) ||
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_DEFAULT && defaultButton==DONT_SAVE_OPTION ));
    	}
    	if(options==NO_OPTION || options==YES_NO_OPTION || options==YES_NO_CANCEL_OPTION) {
    		noButton = createNoButton(
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_NONDEFAULT && defaultButton!=NO_OPTION) ||
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_DEFAULT && defaultButton==NO_OPTION ));
    	}
    	if(options==OK_OPTION || 
    			options==OK_CANCEL_OPTION) {
    		okButton = createOKButton(
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_NONDEFAULT && defaultButton!=OK_OPTION) ||
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_DEFAULT && defaultButton==OK_OPTION ));
    	}
    	if(options==SAVE_OPTION || options==SAVE_DONT_SAVE_CANCEL_OPTION || options==DONT_SAVE_SAVE_OPTION) {
    		saveButton = createSaveButton(
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_NONDEFAULT && defaultButton!=SAVE_OPTION) ||
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_DEFAULT && defaultButton==SAVE_OPTION ));
    	}
    	if(options==YES_OPTION || options==YES_NO_OPTION || options==YES_NO_CANCEL_OPTION) {
    		yesButton = createYesButton(
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_NONDEFAULT && defaultButton!=YES_OPTION) ||
    				(escapeKeyBehavior==EscapeKeyBehavior.TRIGGERS_DEFAULT && defaultButton==YES_OPTION ));
    	}
    	
    	if(options==CANCEL_OPTION) {
    		dismissControls = new JButton[] { cancelButton };
    	} else if(options==DONT_SAVE_OPTION) {
    		dismissControls = new JButton[] { dontSaveButton };
    	} else if(options==NO_OPTION) {
    		dismissControls = new JButton[] { noButton };
    	} else if(options==OK_CANCEL_OPTION) {
    		dismissControls = new JButton[] { okButton, cancelButton };
    	} else if(options==OK_OPTION) {
    		dismissControls = new JButton[] { okButton };
    	} else if(options==DONT_SAVE_SAVE_OPTION) {
    		dismissControls = new JButton[] { dontSaveButton, saveButton };
    	} else if(options==SAVE_DONT_SAVE_CANCEL_OPTION) {
    		setUnsafe(dontSaveButton, true);
    		dismissControls = new JButton[] { saveButton, dontSaveButton, cancelButton };
    	} else if(options==SAVE_OPTION) {
    		dismissControls = new JButton[] { saveButton };
    	} else if(options==YES_NO_CANCEL_OPTION) {
    		dismissControls = new JButton[] { yesButton, noButton, cancelButton };
    	} else if(options==YES_NO_OPTION) {
    		dismissControls = new JButton[] { yesButton, noButton };
    	} else if(options==YES_OPTION) {
    		dismissControls = new JButton[] { yesButton };
    	} else {
    		throw new IllegalArgumentException("Unrecognized dialog type.");
    	}
    	

    	JButton theDefaultButton = null;
    	for(int a = 0; a<dismissControls.length; a++) {
    		int i = ((Integer)dismissControls[a].getClientProperty(PROPERTY_OPTION)).intValue();
    		if(i==defaultButton)
    			theDefaultButton = dismissControls[a];
    	}
    	
    	DialogFooter footer = new DialogFooter(leftComponents,dismissControls,true,theDefaultButton);
    	return footer;
    }

	/** This action calls <code>button.doClick()</code>. */
	public static class ClickAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		JButton button;
		
		public ClickAction(JButton button) {
			this.button = button;
		}
		public void actionPerformed(ActionEvent e) {
			button.doClick();
		}
	}
	
	/** This client property is used to impose a meta-shortcut to click a button.
	 * This should map to a Character.
	 */
	public static final String PROPERTY_META_SHORTCUT = "Dialog.meta.shortcut";
	
	/** This client property is used to indicate a button is "unsafe".  Apple
	 * guidelines state that "unsafe" buttons (such as "discard changes") should
	 * be several pixels away from "safe" buttons.
	 */
	public static final String PROPERTY_UNSAFE = "Dialog.Unsafe.Action";
	
	/** This indicates whether the dismiss controls should be displayed in reverse
	 * order.  When you construct a DialogFooter, the dismiss controls should be listed
	 * in order of priority (with the most preferred listed first, the least preferred last).
	 * If this boolean is false, then those components will be listed in that order.  If this is
	 * true, then those components will be listed in the reverse order.
	 * <P>By default on Mac this is true, because Macs put the default button on the right
	 * side of dialogs.  On all other platforms this is false by default.
	 * <P>Window's <A HREF="http://msdn.microsoft.com/en-us/library/ms997497.aspx">guidelines</A>
	 * advise to, "Position the most important button -- typically the default command --
	 * as the first button in the set."
	 */
	public static boolean reverseButtonOrder = JVM.isMac;
	
	protected JComponent[] leftControls;
	protected JComponent[] dismissControls;
	protected JComponent lastSelectedComponent;
	protected boolean autoClose = false;
	protected JButton defaultButton = null;
	int buttonWidthPadding, buttonHeightPadding, buttonGap, unsafeButtonGap;
	boolean fillWidth = false;
	
	private final ActionListener innerActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			lastSelectedComponent = (JComponent)e.getSource();
			fireActionListeners(e);

			if(autoClose)
				closeDialogAndDisposeAction.actionPerformed(e);
		}
	};
	
	/** Clones an array of JComponents */
	private static JComponent[] copy(JComponent[] c) {
		JComponent[] newArray = new JComponent[c.length];
		for(int a = 0; a<c.length; a++) {
			newArray[a] = c[a];
		}
		return newArray;
	}

	/** This addresses code that must involve the parent RootPane and Window. */
	private final HierarchyListener hierarchyListener = new HierarchyListener() {
		public void hierarchyChanged(HierarchyEvent e)
		{
			processRootPane();
			processWindow();
		}
		private void processRootPane() {
			JRootPane root = SwingUtilities.getRootPane(DialogFooter.this);
			if(root==null) return;
			root.setDefaultButton(defaultButton);

			
			for(int a = 0; a<dismissControls.length; a++) {
				if(dismissControls[a] instanceof JButton) {
					Character ch = (Character)dismissControls[a].getClientProperty(PROPERTY_META_SHORTCUT);
					if(ch!=null) {
						KeyStroke keyStroke = KeyStroke.getKeyStroke(ch.charValue(), Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
						root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put( keyStroke, keyStroke);
						root.getActionMap().put(keyStroke, new ClickAction((JButton)dismissControls[a]));
					}
				}
			}
		} 
		private void processWindow() {
			Window window = SwingUtilities.getWindowAncestor(DialogFooter.this);
			if(window==null) return;
			
			window.setFocusTraversalPolicy(new DelegateFocusTraversalPolicy(window.getFocusTraversalPolicy()) {
				private static final long serialVersionUID = 1L;
				
				@Override
				public Component getDefaultComponent(Container focusCycleRoot) {
					/** If the default component would naturally be in the footer *anyway*:
					 * Make sure the default component is the default button.
					 * 
					 * However if the default component lies elsewhere (a text field or
					 * check box in the dialog): that should retain the default focus.
					 * 
					 */
					Component defaultComponent = super.getDefaultComponent(focusCycleRoot);
					if(DialogFooter.this.isAncestorOf(defaultComponent)) {
						JButton button = DialogFooter.this.defaultButton;
						if(button!=null && button.isShowing() && button.isEnabled() && button.isFocusable())
							return button;
					}
					return defaultComponent;
				}
			} );
		}
	};
	
	/** Create a new <code>DialogFooter</code>.
	 * 
	 * @param leftControls the controls on the left side of this dialog, such as a help component, or a "Reset" button.
	 * @param dismissControls the controls on the right side of this dialog that should dismiss this dialog.  Also
	 * called "action" buttons.
	 * @param autoClose whether the dismiss buttons should automatically close the containing window.
	 * If this is <code>false</code>, then it is assumed someone else is taking care of closing/disposing the
	 * containing dialog
	 * @param defaultButton the optional button in <code>dismissControls</code> to make the default button in this dialog.
	 * (May be null.)
	 */
	public DialogFooter(JComponent[] leftControls,JComponent[] dismissControls,boolean autoClose,JButton defaultButton) {
		super(new GridBagLayout());
		this.autoClose = autoClose;
		//this may be common:
		if(leftControls==null) leftControls = new JComponent[] {};
		//erg, this shouldn't be, but let's not throw an error because of it?
		if(dismissControls==null) dismissControls = new JComponent[] {};
		this.leftControls = copy(leftControls);
		this.dismissControls = copy(dismissControls);
		this.defaultButton = defaultButton;
		
		for(int a = 0; a<dismissControls.length; a++) {
			dismissControls[a].putClientProperty("dialog.footer.index", new Integer(a));
			if(dismissControls[a] instanceof JButton) {
				((JButton)dismissControls[a]).addActionListener(innerActionListener);
			} else {
				//think of things like the JLink: it a label, but it has an ActionListener model
				try {
					Class<?> cl = dismissControls[a].getClass();
					Method m = cl.getMethod("addActionListener", new Class[] {ActionListener.class});
					m.invoke(dismissControls[a], new Object[] {innerActionListener});
				} catch(Throwable t) {
					//do nothing
				}
			}
		}
		
		addHierarchyListener(hierarchyListener);
		
		for(int a = 0; a<leftControls.length; a++) {
			addFocusArrowListener(leftControls[a]);
		}
		for(int a = 0; a<dismissControls.length; a++) {
			addFocusArrowListener(dismissControls[a]);
		}
		
		if(JVM.isMac) {
			setButtonGap(12);
		} else if(JVM.isVista) {
			setButtonGap(8);
		} else {
			setButtonGap(6);
		}
		setUnsafeButtonGap(24);
		
		installGUI();
	}
	
	public void setInternalButtonPadding(int widthPadding,int heightPadding) {
		if(buttonWidthPadding==widthPadding && buttonHeightPadding==heightPadding) {
			return;
		}
		buttonWidthPadding = widthPadding;
		buttonHeightPadding = heightPadding;
		installGUI();
	}
	
	public void setButtonGap(int gap) {
		if(buttonGap==gap)
			return;
		buttonGap = gap;
		installGUI();
	}
	
	public void setFillWidth(boolean b) {
		if(fillWidth==b)
			return;
		
		this.fillWidth = b;
		installGUI();
	}
	
	public void setUnsafeButtonGap(int unsafeGap) {
		if(unsafeButtonGap==unsafeGap)
			return;
		unsafeButtonGap = unsafeGap;
		installGUI();
	}
	
	private void installGUI() {
		removeAll();
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 0;
		c.weightx = 0; c.weighty = 1;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.CENTER;
		for(int a = 0; a<leftControls.length; a++) {
			add(leftControls[a],c);
			c.gridx++;
			c.insets = new Insets(0,0,0,buttonGap);
		}
		c.weightx = 1;
		c.insets = new Insets(0,0,0,0);
		JPanel fluff = new JPanel();
		fluff.setOpaque(false);
		
		if(leftControls.length>0) {
			add(fluff,c); //fluff to enforce the left and right sides
			c.gridx++;
		}
		c.weightx = 0;
		int unsafeCtr = 0;
		int safeCtr = 0;
		for(int a = 0; a<dismissControls.length; a++) {
			if(JVM.isMac && isUnsafe(dismissControls[a])) {
				unsafeCtr++;
			} else {
				safeCtr++;
			}
		}
		JButton[] unsafeButtons = new JButton[unsafeCtr];
		JButton[] safeButtons = new JButton[safeCtr];
		unsafeCtr = 0;
		safeCtr = 0;
		for(int a = 0; a<dismissControls.length; a++) {
			if(dismissControls[a] instanceof JButton) {
				if(JVM.isMac && isUnsafe(dismissControls[a])) {
					unsafeButtons[unsafeCtr++] = (JButton)dismissControls[a];
				} else {
					safeButtons[safeCtr++] = (JButton)dismissControls[a];
				}
			}
		}
		
		c.ipadx = buttonWidthPadding;
		c.ipady = buttonHeightPadding;
		c.insets = new Insets(0,0,0,0);
		for(int a = 0; a<unsafeButtons.length; a++) {
			JComponent comp = reverseButtonOrder ?
					unsafeButtons[unsafeButtons.length-1-a] :
					unsafeButtons[a];
			add(comp, c);
			c.gridx++;
			c.insets.left = buttonGap;
		}
		if(unsafeButtons.length>0) {
			c.insets.left = unsafeButtonGap;
			if(fillWidth && leftControls.length==0) {
				c.weightx = 1;
				add(fluff, c);
				c.weightx = 0;
				c.gridx++;
			}
		} else if(leftControls.length==0) {
			c.weightx = 1;
			add(fluff, c);
			c.weightx = 0;
			c.gridx++;
		}
		
		for(int a = 0; a<safeButtons.length; a++) {
			JComponent comp = reverseButtonOrder ?
					safeButtons[safeButtons.length-1-a] :
					safeButtons[a];
					
			add(comp, c);
			c.gridx++;
			c.insets.left = buttonGap;
		}
				
		normalizeButtons(unsafeButtons);
		normalizeButtons(safeButtons);
	}

	private static void addFocusArrowListener(JComponent jc) {
		/** Check to see if someone already added this kind of listener:
		 */
		KeyListener[] listeners = jc.getKeyListeners();
		for(int a = 0; a<listeners.length; a++) {
			if(listeners[a] instanceof FocusArrowListener)
				return;
		}
		//Add our own:
		jc.addKeyListener(new FocusArrowListener());
	}
	
	/** This takes a set of buttons and gives them all the width/height
	 * of the largest button among them.
	 * <P>(More specifically, this sets the <code>preferredSize</code>
	 * of each button to the largest preferred size in the list of buttons.
	 * 
	 * @param buttons an array of buttons.
	 */
	public static void normalizeButtons(JButton[] buttons) {
		int maxWidth = 0;
		int maxHeight = 0;
		for(int a = 0; a<buttons.length; a++) {
			buttons[a].setPreferredSize(null);
			Dimension d = buttons[a].getPreferredSize();
			Number n = (Number)buttons[a].getClientProperty( DialogFooter.PROPERTY_OPTION );
			if( (n!=null && n.intValue()==DialogFooter.DONT_SAVE_OPTION) ||
					d.width>80 ) {
				buttons[a] = null;
			}
			if(buttons[a]!=null) {
				maxWidth = Math.max(d.width, maxWidth);
				maxHeight = Math.max(d.height, maxHeight);
			}
		}
		for(int a = 0; a<buttons.length; a++) {
			if(buttons[a]!=null)
				buttons[a].setPreferredSize(new Dimension(maxWidth,maxHeight));
		}
	}

	/** This indicates that an action button risks losing user's data.
	 * On Macs an unsafe button is spaced farther away from safe buttons.
	 */
	public static boolean isUnsafe(JComponent c) {
		Boolean b = (Boolean)c.getClientProperty(PROPERTY_UNSAFE);
		if(b==null) b = Boolean.FALSE;
		return b.booleanValue();
	}
	
	/** This sets the unsafe flag for buttons.
	 */
	public static void setUnsafe(JComponent c,boolean b) {
		c.putClientProperty(PROPERTY_UNSAFE, new Boolean(b));
	}
	
	private Vector<ActionListener> listeners;
	
	/** Adds an <code>ActionListener</code>.
	 * 
	 * @param l this listener will be notified when a <code>dismissControl</code> is activated.
	 */
	public void addActionListener(ActionListener l) {
		if(listeners==null) listeners = new Vector<ActionListener>();
		if(listeners.contains(l))
			return;
		listeners.add(l);
	}
	
	/** Removes an <code>ActionListener</code>.
	 */
	public void removeActionListener(ActionListener l) {
		if(listeners==null) return;
		listeners.remove(l);
	}
	
	private void fireActionListeners(ActionEvent e) {
		if(listeners==null) return;
		for(int a = 0; a<listeners.size(); a++) {
			ActionListener l = listeners.get(a);
			try {
				l.actionPerformed(e);
			} catch(Exception e2) {
				e2.printStackTrace();
			}
		}
	}
	
	/** Returns the component last used to dismiss the dialog.
	 * <P>Note the components on the left side of this footer
	 * (such as a "Help" button or a "Reset Preferences" button)
	 * do NOT dismiss the dialog, and so this method has nothing
	 * to do with those components.  This only relates to the components on the
	 * right side of dialog.
	 * @return the component last used to dismiss the dialog.
	 */
	public JComponent getLastSelectedComponent() {
		return lastSelectedComponent;
	}
	
	/** Finds a certain type of button, if it is available.
	 * 
	 * @param buttonType of the options in this class (such as YES_OPTION or CANCEL_OPTION)
	 * @return the button that maps to that option, or null if no such button was found.
	 */
	public JButton getButton(int buttonType) {
		for(int a = 0; a<getComponentCount(); a++) {
			if(getComponent(a) instanceof JButton) {
				JButton button = (JButton)getComponent(a);
				Object value = button.getClientProperty(PROPERTY_OPTION);
				int intValue = -1;
				if(value instanceof Number)
					intValue = ((Number)value).intValue();
				if(intValue==buttonType)
					return button;
			}
		}
		return null;
	}

	/** Returns true if a certain button type is available in this footer.
	 * 
	 * @param buttonType of the options in this class (such as YES_OPTION or CANCEL_OPTION)
	 * @return true if a button corresponding to the option provided exists.
	 */
	public boolean containsButton(int buttonType) {
		return getButton(buttonType)!=null;
	}
	
	/** This resets the value of <code>lastSelectedComponent</code> to null.
	 * <P>If this footer is recycled in different dialogs, then you may
	 * need to nullify this value for <code>getLastSelectedComponent()</code>
	 * to remain relevant.
	 */
	public void reset() {
		lastSelectedComponent = null;
	}
	
	/** Returns a copy of the <code>dismissControls</code> array used to construct this footer. */
	public JComponent[] getDismissControls() {
		return copy(dismissControls);
	}
	
	public void setAutoClose(boolean b) {
		autoClose = b;
	}

	/** Returns a copy of the <code>leftControls</code> array used to construct this footer. */
	public JComponent[] getLeftControls() {
		return copy(leftControls);
	}
	
	/** This action takes the Window associated with the source of this event,
	 * hides it, and then calls <code>dispose()</code> on it.
	 * <P>(This will not throw an exception if there is no parent window,
	 * but it does nothing in that case...)
	 */
	public static Action closeDialogAndDisposeAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			Component src = (Component)e.getSource();
			Container parent = src.getParent();
			while(parent!=null) {
				if( parent instanceof JInternalFrame ) {
					((JInternalFrame)parent).setVisible(false);
					((JInternalFrame)parent).dispose();
					return;
				} else if(parent instanceof Window) {
					((Window)parent).setVisible(false);
					((Window)parent).dispose();
					return;
				}
				parent = parent.getParent();
			}
		}
	};
}
