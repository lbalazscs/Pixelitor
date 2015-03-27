/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.filters;

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.gui.ActionParam;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ChannelMixerAdjustments;
import pixelitor.filters.gui.GUIParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static pixelitor.utils.SliderSpinner.TextPosition.NONE;

public class ChannelMixer extends FilterWithParametrizedGUI {
    private static final int MIN_PERCENT = -200;
    private static final int MAX_PERCENT = 200;

    private final RangeParam redFromRed = new RangeParam("<html><b><font color=red>Red</font></b> from <font color=red>red</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 100, true, NONE);
    private final RangeParam redFromGreen = new RangeParam("<html><b><font color=red>Red</font></b> from <font color=green>green</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 0, true, NONE);
    private final RangeParam redFromBlue = new RangeParam("<html><b><font color=red>Red</font></b> from <font color=blue>blue</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 0, true, NONE);

    private final RangeParam greenFromRed = new RangeParam("<html><b><font color=green>Green</font></b> from <font color=red>red</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 0, true, NONE);
    private final RangeParam greenFromGreen = new RangeParam("<html><b><font color=green>Green</font></b> from <font color=green>green</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 100, true, NONE);
    private final RangeParam greenFromBlue = new RangeParam("<html><b><font color=green>Green</font></b> from <font color=blue>blue</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 0, true, NONE);

    private final RangeParam blueFromRed = new RangeParam("<html><b><font color=blue>Blue</font></b> from <font color=red>red</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 0, true, NONE);
    private final RangeParam blueFromGreen = new RangeParam("<html><b><font color=blue>Blue</font></b> from <font color=green>green</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 0, true, NONE);
    private final RangeParam blueFromBlue = new RangeParam("<html><b><font color=blue>Blue</font></b> from <font color=blue>blue</font> (%):</html>", MIN_PERCENT, MAX_PERCENT, 100, true, NONE);

    private final ActionListener normalizeAction = e -> {
        getParamSet().startPresetAdjusting();

        normalizeChannel(redFromRed, redFromGreen, redFromBlue);
        normalizeChannel(greenFromRed, greenFromGreen, greenFromBlue);
        normalizeChannel(blueFromRed, blueFromGreen, blueFromBlue);

        getParamSet().endPresetAdjusting(false);
    };

    private final Action switchRedGreen
            = new AbstractAction("switch red-green") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getParamSet().startPresetAdjusting();

            redFromRed.setValue(0);
            redFromGreen.setValue(100);
            redFromBlue.setValue(0);

            greenFromRed.setValue(100);
            greenFromGreen.setValue(0);
            greenFromBlue.setValue(0);

            blueFromRed.setValue(0);
            blueFromGreen.setValue(0);
            blueFromBlue.setValue(100);

            getParamSet().endPresetAdjusting(true);
        }
    };

    private final Action switchRedBlue = new AbstractAction("switch red-blue") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getParamSet().startPresetAdjusting();

            redFromRed.setValue(0);
            redFromGreen.setValue(0);
            redFromBlue.setValue(100);

            greenFromRed.setValue(0);
            greenFromGreen.setValue(100);
            greenFromBlue.setValue(0);

            blueFromRed.setValue(100);
            blueFromGreen.setValue(0);
            blueFromBlue.setValue(0);

            getParamSet().endPresetAdjusting(true);
        }
    };

    private final Action switchGreenBlue = new AbstractAction("switch green-blue") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getParamSet().startPresetAdjusting();

            redFromRed.setValue(100);
            redFromGreen.setValue(0);
            redFromBlue.setValue(0);

            greenFromRed.setValue(0);
            greenFromGreen.setValue(0);
            greenFromBlue.setValue(100);

            blueFromRed.setValue(0);
            blueFromGreen.setValue(100);
            blueFromBlue.setValue(0);

            getParamSet().endPresetAdjusting(true);
        }
    };

    private final Action shiftRGBR = new AbstractAction("R -> G -> B -> R") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getParamSet().startPresetAdjusting();

            redFromRed.setValue(0);
            redFromGreen.setValue(0);
            redFromBlue.setValue(100);

            greenFromRed.setValue(100);
            greenFromGreen.setValue(0);
            greenFromBlue.setValue(0);

            blueFromRed.setValue(0);
            blueFromGreen.setValue(100);
            blueFromBlue.setValue(0);

            getParamSet().endPresetAdjusting(true);
        }
    };

    private final Action shiftRBGR = new AbstractAction("R -> B -> G -> R") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getParamSet().startPresetAdjusting();

            redFromRed.setValue(0);
            redFromGreen.setValue(100);
            redFromBlue.setValue(0);

            greenFromRed.setValue(0);
            greenFromGreen.setValue(0);
            greenFromBlue.setValue(100);

            blueFromRed.setValue(100);
            blueFromGreen.setValue(0);
            blueFromBlue.setValue(0);

