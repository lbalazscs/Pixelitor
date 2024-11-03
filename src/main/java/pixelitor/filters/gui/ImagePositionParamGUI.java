/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.WEST;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NORTH;

/**
 * The GUI component for an {@link ImagePositionParam}.
 */
public class ImagePositionParamGUI extends JPanel implements ParamGUI {
    private static final int HORIZONTAL_GAP = 10;
    
    private final ImagePositionParam model;
    private final RangeParam xSliderModel;
    private final RangeParam ySliderModel;

    private final ImagePositionSelector positionSelector;
    private final JComponent xSlider;
    private final JComponent ySlider;

    // flag to prevent infinite update loops between GUI components
    private boolean sliderUpdatesEnabled = true;

    public ImagePositionParamGUI(ImagePositionParam model,
                                 double defaultRelativeX,
                                 double defaultRelativeY) {
        super(new BorderLayout(HORIZONTAL_GAP, 0));
        setBorder(createTitledBorder(model.getName()));
        this.model = model;

        // initialize sliders
        int decimalPlaces = model.getDecimalPlaces();
        xSliderModel = new RangeParam("Horizontal Position (%)",
            0, defaultRelativeX * 100, 100, true, NORTH);
        xSliderModel.setDecimalPlaces(decimalPlaces);
        ySliderModel = new RangeParam("Vertical Position (%)",
            0, defaultRelativeY * 100, 100, true, NORTH);
        ySliderModel.setDecimalPlaces(decimalPlaces);

        // add the thumbnail-based selector
        positionSelector = new ImagePositionSelector(this, model, 100);
        add(positionSelector, WEST);

        // add the two sliders
        Box verticalBox = Box.createVerticalBox();
        xSlider = xSliderModel.createGUI();
        verticalBox.add(xSlider);
        ySlider = ySliderModel.createGUI();
        verticalBox.add(ySlider);
        add(verticalBox, CENTER);

        setupPreferredSize();

        linkSliderChangesToModel(model);
    }

    private void setupPreferredSize() {
        Dimension currentSize = getPreferredSize();
        Dimension sliderSize = xSlider.getPreferredSize();
        setPreferredSize(new Dimension(
            sliderSize.width,
            currentSize.height));
    }

    // if one of the sliders was moved by the users, update the
    // image position selector and run the filter
    private void linkSliderChangesToModel(ImagePositionParam model) {
        xSliderModel.addChangeListener(e -> xSliderChanged(model));
        ySliderModel.addChangeListener(e -> ySliderChanged(model));
    }

    private void xSliderChanged(ImagePositionParam model) {
        if (sliderUpdatesEnabled) {
            model.setRelativeX(xSliderModel.getPercentage(),
                xSliderModel.getValueIsAdjusting());
            positionSelector.repaint();
        }
    }

    private void ySliderChanged(ImagePositionParam model) {
        if (sliderUpdatesEnabled) {
            model.setRelativeY(ySliderModel.getPercentage(),
                ySliderModel.getValueIsAdjusting());
            positionSelector.repaint();
        }
    }

    /**
     * Updates the sliders based on the model changes.
     * This doesn't trigger the running of the filter.
     */
    public void updateSlidersFromModel() {
        sliderUpdatesEnabled = false;
        try {
            updateSlider(xSliderModel, model.getRelativeX() * 100);
            updateSlider(ySliderModel, model.getRelativeY() * 100);
        } finally {
            sliderUpdatesEnabled = true;
        }
    }

    private static void updateSlider(RangeParam sliderModel, double newValue) {
        if (newValue != sliderModel.getValue()) {
            sliderModel.setValue(newValue, true);
        }
    }

    @Override
    public void updateGUI() {
        updateSlidersFromModel();
        positionSelector.repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        positionSelector.setEnabled(enabled);
        xSlider.setEnabled(enabled);
        ySlider.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void setToolTip(String tip) {
        positionSelector.setToolTipText(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
