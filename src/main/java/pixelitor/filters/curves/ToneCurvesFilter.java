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
 * Filter that applies tone curve adjustments for sRGB and Oklab color spaces.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesFilter extends FilterWithGUI {
    public static final String NAME = i18n("curves");

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
            curves.getCurve(ColorSpace.SRGB, Channel.RGB).curveData,
            curves.getCurve(ColorSpace.SRGB, Channel.RED).curveData,
            curves.getCurve(ColorSpace.SRGB, Channel.GREEN).curveData,
            curves.getCurve(ColorSpace.SRGB, Channel.BLUE).curveData
        );

        return filter.filter(src, dest);
    }

    private BufferedImage transformOklab(BufferedImage src, BufferedImage dest) {
        OklabCurvesFilter oklabFilter = new OklabCurvesFilter(
            curves.getCurve(ColorSpace.OKLAB, Channel.OK_L).curveData,
            curves.getCurve(ColorSpace.OKLAB, Channel.OK_A).curveData,
            curves.getCurve(ColorSpace.OKLAB, Channel.OK_B).curveData
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
        preset.put(ColorSpace.PRESET_KEY, activeSpace.name());

        // only save the curves for the currently active color space
        for (Channel channel : activeSpace.getChannels()) {
            String saveString = curves.getCurve(activeSpace, channel).toSaveString();
            preset.put(channel.getPresetKey(), saveString);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        ColorSpace colorSpace = preset.getEnum(ColorSpace.PRESET_KEY, ColorSpace.class);
        curves.setColorSpace(colorSpace);

        curves.reset();

        // load curves for the active color space
        for (Channel channel : colorSpace.getChannels()) {
            String saveString = preset.get(channel.getPresetKey());
            if (saveString != null) {
                curves.getCurve(colorSpace, channel).setStateFrom(saveString);
            }
        }

        stateChanged();
    }

    public ToneCurves getCurves() {
        return curves;
    }
}
