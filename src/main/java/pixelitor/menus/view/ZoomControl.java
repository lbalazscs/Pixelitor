/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.gui.AutoZoom.ACTUAL_PIXELS_ACTION;
import static pixelitor.gui.AutoZoom.ACTUAL_PIXELS_TOOLTIP;
import static pixelitor.gui.AutoZoom.FIT_SPACE_ACTION;
import static pixelitor.gui.AutoZoom.FIT_SPACE_TOOLTIP;
import static pixelitor.gui.GUIText.ZOOM;
import static pixelitor.menus.view.ZoomLevel.zoomLevels;

/**
 * The zoom widget in the status bar
 */
public class ZoomControl extends JPanel implements ViewActivationListener {
    private static final ZoomControl INSTANCE = new ZoomControl();

    private static final int PREFERRED_HEIGHT = 17;
    private static final int ZOOM_PERCENTAGE_WIDTH = 70;
    private static final int ZOOM_SLIDER_WIDTH = 200;
    private static final int BUTTON_WIDTH = 60;
    
    private final JSlider zoomSlider;
    private final JLabel zoomPercentageLabel;
    private final JLabel zoomTextLabel;
    private final JButton fitButton;
    private final JButton actualPixelsButton;

    private ZoomControl() {
        super(new FlowLayout(LEFT, 0, 0));

        zoomSlider = new JSlider(0, zoomLevels.length - 1);

        // Since this control is inside the status bar, various hacks
        // try to ensure that it doesn't increase the status bar height.
        // Normally the JSlider vertical size in Nimbus would be 21.
        zoomSlider.setPreferredSize(new Dimension(ZOOM_SLIDER_WIDTH, PREFERRED_HEIGHT));

        zoomPercentageLabel = new JLabel("100%");
        double preferredHeight = zoomPercentageLabel.getPreferredSize().getHeight();
        Dimension preferredSize = new Dimension(ZOOM_PERCENTAGE_WIDTH, (int) preferredHeight);
        zoomPercentageLabel.setPreferredSize(preferredSize);

        zoomSlider.addChangeListener(e -> Views.onActive(this::applyZoomToView));

        zoomTextLabel = new JLabel("  " + ZOOM + ": ");

        add(zoomTextLabel);
        add(zoomSlider);
        add(zoomPercentageLabel);

        Dimension buttonSize = new Dimension(BUTTON_WIDTH, PREFERRED_HEIGHT);
        fitButton = addZoomButton(buttonSize, "Fit",
            FIT_SPACE_ACTION, FIT_SPACE_TOOLTIP);
        actualPixelsButton = addZoomButton(buttonSize, "100%",
            ACTUAL_PIXELS_ACTION, ACTUAL_PIXELS_TOOLTIP);

        disableAndClear();
        Views.addActivationListener(this);
    }

    private JButton addZoomButton(Dimension buttonSize, String text,
                                  Action action, String tooltip) {
        // the overrides are a UI hack to minimize the vertical
        // space occupied by the button (in Nimbus LaF)
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

    private void disableAndClear() {
        setEnabled(false);

        zoomPercentageLabel.setText("");
    }

    private void applyZoomToView(View view) {
        ZoomLevel newZoom = zoomLevels[zoomSlider.getValue()];
        view.setZoom(newZoom);
        setZoomText(newZoom);
    }

    /**
     * Updates the UI when the zoom changes for external reasons.
     */
    public void syncFromExternalZoom(ZoomLevel newZoom) {
        setEnabled(true);

        zoomSlider.setValue(newZoom.getSliderValue());
        setZoomText(newZoom);
    }

    private void setZoomText(ZoomLevel zoomLevel) {
        zoomPercentageLabel.setText(" " + zoomLevel);
    }

    @Override
    public void allViewsClosed() {
        disableAndClear();
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        syncFromExternalZoom(newView.getZoomLevel());
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);
            zoomTextLabel.setEnabled(enabled);
            zoomSlider.setEnabled(enabled);
            fitButton.setEnabled(enabled);
            actualPixelsButton.setEnabled(enabled);
        }
    }

    public static ZoomControl get() {
        return INSTANCE;
    }
}
