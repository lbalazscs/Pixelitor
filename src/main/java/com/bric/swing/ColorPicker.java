/*
 * @(#)ColorPicker.java
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

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.bric.plaf.ColorPickerSliderUI;

/** <p>This is a panel that offers a robust set of controls to pick a color.
 * <P>This was originally intended to replace the <code>JColorChooser</code>.
 * To use this class to create a color choosing dialog, simply call:
 * <BR><code>ColorPicker.showDialog(frame, originalColor);</code>
 * <P>Here is a screenshot of the dialog that call will invoke:
 * <br><IMG SRC="https://javagraphics.java.net/resources/colorpicker.png" alt="ColorPicker Screenshot">
 * <p>However this does not have to invoked as a black-box color dialog. This class
 * is simply a panel, and you can customize and resize it for other looks.
 * For example, you might try the following panel:</p>
 * <BR><code>ColorPicker picker = new ColorPicker(false, false);</code>
 * <BR><code>picker.setPreferredSize(new Dimension(200,160));</code>
 * <BR><code>picker.setMode(ColorPicker.HUE);</code>
 * <br><IMG SRC="https://javagraphics.java.net/resources/colorpicker3.png" alt="ColorPicker Small Screenshot">
 * <P>This will create a miniature color picker that still lets the user choose
 * from every available color, but it does not include all the buttons and
 * numeric controls on the right side of the panel. This might be ideal if you
 * are working with limited space, or non-power-users who don't need the
 * RGB values of a color.
 * <P>To listen to color changes to this panel, you can add a <code>PropertyChangeListener</code>
 * listening for changes to the <code>SELECTED_COLOR_PROPERTY</code>. This will be triggered only
 * when the RGB value of the selected color changes.
 * <P>To listen to opacity changes to this panel, use 
 * a <code>PropertyChangeListener</code> listening
 * for changes to the <code>OPACITY_PROPERTY</code>.
 * 
 * @see com.bric.swing.ColorPickerDialog
 * @see com.bric.swing.ColorPickerPanel
 * 
 */
public class ColorPicker extends JPanel {
	private static final long serialVersionUID = 3L;
	
	/** The localized strings used in this (and related) panel(s). */
	protected static ResourceBundle strings = ResourceBundle.getBundle("com.bric.swing.resources.ColorPicker");
	
	/** This creates a modal dialog prompting the user to select a color.
	 * <P>This uses a generic dialog title: "Choose a Color", and does not include opacity.
	 * 
	 * @param owner the dialog this new dialog belongs to.  This must be a Frame or a Dialog.
	 * Java 1.6 supports Windows here, but this package is designed/compiled to work in Java 1.4,
	 * so an <code>IllegalArgumentException</code> will be thrown if this component is a <code>Window</code>.
	 * @param originalColor the color the <code>ColorPicker</code> initially points to.
	 * @return the <code>Color</code> the user chooses, or <code>null</code> if the user cancels the dialog.
	 */
	public static Color showDialog(Window owner,Color originalColor) {
		return showDialog(owner, null, originalColor, false );
	}
	
	/** This creates a modal dialog prompting the user to select a color.
	 * <P>This uses a generic dialog title: "Choose a Color".
	 * 
	 * @param owner the dialog this new dialog belongs to.  This must be a Frame or a Dialog.
	 * Java 1.6 supports Windows here, but this package is designed/compiled to work in Java 1.4,
	 * so an <code>IllegalArgumentException</code> will be thrown if this component is a <code>Window</code>.
	 * @param originalColor the color the <code>ColorPicker</code> initially points to.
	 * @param includeOpacity whether to add a control for the opacity of the color.
	 * @return the <code>Color</code> the user chooses, or <code>null</code> if the user cancels the dialog.
	 */
	public static Color showDialog(Window owner,Color originalColor,boolean includeOpacity) {
		return showDialog(owner, null, originalColor, includeOpacity );
	}

