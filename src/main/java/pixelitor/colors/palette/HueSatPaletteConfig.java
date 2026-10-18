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
 * Configuration for a palette with hue and saturation sliders.
 */
public class HueSatPaletteConfig implements PaletteConfig {
    private float hueOffset;
    private float saturation;

    public HueSatPaletteConfig(float hueOffset, float saturation) {
        this.hueOffset = hueOffset;
        this.saturation = saturation;
    }

    @Override
    public JPanel createConfigPanel(Runnable onChange) {
        return new SliderPanelBuilder(onChange)
            .add("Hue:", "Rotate the hue of the colors", hueOffset, v -> hueOffset = v)
            .add("Sat:", "Saturation of the colors", saturation, v -> saturation = v)
            .build();
    }

    public float getHueOffset() {
        return hueOffset;
    }

    public float getSaturation() {
        return saturation;
    }
}
