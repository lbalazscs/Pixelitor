/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NORTH;

/**
 * The GUI component for an {@link ImagePositionParam}
 */
public class ImagePositionParamGUI extends JPanel implements ParamGUI {
    private final ImagePositionParam model;
    private final RangeParam xSliderModel;
    private final RangeParam ySliderModel;

    private final ImagePositionSelector imgPosSelector;
    private final JComponent xSlider;
    private final JComponent ySlider;

    private boolean slidersMovedByUser = true;

    public ImagePositionParamGUI(ImagePositionParam model, double defaultX, double defaultY) {
        this.model = model;

        xSliderModel = new RangeParam(
            "Horizontal Position (%)", 0, defaultX, 100, true, NORTH);
        xSliderModel.setDecimalPlaces(model.getDecimalPlaces());
        ySliderModel = new RangeParam(
            "Vertical Position (%)", 0, defaultY, 100, true, NORTH);
        ySliderModel.setDecimalPlaces(model.getDecimalPlaces());

        setBorder(createTitledBorder(model.getName()));
        setLayout(new BorderLayout(10, 0));

        // add the image position selector
        imgPosSelector = new ImagePositionSelector(this, model, 100);
        add(imgPosSelector, WEST);

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
        Dimension origPS = getPreferredSize();
        Dimension sliderPS = xSlider.getPreferredSize();
        setPreferredSize(new Dimension(sliderPS.width, origPS.height));
    }

    // if one of the sliders was moved by the users, update the
    // image position selector and run the filter
    private void linkSliderChangesToModel(ImagePositionParam model) {
        xSliderModel.addChangeListener(e -> onXSliderChange(model));
        ySliderModel.addChangeListener(e -> onYSliderChange(model));
    }

    private void onXSliderChange(ImagePositionParam model) {
        if (slidersMovedByUser) {
            model.setRelativeX(xSliderModel.getPercentage(),
                xSliderModel.getValueIsAdjusting());
            imgPosSelector.repaint();
        }
    }

    private void onYSliderChange(ImagePositionParam model) {
        if (slidersMovedByUser) {
            model.setRelativeY(ySliderModel.getPercentage(),
                ySliderModel.getValueIsAdjusting());
            imgPosSelector.repaint();
        }
    }

    /**
     * Updates the sliders based on the model changes.
     * This does not trigger the running of the filter
     */
    public void updateSlidersFromModel() {
        slidersMovedByUser = false;

        int xValue = xSliderModel.getValue();
        double modelXValue = model.getRelativeX() * 100;
        if (modelXValue != xValue) {
            xSliderModel.setValue(modelXValue, true);
        }

        int yValue = ySliderModel.getValue();
        double modelYValue = model.getRelativeY() * 100;
        if (modelYValue != yValue) {
            ySliderModel.setValue(modelYValue, true);
        }

        slidersMovedByUser = true;
    }

    @Override
    public void updateGUI() {
        updateSlidersFromModel();
        imgPosSelector.repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        imgPosSelector.setEnabled(enabled);
        xSlider.setEnabled(enabled);
        ySlider.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void setToolTip(String tip) {
        imgPosSelector.setToolTipText(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
