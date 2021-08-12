/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.PAction;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
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

    private final Action swapRedGreen = new PAction("Swap Red-Green") {
        @Override
        public void onClick() {
            runWithDisabledNormalization(() -> {
                redFromRed.setValueNoTrigger(0);
                redFromGreen.setValueNoTrigger(100);
                redFromBlue.setValueNoTrigger(0);

                greenFromRed.setValueNoTrigger(100);
                greenFromGreen.setValueNoTrigger(0);
                greenFromBlue.setValueNoTrigger(0);

                blueFromRed.setValueNoTrigger(0);
                blueFromGreen.setValueNoTrigger(0);
                blueFromBlue.setValueNoTrigger(100);
            });
        }
    };

    private final Action swapRedBlue = new PAction("Swap Red-Blue") {
        @Override
        public void onClick() {
            runWithDisabledNormalization(() -> {
                redFromRed.setValueNoTrigger(0);
                redFromGreen.setValueNoTrigger(0);
                redFromBlue.setValueNoTrigger(100);

                greenFromRed.setValueNoTrigger(0);
                greenFromGreen.setValueNoTrigger(100);
                greenFromBlue.setValueNoTrigger(0);

                blueFromRed.setValueNoTrigger(100);
                blueFromGreen.setValueNoTrigger(0);
                blueFromBlue.setValueNoTrigger(0);
            });
        }
    };

    private final Action swapGreenBlue = new PAction("Swap Green-Blue") {
        @Override
        public void onClick() {
            runWithDisabledNormalization(() -> {
                redFromRed.setValueNoTrigger(100);
                redFromGreen.setValueNoTrigger(0);
                redFromBlue.setValueNoTrigger(0);

                greenFromRed.setValueNoTrigger(0);
                greenFromGreen.setValueNoTrigger(0);
                greenFromBlue.setValueNoTrigger(100);

                blueFromRed.setValueNoTrigger(0);
                blueFromGreen.setValueNoTrigger(100);
                blueFromBlue.setValueNoTrigger(0);
            });
        }
    };

    private final Action shiftRGBR = new PAction("R -> G -> B -> R") {
        @Override
        public void onClick() {
            runWithDisabledNormalization(() -> {
                redFromRed.setValueNoTrigger(0);
                redFromGreen.setValueNoTrigger(0);
                redFromBlue.setValueNoTrigger(100);

                greenFromRed.setValueNoTrigger(100);
                greenFromGreen.setValueNoTrigger(0);
                greenFromBlue.setValueNoTrigger(0);

                blueFromRed.setValueNoTrigger(0);
                blueFromGreen.setValueNoTrigger(100);
                blueFromBlue.setValueNoTrigger(0);
            });
        }
    };

    private final Action shiftRBGR = new PAction("R -> B -> G -> R") {
        @Override
        public void onClick() {
            runWithDisabledNormalization(() -> {
                redFromRed.setValueNoTrigger(0);
                redFromGreen.setValueNoTrigger(100);
                redFromBlue.setValueNoTrigger(0);

                greenFromRed.setValueNoTrigger(0);
                greenFromGreen.setValueNoTrigger(0);
                greenFromBlue.setValueNoTrigger(100);

                blueFromRed.setValueNoTrigger(100);
                blueFromGreen.setValueNoTrigger(0);
                blueFromBlue.setValueNoTrigger(0);
            });
        }
    };

    private final Action removeRed = new PAction("Remove Red") {
        @Override
        public void onClick() {
            assert !autoNormalize;

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

    private final Action removeGreen = new PAction("Remove Green") {
        @Override
        public void onClick() {
            assert !autoNormalize;

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

    private final Action removeBlue = new PAction("Remove Blue") {
        @Override
        public void onClick() {
            assert !autoNormalize;

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

    private final Action averageBW = new PAction("Average BW") {
        @Override
        public void onClick() {
            runWithDisabledNormalization(() -> {
                redFromRed.setValueNoTrigger(33);
                redFromGreen.setValueNoTrigger(33);
                redFromBlue.setValueNoTrigger(33);

                greenFromRed.setValueNoTrigger(33);
                greenFromGreen.setValueNoTrigger(33);
                greenFromBlue.setValueNoTrigger(33);

                blueFromRed.setValueNoTrigger(33);
                blueFromGreen.setValueNoTrigger(33);
                blueFromBlue.setValueNoTrigger(33);
            });
        }
    };

    private final Action luminosityBW = new PAction("Luminosity BW") {
        @Override
        public void onClick() {
            runWithDisabledNormalization(() -> {
                redFromRed.setValueNoTrigger(22);
                redFromGreen.setValueNoTrigger(71);
                redFromBlue.setValueNoTrigger(7);

                greenFromRed.setValueNoTrigger(22);
                greenFromGreen.setValueNoTrigger(71);
                greenFromBlue.setValueNoTrigger(7);

                blueFromRed.setValueNoTrigger(22);
                blueFromGreen.setValueNoTrigger(71);
                blueFromBlue.setValueNoTrigger(7);
            });
        }
    };

    private final Action sepia = new PAction("Sepia") {
        @Override
        public void onClick() {
            assert !autoNormalize;

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
    private boolean autoNormalize = true;
    private boolean monochrome = false;
    private final GroupedRangeParam redPercentageGroup;
    private final GroupedRangeParam greenPercentageGroup;
    private final GroupedRangeParam bluePercentageGroup;

    public ChannelMixer() {
        super(true);

        redPercentageGroup = new GroupedRangeParam("Red Channel", new RangeParam[]{
            redFromRed, redFromGreen, redFromBlue}, false).autoNormalized();
        greenPercentageGroup = new GroupedRangeParam("Green Channel", new RangeParam[]{
            greenFromRed, greenFromGreen, greenFromBlue}, false).autoNormalized();
        bluePercentageGroup = new GroupedRangeParam("Blue Channel", new RangeParam[]{
            blueFromRed, blueFromGreen, blueFromBlue}, false).autoNormalized();

        FilterParam[] params = {
            redPercentageGroup,
            greenPercentageGroup,
            bluePercentageGroup,
        };
        BooleanSupplier ifMonochrome = () -> monochrome;
        redFromRed.linkWith(greenFromRed, ifMonochrome);
        redFromRed.linkWith(blueFromRed, ifMonochrome);

        redFromBlue.linkWith(greenFromBlue, ifMonochrome);
        redFromBlue.linkWith(blueFromBlue, ifMonochrome);

        redFromGreen.linkWith(greenFromGreen, ifMonochrome);
        redFromGreen.linkWith(blueFromGreen, ifMonochrome);

        setParams(params);

        enablePresets();
    }

    public void setMonochrome(boolean monochrome) {
        boolean wasMonochrome = this.monochrome;
        if (wasMonochrome == monochrome) {
            return;
        }
        this.monochrome = monochrome;

        enablePresets();

        if (monochrome) {
            // since the channels will be synchronized, it is enough to have only
            // one auto-normalization constraint - this also prevents feedback loops
            greenPercentageGroup.setAutoNormalizationEnabled(false, true);
            bluePercentageGroup.setAutoNormalizationEnabled(false, true);

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

            getParamSet().runFilter();
        } else {
            // switching back to non-monochrome mode: enable all
            // auto-normalization constraints if necessary
            greenPercentageGroup.setAutoNormalizationEnabled(autoNormalize, false);
            bluePercentageGroup.setAutoNormalizationEnabled(autoNormalize, false);
        }
    }

    public void setAutoNormalize(boolean autoNormalize) {
        boolean wasAutoNormalized = this.autoNormalize;
        if (wasAutoNormalized == autoNormalize) {
            return;
        }

        redPercentageGroup.setAutoNormalizationEnabled(autoNormalize, true);
        greenPercentageGroup.setAutoNormalizationEnabled(autoNormalize, true);
        bluePercentageGroup.setAutoNormalizationEnabled(autoNormalize, true);

        if (autoNormalize) {
            getParamSet().runFilter();
        }
        this.autoNormalize = autoNormalize;
        enablePresets();
    }

    public void enablePresets() {
        boolean allowColors = !monochrome;
        boolean allowAnySum = !autoNormalize;

        swapGreenBlue.setEnabled(allowColors);
        swapRedBlue.setEnabled(allowColors);
        swapRedGreen.setEnabled(allowColors);

        shiftRBGR.setEnabled(allowColors);
        shiftRGBR.setEnabled(allowColors);

        removeRed.setEnabled(allowColors && allowAnySum);
        removeGreen.setEnabled(allowColors && allowAnySum);
        removeBlue.setEnabled(allowColors && allowAnySum);

        sepia.setEnabled(allowColors && allowAnySum);
    }

    public void temporarilyEnableNormalization(boolean enable) {
        autoNormalize = enable;
        redPercentageGroup.setAutoNormalizationEnabled(enable, false);

        // if monochrome, then it's enough to enable the red group
        greenPercentageGroup.setAutoNormalizationEnabled(enable && !monochrome, false);
        greenPercentageGroup.setAutoNormalizationEnabled(enable && !monochrome, false);
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
    public FilterGUI createGUI(Drawable dr, boolean reset) {
        return new ChannelMixerGUI(this, dr, presets, reset);
    }

    private static RangeParam from(String first, String second, int defaultValue) {
        String name = "<html>from <b><font color=" + second + ">" + second + "</font></b> (%)";
        RangeParam param = new RangeParam(name, MIN_PERCENT, defaultValue, MAX_PERCENT, true, NONE);
        param.setPresetKey(first + "From" + second);
        return param;
    }

    private void runWithDisabledNormalization(Runnable task) {
        boolean wasNormalized = autoNormalize;
        if (wasNormalized) {
            temporarilyEnableNormalization(false);
        }

        task.run();

        if (wasNormalized) {
            temporarilyEnableNormalization(true);
        }
        getParamSet().runFilter();
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}