	/** This creates a modal dialog prompting the user to select a color.
	 * 
	 * @param owner the dialog this new dialog belongs to.  This must be a Frame or a Dialog.
	 * Java 1.6 supports Windows here, but this package is designed/compiled to work in Java 1.4,
	 * so an <code>IllegalArgumentException</code> will be thrown if this component is a <code>Window</code>.
	 * @param title the title for the dialog.
	 * @param originalColor the color the <code>ColorPicker</code> initially points to.
	 * @param includeOpacity whether to add a control for the opacity of the color.
	 * @return the <code>Color</code> the user chooses, or <code>null</code> if the user cancels the dialog.
	 */
	public static Color showDialog(Window owner, String title,Color originalColor,boolean includeOpacity) {
		ColorPickerDialog d;
		if(owner instanceof Frame || owner==null) {
			d = new ColorPickerDialog( (Frame)owner, originalColor, includeOpacity);
		} else if(owner instanceof Dialog){
			d = new ColorPickerDialog( (Dialog)owner, originalColor, includeOpacity);
		} else {
			throw new IllegalArgumentException("the owner ("+owner.getClass().getName()+") must be a java.awt.Frame or a java.awt.Dialog");
		}
		
		d.setTitle(title == null ? 
                    strings.getObject("ColorPickerDialogTitle").toString() : 
                    title);
		d.pack();
		d.setVisible(true);
		return d.getColor();
	}

	/** <code>PropertyChangeEvents</code> will be triggered for this property when the selected color
	 * changes.
	 * <P>(Events are only created when then RGB values of the color change.  This means, for example,
	 * that the change from HSB(0,0,0) to HSB(.4,0,0) will <i>not</i> generate events, because when the
	 * brightness stays zero the RGB color remains (0,0,0).  So although the hue moved around, the color
	 * is still black, so no events are created.)
	 * 
	 */
	public static final String SELECTED_COLOR_PROPERTY = "selected color";

	/** <code>PropertyChangeEvents</code> will be triggered for this property when <code>setModeControlsVisible()</code>
	 * is called.
	 */
	public static final String MODE_CONTROLS_VISIBLE_PROPERTY = "mode controls visible";
	
	/** <code>PropertyChangeEvents</code> will be triggered when the opacity value is
	 * adjusted.
	 */
	public static final String OPACITY_PROPERTY = "opacity";
	
	/** <code>PropertyChangeEvents</code> will be triggered when the mode changes.
	 * (That is, when the wheel switches from HUE, SAT, BRI, RED, GREEN, or BLUE modes.)
	 */
	public static final String MODE_PROPERTY = "mode";
	
	/** Used to indicate when we're in "hue mode". */
	public static final int HUE = 0;
	/** Used to indicate when we're in "brightness mode". */
	public static final int BRI = 1;
	/** Used to indicate when we're in "saturation mode". */
	public static final int SAT = 2;
	/** Used to indicate when we're in "red mode". */
	public static final int RED = 3;
	/** Used to indicate when we're in "green mode". */
	public static final int GREEN = 4;
	/** Used to indicate when we're in "blue mode". */
	public static final int BLUE = 5;
	
	/** The vertical slider */
	private JSlider slider = new JSlider(JSlider.VERTICAL,0,100,0);
	
	private int currentRed = 0;
	private int currentGreen = 0;
	private int currentBlue = 0;
	
	ChangeListener changeListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			Object src = e.getSource();

