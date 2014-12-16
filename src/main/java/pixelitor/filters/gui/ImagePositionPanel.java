/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.gui;

import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * The GUI component for an ImagePositionParam
 */
public class ImagePositionPanel extends JPanel implements ParamGUI {
    private final ImagePositionParam model;
    private final RangeParam xSliderModel;
    private final RangeParam ySliderModel;
    private final ImagePositionSelector imagePositionSelector;
    private boolean slidersMovedByUser = true;

    public ImagePositionPanel(final ImagePositionParam model, int defaultX, int defaultY) {
        this.model = model;

        xSliderModel = new RangeParam("Horizontal Position (%)", 0, 100, defaultX, true, SliderSpinner.TextPosition.NORTH);
        ySliderModel = new RangeParam("Vertical Position (%)", 0, 100, defaultY, true, SliderSpinner.TextPosition.NORTH);

        setBorder(BorderFactory.createTitledBorder(model.getName()));
        setLayout(new BorderLayout(10, 0));

        // add the image position selector
        imagePositionSelector = new ImagePositionSelector(this, model, 100);
        add(imagePositionSelector, BorderLayout.WEST);

        // add the two sliders
        Box verticalBox = Box.createVerticalBox();
        JComponent xSlider = xSliderModel.createGUI();
        verticalBox.add(xSlider);
        JComponent ySlider = ySliderModel.createGUI();
        verticalBox.add(ySlider);
        add(verticalBox, BorderLayout.CENTER);

        // if one of the sliders was moved by the users, update the
        // image position selector and run the filter
        xSliderModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (slidersMovedByUser) {
                    model.setRelativeX(xSliderModel.getValue() / 100.0f);
                    imagePositionSelector.repaint();
                    if (!xSliderModel.getValueIsAdjusting()) {
                        model.getAdjustingListener().paramAdjusted();
                    }
                }
            }
        });
        ySliderModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (slidersMovedByUser) {
                    model.setRelativeY(ySliderModel.getValue() / 100.0f);
                    imagePositionSelector.repaint();
                    if (!ySliderModel.getValueIsAdjusting()) {
                        model.getAdjustingListener().paramAdjusted();
                    }
                }
            }
        });

        Dimension preferredSize = getPreferredSize();
        Dimension sliderPreferredSize = xSlider.getPreferredSize();
        setPreferredSize(new Dimension(sliderPreferredSize.width, preferredSize.height));
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
}
