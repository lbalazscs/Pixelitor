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
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class LocalNormalization extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Local Normalization";

    private final RangeParam amplitude = new RangeParam("Amplitude", 0, 2, 60);
    private final RangeParam radius = new RangeParam("Radius", 1, 6, 64);
    private final RangeParam nSmooth = new RangeParam("Neighborhood Smoothness", 0, 5, 40);
    private final RangeParam aSmooth = new RangeParam("Average Smoothness", 0, 20, 40);
    private final BooleanParam cut = new BooleanParam("Constrain Values", true);
    private final IntChoiceParam channel = GMICFilter.createChannelChoice().withDefaultChoice(11);

    public LocalNormalization() {
        initParams(amplitude, radius,
            nSmooth, aSmooth, cut, channel);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_normalize_local",
            amplitude.getValue() + "," +
                radius.getValue() + "," +
                nSmooth.getValue() + "," +
                aSmooth.getValue() + "," +
                cut.isCheckedStr() + "," +
                channel.getValue());
    }
}
