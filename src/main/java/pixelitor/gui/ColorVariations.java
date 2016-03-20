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

import pixelitor.FgBgColors;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

public class ColorVariations extends JPanel {
    private static final int LAYOUT_GAP = 2;
    private final boolean fg;
    private final List<ColorSwatchButton> buttons;
    private int numButtons;

    private final ColorVariationsType type;

    private ColorVariations(ColorVariationsType type, boolean fg) {
        this.type = type;
        this.fg = fg;
        setLayout(new FlowLayout(FlowLayout.LEFT, LAYOUT_GAP, LAYOUT_GAP));
        buttons = new ArrayList<>();

        Color color;
        Color otherColor;
        if (fg) {
            color = FgBgColors.getFG();
            otherColor = FgBgColors.getBG();
        } else {
            color = FgBgColors.getBG();
            otherColor = FgBgColors.getFG();
        }

        regenerate(color, otherColor, 10);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int newNum = (getWidth() - LAYOUT_GAP) / (ColorSwatchButton.SIZE + LAYOUT_GAP);
                if (newNum != numButtons) {
                    numButtons = newNum;
                    regenerate(color, otherColor, numButtons);
                }
            }
        });
    }

    private void regenerate(Color color, Color otherColor, int num) {
        removeAll();
        Color[] colors = generateColors(color, otherColor, num);

        for (int i = 0; i < num; i++) {
            addButton(i, colors[i]);
        }
        repaint();
    }

    private Color[] generateColors(Color orig, Color otherColor, int num) {
        float[] hsb = Color.RGBtoHSB(orig.getRed(), orig.getGreen(), orig.getBlue(), null);
        float hue = hsb[0];
        float sat = hsb[1];
        float bri = hsb[2];

        return type.generate(hue, sat, bri, num, otherColor);
    }

    private void addButton(int index, Color color) {
        ColorSwatchButton button;
        if (buttons.size() - 1 < index) {
            button = new ColorSwatchButton(color, fg);
            buttons.add(button);
        } else {
            button = buttons.get(index);
            button.setColor(color);
        }
        add(button);
    }

    public static void showInDialog(PixelitorWindow pw, boolean fg, ColorVariationsType type) {
        ColorVariations colorVariations = new ColorVariations(type, fg);

        String title = (fg ? "Foreground " : "Background ")
                + type.getName()
                + " Variations";

        new DialogBuilder()
                .title(title)
                .parent(pw)
                .form(colorVariations)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .noGlobalKeyChange()
                .show();

        Messages.showStatusMessage("Color Variations: enlarge for more colors, right-click to clear the marking");
    }
}


