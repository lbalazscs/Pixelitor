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

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class Brushify extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Brushify";

    private final IntChoiceParam shape = new IntChoiceParam("Shape", new Item[]{
        new Item("Ellipse", 7),
        new Item("Rectangle", 2),
        new Item("Diamond", 3),
        new Item("Pentagon", 4),
        new Item("Hexagon", 5),
        new Item("Octogon", 6),
        new Item("Gaussian", 8),
        new Item("Star", 9),
        new Item("Heart", 10)
    });
    private final RangeParam ratio = new RangeParam("Ratio", 0, 25, 100);
    private final RangeParam numberOfSizes = new RangeParam("Number of Sizes", 1, 4, 16);
    private final RangeParam maximalSize = new RangeParam("Maximal Size", 1, 64, 128);
    private final RangeParam minimalSize = new RangeParam("Minimal Size", 0, 25, 100);
    private final RangeParam numberOfOrientations = new RangeParam("Number of Orientations", 1, 12, 24);
    private final RangeParam fuzziness = new RangeParam("Fuzziness", 0, 0, 10);
    private final RangeParam smoothness = new RangeParam("Smoothness", 0, 2, 10);
    private final IntChoiceParam lightType = new IntChoiceParam("Light Type", new Item[]{
        new Item("Full", 4),
        new Item("None", 0),
        new Item("Flat", 1),
        new Item("Darken", 2),
        new Item("Lighten", 3),
    });
    private final RangeParam lightStrength = new RangeParam("Light Strength", 0, 20, 100);
    private final RangeParam opacity = new RangeParam("Opacity", 0, 50, 100);
    private final RangeParam density = new RangeParam("Density", 0, 30, 100);
    private final RangeParam contourCoherence = new RangeParam("Contour Coherence", 0, 100, 100);
    private final RangeParam orientationCoherence = new RangeParam("Orientation Coherence", 0, 100, 100);
    private final RangeParam gradientSmoothness = new RangeParam("Gradient Smoothness", 0, 1, 10);
    private final RangeParam structureSmoothness = new RangeParam("Structure Smoothness", 0, 5, 10);
    private final RangeParam primaryAngle = new RangeParam("Primary Angle", -180, 0, 180);
    private final RangeParam angleDispersion = new RangeParam("Angle Dispersion", 0, 20, 100);

    public Brushify() {
        setParams(shape, ratio,
            numberOfSizes, maximalSize, minimalSize,
            numberOfOrientations, fuzziness, smoothness,
            lightType, lightStrength, opacity,
            density, contourCoherence, orientationCoherence,
            gradientSmoothness, structureSmoothness,
            primaryAngle, angleDispersion);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_brushify",
            shape.getValue() + "," +
                ratio.getPercentage() + "," +
                numberOfSizes.getValue() + "," +
                maximalSize.getValue() + "," +
                minimalSize.getValue() + "," +
                numberOfOrientations.getValue() + "," +
                fuzziness.getValue() + "," +
                smoothness.getValue() + "," +
                lightType.getValue() + "," +
                lightStrength.getPercentage() + "," +
                opacity.getPercentage() + "," +
                density.getValue() + "," +
                contourCoherence.getPercentage() + "," +
                orientationCoherence.getPercentage() + "," +
                gradientSmoothness.getValue() + "," +
                structureSmoothness.getValue() + "," +
                primaryAngle.getValue() + "," +
                angleDispersion.getPercentage() + ",0");
    }
}
