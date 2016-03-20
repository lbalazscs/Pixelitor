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
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

public class ColorPalette extends JPanel {
    private static final int LAYOUT_GAP = 2;

    private int brightnessVariations = 7;
    private int hueVariations = 11;
    private float saturation = 0.9f;
    private float startHue = 0.0f;

    // vertical lists in a horizontal list
    private final List<List<ColorSwatchButton>> buttons;

    private ColorPalette() {
        setLayout(null);

        buttons = new ArrayList<>();
        for (int i = 0; i < brightnessVariations; i++) {
            buttons.add(new ArrayList<>());
        }

        regenerate();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {

                int newHueVariations = (getHeight() - LAYOUT_GAP) / (ColorSwatchButton.SIZE + LAYOUT_GAP);
                int newBrightnessVariations = (getWidth() - LAYOUT_GAP) / (ColorSwatchButton.SIZE + LAYOUT_GAP);

                if (newHueVariations != hueVariations || newBrightnessVariations != brightnessVariations) {
                    hueVariations = newHueVariations;
                    brightnessVariations = newBrightnessVariations;
                    regenerate();
                }
            }
        });
    }

    private void setNewSaturation(float newSat) {
        if (newSat != saturation) {
            saturation = newSat;
            regenerate();
            repaint();
        }
    }

    private void setNewStartHue(float newStartHue) {
        if (newStartHue != startHue) {
            startHue = newStartHue;
            regenerate();
            repaint();
        }
    }

    private void regenerate() {
        removeAll();
        for (int j = 0; j < hueVariations; j++) {
            for (int i = 0; i < brightnessVariations; i++) {
                float bri = (i + 1) / (float) brightnessVariations;
                float hue = startHue + j / (float) hueVariations;
                if (hue > 1.0f) {
                    hue = hue - 1.0f;
                }

                Color c = Color.getHSBColor(hue, saturation, bri);
                addButton(i, j, c);
            }
        }
    }

    private ColorSwatchButton getButton(int x, int y) {
        if (x < buttons.size()) {
            List<ColorSwatchButton> verticalList = buttons.get(x);
            if (y < verticalList.size()) {
                return verticalList.get(y);
            }
        }
        return null;
    }

    private void addNewButtonToList(ColorSwatchButton button, int x, int y) {
        List<ColorSwatchButton> verticalList;
        if (x < buttons.size()) {
            verticalList = buttons.get(x);
            assert y >= verticalList.size();
        } else {
            verticalList = new ArrayList<>();
            buttons.add(verticalList);
        }
        verticalList.add(button);

    }

    private void addButton(int hor, int ver, Color c) {
        ColorSwatchButton button = getButton(hor, ver);
        if (button == null) {
            button = new ColorSwatchButton(c, true);
            addNewButtonToList(button, hor, ver);
        } else {
            button.setColor(c);
        }

        add(button);
        int x = LAYOUT_GAP + hor * (ColorSwatchButton.SIZE + LAYOUT_GAP);
        int y = LAYOUT_GAP + ver * (ColorSwatchButton.SIZE + LAYOUT_GAP);
        button.setLocation(x, y);
        button.setSize(button.getPreferredSize());
    }

    @Override
    public Dimension getPreferredSize() {
        int width = LAYOUT_GAP + brightnessVariations * (ColorSwatchButton.SIZE + LAYOUT_GAP);
        int height = LAYOUT_GAP + hueVariations * (ColorSwatchButton.SIZE + LAYOUT_GAP);
        return new Dimension(width, height);
    }

    public static void showInDialog(PixelitorWindow pw) {
        ColorPalette colorPalette = new ColorPalette();

        JPanel form = new JPanel(new BorderLayout());
        JPanel northPanel = new JPanel(new GridBagLayout());

        JSlider satSlider = new JSlider(0, 100, 90);
        satSlider.setToolTipText("Saturation of the colors");
        satSlider.addChangeListener(e -> colorPalette.setNewSaturation(satSlider.getValue() / 100.0f));

        JSlider hueSlider = new JSlider(0, 100, 0);
        hueSlider.setToolTipText("Rotate the hue of the colors");
        hueSlider.addChangeListener(e -> colorPalette.setNewStartHue(hueSlider.getValue() / 100.0f));

        Insets insets = new Insets(2, 4, 2, 4);
        GridBagConstraints labelConstraint = new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE, insets, 0, 0);
        GridBagConstraints sliderConstraint = new GridBagConstraints(1, 0, 1, 1, 1.0, 0,
                GridBagConstraints.EAST,
                GridBagConstraints.HORIZONTAL, insets, 0, 0);
        northPanel.add(new JLabel("Sat:"), labelConstraint);
        northPanel.add(satSlider, sliderConstraint);
        labelConstraint.gridy = 1;
        northPanel.add(new JLabel("Hue:"), labelConstraint);
        sliderConstraint.gridy = 1;
        northPanel.add(hueSlider, sliderConstraint);

        form.add(northPanel, BorderLayout.NORTH);
        form.add(colorPalette, BorderLayout.CENTER);

        new DialogBuilder()
                .title("Color Palette")
                .parent(pw)
                .form(form)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .noGlobalKeyChange()
                .show();

        Messages.showStatusMessage("Color Palette: enlarge for more colors, right-click to clear the marking");
    }
}
