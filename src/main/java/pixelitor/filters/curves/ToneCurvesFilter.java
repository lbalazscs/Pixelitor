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
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.util.Channel;
import pixelitor.layers.Filterable;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.utils.Texts.i18n;

/**
 * Filter that applies tone curves adjustments for RGB and individual color channels.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesFilter extends FilterWithGUI {
    public static final String NAME = i18n("curves");

    @Serial
    private static final long serialVersionUID = 3679501445608294764L;

    private CurvesFilter filter; // the underlying JHLabs filter
    private final ToneCurves curves;  // the data model for the tone curves

    // reference to the last-used GUI instance for this filter
    private transient ToneCurvesGUI lastGUI;

    public ToneCurvesFilter() {
        helpURL = "https://en.wikipedia.org/wiki/Curve_(tonality)";
        curves = new ToneCurves();
    }

    @Override
    public ToneCurvesGUI createGUI(Filterable layer, boolean reset) {
        if (reset) {
            curves.reset();
            curves.setActiveChannel(Channel.RGB);
        }
        lastGUI = new ToneCurvesGUI(this, layer);
        return lastGUI;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
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
        for (Channel channel : Channel.values()) {
            String saveString = curves.getCurve(channel).toSaveString();
            preset.put(channel.getPresetKey(), saveString);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        for (Channel channel : Channel.values()) {
            String saveString = preset.get(channel.getPresetKey());
            curves.getCurve(channel).setStateFrom(saveString);
        }

        stateChanged();
    }

    public ToneCurves getCurves() {
        return curves;
    }
}