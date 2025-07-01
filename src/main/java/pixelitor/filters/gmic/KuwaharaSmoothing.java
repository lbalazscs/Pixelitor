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

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class KuwaharaSmoothing extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Kuwahara Smoothing";

    private final RangeParam iterations = new RangeParam("Iterations", 1, 2, 20);
    private final RangeParam radius = new RangeParam("Radius", 1, 5, 30);
    private final IntChoiceParam channel = GMICFilter.createChannelChoice();
    private final IntChoiceParam valueAction = GMICFilter.createValueAction();

    public KuwaharaSmoothing() {
        initParams(iterations, radius, channel, valueAction);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_kuwahara",
            iterations.getValue() + "," +
                radius.getValue() + "," +
                channel.getValue() + "," +
                valueAction.getValue(),
            "cut", "0,255"); // doesn't help
    }
}