            getParamSet().endPresetAdjusting(true);
        }
    };

    private final Action averageBW = new AbstractAction("average BW") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getParamSet().startPresetAdjusting();

            redFromRed.setValue(33);
            redFromGreen.setValue(33);
            redFromBlue.setValue(33);

            greenFromRed.setValue(33);
            greenFromGreen.setValue(33);
            greenFromBlue.setValue(33);

            blueFromRed.setValue(33);
            blueFromGreen.setValue(33);
            blueFromBlue.setValue(33);

            getParamSet().endPresetAdjusting(true);
        }
    };

    private final Action luminosityBW = new AbstractAction("luminosity BW") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getParamSet().startPresetAdjusting();

            redFromRed.setValue(22);
            redFromGreen.setValue(71);
            redFromBlue.setValue(7);

            greenFromRed.setValue(22);
            greenFromGreen.setValue(71);
            greenFromBlue.setValue(7);

            blueFromRed.setValue(22);
            blueFromGreen.setValue(71);
            blueFromBlue.setValue(7);

            getParamSet().endPresetAdjusting(true);
        }
    };

    private final Action sepia = new AbstractAction("sepia") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getParamSet().startPresetAdjusting();

            redFromRed.setValue(39);
            redFromGreen.setValue(77);
            redFromBlue.setValue(19);

            greenFromRed.setValue(35);
            greenFromGreen.setValue(69);
            greenFromBlue.setValue(17);

            blueFromRed.setValue(27);
            blueFromGreen.setValue(53);
            blueFromBlue.setValue(13);

            getParamSet().endPresetAdjusting(true);
        }
    };

    private final Action[] actions = {switchRedGreen, switchRedBlue, switchGreenBlue, shiftRGBR, shiftRBGR, averageBW, luminosityBW, sepia};

    public ChannelMixer() {
        super("Channel Mixer", true, false);
        ActionParam normalize = new ActionParam("Normalize", normalizeAction, "Makes sure that the sum of the channel contributions is 100%");
        GUIParam[] params = {
                redFromRed,
                redFromGreen,
                redFromBlue,

                greenFromRed,
                greenFromGreen,
                greenFromBlue,

                blueFromRed,
                blueFromGreen,
                blueFromBlue,

                normalize
        };
        setParamSet(new ParamSet(params));

        // add this extra action, but after the standard "Randomize Settings"
        ActionParam randomizeAndNormalize = new ActionParam("Randomize and Normalize",
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        paramSet.randomize();
                        normalizeAction.actionPerformed(null);
                    }
                }, "Randomizes settings and normalizes brightness");
        // insert it right after "Randomize Settings"
        paramSet.insertActionParam(randomizeAndNormalize, 11);
    }

    private static void normalizeChannel(RangeParam fromRed, RangeParam fromGreen, RangeParam fromBlue) {
        int red = fromRed.getValue();
        int green = fromGreen.getValue();
        int blue = fromBlue.getValue();
        int extra = red + green + blue - 100;
        if (extra != 0) {
            fromRed.setValue(red - extra / 3);
            fromGreen.setValue(green - extra / 3);
            fromBlue.setValue(blue - extra / 3);
        }
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float redFromRed = this.redFromRed.getValueAsPercentage();
        float redFromGreen = this.redFromGreen.getValueAsPercentage();
        float redFromBlue = this.redFromBlue.getValueAsPercentage();

        float greenFromRed = this.greenFromRed.getValueAsPercentage();
        float greenFromGreen = this.greenFromGreen.getValueAsPercentage();
        float greenFromBlue = this.greenFromBlue.getValueAsPercentage();

        float blueFromRed = this.blueFromRed.getValueAsPercentage();
        float blueFromGreen = this.blueFromGreen.getValueAsPercentage();
        float blueFromBlue = this.blueFromBlue.getValueAsPercentage();

        if ((redFromRed == 1.0f) && (redFromGreen == 0.0f) && (redFromBlue == 0.0f)) {
            if ((greenFromRed == 0.0f) && (greenFromGreen == 1.0f) && (greenFromBlue == 0.0f)) {
                if ((blueFromRed == 0.0f) && (blueFromGreen == 0.0f) && (blueFromBlue == 1.0f)) {
                    return src;
                }
            }
        }

        boolean packedInt = ImageUtils.hasPackedIntArray(src);

        if (packedInt) {
            DataBufferInt srcDataBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
            int[] srcData = srcDataBuffer.getData();

            DataBufferInt destDataBuffer = (DataBufferInt) dest.getRaster().getDataBuffer();
            int[] destData = destDataBuffer.getData();

            int length = srcData.length;
            if (length != destData.length) {
                throw new IllegalArgumentException("src and dest are not the same size");
            }

            for (int i = 0; i < length; i++) {
                int rgb = srcData[i];
                int a = rgb & 0xFF000000;
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                int newRed = (int) (redFromRed * r + redFromGreen * g + redFromBlue * b);
                int newGreen = (int) (greenFromRed * r + greenFromGreen * g + greenFromBlue * b);
                int newBlue = (int) (blueFromRed * r + blueFromGreen * g + blueFromBlue * b);

                newRed = PixelUtils.clamp(newRed);
                newGreen = PixelUtils.clamp(newGreen);
                newBlue = PixelUtils.clamp(newBlue);

                destData[i] = a | (newRed << 16) | (newGreen << 8) | newBlue;
            }
        } else { // not packed int
            BandCombineOp bandCombineOp = new BandCombineOp(new float[][]{
                    {redFromRed, redFromGreen, redFromBlue},
                    {greenFromRed, greenFromGreen, greenFromBlue},
                    {blueFromRed, blueFromGreen, blueFromBlue}
            }, null);
            Raster srcRaster = src.getRaster();
            Raster destRaster = dest.getRaster();
            bandCombineOp.filter(srcRaster, (WritableRaster) destRaster);
        }

        return dest;
    }

    @Override
    public AdjustPanel createAdjustPanel() {
        return new ChannelMixerAdjustments(this, actions);
    }

}