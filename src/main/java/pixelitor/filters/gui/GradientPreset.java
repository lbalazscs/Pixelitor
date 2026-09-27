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

package pixelitor.filters.gui;

import pixelitor.colors.Colors;

import java.awt.Color;
import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable representation of a gradient preset.
 */
public record GradientPreset(String name, float[] thumbPositions, Color[] colors) {
    public GradientPreset {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(thumbPositions, "thumbPositions");
        Objects.requireNonNull(colors, "colors");
        if (thumbPositions.length != colors.length) {
            throw new IllegalArgumentException(
                "thumbPositions length (" + thumbPositions.length
                    + ") must match colors length (" + colors.length + ")");
        }
        thumbPositions = thumbPositions.clone();
        colors = colors.clone();
    }

    public GradientPreset(String name, Color startColor, Color endColor) {
        this(name, new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{
                startColor,
                Colors.averageRgb(startColor, endColor),
                endColor
            });
    }

    @Override
    public float[] thumbPositions() {
        return thumbPositions.clone();
    }

    @Override
    public Color[] colors() {
        return colors.clone();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GradientPreset preset)) {
            return false;
        }
        return Objects.equals(name, preset.name)
            && Arrays.equals(thumbPositions, preset.thumbPositions)
            && Arrays.equals(colors, preset.colors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(name);
        result = 31 * result + Arrays.hashCode(thumbPositions);
        result = 31 * result + Arrays.hashCode(colors);
        return result;
    }
}
