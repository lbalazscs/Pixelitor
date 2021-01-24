
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

package pixelitor.filters.curves;

import com.jhlabs.image.CurvesFilter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.levels.Channel;
import pixelitor.layers.Drawable;

import java.awt.image.BufferedImage;

import static pixelitor.utils.Texts.i18n;

/**
 * Tone ToneCurvesFilter filter
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesFilter extends FilterWithGUI {
    public static final String NAME = i18n("curves");

    private CurvesFilter filter;
    private ToneCurves curves;

    private ToneCurvesGUI lastGUI;

    public ToneCurvesFilter() {
        helpURL = "https://en.wikipedia.org/wiki/Curve_(tonality)";
    }

    @Override
    public FilterGUI createGUI(Drawable dr) {
        lastGUI = new ToneCurvesGUI(this, dr);
        curves = lastGUI.getCurves();
        return lastGUI;
    }

    public void setCurves(ToneCurves curves) {
        this.curves = curves;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CurvesFilter(NAME);
        }
        if (curves == null) {
            return src;
        }

        filter.setCurves(
            curves.getCurve(Channel.RGB).curve,
            curves.getCurve(Channel.RED).curve,
            curves.getCurve(Channel.GREEN).curve,
            curves.getCurve(Channel.BLUE).curve
        );

        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public void randomizeSettings() {
        // not supported yet
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public UserPreset createUserPreset(String presetName) {
        UserPreset preset = new UserPreset(presetName, NAME);

        Channel[] channels = Channel.values();
        for (Channel channel : channels) {
            String saveString = curves.getCurve(channel).toSaveString();
            preset.put(channel.getPresetKey(), saveString);
        }

        return preset;
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        Channel[] channels = Channel.values();
        for (Channel channel : channels) {
            String saveString = preset.get(channel.getPresetKey());
            curves.getCurve(channel).setStateFrom(saveString);
        }

        lastGUI.stateChanged();
    }
}