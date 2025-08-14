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
 * Filter that applies tone curve adjustments for sRGB and Oklab color spaces.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesFilter extends FilterWithGUI {
    public static final String NAME = i18n("curves");
    private static final String PRESET_KEY_COLOR_SPACE = "colorSpace";

    @Serial
    private static final long serialVersionUID = 3679501445608294764L;

    private CurvesFilter filter; // the underlying JHLabs filter for sRGB
    private final ToneCurves curves;  // the data model for the tone curves

    // reference to the last-used GUI instance for this filter
    private transient ToneCurvesGUI lastGUI;

    public ToneCurvesFilter() {
        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Curve_(tonality)");
        curves = new ToneCurves();
    }

    @Override
    public ToneCurvesGUI createGUI(Filterable layer, boolean reset) {
        if (reset) {
            curves.reset();
            // default to sRGB and its master channel
            curves.setColorSpace(ColorSpace.SRGB);
            curves.setActiveChannel(Channel.RGB);
        }
        lastGUI = new ToneCurvesGUI(this, layer);
        return lastGUI;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return switch (curves.getColorSpace()) {
            case SRGB -> transformSrgb(src, dest);
            case OKLAB -> transformOklab(src, dest);
        };
    }

    private BufferedImage transformSrgb(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CurvesFilter(NAME);
        }

        filter.setCurves(
            curves.getCurve(Channel.RGB).curveData,
            curves.getCurve(Channel.RED).curveData,
            curves.getCurve(Channel.GREEN).curveData,
            curves.getCurve(Channel.BLUE).curveData
        );
        return filter.filter(src, dest);
    }

    private BufferedImage transformOklab(BufferedImage src, BufferedImage dest) {
        // use a dedicated filter for Oklab processing
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
        stateChanged();
    }

    private void stateChanged() {
        if (lastGUI != null) { // it's null when loading a smart filter
            lastGUI.stateChanged();
        }
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        ColorSpace activeSpace = curves.getColorSpace();
        preset.put(PRESET_KEY_COLOR_SPACE, activeSpace.name());

        // only save the curves for the currently active color space
        for (Channel channel : Channel.getChoices(activeSpace)) {
            String saveString = curves.getCurve(channel).toSaveString();
            preset.put(channel.getPresetKey(), saveString);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        String csName = preset.get(PRESET_KEY_COLOR_SPACE);
        // default to sRGB for legacy presets that don't have this key
        ColorSpace colorSpace = (csName != null) ? ColorSpace.valueOf(csName) : ColorSpace.SRGB;
        curves.setColorSpace(colorSpace);

        // reset all curves to clear state from other color spaces before loading
        curves.reset();

        // load curves for all channels defined in the preset
        for (Channel channel : Channel.values()) {
            String saveString = preset.get(channel.getPresetKey());
            if (saveString != null) {
                curves.getCurve(channel).setStateFrom(saveString);
            }
        }

        stateChanged();
    }

    public ToneCurves getCurves() {
        return curves;
    }
}
