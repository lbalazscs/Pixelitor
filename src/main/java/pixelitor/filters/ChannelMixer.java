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

package pixelitor.filters;

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.gui.ActionSetting;
import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ChannelMixerAdjustments;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

public class ChannelMixer extends FilterWithParametrizedGUI {
    private static final int MIN_PERCENT = -200;
    private static final int MAX_PERCENT = 200;

    private final RangeParam redFromRed = createParam("Red", "Red", 100);
    private final RangeParam redFromGreen = createParam("Red", "Green", 0);
    private final RangeParam redFromBlue = createParam("Red", "Blue", 0);

    private final RangeParam greenFromRed = createParam("Green", "Red", 0);
    private final RangeParam greenFromGreen = createParam("Green", "Green", 100);
    private final RangeParam greenFromBlue = createParam("Green", "Blue", 0);

    private final RangeParam blueFromRed = createParam("Blue", "Red", 0);
    private final RangeParam blueFromGreen = createParam("Blue", "Green", 0);
    private final RangeParam blueFromBlue = createParam("Blue", "Blue", 100);

    private final ActionListener normalizeAction = e -> {
        normalizeChannel(redFromRed, redFromGreen, redFromBlue);
        normalizeChannel(greenFromRed, greenFromGreen, greenFromBlue);
        normalizeChannel(blueFromRed, blueFromGreen, blueFromBlue);

        // no need for filter triggering here, because this will happen automatically via ActionParam
        // the actions on the right side DO require explicit triggering because they are simple JButtons
    };

    private final Action switchRedGreen
            = new AbstractAction("Swap Red-Green") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(0);
            redFromGreen.setValueNoTrigger(100);
            redFromBlue.setValueNoTrigger(0);

            greenFromRed.setValueNoTrigger(100);
            greenFromGreen.setValueNoTrigger(0);
            greenFromBlue.setValueNoTrigger(0);

            blueFromRed.setValueNoTrigger(0);
            blueFromGreen.setValueNoTrigger(0);
            blueFromBlue.setValueNoTrigger(100);

