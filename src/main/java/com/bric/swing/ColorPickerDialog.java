/*
 * @(#)ColorPickerDialog.java
 *
 * $Date: 2009-11-04 22:22:34 +0100 (Sze, 04 nov. 2009) $
 *
 * Copyright (c) 2009 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood.
 * You may not use, copy or modify this software, except in
 * accordance with the license agreement you entered into with
 * Jeremy Wood. For details see accompanying license terms.
 *
 * This software is probably, but not necessarily, discussed here:
 * http://javagraphics.blogspot.com/
 *
 * And the latest version should be available here:
 * https://javagraphics.dev.java.net/
 */
package com.bric.swing;

import com.bric.util.JVM;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

/** This wraps a <code>ColorPicker</code> in a simple dialog with "OK" and "Cancel" options.
 * <P>(This object is used by the static calls in <code>ColorPicker</code> to show a dialog.)
 *
 */
class ColorPickerDialog extends JDialog {

	private static final long serialVersionUID = 2L;

	ColorPicker cp;
	int alpha;
	JButton ok = new JButton(ColorPicker.strings.getObject("OK").toString());
	JButton cancel = new JButton(ColorPicker.strings.getObject("Cancel").toString());
	Color returnValue = null;
	ActionListener buttonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if(src==ok) {
				returnValue = cp.getColor();
			}
			setVisible(false);
			dispose();
		}
	};

	public ColorPickerDialog(Frame owner, Color color,boolean includeOpacity) {
		super(owner);
		initialize(owner,color,includeOpacity);
	}

	public ColorPickerDialog(Dialog owner, Color color,boolean includeOpacity) {
		super(owner);
		initialize(owner,color,includeOpacity);
	}

	private void initialize(Component owner,Color color,boolean includeOpacity) {
		cp = new ColorPicker(true,includeOpacity);
        cp.setMode(ColorPicker.HUE);
		setModal(true);
		setResizable(false);
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 0;
		c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(10,10,10,10);
		contentPane.add(cp,c);

        c.gridy++; c.gridwidth = 1;

        if(JVM.isMac) {
            contentPane.add(new JPanel(),c);

            c.gridx++; c.weightx = 0;
            contentPane.add(cancel,c);
            c.gridx++; c.weightx = 0;
            contentPane.add(ok,c);
        } else {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
            p.add(ok);
            p.add(cancel);
            contentPane.add(p, c);
        }

		cp.setRGB(color.getRed(), color.getGreen(), color.getBlue());
		cp.setOpacity( color.getAlpha() );
		alpha = color.getAlpha();
		pack();
        setLocationRelativeTo(owner);

		ok.addActionListener(buttonListener);
		cancel.addActionListener(buttonListener);

		getRootPane().setDefaultButton(ok);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	/** @return the color committed when the user clicked 'OK'.  Note this returns <code>null</code>
	 * if the user canceled this dialog, or exited via the close decoration.
	 */
	public Color getColor() {
		return returnValue;
	}
}

