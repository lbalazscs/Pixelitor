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
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * The zoom widget in the status bar
 */
public class ZoomComponent extends JPanel implements ImageSwitchListener {
    public static ZoomComponent INSTANCE = new ZoomComponent();
    private final JSlider zoomSlider;
    private final JLabel zoomDisplay;
    private final JLabel zoomLabel;

    private ZoomComponent() {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));

        ZoomLevel[] values = ZoomLevel.values();

        zoomSlider = new JSlider(0, values.length - 1);
        // normally the JSlider vertical size would be 21,
        // but let's save 4 pixels so that the status bar height
        // does not increase because of this control
        zoomSlider.setPreferredSize(new Dimension(190, 17));

        zoomDisplay = new JLabel("100%");
        double preferredHeight = zoomDisplay.getPreferredSize().getHeight();
        Dimension preferredSize = new Dimension(70, (int) preferredHeight);
        zoomDisplay.setPreferredSize(preferredSize);

        zoomSlider.addChangeListener(e -> {
            int selectedZoomIndex = zoomSlider.getValue();
            ZoomLevel value = values[selectedZoomIndex];
            ImageComponent activeIC = ImageComponents.getActiveIC();
            if (activeIC != null) {
                activeIC.setZoom(value, false);
                setNewZoomText(value);
            }
        });

        zoomLabel = new JLabel("Zoom: ");
        add(zoomLabel);
        add(zoomSlider);
        add(zoomDisplay);

        setLookIfNoImage();
        ImageComponents.addImageSwitchListener(this);
    }

    private void setLookIfNoImage() {
        zoomLabel.setEnabled(false);
        zoomSlider.setEnabled(false);
        zoomDisplay.setText("");
    }

    public void setToNewZoom(ZoomLevel newZoom) {
        zoomLabel.setEnabled(true);
        zoomSlider.setEnabled(true);
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
}
