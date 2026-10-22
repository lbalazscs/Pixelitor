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

import pixelitor.filters.channels.ChannelAdjustmentFilter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.Help;
import pixelitor.layers.Filterable;

import java.io.Serial;

import static pixelitor.utils.Texts.i18n;

/**
 * Filter that applies tone-curve adjustments in the sRGB and Oklab color spaces.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesFilter extends ChannelAdjustmentFilter<ToneCurves> {
    public static final String NAME = i18n("curves");

    @Serial
    private static final long serialVersionUID = 3679501445608294764L;

    public ToneCurvesFilter() {
        super(new ToneCurves());
        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Curve_(tonality)");
    }

    @Override
    protected FilterGUI createChannelGUI(Filterable layer) {
        return new ToneCurvesGUI(this, layer);
    }
}
