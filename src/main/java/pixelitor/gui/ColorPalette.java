/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.gui;

import pixelitor.gui.utils.DialogBuilder;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridLayout;

public class ColorPalette extends JPanel {
    int brightnessVariations = 10;
    int hueVariations = 20;
    float saturation = 0.9f;

    public ColorPalette() {
        setLayout(new GridLayout(hueVariations, brightnessVariations));

        for (int j = 0; j < hueVariations; j++) {
            for (int i = 0; i < brightnessVariations; i++) {
                float bri = (i + 1) / (float) brightnessVariations;
                float hue = j / (float) hueVariations;

                Color c = Color.getHSBColor(hue, saturation, bri);
                add(new ColorSwatchButton(c, true));
            }
        }
    }

    public static void showInDialog(PixelitorWindow pw) {
        ColorPalette colorPalette = new ColorPalette();

        new DialogBuilder()
                .title("Color Palette")
                .parent(pw)
                .form(colorPalette)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .noGlobalKeyChange()
                .show();
    }

}
