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

package pixelitor.filters.convolve;

import java.util.List;

/**
 * The built-in kernels offered as buttons in the {@link ConvolveParamGUI}.
 * These are not related to the filter presets menu.
 */
final class ConvolvePresets {
    /**
     * A named kernel. The array must not be modified.
     */
    record KernelPreset(String name, float[] kernel) {
    }

    private ConvolvePresets() {
    }

    static List<KernelPreset> forSize(int kernelSize) {
        return switch (kernelSize) {
            case 3 -> KERNELS_3X3;
            case 5 -> KERNELS_5X5;
            default -> throw new IllegalArgumentException("kernelSize = " + kernelSize);
        };
    }

    private static final List<KernelPreset> KERNELS_3X3 = List.of(
        new KernelPreset("Corner Blur", new float[]{
            0.25f, 0.0f, 0.25f,
            0.0f, 0.0f, 0.0f,
            0.25f, 0.0f, 0.25f}),

        new KernelPreset("\"Gaussian\" Blur", new float[]{
            1 / 16.0f, 2 / 16.0f, 1 / 16.0f,
            2 / 16.0f, 4 / 16.0f, 2 / 16.0f,
            1 / 16.0f, 2 / 16.0f, 1 / 16.0f}),

        new KernelPreset("Mean Blur", new float[]{
            0.1115f, 0.1115f, 0.1115f,
            0.1115f, 0.1115f, 0.1115f,
            0.1115f, 0.1115f, 0.1115f}),

        new KernelPreset("Sharpen", new float[]{
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0}),

        new KernelPreset("Sharpen 2", new float[]{
            -1, -1, -1,
            -1, 9, -1,
            -1, -1, -1}),

        new KernelPreset("Edge Detection", new float[]{
            0, -1, 0,
            -1, 4, -1,
            0, -1, 0}),

        new KernelPreset("Edge Detection 2", new float[]{
            -1, -1, -1,
            -1, 8, -1,
            -1, -1, -1}),

        new KernelPreset("Horizontal Edge Detection", new float[]{
            -1, -1, -1,
            0, 0, 0,
            1, 1, 1}),

        new KernelPreset("Vertical Edge Detection", new float[]{
            -1, 0, 1,
            -1, 0, 1,
            -1, 0, 1}),

        new KernelPreset("Emboss", new float[]{
            -2, -2, 0,
            -2, 6, 0,
            0, 0, 0}),

        new KernelPreset("Emboss 2", new float[]{
            -2, 0, 0,
            0, 0, 0,
            0, 0, 2}),

        new KernelPreset("Color Emboss", new float[]{
            -1, -1, 0,
            -1, 1, 1,
            0, 1, 1})
    );

    private static final List<KernelPreset> KERNELS_5X5 = List.of(
        new KernelPreset("Diamond Blur", new float[]{
            0.0f, 0.0f, 0.077f, 0.0f, 0.0f,
            0.0f, 0.077f, 0.077f, 0.077f, 0.0f,
            0.077f, 0.077f, 0.077f, 0.077f, 0.077f,
            0.0f, 0.077f, 0.077f, 0.077f, 0.0f,
            0.0f, 0.0f, 0.077f, 0.0f, 0.0f,
        }),

        new KernelPreset("Motion Blur", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, 0.2f,
            0.0f, 0.0f, 0.0f, 0.2f, 0.0f,
            0.0f, 0.0f, 0.2f, 0.0f, 0.0f,
            0.0f, 0.2f, 0.0f, 0.0f, 0.0f,
            0.2f, 0.0f, 0.0f, 0.0f, 0.0f,
        }),

        new KernelPreset("Find Horizontal Edges", new float[]{
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -2.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -2.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
        }),

        new KernelPreset("Find Vertical Edges", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, -2.0f, 6.0f, -2.0f, -1.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        }),

        new KernelPreset("Find \\ Edges", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, 0.0f, -2.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, -2.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        }),

        new KernelPreset("Find / Edges", new float[]{
            -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -2.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, -2.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, -1.0f,
        }),

        new KernelPreset("Sharpen", new float[]{
            -0.125f, -0.125f, -0.125f, -0.125f, -0.125f,
            -0.125f, 0.25f, 0.25f, 0.25f, -0.125f,
            -0.125f, 0.25f, 1.0f, 0.25f, -0.125f,
            -0.125f, 0.25f, 0.25f, 0.25f, -0.125f,
            -0.125f, -0.125f, -0.125f, -0.125f, -0.125f,
        })
    );
}
