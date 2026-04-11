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

import com.bric.plaf.ColorPickerSliderUI;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.Serial;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static com.bric.swing.ColorPicker.Mode.BLUE;
import static com.bric.swing.ColorPicker.Mode.BRI;
import static com.bric.swing.ColorPicker.Mode.GREEN;
import static com.bric.swing.ColorPicker.Mode.HUE;
import static com.bric.swing.ColorPicker.Mode.RED;
import static com.bric.swing.ColorPicker.Mode.SAT;

/**
 * <p>This is a panel that offers a robust set of controls to pick a color.
 * <P>This was originally intended to replace the <code>JColorChooser</code>.
 * To use this class to create a color choosing dialog, simply call:
 * <BR><code>ColorPicker.showDialog(frame, originalColor);</code>
 * <p>However this does not have to be invoked as a black-box color dialog. This class
 * is simply a panel, and you can customize and resize it for other looks.
 * For example, you might try the following panel:</p>
 * <BR><code>ColorPicker picker = new ColorPicker(false, false);</code>
 * <BR><code>picker.setPreferredSize(new Dimension(200,160));</code>
 * <BR><code>picker.setMode(ColorPicker.Mode.HUE);</code>
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
 */
public class ColorPicker extends JPanel {
    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * The localized strings used in this (and related) panel(s).
     */
    private static final ResourceBundle strings = ResourceBundle.getBundle("com.bric.swing.resources.ColorPicker");
    private Consumer<Color> colorChangeListener;

    /**
     * This creates a modal dialog prompting the user to select a color.
     * <P>This uses a generic dialog title: "Choose a Color".
     *
     * @param owner          the window this new dialog belongs to.
     * @param originalColor  the color the <code>ColorPicker</code> initially selects.
     * @param includeOpacity whether to add a control for the opacity of the color.
     * @return the <code>Color</code> the user chooses, or <code>null</code> if the user cancels the dialog.
     */
    public static Color showDialog(Window owner, Color originalColor,
                                   boolean includeOpacity, Consumer<Color> listener) {
        return showDialog(owner, null, originalColor, includeOpacity, listener);
    }

    /**
     * This creates a modal dialog prompting the user to select a color.
     *
     * @param owner               the window this new dialog belongs to
     * @param title               the title for the dialog.
     * @param originalColor       the color the <code>ColorPicker</code> initially selects.
     * @param includeOpacity      whether to add a control for the opacity of the color.
     * @param colorChangeListener a callback notified of color adjustments (except drag events)
     * @return the <code>Color</code> the user chooses, or <code>null</code> if the user cancels the dialog.
     */
    public static Color showDialog(Window owner, String title,
                                   Color originalColor, boolean includeOpacity,
                                   Consumer<Color> colorChangeListener) {
        var d = new ColorPickerDialog(owner, originalColor, includeOpacity, colorChangeListener);

        d.setTitle(title == null ?
            strings.getString("ColorPickerDialogTitle") :
            title);
        d.pack();
        d.setVisible(true);
        return d.getColor();
    }

    /**
     * <code>PropertyChangeEvents</code> will be triggered for this property when the selected color
     * changes.
     * <P>(Events are only created when the RGB values of the color change.  This means, for example,
     * that the change from HSB(0,0,0) to HSB(.4,0,0) will <i>not</i> generate events, because when the
     * brightness stays zero the RGB color remains (0,0,0).  So although the hue moved around, the color
     * is still black, so no events are created.)
     */
    public static final String SELECTED_COLOR_PROPERTY = "selected color";

    /**
     * <code>PropertyChangeEvents</code> will be triggered for this property when <code>setModeControlsVisible()</code>
     * is called.
     */
    public static final String MODE_CONTROLS_VISIBLE_PROPERTY = "mode controls visible";

    /**
     * <code>PropertyChangeEvents</code> will be triggered when the opacity value is
     * adjusted.
     */
    public static final String OPACITY_PROPERTY = "opacity";

