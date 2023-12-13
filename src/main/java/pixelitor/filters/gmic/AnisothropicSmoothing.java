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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class AnisothropicSmoothing extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Anisothropic Smoothing";

    private final RangeParam amplitude = new RangeParam("Amplitude", 0, 60, 1000);
    private final RangeParam sharpness = new RangeParam("Sharpness", 0, 70, 200);
    private final RangeParam anisotropy = new RangeParam("Anisotropy", 0, 30, 100);
    private final RangeParam gradientSmoothness = new RangeParam("Gradient Smoothness", 0, 60, 1000);
    private final RangeParam tensorSmoothness = new RangeParam("Tensor Smoothness", 0, 110, 1000);
    private final RangeParam spatialPrecision = new RangeParam("Spatial Precision", 10, 80, 200);
    private final RangeParam angularPrecision = new RangeParam("Angular Precision", 1, 30, 180);
    private final RangeParam valuePrecision = new RangeParam("Value Precision", 10, 200, 500);
    private final IntChoiceParam interpolation = new IntChoiceParam("Interpolation", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Nearest Neighbor", 0),
        new IntChoiceParam.Item("Linear", 1),
        new IntChoiceParam.Item("Runge-Kutta", 2)
    });
    private final BooleanParam fastApproximation = new BooleanParam("Fast Approximation", true);
    private final RangeParam iterations = new RangeParam("Iterations", 1, 1, 10);
    private final IntChoiceParam channel = GMICFilter.createChannelChoice();

    public AnisothropicSmoothing() {
        setParams(amplitude, sharpness, anisotropy,
            gradientSmoothness, tensorSmoothness,
            spatialPrecision, angularPrecision, valuePrecision,
            interpolation, fastApproximation, iterations, channel);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_smooth_anisotropic",
            amplitude.getValue() + "," +
                sharpness.getPercentage() + "," +
                anisotropy.getPercentage() + "," +
                gradientSmoothness.getPercentage() + "," +
                tensorSmoothness.getPercentage() + "," +
                spatialPrecision.getPercentage() + "," +
                angularPrecision.getValue() + "," +
                valuePrecision.getPercentage() + "," +
                interpolation.getValue() + "," +
                fastApproximation.isCheckedStr() + "," +
                iterations.getValue() + "," +
                channel.getValue());
    }
}