            getParamSet().triggerFilter();
        }
    };

    private final Action switchRedBlue = new AbstractAction("Swap Red-Blue") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(0);
            redFromGreen.setValueNoTrigger(0);
            redFromBlue.setValueNoTrigger(100);

            greenFromRed.setValueNoTrigger(0);
            greenFromGreen.setValueNoTrigger(100);
            greenFromBlue.setValueNoTrigger(0);

            blueFromRed.setValueNoTrigger(100);
            blueFromGreen.setValueNoTrigger(0);
            blueFromBlue.setValueNoTrigger(0);

            getParamSet().triggerFilter();
        }
    };

    private final Action switchGreenBlue = new AbstractAction("Swap Green-Blue") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(100);
            redFromGreen.setValueNoTrigger(0);
            redFromBlue.setValueNoTrigger(0);

            greenFromRed.setValueNoTrigger(0);
            greenFromGreen.setValueNoTrigger(0);
            greenFromBlue.setValueNoTrigger(100);

            blueFromRed.setValueNoTrigger(0);
            blueFromGreen.setValueNoTrigger(100);
            blueFromBlue.setValueNoTrigger(0);

            getParamSet().triggerFilter();
        }
    };

    private final Action shiftRGBR = new AbstractAction("R -> G -> B -> R") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(0);
            redFromGreen.setValueNoTrigger(0);
            redFromBlue.setValueNoTrigger(100);

            greenFromRed.setValueNoTrigger(100);
            greenFromGreen.setValueNoTrigger(0);
            greenFromBlue.setValueNoTrigger(0);

            blueFromRed.setValueNoTrigger(0);
            blueFromGreen.setValueNoTrigger(100);
            blueFromBlue.setValueNoTrigger(0);

            getParamSet().triggerFilter();
        }
    };

    private final Action shiftRBGR = new AbstractAction("R -> B -> G -> R") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(0);
            redFromGreen.setValueNoTrigger(100);
            redFromBlue.setValueNoTrigger(0);

            greenFromRed.setValueNoTrigger(0);
            greenFromGreen.setValueNoTrigger(0);
            greenFromBlue.setValueNoTrigger(100);

            blueFromRed.setValueNoTrigger(100);
            blueFromGreen.setValueNoTrigger(0);
            blueFromBlue.setValueNoTrigger(0);

            getParamSet().triggerFilter();
        }
    };

    private final Action averageBW = new AbstractAction("Average BW") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(33);
            redFromGreen.setValueNoTrigger(33);
            redFromBlue.setValueNoTrigger(33);

            greenFromRed.setValueNoTrigger(33);
            greenFromGreen.setValueNoTrigger(33);
            greenFromBlue.setValueNoTrigger(33);

            blueFromRed.setValueNoTrigger(33);
            blueFromGreen.setValueNoTrigger(33);
            blueFromBlue.setValueNoTrigger(33);

            getParamSet().triggerFilter();
        }
    };

    private final Action luminosityBW = new AbstractAction("Luminosity BW") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(22);
            redFromGreen.setValueNoTrigger(71);
            redFromBlue.setValueNoTrigger(7);

            greenFromRed.setValueNoTrigger(22);
            greenFromGreen.setValueNoTrigger(71);
            greenFromBlue.setValueNoTrigger(7);

            blueFromRed.setValueNoTrigger(22);
            blueFromGreen.setValueNoTrigger(71);
            blueFromBlue.setValueNoTrigger(7);

            getParamSet().triggerFilter();
        }
    };

    private final Action sepia = new AbstractAction("Sepia") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(39);
            redFromGreen.setValueNoTrigger(77);
            redFromBlue.setValueNoTrigger(19);

            greenFromRed.setValueNoTrigger(35);
            greenFromGreen.setValueNoTrigger(69);
            greenFromBlue.setValueNoTrigger(17);

            blueFromRed.setValueNoTrigger(27);
            blueFromGreen.setValueNoTrigger(53);
            blueFromBlue.setValueNoTrigger(13);

            getParamSet().triggerFilter();
        }
    };

    private final Action[] actions = {switchRedGreen, switchRedBlue, switchGreenBlue, shiftRGBR, shiftRBGR, averageBW, luminosityBW, sepia};

    public ChannelMixer() {
        super(ShowOriginal.YES);
        ActionSetting normalize = new ActionSetting("Normalize", normalizeAction, "Makes sure that the sum of the channel contributions is 100%");
        FilterParam[] params = {
                redFromRed,
                redFromGreen,
                redFromBlue,

                greenFromRed,
                greenFromGreen,
                greenFromBlue,

                blueFromRed,
                blueFromGreen,
                blueFromBlue,
        };
        setParamSet(new ParamSet(params)
                .withAction(normalize));

        // add this extra action, but after the standard "Randomize Settings"
        ActionSetting randomizeAndNormalize = new ActionSetting("Randomize and Normalize",
                e -> {
                    paramSet.randomize();
                    normalizeAction.actionPerformed(null);
                }, "Randomizes settings and normalizes the brightness");
        // insert it right after "Randomize Settings"
        paramSet.insertAction(randomizeAndNormalize, 2);
    }

    private static void normalizeChannel(RangeParam fromRed, RangeParam fromGreen, RangeParam fromBlue) {
        int red = fromRed.getValue();
        int green = fromGreen.getValue();
        int blue = fromBlue.getValue();
        int extra = red + green + blue - 100;
        if (extra != 0) {
            fromRed.setValueNoTrigger(red - extra / 3);
            fromGreen.setValueNoTrigger(green - extra / 3);
            fromBlue.setValueNoTrigger(blue - extra / 3);
        }
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float rfr = redFromRed.getValueAsPercentage();
        float rfg = redFromGreen.getValueAsPercentage();
        float rfb = redFromBlue.getValueAsPercentage();

        float gfr = greenFromRed.getValueAsPercentage();
        float gfg = greenFromGreen.getValueAsPercentage();
        float gfb = greenFromBlue.getValueAsPercentage();

        float bfr = blueFromRed.getValueAsPercentage();
        float bfg = blueFromGreen.getValueAsPercentage();
        float bfb = blueFromBlue.getValueAsPercentage();

        if ((rfr == 1.0f) && (rfg == 0.0f) && (rfb == 0.0f)) {
            if ((gfr == 0.0f) && (gfg == 1.0f) && (gfb == 0.0f)) {
                if ((bfr == 0.0f) && (bfg == 0.0f) && (bfb == 1.0f)) {
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
            assert length == destData.length;

            for (int i = 0; i < length; i++) {
                int rgb = srcData[i];
                int a = rgb & 0xFF000000;
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                int newRed = (int) (rfr * r + rfg * g + rfb * b);
                int newGreen = (int) (gfr * r + gfg * g + gfb * b);
                int newBlue = (int) (bfr * r + bfg * g + bfb * b);

                newRed = PixelUtils.clamp(newRed);
                newGreen = PixelUtils.clamp(newGreen);
                newBlue = PixelUtils.clamp(newBlue);

                destData[i] = a | (newRed << 16) | (newGreen << 8) | newBlue;
            }
        } else { // not packed int
            BandCombineOp bandCombineOp = new BandCombineOp(new float[][]{
                    {rfr, rfg, rfb},
                    {gfr, gfg, gfb},
                    {bfr, bfg, bfb}
            }, null);
            Raster srcRaster = src.getRaster();
            Raster destRaster = dest.getRaster();
            bandCombineOp.filter(srcRaster, (WritableRaster) destRaster);
        }

        return dest;
    }

    @Override
    public AdjustPanel createAdjustPanel(ImageLayer layer) {
        return new ChannelMixerAdjustments(this, layer, actions);
    }

    private static RangeParam createParam(String first, String second, int defaultValue) {
        String name = "<html><b><font color=" + first + ">" + first
                + "</font></b> from <b><font color=" + second + ">" + second + "</font></b> (%):</html>";
        return new RangeParam(name, MIN_PERCENT, defaultValue, MAX_PERCENT, AddDefaultButton.YES, NONE);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}