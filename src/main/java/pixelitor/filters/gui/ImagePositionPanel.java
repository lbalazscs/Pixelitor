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

package pixelitor.filters.gui;

import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NORTH;

/**
 * The GUI component for an ImagePositionParam
 */
public class ImagePositionPanel extends JPanel implements ParamGUI {
    private final ImagePositionParam model;
    private final RangeParam xSliderModel;
    private final RangeParam ySliderModel;
    private final ImagePositionSelector imagePositionSelector;
    private boolean slidersMovedByUser = true;
    private final JComponent xSlider;
    private final JComponent ySlider;

    public ImagePositionPanel(ImagePositionParam model, int defaultX, int defaultY) {
        this.model = model;

        xSliderModel = new RangeParam("Horizontal Position (%)", 0, defaultX, 100, AddDefaultButton.YES, NORTH);
        ySliderModel = new RangeParam("Vertical Position (%)", 0, defaultY, 100, AddDefaultButton.YES, NORTH);

        setBorder(BorderFactory.createTitledBorder(model.getName()));
        setLayout(new BorderLayout(10, 0));

        // add the image position selector
        imagePositionSelector = new ImagePositionSelector(this, model, 100);
        add(imagePositionSelector, BorderLayout.WEST);

        // add the two sliders
        Box verticalBox = Box.createVerticalBox();
        xSlider = new SliderSpinner(xSliderModel, NORTH, AddDefaultButton.YES);
        verticalBox.add(xSlider);
        ySlider = new SliderSpinner(ySliderModel, NORTH, AddDefaultButton.YES);
        verticalBox.add(ySlider);
        add(verticalBox, BorderLayout.CENTER);

        Dimension preferredSize = getPreferredSize();
        Dimension sliderPreferredSize = xSlider.getPreferredSize();
        setPreferredSize(new Dimension(sliderPreferredSize.width, preferredSize.height));

        linkSliderChangesToModel(model);
    }

    // if one of the sliders was moved by the users, update the
    // image position selector and run the filter
    private void linkSliderChangesToModel(ImagePositionParam model) {
        xSliderModel.addChangeListener(e -> {
            if (slidersMovedByUser) {
                model.setRelativeX(xSliderModel.getValue() / 100.0f, xSliderModel.getValueIsAdjusting());
                imagePositionSelector.repaint();
            }
        });
        ySliderModel.addChangeListener(e -> {
            if (slidersMovedByUser) {
                model.setRelativeY(ySliderModel.getValue() / 100.0f, ySliderModel.getValueIsAdjusting());
                imagePositionSelector.repaint();
            }
        });
    }

    /**
     * Updates the sliders based on the model changes.
     * This does not trigger the execution of the filter
     */
    public void updateSlidersFromModel() {
        slidersMovedByUser = false;
        int xValue = xSliderModel.getValue();
        int modelXValue = (int) (model.getRelativeX() * 100);
        if (modelXValue != xValue) {
            xSliderModel.setValue(modelXValue);
        }
        int yValue = ySliderModel.getValue();
        int modelYValue = (int) (model.getRelativeY() * 100);
        if (modelYValue != yValue) {
            ySliderModel.setValue(modelYValue);
        }
        slidersMovedByUser = true;
    }

    @Override
    public void updateGUI() {
        updateSlidersFromModel();
        imagePositionSelector.repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        imagePositionSelector.setEnabled(enabled);
        xSlider.setEnabled(enabled);
        ySlider.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void setToolTip(String tip) {
        // TODO should have some generic tooltip
    }
}
