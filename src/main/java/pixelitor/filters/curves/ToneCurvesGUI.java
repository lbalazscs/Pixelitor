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

import pixelitor.filters.Filter;
import pixelitor.filters.gui.ChannelAdjustmentGUI;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.layers.Filterable;

/**
 * The {@link FilterGUI} for the {@link ToneCurvesFilter}.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesGUI extends ChannelAdjustmentGUI<ToneCurves> {
    public ToneCurvesGUI(ToneCurvesFilter filter, Filterable layer) {
        this(filter, layer, filter.getModel());
    }

    public ToneCurvesGUI(Filter filter, Filterable layer, ToneCurves curves) {
        super(filter, layer, curves, new ToneCurvesPanel(curves));
    }

    @Override
    protected void refreshEditor() {
        editorComponent.repaint();
    }
}
