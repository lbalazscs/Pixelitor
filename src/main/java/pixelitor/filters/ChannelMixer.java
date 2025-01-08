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

package pixelitor.filters;

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.gui.*;
import pixelitor.gui.utils.TaskAction;
import pixelitor.layers.Filterable;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.function.BooleanSupplier;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NONE;
import static pixelitor.utils.Texts.i18n;

/**
 * The "Channel Mixer" filter.
 */
public class ChannelMixer extends ParametrizedFilter {
    public static final String NAME = i18n("channel_mixer");

    @Serial
    private static final long serialVersionUID = 1226064290403623160L;

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

    private final BooleanParam preserveBrightnessParam = new BooleanParam(
        "Preserve Brightness", true, IGNORE_RANDOMIZE);
    private final BooleanParam autoBWParam = new BooleanParam(
        "Allow only Black and White", false, IGNORE_RANDOMIZE);

    private final Action swapRedGreen = new TaskAction("Swap Red-Green", () -> withoutNormalization(() -> {
        setRedSource(0, 100, 0);
        setGreenSource(100, 0, 0);
        setBlueSource(0, 0, 100);
    }));

    private final Action swapRedBlue = new TaskAction("Swap Red-Blue", () -> withoutNormalization(() -> {
        setRedSource(0, 0, 100);
        setGreenSource(0, 100, 0);
        setBlueSource(100, 0, 0);
    }));

    private final Action swapGreenBlue = new TaskAction("Swap Green-Blue", () -> withoutNormalization(() -> {
        setRedSource(100, 0, 0);
        setGreenSource(0, 0, 100);
        setBlueSource(0, 100, 0);
    }));

    private final Action shiftRGBR = new TaskAction("R -> G -> B -> R", () -> withoutNormalization(() -> {
        setRedSource(0, 0, 100);
        setGreenSource(100, 0, 0);
        setBlueSource(0, 100, 0);
    }));

    private final Action shiftRBGR = new TaskAction("R -> B -> G -> R", () -> withoutNormalization(() -> {
        setRedSource(0, 100, 0);
        setGreenSource(0, 0, 100);
        setBlueSource(100, 0, 0);
    }));

    private final Action removeRed = new TaskAction("Remove Red", () -> {
        assert !preserveBrightnessParam.isChecked();

        setRedSource(0, 0, 0);
        setGreenSource(0, 100, 0);
        setBlueSource(0, 0, 100);

        getParamSet().runFilter();
    });

    private final Action removeGreen = new TaskAction("Remove Green", () -> {
        assert !preserveBrightnessParam.isChecked();

        setRedSource(100, 0, 0);
        setGreenSource(0, 0, 0);
        setBlueSource(0, 0, 100);

        getParamSet().runFilter();
    });

    private final Action removeBlue = new TaskAction("Remove Blue", () -> {
        assert !preserveBrightnessParam.isChecked();

        setRedSource(100, 0, 0);
        setGreenSource(0, 100, 0);
        setBlueSource(0, 0, 0);

        getParamSet().runFilter();
    });

    private final Action averageBW = new TaskAction("Average BW", () -> withoutNormalization(() -> {
        setRedSource(33, 33, 33);
        setGreenSource(33, 33, 33);
        setBlueSource(33, 33, 33);
    }));

    private final Action luminosityBW = new TaskAction("Luminosity BW", () -> withoutNormalization(() -> {
        setRedSource(22, 71, 7);
        setGreenSource(22, 71, 7);
        setBlueSource(22, 71, 7);
    }));

    private final Action sepia = new TaskAction("Sepia", () -> {
        assert !preserveBrightnessParam.isChecked();

        setRedSource(39, 77, 19);
        setGreenSource(35, 69, 17);
        setBlueSource(27, 53, 13);

        getParamSet().runFilter();
    });

    private final Action[] presets = {swapRedGreen, swapRedBlue, swapGreenBlue,
        shiftRGBR, shiftRBGR, removeRed, removeGreen, removeBlue,
        averageBW, luminosityBW, sepia};

    private final GroupedRangeParam redPercentageGroup;
    private final GroupedRangeParam greenPercentageGroup;
    private final GroupedRangeParam bluePercentageGroup;

