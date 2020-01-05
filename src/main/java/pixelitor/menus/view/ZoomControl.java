/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.utils.CompActivationListener;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.gui.AutoZoom.ACTUAL_PIXELS_ACTION;
import static pixelitor.gui.AutoZoom.FIT_SPACE_ACTION;
import static pixelitor.menus.view.ZoomMenu.ACTUAL_PIXELS_TOOLTIP;
import static pixelitor.menus.view.ZoomMenu.FIT_SPACE_TOOLTIP;

/**
 * The zoom widget in the status bar
 */
public class ZoomControl extends JPanel implements CompActivationListener {

    public static final ZoomControl INSTANCE = new ZoomControl();

    private static final int PREFERRED_HEIGHT = 17;
    private final JSlider zoomSlider;
    private final JLabel zoomDisplay;
    private final JLabel zoomLabel;
    private final JButton fitButton;
    private final JButton actualPixelsButton;

    private boolean enabled = true;
    private final ZoomLevel[] zoomLevels = ZoomLevel.values();

    private ZoomControl() {
        super(new FlowLayout(LEFT, 0, 0));

        zoomSlider = new JSlider(0, zoomLevels.length - 1);

        // Make sure that the status bar height does not increase because of this control.
        // Normally the JSlider vertical size in Nimbus would be 21.
        zoomSlider.setPreferredSize(new Dimension(200, PREFERRED_HEIGHT));

        zoomDisplay = new JLabel("100%");
        double preferredHeight = zoomDisplay.getPreferredSize().getHeight();
        Dimension preferredSize = new Dimension(70, (int) preferredHeight);
        zoomDisplay.setPreferredSize(preferredSize);

        zoomSlider.addChangeListener(e ->
                OpenImages.onActiveView(
                        this::zoomAccordingToTheSlider));

        zoomLabel = new JLabel("  Zoom: ");

        add(zoomLabel);
        add(zoomSlider);
        add(zoomDisplay);

        Dimension buttonSize = new Dimension(60, PREFERRED_HEIGHT);
        fitButton = addZoomButton(buttonSize, "Fit",
                FIT_SPACE_ACTION, FIT_SPACE_TOOLTIP);
        actualPixelsButton = addZoomButton(buttonSize, "100%",
                ACTUAL_PIXELS_ACTION, ACTUAL_PIXELS_TOOLTIP);

        setLookIfNoImage();
        OpenImages.addActivationListener(this);
    }

    private void zoomAccordingToTheSlider(View view) {
        int sliderValue = zoomSlider.getValue();
        ZoomLevel zoomLevel = zoomLevels[sliderValue];
        view.setZoomAtCenter(zoomLevel);
        setNewZoomText(zoomLevel);
    }

    private JButton addZoomButton(Dimension buttonSize, String text,
                                  Action action, String tooltip) {
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

        b.setBorder(createEmptyBorder());

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

    private void setNewZoomText(ZoomLevel zoomLevel) {
        zoomDisplay.setText(" " + zoomLevel);
    }

    @Override
    public void allCompsClosed() {
        setLookIfNoImage();
    }

    @Override
    public void compActivated(View oldView, View newView) {
        setToNewZoom(newView.getZoomLevel());
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
