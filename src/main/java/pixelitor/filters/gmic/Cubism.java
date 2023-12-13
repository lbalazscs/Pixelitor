/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class Cubism extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Cubism";

    private final RangeParam iterations = new RangeParam("Iterations", 0, 2, 10);
    private final RangeParam density = new RangeParam("Density", 0, 50, 200);
    private final RangeParam thickness = new RangeParam("Thickness", 0, 10, 50);
    private final RangeParam angle = new RangeParam("Angle", 0, 90, 360);
    private final RangeParam opacity = new RangeParam("Opacity", 1, 70, 100);
    private final RangeParam smoothness = new RangeParam("Smoothness", 0, 0, 5);

    public Cubism() {
        setParams(iterations, density,
            thickness, angle, opacity, smoothness);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_cubism",
            iterations.getValue() + "," +
                density.getValue() + "," +
                thickness.getValue() + "," +
                angle.getValue() + "," +
                opacity.getPercentage() + "," +
                smoothness.getValue()
        );
    }
}
