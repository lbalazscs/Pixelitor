/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * The panel containing the color swatch buttons in a grid.
 */
public class PalettePanel extends JPanel {
    private static final int GAP = 2; // Spacing between swatches
    private final Palette palette;
    private final ColorSwatchClickHandler clickHandler;

    private int numCols;
    private int numRows;

    // vertical lists in a horizontal list
    private final List<List<ColorSwatchButton>> grid;

    private PalettePanel(Palette palette, ColorSwatchClickHandler clickHandler) {
        this.palette = palette;
        this.clickHandler = clickHandler;

        numCols = palette.getColumnCount();
        numRows = palette.getRowCount();

        setLayout(null); // manual button positioning

        grid = new ArrayList<>();
        for (int i = 0; i < numCols; i++) {
            grid.add(new ArrayList<>());
        }

        regenerate(numRows, numCols);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Calculate new dimensions based on available space and button size
                int newNumRows = (getHeight() - GAP) / (ColorSwatchButton.SIZE + GAP);
                int newNumCols = (getWidth() - GAP) / (ColorSwatchButton.SIZE + GAP);
                setNewSizes(newNumRows, newNumCols);
            }
        });
    }

    private void setNewSizes(int newNumRows, int newNumCols) {
        if (newNumRows != numRows || newNumCols != numCols) {
            palette.setDimensions(newNumRows, newNumCols);
            regenerate(newNumRows, newNumCols);
        }
    }

    public void onConfigChanged() {
        palette.onConfigChanged();
        regenerate(numRows, numCols);
        repaint();
    }

    private void regenerate(int newNumRows, int newNumCols) {
        ColorSwatchButton.lastClickedSwatch = null;

        // If shrinking the palette, remove only the buttons
        // that are outside the new dimensions.
        if (newNumRows < numRows || newNumCols < numCols) {
            int count = getComponentCount();
            for (int i = count - 1; i >= 0; i--) {
                var swatch = (ColorSwatchButton) getComponent(i);
                if (swatch.getGridX() >= newNumCols || swatch.getGridY() >= newNumRows) {
                    remove(i);
                }
            }
        }

        // update dimensions
        numRows = newNumRows;
        numCols = newNumCols;

        // add new buttons or change the color of the existing ones
        // according to the palette's rules
        palette.addButtons(this);
    }

    /**
     * Returns the swatch at the given grid position or null if not found.
     */
    private ColorSwatchButton getButton(int col, int row) {
        if (col < grid.size()) { // check if the column exists
            List<ColorSwatchButton> verticalSwatches = grid.get(col);
            if (row < verticalSwatches.size()) { // check if the row exists
                return verticalSwatches.get(row);
            }
        }
        return null;
    }

    private void addNewButtonToGrid(ColorSwatchButton button, int col, int row) {
        List<ColorSwatchButton> column;
        if (col < grid.size()) {
            // get the already existing column
            column = grid.get(col);

            // ensure we're adding at the end
            assert row >= column.size();
        } else {
            // start a new column
            column = new ArrayList<>();
            grid.add(column);
        }
        column.add(button);
    }

    // adds a new button or changes the color of an existing button
    public void addButton(int col, int row, Color c) {
        ColorSwatchButton button = getButton(col, row);
        if (button == null) {
            button = new ColorSwatchButton(c, clickHandler, col, row);
            addNewButtonToGrid(button, col, row);

            int x = GAP + col * (ColorSwatchButton.SIZE + GAP);
            int y = GAP + row * (ColorSwatchButton.SIZE + GAP);
            button.setLocation(x, y);
            button.setSize(button.getPreferredSize());
        } else {
            button.setColor(c);
        }

        if (button.getParent() == null) {
            add(button);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int width = GAP + numCols * (ColorSwatchButton.SIZE + GAP);
        int height = GAP + numRows * (ColorSwatchButton.SIZE + GAP);
        return new Dimension(width, height);
    }

    public static void showFGVariationsDialog(PixelitorWindow pw) {
        var palette = new VariationsPalette(getFGColor(),
            "Foreground Color Variations");
        showDialog(pw, palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showBGVariationsDialog(PixelitorWindow pw) {
        var palette = new VariationsPalette(getBGColor(),
            "Background Color Variations");
        showDialog(pw, palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showFilterVariationsDialog(Window window, Color refColor,
                                                  ColorSwatchClickHandler clickHandler) {
        var palette = new VariationsPalette(refColor,
            "Filter Color Variations");
        showDialog(window, palette, clickHandler);
    }

    public static void showHSBMixDialog(PixelitorWindow pw, boolean fg) {
        var palette = new HSBColorMixPalette(fg);
        showDialog(pw, palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showRGBMixDialog(PixelitorWindow pw, boolean fg) {
        var palette = new RGBColorMixPalette(fg);
        showDialog(pw, palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showDialog(Window window, Palette palette,
                                  ColorSwatchClickHandler clickHandler) {
        assert window != null;

        var palettePanel = new PalettePanel(palette, clickHandler);

        JPanel form = new JPanel(new BorderLayout());

        form.add(palette.getConfig()
            .createConfigPanel(palettePanel), NORTH);
        form.add(palettePanel, CENTER);

        new DialogBuilder()
            .title(palette.getDialogTitle())
            .owner(window)
            .content(form)
            .notModal()
            .noOKButton()
            .noCancelButton()
            .show();

        Messages.showStatusMessage(palette.getHelpText());
    }
}
