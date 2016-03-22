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

package pixelitor.colors.palette;

import pixelitor.colors.FgBgColors;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

public class VariationsPanel extends JPanel {
    private static final int LAYOUT_GAP = 2;
    private final Palette palette;

    private int numCols;
    private int numRows;

    // vertical lists in a horizontal list
    private final List<List<ColorSwatchButton>> buttons;

    private VariationsPanel(Palette palette) {
        this.palette = palette;

        this.numCols = palette.getNumCols();
        this.numRows = palette.getNumRows();

        setLayout(null);

        buttons = new ArrayList<>();
        for (int i = 0; i < numCols; i++) {
            buttons.add(new ArrayList<>());
        }

        regenerate(numRows, numCols);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int newNumRows = (getHeight() - LAYOUT_GAP) / (ColorSwatchButton.SIZE + LAYOUT_GAP);
                int newNumBrightnessLevels = (getWidth() - LAYOUT_GAP) / (ColorSwatchButton.SIZE + LAYOUT_GAP);
                setNewSizes(newNumRows, newNumBrightnessLevels);
            }
        });
    }

    private void setNewSizes(int newNumRows, int newNumCols) {
        if (newNumRows != numRows
                || newNumCols != numCols) {
            palette.setSize(newNumRows, newNumCols);
            regenerate(newNumRows, newNumCols);
        }
    }

    public void configChanged() {
        palette.onConfigChange();
        regenerate(numRows, numCols);
        repaint();
    }

    private void regenerate(int newNumRows, int newNumCols) {
        if (newNumRows < numRows || newNumCols < numCols) {
            // remove only the unnecessary
            int count = getComponentCount();
            for (int i = count - 1; i >= 0; i--) {
                ColorSwatchButton swatch = (ColorSwatchButton) getComponent(i);
                if (swatch.getXPos() >= newNumCols || swatch.getYPos() >= newNumRows) {
                    remove(i);
                }
            }
        }
        numRows = newNumRows;
        numCols = newNumCols;

        palette.addButtons(this);
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

    public void addButton(int hor, int ver, Color c) {
        ColorSwatchButton button = getButton(hor, ver);
        if (button == null) {
            button = new ColorSwatchButton(c, hor, ver);
            addNewButtonToList(button, hor, ver);
        } else {
            button.setColor(c);
        }

        if (button.getParent() == null) {
            add(button);
        }

        int x = LAYOUT_GAP + hor * (ColorSwatchButton.SIZE + LAYOUT_GAP);
        int y = LAYOUT_GAP + ver * (ColorSwatchButton.SIZE + LAYOUT_GAP);
        button.setLocation(x, y);
        button.setSize(button.getPreferredSize());
    }

    @Override
    public Dimension getPreferredSize() {
        int width = LAYOUT_GAP + numCols * (ColorSwatchButton.SIZE + LAYOUT_GAP);
        int height = LAYOUT_GAP + numRows * (ColorSwatchButton.SIZE + LAYOUT_GAP);
        return new Dimension(width, height);
    }

    public static void showVariationsDialog(PixelitorWindow pw, boolean fg) {
        Color refColor = fg ? FgBgColors.getFG() : FgBgColors.getBG();
        showInDialog(pw, new VariationsPalette(refColor, 7, 10), fg);
    }

    public static void showMixDialog(PixelitorWindow pw, boolean fg) {
        ColorMixPalette palette = new ColorMixPalette(7, 10, fg);
        showInDialog(pw, palette, fg);
    }

    public static void showRGBMixDialog(PixelitorWindow pw, boolean fg) {
        RGBColorMixPalette palette = new RGBColorMixPalette(7, 10, fg);
        showInDialog(pw, palette, fg);
    }

    public static void showInDialog(PixelitorWindow pw, Palette palette, boolean fg) {
        VariationsPanel variationsPanel = new VariationsPanel(palette);

        JPanel form = new JPanel(new BorderLayout());

        form.add(palette.getConfig().createConfigPanel(variationsPanel), BorderLayout.NORTH);
        form.add(variationsPanel, BorderLayout.CENTER);

        new DialogBuilder()
                .title(palette.getDialogTitle(fg))
                .parent(pw)
                .form(form)
                .notModal()
                .noOKButton()
                .noCancelButton()
                .noGlobalKeyChange()
                .show();

        Messages.showStatusMessage(palette.getStatusHelp(fg));
    }
}
