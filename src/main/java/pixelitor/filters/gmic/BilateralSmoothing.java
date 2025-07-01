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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class BilateralSmoothing extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Bilateral Smoothing";

    private final GroupedRangeParam variance = new GroupedRangeParam("Variance",
        new RangeParam[]{
            new RangeParam("Spatial", 0, 10, 100),
            new RangeParam("Value", 0, 7, 100),
        }, false).notLinkable();
    private final RangeParam iterations = new RangeParam("Iterations", 1, 2, 10);
    private final IntChoiceParam channel = GMICFilter.createChannelChoice();

    public BilateralSmoothing() {
        initParams(variance, iterations, channel);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_smooth_bilateral",
            variance.getValue(0) + "," +
                variance.getValue(1) + "," +
                iterations.getValue() + "," +
                channel.getValue());
    }
}