    public ChannelMixer() {
        super(true);

        BooleanSupplier ifMonochrome = autoBWParam::isChecked;
        redFromRed.linkWith(greenFromRed, ifMonochrome);
        redFromRed.linkWith(blueFromRed, ifMonochrome);

        redFromBlue.linkWith(greenFromBlue, ifMonochrome);
        redFromBlue.linkWith(blueFromBlue, ifMonochrome);

        redFromGreen.linkWith(greenFromGreen, ifMonochrome);
        redFromGreen.linkWith(blueFromGreen, ifMonochrome);

        redPercentageGroup = new GroupedRangeParam("Red Channel", new RangeParam[]{
            redFromRed, redFromGreen, redFromBlue}, false).autoNormalized();
        greenPercentageGroup = new GroupedRangeParam("Green Channel", new RangeParam[]{
            greenFromRed, greenFromGreen, greenFromBlue}, false).autoNormalized();
        bluePercentageGroup = new GroupedRangeParam("Blue Channel", new RangeParam[]{
            blueFromRed, blueFromGreen, blueFromBlue}, false).autoNormalized();

        autoBWParam.setToolTip("Link the sliders so that the image always stays black and white");
        preserveBrightnessParam.setToolTip("Preserve brightness by ensuring that the sum of percentages is around 100%");

        setParams(
            autoBWParam,
            preserveBrightnessParam,
            redPercentageGroup,
            greenPercentageGroup,
            bluePercentageGroup);

        paramSet.setAfterResetAllAction(this::afterResetAll);
        enablePresets();
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        return new ChannelMixerGUI(this, layer, presets, reset);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        float rfr = (float) redFromRed.getPercentage();
        float rfg = (float) redFromGreen.getPercentage();
        float rfb = (float) redFromBlue.getPercentage();

        float gfr = (float) greenFromRed.getPercentage();
        float gfg = (float) greenFromGreen.getPercentage();
        float gfb = (float) greenFromBlue.getPercentage();

        float bfr = (float) blueFromRed.getPercentage();
        float bfg = (float) blueFromGreen.getPercentage();
        float bfb = (float) blueFromBlue.getPercentage();

        if (rfr == 1.0f && rfg == 0.0f && rfb == 0.0f
            && gfr == 0.0f && gfg == 1.0f && gfb == 0.0f
            && bfr == 0.0f && bfg == 0.0f && bfb == 1.0f) {
            return src;
        }

        boolean packedInt = ImageUtils.hasPackedIntArray(src);
        if (packedInt) {
            int[] srcPixels = ImageUtils.getPixels(src);
            int[] destPixels = ImageUtils.getPixels(dest);

            int length = srcPixels.length;
            assert length == destPixels.length;

            for (int i = 0; i < length; i++) {
                int rgb = srcPixels[i];
                int a = rgb & 0xFF_00_00_00;
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;

                int newRed = (int) (rfr * r + rfg * g + rfb * b);
                int newGreen = (int) (gfr * r + gfg * g + gfb * b);
                int newBlue = (int) (bfr * r + bfg * g + bfb * b);

                newRed = PixelUtils.clamp(newRed);
                newGreen = PixelUtils.clamp(newGreen);
                newBlue = PixelUtils.clamp(newBlue);

                destPixels[i] = a | newRed << 16 | newGreen << 8 | newBlue;
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

    // Replace the adjustment listeners with custom versions that
    // change other values before triggering the filter.
    public void replaceAdjustmentListeners() {
        autoBWParam.setAdjustmentListener(this::updateAutoBW);
        preserveBrightnessParam.setAdjustmentListener(this::updatePreserveBrightness);
    }

    private void updateAutoBW() {
        boolean autoBW = autoBWParam.isChecked();
        enablePresets();

        if (autoBW) {
            // since the channels will be synchronized, it's enough to have only
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
            boolean autoNormalize = preserveBrightnessParam.isChecked();
            greenPercentageGroup.setAutoNormalizationEnabled(autoNormalize, false);
            bluePercentageGroup.setAutoNormalizationEnabled(autoNormalize, false);
        }
    }

    private void updatePreserveBrightness() {
        boolean preserveBrightness = preserveBrightnessParam.isChecked();
        boolean autoBW = autoBWParam.isChecked();

        redPercentageGroup.setAutoNormalizationEnabled(preserveBrightness, true);
        if (!autoBW) {
            greenPercentageGroup.setAutoNormalizationEnabled(preserveBrightness, true);
            bluePercentageGroup.setAutoNormalizationEnabled(preserveBrightness, true);
        }

        enablePresets();
        if (preserveBrightness) {
            getParamSet().runFilter();
        }
    }

    private void afterResetAll() {
        enablePresets();
    }

    private void enablePresets() {
        boolean allowColors = !autoBWParam.isChecked();
        boolean allowAnySum = !preserveBrightnessParam.isChecked();

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

    private void temporarilyEnableNormalization(boolean enable) {
        redPercentageGroup.setAutoNormalizationEnabled(enable, false);

        // if monochrome, then it's enough to enable the red group
        boolean monochrome = autoBWParam.isChecked();
        greenPercentageGroup.setAutoNormalizationEnabled(enable && !monochrome, false);
        bluePercentageGroup.setAutoNormalizationEnabled(enable && !monochrome, false);
    }

    private static RangeParam from(String first, String second, int defaultValue) {
        String name = "<html>from <b><font color=" + second + ">" + second + "</font></b> (%)";
        RangeParam param = new RangeParam(name, MIN_PERCENT, defaultValue, MAX_PERCENT, true, NONE);
        param.setPresetKey(first + "From" + second);
        return param;
    }

    private void withoutNormalization(Runnable task) {
        boolean wasNormalized = preserveBrightnessParam.isChecked();
        if (wasNormalized) {
            temporarilyEnableNormalization(false);
        }

        task.run();

        if (wasNormalized) {
            temporarilyEnableNormalization(true);
        }
        getParamSet().runFilter();
    }

    private void setRedSource(int fromRed, int fromGreen, int fromBlue) {
        redFromRed.setValueNoTrigger(fromRed);
        redFromGreen.setValueNoTrigger(fromGreen);
        redFromBlue.setValueNoTrigger(fromBlue);
    }

    private void setGreenSource(int fromRed, int fromGreen, int fromBlue) {
        greenFromRed.setValueNoTrigger(fromRed);
        greenFromGreen.setValueNoTrigger(fromGreen);
        greenFromBlue.setValueNoTrigger(fromBlue);
    }

    private void setBlueSource(int fromRed, int fromGreen, int fromBlue) {
        blueFromRed.setValueNoTrigger(fromRed);
        blueFromGreen.setValueNoTrigger(fromGreen);
        blueFromBlue.setValueNoTrigger(fromBlue);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}