			if(hue.contains(src) || sat.contains(src) || bri.contains(src)) {
				if(adjustingSpinners>0)
					return;
				
				setHSB( hue.getFloatValue()/360f,
						sat.getFloatValue()/100f,
						bri.getFloatValue()/100f );
			} else if(red.contains(src) || green.contains(src) || blue.contains(src)) {
				if(adjustingSpinners>0)
					return;
				
				setRGB( red.getIntValue(),
						green.getIntValue(),
						blue.getIntValue() );
			} else if(src==colorPanel) {
				if(adjustingColorPanel>0)
					return;
				
				int mode = getMode();
				if(mode==HUE || mode==BRI || mode==SAT) {
					float[] hsb = colorPanel.getHSB();
					setHSB(hsb[0],hsb[1],hsb[2]);
				} else {
					int[] rgb = colorPanel.getRGB();
					setRGB(rgb[0],rgb[1],rgb[2]);
				}
			} else if(src==slider) {
				if(adjustingSlider>0)
					return;
				
				int v = slider.getValue();
				Option option = getSelectedOption();
				option.setValue(v);
			} else if(alpha.contains(src)) {
				if(adjustingOpacity>0)
					return;
				int v = alpha.getIntValue();
				setOpacity( v );
			} else if(src==opacitySlider) {
				if(adjustingOpacity>0) return;
				
				setOpacity( opacitySlider.getValue() );
			}
		}
	};
	
	ActionListener actionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if(src==hue.radioButton) {
				setMode(HUE);
			} else if(src==bri.radioButton) {
				setMode(BRI);
			} else if(src==sat.radioButton) {
				setMode(SAT);
			} else if(src==red.radioButton) {
				setMode(RED);
			} else if(src==green.radioButton) {
				setMode(GREEN);
			} else if(src==blue.radioButton) {
				setMode(BLUE);
			}
		}
	};
	
	/** @return the currently selected <code>Option</code>
	 */
	private Option getSelectedOption() {
		int mode = getMode();
		if(mode==HUE) {
			return hue;
		} else if(mode==SAT) {
			return sat;
		} else if(mode==BRI) {
			return bri;
		} else if(mode==RED) {
			return red;
		} else if(mode==GREEN) {
			return green;
		} else {
			return blue;
		}
	}

	HexDocumentListener hexDocListener = new HexDocumentListener();

	class SetRGBRunnable implements Runnable {
		final int red, green, blue;
		
		SetRGBRunnable(int red,int green,int blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}
		
		public void run() {
			int pos = hexField.getCaretPosition();
			setRGB(red, green, blue);
			pos = Math.min(pos, hexField.getText().length());
			hexField.setCaretPosition(pos);
		}
	}
	
	class HexDocumentListener implements DocumentListener {

		/* The delay (in ms) to commit text that might not be finished.
		 * 
		 */
		int DELAY = 1500;
		String uncommittedText = null;
		
		Timer delayedUpdater = new Timer(DELAY, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(uncommittedText!=null) {
					int pos = hexField.getCaretPosition();
					pos = Math.min(pos, uncommittedText.length());
					hexField.setText(uncommittedText);
					hexField.setCaretPosition(pos);
				}
			}
		});
		
		public void changedUpdate(DocumentEvent e) {
			if(adjustingHexField>0)
				return;
			
			String s = hexField.getText();
			s = stripToHex(s, 6);
			
			/* If we don't have 6 characters, then use a delay.
			 * If, after a second or two, the user has just
			 * stopped typing: then we can try to make
			 * sense of what they input even if its
			 * incomplete.
			 */
			boolean delay = false;
			if(s.length()<6) {
				delay = true;
				while(s.length()<6) {
					s = s+"0";
				}
			}
			
			try {
				int i = Integer.parseInt(s,16);
				int red = ((i >> 16) & 0xff);
				int green = ((i >> 8) & 0xff);
				int blue = ((i) & 0xff);
				
				if(delay) {
					delayedUpdater.setRepeats(false);
					delayedUpdater.restart();
					uncommittedText = s;
				} else {
					delayedUpdater.stop();
					
					/* Be sure to invoke this separately, otherwise we'll risk getting
					 * a "attempt to mutate in notification".
					 * ( https://java.net/jira/browse/JAVAGRAPHICS-19 )
					 */
					SwingUtilities.invokeLater(new SetRGBRunnable(red, green, blue));
					uncommittedText = null;
				}
				return;
			} catch(NumberFormatException e2) {
				//this shouldn't happen, since we already stripped out non-hex characters.
				e2.printStackTrace();
			}
		}
		
		/** Strips a string down to only uppercase hex-supported characters.
		 * @param s the string to strip
		 * @param charLimit the maximum number of characters in the return value
		 * @return an uppercase version of <code>s</code> that only includes hexadecimal
		 * characters and is not longer than <code>charLimit</code>. 
		 */
		private String stripToHex(String s,int charLimit) {
			s = s.toUpperCase();
			StringBuffer returnValue = new StringBuffer(6);
			for(int a = 0; a<s.length() && returnValue.length()<charLimit; a++) {
				char c = s.charAt(a);
				if(Character.isDigit(c) || (c>='A' && c<='F')) {
					returnValue.append(c);
				}
			}
			return returnValue.toString();
		}

		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}
	};

	private Option alpha = new Option(strings.getObject("alphaLabel").toString(), 255);
	private Option hue = new Option(strings.getObject("hueLabel").toString(), 360);
	private Option sat = new Option(strings.getObject("saturationLabel").toString(), 100);
	private Option bri = new Option(strings.getObject("brightnessLabel").toString(), 100);
	private Option red = new Option(strings.getObject("redLabel").toString(), 255);
	private Option green = new Option(strings.getObject("greenLabel").toString(), 255);
	private Option blue = new Option(strings.getObject("blueLabel").toString(), 255);
	private ColorSwatch preview = new ColorSwatch(50);
	private JLabel hexLabel = new JLabel(strings.getObject("hexLabel").toString());
	private JTextField hexField = new JTextField("000000");
	
	/** Used to indicate when we're internally adjusting the value of the spinners.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingSpinners = 0;

	/** Used to indicate when we're internally adjusting the value of the slider.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingSlider = 0;

	/** Used to indicate when we're internally adjusting the selected color of the ColorPanel.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingColorPanel = 0;

	/** Used to indicate when we're internally adjusting the value of the hex field.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingHexField = 0;

	/** Used to indicate when we're internally adjusting the value of the opacity.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingOpacity = 0;
	
	/** The "expert" controls are the controls on the right side
	 * of this panel: the labels/spinners/radio buttons.
	 */
	private JPanel expertControls = new JPanel(new GridBagLayout());
	
	private ColorPickerPanel colorPanel = new ColorPickerPanel();
	
	private JSlider opacitySlider = new JSlider(0,255,255);
	private JLabel opacityLabel = new JLabel(strings.getObject("opacityLabel").toString());
	
	/** Create a new <code>ColorPicker</code> with all controls visible except opacity. */
	public ColorPicker() {
		this(true,false);
	}
	
	/** Create a new <code>ColorPicker</code>.
	 * 
	 * @param showExpertControls the labels/spinners/buttons on the right side of a
	 * <code>ColorPicker</code> are optional.  This boolean will control whether they
	 * are shown or not.
	 * <P>It may be that your users will never need or want numeric control when
	 * they choose their colors, so hiding this may simplify your interface.
	 * @param includeOpacity whether the opacity controls will be shown
	 */
	public ColorPicker(boolean showExpertControls,boolean includeOpacity) {
		super(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		Insets normalInsets = new Insets(3,3,3,3);
		
		JPanel options = new JPanel(new GridBagLayout());
		c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1;
		c.insets = normalInsets;
		ButtonGroup bg = new ButtonGroup();
		
		//put them in order
		Option[] optionsArray = new Option[] {
				hue, sat, bri, red, green, blue
		};
		
		for(int a = 0; a<optionsArray.length; a++) {
			if(a==3 || a==6) {
				c.insets = new Insets(normalInsets.top+10,normalInsets.left,normalInsets.bottom,normalInsets.right);
			} else {
				c.insets = normalInsets;
			}
			c.anchor = GridBagConstraints.EAST;
			c.fill = GridBagConstraints.NONE;
			options.add(optionsArray[a].label,c);
			c.gridx++;
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.HORIZONTAL;
			if(optionsArray[a].spinner!=null) {
				options.add(optionsArray[a].spinner,c);
			} else {
				options.add(optionsArray[a].slider,c);
			}
			c.gridx++; c.fill = GridBagConstraints.NONE;
			options.add(optionsArray[a].radioButton,c);
			c.gridy++;
			c.gridx = 0;
			bg.add(optionsArray[a].radioButton);
		}
		c.insets = new Insets(normalInsets.top+10,normalInsets.left,normalInsets.bottom,normalInsets.right);
		c.anchor = GridBagConstraints.EAST; c.fill = GridBagConstraints.NONE;
		options.add(hexLabel,c);
		c.gridx++;
		c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		options.add(hexField,c);
		c.gridy++; c.gridx = 0;
		c.anchor = GridBagConstraints.EAST; c.fill = GridBagConstraints.NONE;
		options.add(alpha.label,c);
		c.gridx++;
		c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		options.add(alpha.spinner,c);
		
		c.gridx = 0; c.gridy = 0; c.weightx = 1;
		c.weighty = 1; c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER; c.insets = normalInsets;
		c.gridwidth = 2;
		add(colorPanel,c);
		
		c.gridwidth = 1;
		c.insets = normalInsets;
		c.gridx+=2; c.weighty = 1; c.gridwidth = 1;
		c.fill = GridBagConstraints.VERTICAL; c.weightx = 0;
		add(slider,c);
		
		c.gridx++; c.fill = GridBagConstraints.VERTICAL; c.gridheight = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER; c.insets = new Insets(0,0,0,0);
		add(expertControls,c);
		
		c.gridx = 0; c.gridheight = 1;
		c.gridy = 1; c.weightx = 0; c.weighty = 0;
		c.insets = normalInsets; c.anchor = GridBagConstraints.CENTER;
		add(opacityLabel,c);
		c.gridx++; c.gridwidth = 2;
		c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
		add(opacitySlider,c);
		
		c.gridx = 0; c.gridy = 0;
		c.gridheight = 1; c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH; 
		c.weighty = 1; c.anchor = GridBagConstraints.CENTER; 
		c.weightx = 1;
		c.insets = new Insets(normalInsets.top,normalInsets.left+8,normalInsets.bottom+10,normalInsets.right+8);
		expertControls.add(preview,c);
		c.gridy++; c.weighty = 0; c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(normalInsets.top,normalInsets.left,0,normalInsets.right);
		expertControls.add(options,c);
		
		preview.setOpaque(true);
		colorPanel.setPreferredSize(new Dimension(expertControls.getPreferredSize().height, 
												expertControls.getPreferredSize().height));
		
		slider.addChangeListener(changeListener);
		colorPanel.addChangeListener(changeListener);
		slider.setUI(new ColorPickerSliderUI(slider,this));
		hexField.getDocument().addDocumentListener(hexDocListener);
		setMode(BRI);

		setExpertControlsVisible(showExpertControls);
		
		setOpacityVisible(includeOpacity);
		
		opacitySlider.addChangeListener(changeListener);
		
		setOpacity( 255 );
		setOpaque(this,false);
		
		preview.setForeground( getColor() );
	}
	
	private static void setOpaque(JComponent jc,boolean opaque) {
		if(jc instanceof JTextField)
			return;
		
		jc.setOpaque(false);
		if(jc instanceof JSpinner)
			return;
		
		for(int a = 0; a<jc.getComponentCount(); a++) {
			JComponent child = (JComponent)jc.getComponent(a);
			setOpaque(child,opaque);
		}
	}
	
	/** This controls whether the hex field (and label) are visible or not.
	 * <P>Note this lives inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
	 * has been called, then calling this method makes no difference: the hex controls will be hidden.
	 */
	public void setHexControlsVisible(boolean b) {
		hexLabel.setVisible(b);
		hexField.setVisible(b);
	}

	/** This controls whether the preview swatch visible or not.
	 * <P>Note this lives inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
	 * has been called, then calling this method makes no difference: the swatch will be hidden.
	 */
	public void setPreviewSwatchVisible(boolean b) {
		preview.setVisible(b);
	}
	
	/** The labels/spinners/buttons on the right side of a <code>ColorPicker</code>
	 * are optional.  This method will control whether they are shown or not.
	 * <P>It may be that your users will never need or want numeric control when
	 * they choose their colors, so hiding this may simplify your interface.
	 * 
	 * @param b whether to show or hide the expert controls.
	 */
	public void setExpertControlsVisible(boolean b) {
		expertControls.setVisible(b);
	}
	
	/** @return the current HSB coordinates of this <code>ColorPicker</code>.
	 * Each value is between [0,1].
	 * 
	 */
	public float[] getHSB() {
		return new float[] {
				hue.getFloatValue()/360f,
				sat.getFloatValue()/100f,
				bri.getFloatValue()/100f
		};
	}

	/** @return the current RGB coordinates of this <code>ColorPicker</code>.
	 * Each value is between [0,255].
	 * 
	 */
	public int[] getRGB() {
		return new int[] {
				currentRed,
				currentGreen,
				currentBlue
		};
	}
	
	/** Returns the currently selected opacity (a float between 0 and 1). 
	 * 
	 * @return the currently selected opacity (a float between 0 and 1).
	 */
	public float getOpacity() {
		return (opacitySlider.getValue())/255f;
	}
	
	private int lastOpacity = 255;
	
	/** Sets the currently selected opacity.
	 * 
	 * @param v an int between 0 and 255.
	 */
	public void setOpacity(int v) {
		if(v<0 || v>255) 
			throw new IllegalArgumentException("The opacity ("+v+") must be between 0 and 255.");
		adjustingOpacity++;
		try {
			opacitySlider.setValue( v );
			alpha.spinner.setValue( new Integer(v) );
			if(lastOpacity!=v) {
				firePropertyChange(OPACITY_PROPERTY,new Integer(lastOpacity),new Integer(v));
				Color c = preview.getForeground();
				preview.setForeground(new Color(c.getRed(), c.getGreen(), c.getBlue(), v));
			}
			lastOpacity = v;
		} finally {
			adjustingOpacity--;
		}
	}
	
	/** Sets the mode of this <code>ColorPicker</code>.
	 * This is especially useful if this picker is in non-expert mode, so
	 * the radio buttons are not visible for the user to directly select.
	 * 
	 * @param mode must be HUE, SAT, BRI, RED, GREEN or BLUE.
	 */
	public void setMode(int mode) {
		if(!(mode==HUE || mode==SAT || mode==BRI || mode==RED || mode==GREEN || mode==BLUE))
			throw new IllegalArgumentException("mode must be HUE, SAT, BRI, REd, GREEN, or BLUE");
		putClientProperty(MODE_PROPERTY,new Integer(mode));
		hue.radioButton.setSelected(mode==HUE);
		sat.radioButton.setSelected(mode==SAT);
		bri.radioButton.setSelected(mode==BRI);
		red.radioButton.setSelected(mode==RED);
		green.radioButton.setSelected(mode==GREEN);
		blue.radioButton.setSelected(mode==BLUE);

		colorPanel.setMode(mode);
		adjustingSlider++;
		try {
			slider.setValue(0);
			Option option = getSelectedOption();
			slider.setInverted(mode==HUE);
			int max = option.getMaximum();
			slider.setMaximum(max);
			slider.setValue( option.getIntValue() );
			slider.repaint();
			
			if(mode==HUE || mode==SAT || mode==BRI) {
				setHSB( hue.getFloatValue()/360f,
						sat.getFloatValue()/100f,
						bri.getFloatValue()/100f );
			} else {
				setRGB( red.getIntValue(),
						green.getIntValue(),
						blue.getIntValue() );
				
			}
		} finally {
			adjustingSlider--;
		}
	}
	
	/** This controls whether the radio buttons that adjust the mode are visible.
	 * <P>(These buttons appear next to the spinners in the expert controls.)
	 * <P>Note these live inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
	 * has been called, then these will never be visible.
	 * 
	 * @param b
	 */
	public void setModeControlsVisible(boolean b) {
		hue.radioButton.setVisible(b && hue.isVisible());
		sat.radioButton.setVisible(b && sat.isVisible());
		bri.radioButton.setVisible(b && bri.isVisible());
		red.radioButton.setVisible(b && red.isVisible());
		green.radioButton.setVisible(b && green.isVisible());
		blue.radioButton.setVisible(b && blue.isVisible());
		putClientProperty(MODE_CONTROLS_VISIBLE_PROPERTY,new Boolean(b));
	}
	
	/** @return the current mode of this <code>ColorPicker</code>.
	 * <BR>This will return <code>HUE</code>,  <code>SAT</code>,  <code>BRI</code>, 
	 * <code>RED</code>,  <code>GREEN</code>, or <code>BLUE</code>.
	 * <P>The default mode is <code>BRI</code>, because that provides the most
	 * aesthetic/recognizable color wheel.
	 */
	public int getMode() {
		Integer i = (Integer)getClientProperty(MODE_PROPERTY);
		if(i==null) return -1;
		return i.intValue();
	}

	/** Sets the current color of this <code>ColorPicker</code>.
	 * This method simply calls <code>setRGB()</code> and <code>setOpacity()</code>.
	 * @param c the new color to use.
	 */
	public void setColor(Color c) {
		setRGB(c.getRed(),c.getGreen(),c.getBlue());
		setOpacity(c.getAlpha());
	}
	
	/** Sets the current color of this <code>ColorPicker</code>
	 * 
	 * @param r the red value.  Must be between [0,255].
	 * @param g the green value.  Must be between [0,255].
	 * @param b the blue value.  Must be between [0,255].
	 */
	public void setRGB(int r,int g,int b) {
		if(r<0 || r>255)
			throw new IllegalArgumentException("The red value ("+r+") must be between [0,255].");
		if(g<0 || g>255)
			throw new IllegalArgumentException("The green value ("+g+") must be between [0,255].");
		if(b<0 || b>255)
			throw new IllegalArgumentException("The blue value ("+b+") must be between [0,255].");
		
		Color lastColor = getColor();
		
		boolean updateRGBSpinners = adjustingSpinners==0;
		
		adjustingSpinners++;
		adjustingColorPanel++;
		int alpha = this.alpha.getIntValue();
		try {
			if(updateRGBSpinners) {
				red.setValue(r);
				green.setValue(g);
				blue.setValue(b);
			}
			preview.setForeground(new Color(r,g,b, alpha));
			float[] hsb = new float[3];
			Color.RGBtoHSB(r, g, b, hsb);
			hue.setValue( (int)(hsb[0]*360f+.49f));
			sat.setValue( (int)(hsb[1]*100f+.49f));
			bri.setValue( (int)(hsb[2]*100f+.49f));
			colorPanel.setRGB(r, g, b);
			updateHexField();
			updateSlider();
		} finally {
			adjustingSpinners--;
			adjustingColorPanel--;
		}
		currentRed = r;
		currentGreen = g;
		currentBlue = b;
		Color newColor = getColor();
		if(lastColor.equals(newColor)==false)
			firePropertyChange(SELECTED_COLOR_PROPERTY,lastColor,newColor);
	}
	
	/** @return the current <code>Color</code> this <code>ColorPicker</code> has selected.
	 * <P>This is equivalent to:
	 * <BR><code>int[] i = getRGB();</code>
	 * <BR><code>return new Color(i[0], i[1], i[2], opacitySlider.getValue());</code>
	 */
	public Color getColor() {
		int[] i = getRGB();
		return new Color(i[0], i[1], i[2], opacitySlider.getValue());
	}
	
	private void updateSlider() {
		adjustingSlider++;
		try {
			int mode = getMode();
			if(mode==HUE) {
				slider.setValue( hue.getIntValue() );
			} else if(mode==SAT) {
				slider.setValue( sat.getIntValue() );
			} else if(mode==BRI) {
				slider.setValue( bri.getIntValue() );
			} else if(mode==RED) {
				slider.setValue( red.getIntValue() );
			} else if(mode==GREEN) {
				slider.setValue( green.getIntValue() );
			} else if(mode==BLUE) {
				slider.setValue( blue.getIntValue() );
			}
		} finally {
			adjustingSlider--;
		}
		slider.repaint();
	}
	
	/** This returns the panel with several rows of spinner controls.
	 * <P>Note you can also call methods such as <code>setRGBControlsVisible()</code> to adjust
	 * which controls are showing.
	 * <P>(This returns the panel this <code>ColorPicker</code> uses, so if you put it in
	 * another container, it will be removed from this <code>ColorPicker</code>.)
	 * @return the panel with several rows of spinner controls.
	 */
	public JPanel getExpertControls() {
		return expertControls;
	}
	
	/** This shows or hides the RGB spinner controls.
	 * <P>Note these live inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
	 * has been called, then calling this method makes no difference: the RGB controls will be hidden.
	 * 
	 * @param b whether the controls should be visible or not.
	 */
	public void setRGBControlsVisible(boolean b) {
		red.setVisible(b);
		green.setVisible(b);
		blue.setVisible(b);
	}

	/** This shows or hides the HSB spinner controls.
	 * <P>Note these live inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
	 * has been called, then calling this method makes no difference: the HSB controls will be hidden.
	 * 
	 * @param b whether the controls should be visible or not.
	 */
	public void setHSBControlsVisible(boolean b) {
		hue.setVisible(b);
		sat.setVisible(b);
		bri.setVisible(b);
	}

	/** This shows or hides the alpha controls.
	 * <P>Note the alpha spinner live inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
	 * has been called, then this method does not affect that spinner.
	 * However, the opacity slider is <i>not</i> affected by the visibility of the export controls.
	 * @param b
	 */
	public void setOpacityVisible(boolean b) {
		opacityLabel.setVisible(b);
		opacitySlider.setVisible(b);
		alpha.label.setVisible(b);
		alpha.spinner.setVisible(b);
	}
	
	/** @return the <code>ColorPickerPanel</code> this <code>ColorPicker</code> displays. */
	public ColorPickerPanel getColorPanel() {
		return colorPanel;
	}

	/** Sets the current color of this <code>ColorPicker</code>
	 * 
	 * @param h the hue value.
	 * @param s the saturation value.  Must be between [0,1].
	 * @param b the blue value.  Must be between [0,1].
	 */
	public void setHSB(float h, float s, float b) {
		if(Float.isInfinite(h) || Float.isNaN(h))
			throw new IllegalArgumentException("The hue value ("+h+") is not a valid number.");
		//hue is cyclic, so it can be any value:
		while(h<0) h++;
		while(h>1) h--;
		
		if(s<0 || s>1)
			throw new IllegalArgumentException("The saturation value ("+s+") must be between [0,1]");
		if(b<0 || b>1)
			throw new IllegalArgumentException("The brightness value ("+b+") must be between [0,1]");
		
		Color lastColor = getColor();
		
		boolean updateHSBSpinners = adjustingSpinners==0;
		adjustingSpinners++;
		adjustingColorPanel++;
		try {
			if(updateHSBSpinners) {
				hue.setValue( (int)(h*360f+.49f));
				sat.setValue( (int)(s*100f+.49f));
				bri.setValue( (int)(b*100f+.49f));
			}
			
			Color c = new Color(Color.HSBtoRGB(h, s, b));
			int alpha = this.alpha.getIntValue();
			c = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
			preview.setForeground(c);
			currentRed = c.getRed();
			currentGreen = c.getGreen();
			currentBlue = c.getBlue();
			red.setValue(currentRed);
			green.setValue(currentGreen);
			blue.setValue(currentBlue);
			colorPanel.setHSB(h, s, b);
			updateHexField();
			updateSlider();
			slider.repaint();
		} finally {
			adjustingSpinners--;
			adjustingColorPanel--;
		}
		Color newColor = getColor();
		if(lastColor.equals(newColor)==false)
			firePropertyChange(SELECTED_COLOR_PROPERTY,lastColor,newColor);
	}
	
	private void updateHexField() {
		adjustingHexField++;
		try {
			int r = red.getIntValue();
			int g = green.getIntValue();
			int b = blue.getIntValue();
			
			int i = (r << 16) + (g << 8) +b;
			String s = Integer.toHexString(i).toUpperCase();
			while(s.length()<6)
				s = "0"+s;
			if(hexField.getText().equalsIgnoreCase(s)==false)
				hexField.setText(s);
		} finally {
			adjustingHexField--;
		}
	}

	class Option {
		JRadioButton radioButton = new JRadioButton();
		JSpinner spinner;
		JSlider slider;
		JLabel label;
		public Option(String text,int max) {
			spinner = new JSpinner(new SpinnerNumberModel(0,0,max,5));
			spinner.addChangeListener(changeListener);
			
			/*this tries out Tim Boudreaux's new slider UI.
			* It's a good UI, but I think for the ColorPicker
			* the numeric controls are more useful.
			* That is: users who want click-and-drag control to choose
			* their colors don't need any of these Option objects
			* at all; only power users who may have specific RGB
			* values in mind will use these controls: and when they do
			* limiting them to a slider is unnecessary.
			* That's my current position... of course it may
			* not be true in the real world... :)
			*/
			//slider = new JSlider(0,max);
			//slider.addChangeListener(changeListener);
			//slider.setUI(new org.netbeans.paint.api.components.PopupSliderUI());
				
			label = new JLabel(text);
			radioButton.addActionListener(actionListener);
		}
		
		public void setValue(int i) {
			if(slider!=null) {
				slider.setValue(i);
			}
			if(spinner!=null) {
				spinner.setValue(new Integer(i));
			}
		}
		
		public int getMaximum() {
			if(slider!=null)
				return slider.getMaximum();
			return ((Number) ((SpinnerNumberModel)spinner.getModel()).getMaximum() ).intValue();
		}
		
		public boolean contains(Object src) {
			return (src==slider || src==spinner || src==radioButton || src==label);
		}
		
		public float getFloatValue() {
			return getIntValue();
		}
		
		public int getIntValue() {
			if(slider!=null)
				return slider.getValue();
			return ((Number)spinner.getValue()).intValue();
		}
		
		public boolean isVisible() {
			return label.isVisible();
		}
		
		public void setVisible(boolean b) {
			boolean radioButtonsAllowed = true;
			Boolean z = (Boolean)getClientProperty(MODE_CONTROLS_VISIBLE_PROPERTY);
			if(z!=null) radioButtonsAllowed = z.booleanValue();
			
			radioButton.setVisible(b && radioButtonsAllowed);
			if(slider!=null)
				slider.setVisible(b);
			if(spinner!=null)
				spinner.setVisible(b);
			label.setVisible(b);
		}
	}
}
