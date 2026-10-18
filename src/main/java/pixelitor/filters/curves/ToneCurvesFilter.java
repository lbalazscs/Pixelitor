/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.curves;

import com.jhlabs.image.CurvesFilter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.Help;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.layers.Filterable;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.utils.Texts.i18n;

/**
 * Filter that applies tone-curve adjustments in the sRGB and Oklab color spaces.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesFilter extends FilterWithGUI {
    public static final String NAME = i18n("curves");

    @Serial
    private static final long serialVersionUID = 3679501445608294764L;

    private final ToneCurves curves;  // the data model for the tone curves

    // reference to the last-used GUI instance for this filter
    private transient ToneCurvesGUI lastGui;

    public ToneCurvesFilter() {
        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Curve_(tonality)");
        curves = new ToneCurves();
    }

    @Override
    public ToneCurvesGUI createGUI(Filterable layer, boolean resetSettings) {
        if (resetSettings) {
            curves.reset();
            curves.selectDefaults();
        }
        lastGui = new ToneCurvesGUI(this, layer);
        return lastGui;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return switch (curves.getColorSpace()) {
            case SRGB -> transformSrgb(src, dest);
            case OKLAB -> transformOklab(src, dest);
        };
    }

    private BufferedImage transformSrgb(BufferedImage src, BufferedImage dest) {
        // the underlying JH Labs filter used for sRGB
        CurvesFilter srgbFilter = new CurvesFilter(
            NAME,
            curves.getCurve(Channel.RED).curveData,
            curves.getCurve(Channel.GREEN).curveData,
            curves.getCurve(Channel.BLUE).curveData,
            curves.getCurve(Channel.RGB).curveData
        );

        return srgbFilter.filter(src, dest);
    }

    private BufferedImage transformOklab(BufferedImage src, BufferedImage dest) {
        OklabCurvesFilter oklabFilter = new OklabCurvesFilter(
            curves.getCurve(Channel.OK_L).curveData,
            curves.getCurve(Channel.OK_A).curveData,
            curves.getCurve(Channel.OK_B).curveData
        );
        return oklabFilter.filter(src, dest);
    }

    @Override
    public void randomize() {
        curves.randomize();
        modelChanged();
    }

    private void modelChanged() {
        if (lastGui != null) { // it's null when loading a smart filter
            lastGui.modelChanged();
        }
    }

    void guiDisposed(ToneCurvesGUI gui) {
        if (lastGui == gui) {
            // clear the reference after the dialog closes
            lastGui = null;
        }
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        ColorSpace activeSpace = curves.getColorSpace();
        preset.put(ColorSpace.PRESET_KEY, activeSpace.name());

        // only save the curves for the currently active color space
        for (Channel channel : activeSpace.getChannels()) {
            String encodedCurve = curves.getCurve(channel).toSaveString();
            preset.put(channel.getPresetKey(), encodedCurve);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        ColorSpace colorSpace = preset.getEnum(ColorSpace.PRESET_KEY, ColorSpace.class);
        curves.setColorSpace(colorSpace);

        // resets the curves for all color spaces so that
        // channels missing from the preset fall back to defaults
        curves.reset();

        // load curves for the active color space
        for (Channel channel : colorSpace.getChannels()) {
            String presetValue = preset.get(channel.getPresetKey());
            if (presetValue != null) {
                curves.getCurve(channel).setStateFrom(presetValue);
            }
        }

        modelChanged();
    }

    public ToneCurves getCurves() {
        return curves;
    }
}
