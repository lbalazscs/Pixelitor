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

package pixelitor.filters;

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.gui.*;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;
import java.util.function.BooleanSupplier;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;
import static pixelitor.utils.Texts.i18n;

/**
 * The Channel Mixer filter
 */
public class ChannelMixer extends ParametrizedFilter {
    public static final String NAME = i18n("channel_mixer");

    private static final int MIN_PERCENT = -200;
    private static final int MAX_PERCENT = 200;

    private static final String GREEN = "Green";
    private static final String RED = "Red";
    private static final String BLUE = "Blue";

    private final RangeParam redFromRed = from(RED, RED, 100);
    private final RangeParam redFromGreen = from(RED, GREEN, 0);
    private final RangeParam redFromBlue = from(RED, BLUE, 0);

    private final RangeParam greenFromRed = from(GREEN, RED, 0);
    private final RangeParam greenFromGreen = from(GREEN, GREEN, 100);
    private final RangeParam greenFromBlue = from(GREEN, BLUE, 0);

    private final RangeParam blueFromRed = from(BLUE, RED, 0);
    private final RangeParam blueFromGreen = from(BLUE, GREEN, 0);
    private final RangeParam blueFromBlue = from(BLUE, BLUE, 100);

    private final Runnable normalizeAction = () -> {
        normalizeChannel(redFromRed, redFromGreen, redFromBlue);
        normalizeChannel(greenFromRed, greenFromGreen, greenFromBlue);
        normalizeChannel(blueFromRed, blueFromGreen, blueFromBlue);

        // no need for triggering the filter here, because it will happen
        // automatically via ActionParam, but the presets on the right side
        // DO require explicit triggering because they are simple JButtons
    };

    private final Action swapRedGreen
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

