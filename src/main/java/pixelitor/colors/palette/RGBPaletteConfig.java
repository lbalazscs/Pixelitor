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

package pixelitor.colors.palette;

import javax.swing.*;

/**
 * Configuration for RGB color mixing with cyan-red, magenta-green, and yellow-blue sliders.
 */
public class RGBPaletteConfig implements PaletteConfig {
    public static final float NEUTRAL = 0.5f;

    private float cyanRed = NEUTRAL;
    private float magentaGreen = NEUTRAL;
    private float yellowBlue = NEUTRAL;

    @Override
    public JPanel createConfigPanel(Runnable onChange) {
        return new SliderPanelBuilder(onChange)
            .add("C-R:", "Cyan-red shift", cyanRed, v -> cyanRed = v)
            .add("M-G:", "Magenta-green shift", magentaGreen, v -> magentaGreen = v)
            .add("Y-B:", "Yellow-blue shift", yellowBlue, v -> yellowBlue = v)
            .build();
    }

    public float getCyanRed() {
        return cyanRed;
    }

    public float getMagentaGreen() {
        return magentaGreen;
    }

    public float getYellowBlue() {
        return yellowBlue;
    }
}
