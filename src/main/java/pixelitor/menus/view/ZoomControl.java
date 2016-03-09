/*
 * Copyright 2016 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.menus.view;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FlowLayout;

import static pixelitor.tools.AutoZoomButtons.ACTUAL_PIXELS_ACTION;
import static pixelitor.tools.AutoZoomButtons.ACTUAL_PIXELS_TOOLTIP;
import static pixelitor.tools.AutoZoomButtons.FIT_SCREEN_ACTION;
import static pixelitor.tools.AutoZoomButtons.FIT_SCREEN_TOOLTIP;

/**
 * The zoom widget in the status bar
 */
public class ZoomControl extends JPanel implements ImageSwitchListener {
    public static final ZoomControl INSTANCE = new ZoomControl();

    private static final int PREFERRED_HEIGHT = 17;
    private final JSlider zoomSlider;
    private final JLabel zoomDisplay;
    private final JLabel zoomLabel;
    private final JButton fitButton;
    private final JButton actualPixelsButton;

    private boolean enabled = true;

    private ZoomControl() {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));

        ZoomLevel[] values = ZoomLevel.values();

        zoomSlider = new JSlider(0, values.length - 1);
        // normally the JSlider vertical size would be 21,
        // but let's save 4 pixels so that the status bar height
        // does not increase because of this control
        zoomSlider.setPreferredSize(new Dimension(200, PREFERRED_HEIGHT));

        zoomDisplay = new JLabel("100%");
        double preferredHeight = zoomDisplay.getPreferredSize().getHeight();
        Dimension preferredSize = new Dimension(70, (int) preferredHeight);
        zoomDisplay.setPreferredSize(preferredSize);

        zoomSlider.addChangeListener(e -> {
            int selectedZoomIndex = zoomSlider.getValue();
            ZoomLevel value = values[selectedZoomIndex];
            ImageComponent activeIC = ImageComponents.getActiveIC();
            if (activeIC != null) {
                activeIC.setZoomAtCenter(value);
                setNewZoomText(value);
            }
        });

        zoomLabel = new JLabel("  Zoom: ");

        add(zoomLabel);
        add(zoomSlider);
        add(zoomDisplay);

        Dimension buttonSize = new Dimension(60, PREFERRED_HEIGHT);
        fitButton = addZoomButton(buttonSize, "Fit", FIT_SCREEN_ACTION, FIT_SCREEN_TOOLTIP);
        actualPixelsButton = addZoomButton(buttonSize, "100%", ACTUAL_PIXELS_ACTION, ACTUAL_PIXELS_TOOLTIP);

        setLookIfNoImage();
        ImageComponents.addImageSwitchListener(this);
    }

    private JButton addZoomButton(Dimension buttonSize, String text, Action action, String tooltip) {
        JButton b = new JButton(text) {
            boolean shiftLocation = true;

            @Override
            public void setLocation(int x, int y) {
                if (shiftLocation) {
                    super.setLocation(x, y - 1);
                    shiftLocation = false;
                } else {
                    super.setLocation(x, y);
                }
            }

            @Override
            public void setSize(int width, int height) {
                super.setSize(width, height + 2);
            }
        };

        b.addActionListener(action);
        b.setToolTipText(tooltip);
        b.setPreferredSize(buttonSize);

//        b.setBorder(null);
        b.setBorder(BorderFactory.createEmptyBorder());

//        b.setBorderPainted(false);
//        b.setMargin(new Insets(0,0,0,0));
//        b.setMaximumSize(buttonSize);
//        b.putClientProperty("JComponent.sizeVariant", "large");

//        UIDefaults def = new UIDefaults();
//        def.put("Button.contentMargins", new Insets(2,8,2,8));
//        b.putClientProperty("Nimbus.Overrides", def);

        add(b);
        return b;
    }

    private void setLookIfNoImage() {
        setEnabled(false);

        zoomDisplay.setText("");
    }

    public void setToNewZoom(ZoomLevel newZoom) {
        setEnabled(true);

        zoomSlider.setValue(newZoom.ordinal());
        setNewZoomText(newZoom);
    }

    private void setNewZoomText(ZoomLevel value) {
        zoomDisplay.setText(" " + value.toString());
    }

    @Override
    public void noOpenImageAnymore() {
        setLookIfNoImage();
    }

    @Override
    public void newImageOpened(Composition comp) {
        ZoomLevel zoomLevel = comp.getIC().getZoomLevel();
        setToNewZoom(zoomLevel);
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        setToNewZoom(newIC.getZoomLevel());
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            zoomLabel.setEnabled(enabled);
            zoomSlider.setEnabled(enabled);
            fitButton.setEnabled(enabled);
            actualPixelsButton.setEnabled(enabled);
        }
        this.enabled = enabled;
    }
}
