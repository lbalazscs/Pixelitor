/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
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

package pixelitor.filters.levels.gui;

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.levels.GrayScaleLookup;
import pixelitor.utils.CardPanelWithCombo;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

public class OneChannelLevelsPanel extends CardPanelWithCombo.Card implements ParamAdjustmentListener {
    //    private String channelName;
    private final Collection<SliderSpinner> sliders = new ArrayList<>();
    private final Box box = Box.createVerticalBox();

    private GrayScaleLookup adjustment = GrayScaleLookup.getDefaultAdjustment();
    private static final int BLACK_DEFAULT = 0;
    private static final int WHITE_DEFAULT = 255;
    private static final Color DARK_CYAN = new Color(0, 128, 128);
    private static final Color LIGHT_PINK = new Color(255, 128, 128);
    private static final Color DARK_PURPLE = new Color(128, 0, 128);
    private static final Color LIGHT_GREEN = new Color(128, 255, 128);
    private static final Color DARK_YELLOW_GREEN = new Color(128, 128, 0);
    private static final Color LIGHT_BLUE = new Color(128, 128, 255);
    private static final Color DARK_BLUE = new Color(0, 0, 128);
    private static final Color LIGHT_YELLOW = new Color(255, 255, 128);
    private static final Color DARK_GREEN = new Color(0, 128, 0);
    private static final Color LIGHT_PURPLE = new Color(255, 128, 255);
    private static final Color DARK_RED = new Color(128, 0, 0);
    private static final Color LIGHT_CYAN = new Color(128, 255, 128);

    private final SliderSpinner inputBlackSlider;
    private final SliderSpinner inputWhiteSlider;
    private final SliderSpinner outputBlackSlider;
    private final SliderSpinner outputWhiteSlider;
    private final GrayScaleAdjustmentChangeListener grayScaleAdjustmentChangeListener;

    public OneChannelLevelsPanel(Type type, GrayScaleAdjustmentChangeListener grayScaleAdjustmentChangeListener) {
        super(type.getName());
        this.grayScaleAdjustmentChangeListener = grayScaleAdjustmentChangeListener;
        add(box);

        RangeParam inputBlackParam = new RangeParam("Input Dark", 0, 255, BLACK_DEFAULT);
        inputBlackParam.setAdjustmentListener(this);
        inputBlackSlider = new SliderSpinner(Color.GRAY, type.getBackColor(), inputBlackParam);

        RangeParam inputWhiteParam = new RangeParam("Input Light", 0, 255, WHITE_DEFAULT);
        inputWhiteParam.setAdjustmentListener(this);
        inputWhiteSlider = new SliderSpinner(type.getWhiteColor(), Color.GRAY, inputWhiteParam);

        RangeParam outputBlackParam = new RangeParam("Output Dark", 0, 255, BLACK_DEFAULT);
        outputBlackParam.setAdjustmentListener(this);
        outputBlackSlider = new SliderSpinner(Color.GRAY, type.getWhiteColor(), outputBlackParam);

        RangeParam outputWhiteParam = new RangeParam("Output Light", 0, 255, WHITE_DEFAULT);
        outputWhiteParam.setAdjustmentListener(this);
        outputWhiteSlider = new SliderSpinner(type.getBackColor(), Color.GRAY, outputWhiteParam);

        addSliderSpinner(inputBlackSlider);
        addSliderSpinner(inputWhiteSlider);
        addSliderSpinner(outputBlackSlider);
        addSliderSpinner(outputWhiteSlider);
    }

    public void resetToDefaultSettings() {
        for (SliderSpinner slider : sliders) {
            slider.resetToDefaultSettings();
        }
        updateAdjustment();
    }

    private void addSliderSpinner(SliderSpinner sp) {
        box.add(sp);
        sliders.add(sp);
    }

    public GrayScaleLookup getAdjustment() {
        return adjustment;
    }

    @Override
    public void paramAdjusted() {
        updateAdjustment();

        grayScaleAdjustmentChangeListener.grayScaleAdjustmentHasChanged();
    }

    private void updateAdjustment() {
        int inputBlackValue = inputBlackSlider.getCurrentValue();
        int inputWhiteValue = inputWhiteSlider.getCurrentValue();
        int outputBlackValue = outputBlackSlider.getCurrentValue();
        int outputWhiteValue = outputWhiteSlider.getCurrentValue();

        adjustment = new GrayScaleLookup(inputBlackValue, inputWhiteValue, outputBlackValue, outputWhiteValue);
    }

    enum Type {
        RGB {
            @Override
            public String getName() {
                return "Red, Green, Blue";
            }

            @Override
            Color getBackColor() {
                return Color.BLACK;
            }

            @Override
            Color getWhiteColor() {
                return Color.WHITE;
            }
        }, R {
            @Override
            public String getName() {
                return "Red";
            }

            @Override
            Color getBackColor() {
                return DARK_CYAN;
            }

            @Override
            Color getWhiteColor() {
                return LIGHT_PINK;
            }
        }, G {
            @Override
            public String getName() {
                return "Green";
            }

            @Override
            Color getBackColor() {
                return DARK_PURPLE;
            }

            @Override
            Color getWhiteColor() {
                return LIGHT_GREEN;
            }
        }, B {
            @Override
            public String getName() {
                return "Blue";
            }

            @Override
            Color getBackColor() {
                return DARK_YELLOW_GREEN;
            }

            @Override
            Color getWhiteColor() {
                return LIGHT_BLUE;
            }
        }, RG {
            @Override
            public String getName() {
                return "Red, Green";
            }

            @Override
            Color getBackColor() {
                return DARK_BLUE;
            }

            @Override
            Color getWhiteColor() {
                return LIGHT_YELLOW;
            }
        }, RB {
            @Override
            public String getName() {
                return "Red, Blue";
            }

            @Override
            Color getBackColor() {
                return DARK_GREEN;
            }

            @Override
            Color getWhiteColor() {
                return LIGHT_PURPLE;
            }
        }, GB {
            @Override
            public String getName() {
                return "Green, Blue";
            }

            @Override
            Color getBackColor() {
                return DARK_RED;
            }

            @Override
            Color getWhiteColor() {
                return LIGHT_CYAN;
            }
        };

        abstract String getName();

        abstract Color getBackColor();

        abstract Color getWhiteColor();
    }
}