            getParamSet().runFilter();
        }
    };

    private final Action swapRedBlue = new AbstractAction("Swap Red-Blue") {
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

            getParamSet().runFilter();
        }
    };

    private final Action swapGreenBlue = new AbstractAction("Swap Green-Blue") {
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

            getParamSet().runFilter();
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

            getParamSet().runFilter();
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

            getParamSet().runFilter();
        }
    };

    private final Action removeRed = new AbstractAction("Remove Red") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(0);
            redFromGreen.setValueNoTrigger(0);
            redFromBlue.setValueNoTrigger(0);

            greenFromRed.setValueNoTrigger(0);
            greenFromGreen.setValueNoTrigger(100);
            greenFromBlue.setValueNoTrigger(0);

            blueFromRed.setValueNoTrigger(0);
            blueFromGreen.setValueNoTrigger(0);
            blueFromBlue.setValueNoTrigger(100);

            getParamSet().runFilter();
        }
    };

    private final Action removeGreen = new AbstractAction("Remove Green") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(100);
            redFromGreen.setValueNoTrigger(0);
            redFromBlue.setValueNoTrigger(0);

            greenFromRed.setValueNoTrigger(0);
            greenFromGreen.setValueNoTrigger(0);
            greenFromBlue.setValueNoTrigger(0);

            blueFromRed.setValueNoTrigger(0);
            blueFromGreen.setValueNoTrigger(0);
            blueFromBlue.setValueNoTrigger(100);

            getParamSet().runFilter();
        }
    };

    private final Action removeBlue = new AbstractAction("Remove Blue") {
        @Override
        public void actionPerformed(ActionEvent e) {
            redFromRed.setValueNoTrigger(100);
            redFromGreen.setValueNoTrigger(0);
            redFromBlue.setValueNoTrigger(0);

            greenFromRed.setValueNoTrigger(0);
            greenFromGreen.setValueNoTrigger(100);
            greenFromBlue.setValueNoTrigger(0);

            blueFromRed.setValueNoTrigger(0);
            blueFromGreen.setValueNoTrigger(0);
            blueFromBlue.setValueNoTrigger(0);

            getParamSet().runFilter();
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

            getParamSet().runFilter();
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

            getParamSet().runFilter();
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

            getParamSet().runFilter();
        }
    };

    private final Action[] presets = {swapRedGreen, swapRedBlue, swapGreenBlue,
            shiftRGBR, shiftRBGR, removeRed, removeGreen, removeBlue,
            averageBW, luminosityBW, sepia};
    private boolean monochrome = false;

    public ChannelMixer() {
        super(ShowOriginal.YES);

        var normalize = new FilterButtonModel("Normalize", normalizeAction,
                "Makes sure that the sum of the channel contributions is 100%");
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
        BooleanSupplier ifMonochrome = () -> monochrome;
        redFromRed.linkWith(greenFromRed, ifMonochrome);
        redFromRed.linkWith(blueFromRed, ifMonochrome);

        redFromBlue.linkWith(greenFromBlue, ifMonochrome);
        redFromBlue.linkWith(blueFromBlue, ifMonochrome);

        redFromGreen.linkWith(greenFromGreen, ifMonochrome);
        redFromGreen.linkWith(blueFromGreen, ifMonochrome);

        setParams(params)
                .withAction(normalize);

        // add this extra action, but after the standard "Randomize Settings"
        var randomizeAndNormalize = new FilterButtonModel("Randomize and Normalize",
                () -> {
                    paramSet.randomize();
                    normalizeAction.run();
                }, "Randomizes settings and normalizes the brightness");
        // insert it right after "Randomize Settings"
        paramSet.insertAction(randomizeAndNormalize, 2);
    }

    public void setMonochrome(boolean monochrome) {
        boolean wasMonochrome = this.monochrome;
        if (wasMonochrome == monochrome) {
            return;
        }
        this.monochrome = monochrome;

        boolean allowColors = !monochrome;

        swapGreenBlue.setEnabled(allowColors);
        swapRedBlue.setEnabled(allowColors);
        swapRedGreen.setEnabled(allowColors);

        shiftRBGR.setEnabled(allowColors);
        shiftRGBR.setEnabled(allowColors);

        removeRed.setEnabled(allowColors);
        removeGreen.setEnabled(allowColors);
        removeBlue.setEnabled(allowColors);

        sepia.setEnabled(allowColors);

        if (monochrome) {
            int fromRed = (redFromRed.getValue() + greenFromRed.getValue() + blueFromRed.getValue()) / 3;
            redFromRed.setValueNoTrigger(fromRed);
            greenFromRed.setValueNoTrigger(fromRed);
            blueFromRed.setValueNoTrigger(fromRed);

            int fromGreen = (redFromGreen.getValue() + greenFromGreen.getValue() + blueFromGreen.getValue()) / 3;
            redFromGreen.setValueNoTrigger(fromGreen);
            greenFromGreen.setValueNoTrigger(fromGreen);
            blueFromGreen.setValueNoTrigger(fromGreen);

            int fromBlue = (redFromBlue.getValue() + greenFromBlue.getValue() + blueFromBlue.getValue()) / 3;
            redFromBlue.setValueNoTrigger(fromBlue);
            greenFromBlue.setValueNoTrigger(fromBlue);
            blueFromBlue.setValueNoTrigger(fromBlue);

            if (!wasMonochrome) {
                getParamSet().runFilter();
            }
        }
    }

    private static void normalizeChannel(RangeParam fromRed,
                                         RangeParam fromGreen,
                                         RangeParam fromBlue) {
        int red = fromRed.getValue();
        int green = fromGreen.getValue();
        int blue = fromBlue.getValue();
        int extra = red + green + blue - 100;
        if (extra != 0) {
            fromRed.setValueNoTrigger(red - extra / 3.0);
            fromGreen.setValueNoTrigger(green - extra / 3.0);
            fromBlue.setValueNoTrigger(blue - extra / 3.0);
        }
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float rfr = redFromRed.getPercentageValF();
        float rfg = redFromGreen.getPercentageValF();
        float rfb = redFromBlue.getPercentageValF();

        float gfr = greenFromRed.getPercentageValF();
        float gfg = greenFromGreen.getPercentageValF();
        float gfb = greenFromBlue.getPercentageValF();

        float bfr = blueFromRed.getPercentageValF();
        float bfg = blueFromGreen.getPercentageValF();
        float bfb = blueFromBlue.getPercentageValF();

        if (rfr == 1.0f && rfg == 0.0f && rfb == 0.0f
            && gfr == 0.0f && gfg == 1.0f && gfb == 0.0f
            && bfr == 0.0f && bfg == 0.0f && bfb == 1.0f) {
            return src;
        }

        boolean packedInt = ImageUtils.hasPackedIntArray(src);
        if (packedInt) {
            int[] srcData = ImageUtils.getPixelsAsArray(src);
            int[] destData = ImageUtils.getPixelsAsArray(dest);

            int length = srcData.length;
            assert length == destData.length;

            for (int i = 0; i < length; i++) {
                int rgb = srcData[i];
                int a = rgb & 0xFF000000;
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;

                int newRed = (int) (rfr * r + rfg * g + rfb * b);
                int newGreen = (int) (gfr * r + gfg * g + gfb * b);
                int newBlue = (int) (bfr * r + bfg * g + bfb * b);

                newRed = PixelUtils.clamp(newRed);
                newGreen = PixelUtils.clamp(newGreen);
                newBlue = PixelUtils.clamp(newBlue);

                destData[i] = a | newRed << 16 | newGreen << 8 | newBlue;
            }
        } else { // not packed int
            var bandCombineOp = new BandCombineOp(new float[][]{
                    {rfr, rfg, rfb},
                    {gfr, gfg, gfb},
                    {bfr, bfg, bfb}
            }, null);
            var srcRaster = src.getRaster();
            var destRaster = dest.getRaster();
            bandCombineOp.filter(srcRaster, destRaster);
        }

        return dest;
    }

    @Override
    public FilterGUI createGUI(Drawable dr) {
        return new ChannelMixerGUI(this, dr, presets);
    }

    private static RangeParam from(String first, String second, int defaultValue) {
        String name = "<html><b><font color=" + first + ">" + first
            + "</font></b> from <b><font color=" + second + ">" + second + "</font></b> (%)";
        return new RangeParam(name, MIN_PERCENT, defaultValue, MAX_PERCENT, true, NONE);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}