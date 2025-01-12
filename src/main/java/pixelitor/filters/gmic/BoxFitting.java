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

package pixelitor.filters.gmic;

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class BoxFitting extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Box Fitting";

    private final RangeParam minSize = new RangeParam("Minimal Size", 1, 10, 100);
    private final RangeParam maxSize = new RangeParam("Maximal Size", 1, 50, 100);
    private final RangeParam density = new RangeParam("Initial Density", 0, 25, 100);
    private final RangeParam minSpacing = new RangeParam("Minimal Spacing", 1, 1, 100);
    private final BooleanParam transparency = new BooleanParam("Transparency");

    public BoxFitting() {
        maxSize.ensureHigherValueThan(minSize);

        setParams(minSize, maxSize,
            density, minSpacing,
            transparency).withReseedGmicAction(this);
    }

    @Override
    public List<String> getArgs() {
        return List.of("srand", String.valueOf(seed), "fx_boxfitting",
            minSize.getValue() + "," +
                maxSize.getValue() + "," +
                density.getPercentage() + "," +
                minSpacing.getValue() + "," +
                transparency.isCheckedStr());
    }
}