    /**
     * <code>PropertyChangeEvents</code> will be triggered when the mode changes.
     * (That is, when the wheel switches from HUE, SAT, BRI, RED, GREEN, or BLUE modes.)
     */
    public static final String MODE_PROPERTY = "mode";

    public enum Mode {
        /**
         * Used to indicate when we're in "hue mode".
         */
        HUE,
        /**
         * Used to indicate when we're in "brightness mode".
         */
        BRI,
        /**
         * Used to indicate when we're in "saturation mode".
         */
        SAT,
        /**
         * Used to indicate when we're in "red mode".
         */
        RED,
        /**
         * Used to indicate when we're in "green mode".
         */
        GREEN,
        /**
         * Used to indicate when we're in "blue mode".
         */
        BLUE
    }

    /**
     * The vertical slider
     */
    private final JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, 0);

    private int currentRed = 0;
    private int currentGreen = 0;
    private int currentBlue = 0;

    /**
     * Returns the currently selected {@link ChannelUI}.
     */
    private ChannelUI getSelectedChannelUI() {
        return switch (getMode()) {
            case HUE -> hue;
            case SAT -> sat;
            case BRI -> bri;
            case RED -> red;
            case GREEN -> green;
            case BLUE -> blue;
        };
    }

    class HexDocumentListener implements DocumentListener {
        // the delay (in ms) to commit text that might not be finished
        static final int DELAY = 1500;

        String uncommittedText = null;

        final Timer delayedUpdater = new Timer(DELAY, e -> {
            if (uncommittedText != null) {
                int pos = hexField.getCaretPosition();
                pos = Math.min(pos, uncommittedText.length());
                hexField.setText(uncommittedText);
                hexField.setCaretPosition(pos);
            }
        });

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (hexFieldUpdateDepth > 0) {
                return;
            }

            String s = hexField.getText();
            s = stripToHex(s, 6);

            /* If we don't have 6 characters, then use a delay.
             * If, after a second or two, the user has just
             * stopped typing: then we can try to make
             * sense of what they input even if it's incomplete.
             */
            boolean delay = false;
            if (s.length() < 6) {
                delay = true;
                s += "0".repeat(6 - s.length());
            }

            try {
                int i = Integer.parseInt(s, 16);
                int r = ((i >> 16) & 0xFF);
                int g = ((i >> 8) & 0xFF);
                int b = (i & 0xFF);

                if (delay) {
                    delayedUpdater.setRepeats(false);
                    delayedUpdater.restart();
                    uncommittedText = s;
                } else {
                    delayedUpdater.stop();

                    /* Be sure to invoke this separately, otherwise we'll risk getting
                     * an "attempt to mutate in notification".
                     * ( https://java.net/jira/browse/JAVAGRAPHICS-19 )
                     */
                    SwingUtilities.invokeLater(() -> {
                        int pos = hexField.getCaretPosition();
                        setRGB(r, g, b);
                        pos = Math.min(pos, hexField.getText().length());
                        hexField.setCaretPosition(pos);
                        if (colorChangeListener != null && hexFieldUpdateDepth == 0) {
                            colorChangeListener.accept(getColor());
                        }
                    });
                    uncommittedText = null;
                }
                return;
            } catch (NumberFormatException e2) {
                //this shouldn't happen, since we already stripped out non-hex characters.
                e2.printStackTrace();
            }
        }

        /**
         * Strips a string down to only uppercase hex-supported characters.
         *
         * @param s         the string to strip
         * @param charLimit the maximum number of characters in the return value
         * @return an uppercase version of <code>s</code> that only includes hexadecimal
         * characters and is not longer than <code>charLimit</code>.
         */
        private static String stripToHex(String s, int charLimit) {
            s = s.toUpperCase(Locale.ROOT);
            StringBuilder returnValue = new StringBuilder(6);
            for (int a = 0; a < s.length() && returnValue.length() < charLimit; a++) {
                char c = s.charAt(a);
                //noinspection CharacterComparison
                if (Character.isDigit(c) || (c >= 'A' && c <= 'F')) {
                    returnValue.append(c);
                }
            }
            return returnValue.toString();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changedUpdate(e);
        }
    }

    private final ChannelUI alpha = new ChannelUI(strings.getString("alphaLabel"), 255);
    private final ChannelUI hue = new ChannelUI(strings.getString("hueLabel"), 360);
    private final ChannelUI sat = new ChannelUI(strings.getString("saturationLabel"), 100);
    private final ChannelUI bri = new ChannelUI(strings.getString("brightnessLabel"), 100);
    private final ChannelUI red = new ChannelUI(strings.getString("redLabel"), 255);
    private final ChannelUI green = new ChannelUI(strings.getString("greenLabel"), 255);
    private final ChannelUI blue = new ChannelUI(strings.getString("blueLabel"), 255);
    private final ColorSwatch preview = new ColorSwatch(50);
    private final JLabel hexLabel = new JLabel(strings.getString("hexLabel"));
    private final JTextField hexField = new JTextField("000000");

    /**
     * Used to indicate when we're internally adjusting the value of the spinners.
     * If this equals zero, then incoming events are triggered by the user and must be processed.
     * If this is not equal to zero, then incoming events are triggered by another method
     * that's already responding to the user's actions.
     */
    private int spinnerUpdateDepth = 0;

    /**
     * Used to indicate when we're internally adjusting the value of the slider.
     * If this equals zero, then incoming events are triggered by the user and must be processed.
     * If this is not equal to zero, then incoming events are triggered by another method
     * that's already responding to the user's actions.
     */
    private int sliderUpdateDepth = 0;

    /**
     * Used to indicate when we're internally adjusting the selected color of the ColorPanel.
     * If this equals zero, then incoming events are triggered by the user and must be processed.
     * If this is not equal to zero, then incoming events are triggered by another method
     * that's already responding to the user's actions.
     */
    private int colorPanelUpdateDepth = 0;

    /**
     * Used to indicate when we're internally adjusting the value of the hex field.
     * If this equals zero, then incoming events are triggered by the user and must be processed.
     * If this is not equal to zero, then incoming events are triggered by another method
     * that's already responding to the user's actions.
     */
    private int hexFieldUpdateDepth = 0;

    /**
     * Used to indicate when we're internally adjusting the value of the opacity.
     * If this equals zero, then incoming events are triggered by the user and must be processed.
     * If this is not equal to zero, then incoming events are triggered by another method
     * that's already responding to the user's actions.
     */
    private int opacityUpdateDepth = 0;

    // this new flag is needed because
    // the variables above don't fully work as described
    private boolean sliderUpdatingSpinner = false;

    /**
     * The "expert" controls are the controls on the right side
     * of this panel: the labels/spinners/radio buttons.
     */
    private final JPanel expertControls = new JPanel(new GridBagLayout());

    private final ColorPickerPanel colorPanel = new ColorPickerPanel();

    private final JSlider opacitySlider = new JSlider(0, 255, 255);
    private final JLabel opacityLabel = new JLabel(strings.getString("opacityLabel"));

    /**
     * Create a new <code>ColorPicker</code> with all controls visible except opacity.
     */
    public ColorPicker() {
        this(true, false);
    }

    /**
     * Create a new <code>ColorPicker</code>.
     *
     * @param showExpertControls the labels/spinners/buttons on the right side of a
     *                           <code>ColorPicker</code> are optional.  This boolean will control whether they
     *                           are shown or not.
     *                           <P>It may be that your users will never need or want numeric control when
     *                           they choose their colors, so hiding this may simplify your interface.
     * @param includeOpacity     whether the opacity controls will be shown
     */
    public ColorPicker(boolean showExpertControls, boolean includeOpacity) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        Insets normalInsets = new Insets(3, 3, 3, 3);
        Insets tallInsets = new Insets(normalInsets.top + 10, normalInsets.left, normalInsets.bottom, normalInsets.right);

        JPanel channelUIs = new JPanel(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = normalInsets;
        ButtonGroup bg = new ButtonGroup();

        // put them in order
        ChannelUI[] channelUIsArray = {
            hue, sat, bri, red, green, blue
        };

        for (int a = 0; a < channelUIsArray.length; a++) {
            if (a == 3 || a == 6) {
                c.insets = tallInsets;
            } else {
                c.insets = normalInsets;
            }
            c.anchor = GridBagConstraints.EAST;
            c.fill = GridBagConstraints.NONE;
            channelUIs.add(channelUIsArray[a].label, c);
            c.gridx++;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            channelUIs.add(channelUIsArray[a].spinner, c);
            c.gridx++;
            c.fill = GridBagConstraints.NONE;
            channelUIs.add(channelUIsArray[a].radioButton, c);
            c.gridy++;
            c.gridx = 0;
            bg.add(channelUIsArray[a].radioButton);
        }
        c.insets = tallInsets;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        channelUIs.add(hexLabel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        channelUIs.add(hexField, c);
        c.gridy++;
        c.gridx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        channelUIs.add(alpha.label, c);
        c.gridx++;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        channelUIs.add(alpha.spinner, c);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = normalInsets;
        c.gridwidth = 2;
        add(colorPanel, c);

        c.insets = normalInsets;
        c.gridx += 2;
        c.weighty = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        add(slider, c);

        c.gridx++;
        c.fill = GridBagConstraints.VERTICAL;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0);
        add(expertControls, c);

        c.gridx = 0;
        c.gridheight = 1;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = normalInsets;
        c.anchor = GridBagConstraints.CENTER;
        add(opacityLabel, c);
        c.gridx++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(opacitySlider, c);

        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1;
        c.insets = new Insets(normalInsets.top, normalInsets.left + 8, normalInsets.bottom + 10, normalInsets.right + 8);
        expertControls.add(preview, c);
        c.gridy++;
        c.weighty = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(normalInsets.top, normalInsets.left, 0, normalInsets.right);
        expertControls.add(channelUIs, c);

        preview.setOpaque(true);
        int colorPanelSize = expertControls.getPreferredSize().height;
        colorPanel.setPreferredSize(new Dimension(colorPanelSize, colorPanelSize));

        ChangeListener hsbChangeListener = e -> {
            if (spinnerUpdateDepth > 0) {
                return;
            }
            setHSB(hue.getValue() / 360.0f,
                sat.getValue() / 100.0f,
                bri.getValue() / 100.0f);

            // dispatch notification event when state explicitly updates internal mechanisms
            if (colorChangeListener != null && !sliderUpdatingSpinner && opacityUpdateDepth == 0) {
                colorChangeListener.accept(getColor());
            }
        };
        hue.spinner.addChangeListener(hsbChangeListener);
        sat.spinner.addChangeListener(hsbChangeListener);
        bri.spinner.addChangeListener(hsbChangeListener);

        ChangeListener rgbChangeListener = e -> {
            if (spinnerUpdateDepth > 0) {
                return;
            }
            setRGB(red.getValue(),
                green.getValue(),
                blue.getValue());

            if (colorChangeListener != null && !sliderUpdatingSpinner && opacityUpdateDepth == 0) {
                colorChangeListener.accept(getColor());
            }
        };
        red.spinner.addChangeListener(rgbChangeListener);
        green.spinner.addChangeListener(rgbChangeListener);
        blue.spinner.addChangeListener(rgbChangeListener);

        alpha.spinner.addChangeListener(e -> {
            if (opacityUpdateDepth > 0) {
                return;
            }
            int v = alpha.getValue();
            setOpacity(v);

            if (colorChangeListener != null && spinnerUpdateDepth == 0 && !sliderUpdatingSpinner) {
                colorChangeListener.accept(getColor());
            }
        });

        hue.radioButton.addActionListener(e -> setMode(HUE));
        sat.radioButton.addActionListener(e -> setMode(SAT));
        bri.radioButton.addActionListener(e -> setMode(BRI));
        red.radioButton.addActionListener(e -> setMode(RED));
        green.radioButton.addActionListener(e -> setMode(GREEN));
        blue.radioButton.addActionListener(e -> setMode(BLUE));

        slider.addChangeListener(e -> {
            if (sliderUpdateDepth > 0) {
                return;
            }

            int v = slider.getValue();
            ChannelUI channelUI = getSelectedChannelUI();
            try {
                sliderUpdatingSpinner = true;
                channelUI.setValue(v);
            } finally {
                sliderUpdatingSpinner = false;
            }
        });

        colorPanel.addChangeListener(e -> {
            if (colorPanelUpdateDepth > 0) {
                return;
            }

            Mode mode = getMode();
            if (mode == HUE || mode == BRI || mode == SAT) {
                float[] hsb = colorPanel.getHSB();
                setHSB(hsb[0], hsb[1], hsb[2]);
            } else {
                int[] rgb = colorPanel.getRGB();
                setRGB(rgb[0], rgb[1], rgb[2]);
            }
        });

        slider.setUI(new ColorPickerSliderUI(slider, this));
        hexField.getDocument().addDocumentListener(new HexDocumentListener());
        setMode(BRI);

        setExpertControlsVisible(showExpertControls);

        setOpacityVisible(includeOpacity);

        opacitySlider.addChangeListener(e -> {
            if (opacityUpdateDepth > 0) {
                return;
            }
            setOpacity(opacitySlider.getValue());
        });

        setOpacity(255);
        setDescendantsOpaque(this, false);

        preview.setForeground(getColor());
    }

    public void setupColorChangeListener(Consumer<Color> colorChangeListener) {
        this.colorChangeListener = colorChangeListener;
        // notify the color change listener, but only if the value is not adjusting
        opacitySlider.addChangeListener(e -> {
            if (opacityUpdateDepth == 0 && !opacitySlider.getValueIsAdjusting()) {
                colorChangeListener.accept(getColor());
            }
        });
        slider.addChangeListener(e -> {
            if (sliderUpdateDepth == 0 && !slider.getValueIsAdjusting()) {
                colorChangeListener.accept(getColor());
            }
        });
        colorPanel.addChangeListener(e -> {
            if (colorPanelUpdateDepth == 0 && !colorPanel.isAdjusting()) {
                colorChangeListener.accept(getColor());
            }
        });
    }

    private static void setDescendantsOpaque(JComponent c, boolean opaque) {
        if (c instanceof JTextField) {
            return;
        }

        c.setOpaque(opaque);
        if (c instanceof JSpinner) {
            return;
        }

        for (int a = 0; a < c.getComponentCount(); a++) {
            JComponent child = (JComponent) c.getComponent(a);
            setDescendantsOpaque(child, opaque);
        }
    }

    /**
     * This controls whether the hex field (and label) are visible or not.
     * <P>Note this lives inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
     * has been called, then calling this method makes no difference: the hex controls will be hidden.
     */
    public void setHexControlsVisible(boolean b) {
        hexLabel.setVisible(b);
        hexField.setVisible(b);
    }

    /**
     * This controls whether the preview swatch visible or not.
     * <P>Note this lives inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
     * has been called, then calling this method makes no difference: the swatch will be hidden.
     */
    public void setPreviewSwatchVisible(boolean b) {
        preview.setVisible(b);
    }

    /**
     * The labels/spinners/buttons on the right side of a <code>ColorPicker</code>
     * are optional.  This method will control whether they are shown or not.
     * <P>It may be that your users will never need or want numeric control when
     * they choose their colors, so hiding this may simplify your interface.
     *
     * @param b whether to show or hide the expert controls.
     */
    public void setExpertControlsVisible(boolean b) {
        expertControls.setVisible(b);
    }

    /**
     * @return the current HSB coordinates of this <code>ColorPicker</code>.
     * Each value is between [0,1].
     */
    public float[] getHSB() {
        return new float[]{
            hue.getValue() / 360.0f,
            sat.getValue() / 100.0f,
            bri.getValue() / 100.0f
        };
    }

    /**
     * @return the current RGB coordinates of this <code>ColorPicker</code>.
     * Each value is between [0,255].
     */
    public int[] getRGB() {
        return new int[]{
            currentRed,
            currentGreen,
            currentBlue
        };
    }

    /**
     * Returns the currently selected opacity (a float between 0 and 1).
     */
    public float getOpacity() {
        return opacitySlider.getValue() / 255.0f;
    }

    private int lastOpacity = 255;

    /**
     * Sets the currently selected opacity.
     *
     * @param v an int between 0 and 255.
     */
    public void setOpacity(int v) {
        if (v < 0 || v > 255) {
            throw new IllegalArgumentException("The opacity (" + v + ") must be between 0 and 255.");
        }
        opacityUpdateDepth++;
        try {
            opacitySlider.setValue(v);
            alpha.spinner.setValue(v);
            if (lastOpacity != v) {
                firePropertyChange(OPACITY_PROPERTY, lastOpacity, v);
                Color c = preview.getForeground();
                preview.setForeground(new Color(c.getRed(), c.getGreen(), c.getBlue(), v));
            }
            lastOpacity = v;
        } finally {
            opacityUpdateDepth--;
        }
    }

    /**
     * Sets the mode of this <code>ColorPicker</code>.
     * This is especially useful if this picker is in non-expert mode, so
     * the radio buttons are not visible for the user to directly select.
     *
     * @param mode the mode to set.
     */
    public void setMode(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        putClientProperty(MODE_PROPERTY, mode);
        hue.radioButton.setSelected(mode == HUE);
        sat.radioButton.setSelected(mode == SAT);
        bri.radioButton.setSelected(mode == BRI);
        red.radioButton.setSelected(mode == RED);
        green.radioButton.setSelected(mode == GREEN);
        blue.radioButton.setSelected(mode == BLUE);

        colorPanel.setMode(mode);
        sliderUpdateDepth++;
        try {
            slider.setValue(0);
            ChannelUI channelUI = getSelectedChannelUI();
            slider.setInverted(mode == HUE);
            int max = channelUI.getMaximum();
            slider.setMaximum(max);
            slider.setValue(channelUI.getValue());
            slider.repaint();

            if (mode == HUE || mode == SAT || mode == BRI) {
                setHSB(hue.getValue() / 360.0f,
                    sat.getValue() / 100.0f,
                    bri.getValue() / 100.0f);
            } else {
                setRGB(red.getValue(),
                    green.getValue(),
                    blue.getValue());

            }
        } finally {
            sliderUpdateDepth--;
        }
    }

    /**
     * This controls whether the radio buttons that adjust the mode are visible.
     * <P>(These buttons appear next to the spinners in the expert controls.)
     * <P>Note these live inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
     * has been called, then these will never be visible.
     */
    public void setModeControlsVisible(boolean b) {
        hue.radioButton.setVisible(b && hue.isVisible());
        sat.radioButton.setVisible(b && sat.isVisible());
        bri.radioButton.setVisible(b && bri.isVisible());
        red.radioButton.setVisible(b && red.isVisible());
        green.radioButton.setVisible(b && green.isVisible());
        blue.radioButton.setVisible(b && blue.isVisible());
        putClientProperty(MODE_CONTROLS_VISIBLE_PROPERTY, b);
    }

    /**
     * @return the current mode of this <code>ColorPicker</code>.
     * <BR>This will return <code>Mode.HUE</code>,  <code>Mode.SAT</code>,  <code>Mode.BRI</code>,
     * <code>Mode.RED</code>,  <code>Mode.GREEN</code>, or <code>Mode.BLUE</code>.
     * <P>The default mode is <code>Mode.BRI</code>, because that provides the most
     * aesthetic/recognizable color wheel.
     */
    public Mode getMode() {
        Mode m = (Mode) getClientProperty(MODE_PROPERTY);
        return m == null ? BRI : m;
    }

    /**
     * Sets the current color of this <code>ColorPicker</code>.
     * This method simply calls <code>setRGB()</code> and <code>setOpacity()</code>.
     *
     * @param c the new color to use.
     */
    public void setColor(Color c) {
        setRGB(c.getRed(), c.getGreen(), c.getBlue());
        setOpacity(c.getAlpha());
    }

    /**
     * Sets the current color of this <code>ColorPicker</code>
     *
     * @param r the red value.  Must be between [0,255].
     * @param g the green value.  Must be between [0,255].
     * @param b the blue value.  Must be between [0,255].
     */
    public void setRGB(int r, int g, int b) {
        if (r < 0 || r > 255) {
            throw new IllegalArgumentException("The red value (" + r + ") must be between [0,255].");
        }
        if (g < 0 || g > 255) {
            throw new IllegalArgumentException("The green value (" + g + ") must be between [0,255].");
        }
        if (b < 0 || b > 255) {
            throw new IllegalArgumentException("The blue value (" + b + ") must be between [0,255].");
        }

        Color lastColor = getColor();

        boolean updateRGBSpinners = spinnerUpdateDepth == 0;

        spinnerUpdateDepth++;
        colorPanelUpdateDepth++;
        int a = this.alpha.getValue();
        try {
            if (updateRGBSpinners) {
                red.setValue(r);
                green.setValue(g);
                blue.setValue(b);
            }
            preview.setForeground(new Color(r, g, b, a));
            float[] hsb = new float[3];
            Color.RGBtoHSB(r, g, b, hsb);
            hue.setValue(Math.round(hsb[0] * 360.0f));
            sat.setValue(Math.round(hsb[1] * 100.0f));
            bri.setValue(Math.round(hsb[2] * 100.0f));
            colorPanel.setRGB(r, g, b);
            updateHexField();
            updateSlider();
        } finally {
            spinnerUpdateDepth--;
            colorPanelUpdateDepth--;
        }
        currentRed = r;
        currentGreen = g;
        currentBlue = b;
        Color newColor = getColor();
        if (!lastColor.equals(newColor)) {
            firePropertyChange(SELECTED_COLOR_PROPERTY, lastColor, newColor);
        }
    }

    /**
     * @return the current <code>Color</code> this <code>ColorPicker</code> has selected.
     * <P>This is equivalent to:
     * <BR><code>int[] i = getRGB();</code>
     * <BR><code>return new Color(i[0], i[1], i[2], opacitySlider.getValue());</code>
     */
    public Color getColor() {
        int[] i = getRGB();
        return new Color(i[0], i[1], i[2], opacitySlider.getValue());
    }

    private void updateSlider() {
        sliderUpdateDepth++;
        try {
            int newValue = switch (getMode()) {
                case HUE -> hue.getValue();
                case SAT -> sat.getValue();
                case BRI -> bri.getValue();
                case RED -> red.getValue();
                case GREEN -> green.getValue();
                case BLUE -> blue.getValue();
            };
            slider.setValue(newValue);
        } finally {
            sliderUpdateDepth--;
        }
        slider.repaint();
    }

    /**
     * This returns the panel with several rows of spinner controls.
     * <P>Note you can also call methods such as <code>setRGBControlsVisible()</code> to adjust
     * which controls are showing.
     * <P>(This returns the panel this <code>ColorPicker</code> uses, so if you put it in
     * another container, it will be removed from this <code>ColorPicker</code>.)
     *
     * @return the panel with several rows of spinner controls.
     */
    public JPanel getExpertControls() {
        return expertControls;
    }

    /**
     * This shows or hides the RGB spinner controls.
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

    /**
     * This shows or hides the HSB spinner controls.
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

    /**
     * This shows or hides the alpha controls.
     * <P>Note the alpha spinner lives inside the "expert controls", so if <code>setExpertControlsVisible(false)</code>
     * has been called, then this method does not affect that spinner.
     * However, the opacity slider is <i>not</i> affected by the visibility of the expert controls.
     */
    public void setOpacityVisible(boolean b) {
        opacityLabel.setVisible(b);
        opacitySlider.setVisible(b);
        alpha.label.setVisible(b);
        alpha.spinner.setVisible(b);
    }

    /**
     * @return the <code>ColorPickerPanel</code> this <code>ColorPicker</code> displays.
     */
    public ColorPickerPanel getColorPanel() {
        return colorPanel;
    }

    /**
     * Sets the current color of this <code>ColorPicker</code>
     *
     * @param h the hue value.
     * @param s the saturation value.  Must be between [0,1].
     * @param b the blue value.  Must be between [0,1].
     */
    public void setHSB(float h, float s, float b) {
        if (Float.isInfinite(h) || Float.isNaN(h)) {
            throw new IllegalArgumentException("The hue value (" + h + ") is not a valid number.");
        }
        //hue is cyclic, so it can be any value:
        while (h < 0) {
            h++;
        }
        while (h > 1) {
            h--;
        }

        if (s < 0 || s > 1) {
            throw new IllegalArgumentException("The saturation value (" + s + ") must be between [0,1]");
        }
        if (b < 0 || b > 1) {
            throw new IllegalArgumentException("The brightness value (" + b + ") must be between [0,1]");
        }

        Color lastColor = getColor();

        boolean updateHSBSpinners = spinnerUpdateDepth == 0;
        spinnerUpdateDepth++;
        colorPanelUpdateDepth++;
        try {
            if (updateHSBSpinners) {
                hue.setValue(Math.round(h * 360.0f));
                sat.setValue(Math.round(s * 100.0f));
                bri.setValue(Math.round(b * 100.0f));
            }

            Color c = new Color(Color.HSBtoRGB(h, s, b));
            int a = this.alpha.getValue();
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
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
            spinnerUpdateDepth--;
            colorPanelUpdateDepth--;
        }
        Color newColor = getColor();
        if (!lastColor.equals(newColor)) {
            firePropertyChange(SELECTED_COLOR_PROPERTY, lastColor, newColor);
        }
    }

    private void updateHexField() {
        hexFieldUpdateDepth++;
        try {
            int r = red.getValue();
            int g = green.getValue();
            int b = blue.getValue();

            int rgb = (r << 16) + (g << 8) + b;
            String s = Integer.toHexString(rgb).toUpperCase(Locale.ROOT);
            while (s.length() < 6) {
                s = "0" + s;
            }
            if (!hexField.getText().equalsIgnoreCase(s)) {
                hexField.setText(s);
            }
        } finally {
            hexFieldUpdateDepth--;
        }
    }

    /**
     * The UI controls for a single color channel.
     */
    class ChannelUI {
        final JRadioButton radioButton = new JRadioButton();
        final JSpinner spinner;
        final JLabel label;

        public ChannelUI(String text, int max) {
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, max, 5));
            label = new JLabel(text);
        }

        public void setValue(int i) {
            spinner.setValue(i);
        }

        public int getMaximum() {
            return ((Number) ((SpinnerNumberModel) spinner.getModel()).getMaximum()).intValue();
        }

        public int getValue() {
            return ((Number) spinner.getValue()).intValue();
        }

        public boolean isVisible() {
            return label.isVisible();
        }

        public void setVisible(boolean b) {
            boolean radioButtonsAllowed = true;
            Boolean modeVisible = (Boolean) getClientProperty(MODE_CONTROLS_VISIBLE_PROPERTY);
            if (modeVisible != null) {
                radioButtonsAllowed = modeVisible;
            }

            radioButton.setVisible(b && radioButtonsAllowed);
            spinner.setVisible(b);
            label.setVisible(b);
        }
    }
}
