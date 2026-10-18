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

import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static pixelitor.colors.FgBgColors.getBgColor;
import static pixelitor.colors.FgBgColors.getFgColor;

/**
 * A panel that displays color swatches in a grid. It can display both
 * dynamically generated palettes and static palettes with a fixed list of colors.
 */
public class PalettePanel extends JPanel implements Scrollable {
    private static final int GAP = 2; // spacing between swatches and around the grid
    private static final int STEP = ColorSwatchButton.SIZE + GAP;
    private static final int DEFAULT_STATIC_COLUMNS = 10;

    private final Palette palette;
    private final ColorSwatchClickHandler clickHandler;
    private final SwatchSelection selection = new SwatchSelection();
    private final List<ColorSwatchButton> swatches = new ArrayList<>();

    private int numCols;
    private int numRows;
    private int laidOutCols = -1;

    private PalettePanel(Palette palette, ColorSwatchClickHandler clickHandler) {
        this.palette = palette;
        this.clickHandler = clickHandler;

        // swatches are absolutely positioned in a grid
        // that depends on the current panel size
        setLayout(null);

        // static palettes start with a reasonable default column count
        // that will be immediately recalculated on first display/resize
        numCols = palette instanceof DynamicPalette dp
            ? dp.getNumCols()
            : DEFAULT_STATIC_COLUMNS;
        rebuildGrid(); // also sets numRows

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize();
            }
        });
    }

    private void handleResize() {
        switch (palette) {
            case StaticPalette _ -> handleStaticPaletteResize();
            case DynamicPalette dp -> handleDynamicPaletteResize(dp);
        }
    }

    // static palette: reflow the existing colors
    private void handleStaticPaletteResize() {
        int newNumCols = fittingCells(getWidth());
        if (newNumCols != numCols) {
            numCols = newNumCols;
            rebuildGrid();
        }
    }

    // dynamic palette: regenerate colors based on new grid size
    private void handleDynamicPaletteResize(DynamicPalette dp) {
        int newNumRows = fittingCells(getHeight());
        int newNumCols = fittingCells(getWidth());
        if (newNumRows != dp.getNumRows() || newNumCols != numCols) {
            numCols = newNumCols;
            dp.setGridSize(newNumRows, newNumCols);
            rebuildGrid();
        }
    }

    private void refreshColors() {
        List<Color> colors = palette.getColors();
        if (colors.size() == swatches.size()) {
            selection.clear();
            for (int i = 0; i < colors.size(); i++) {
                swatches.get(i).setColor(colors.get(i));
            }
            repaint();
        } else {
            // should not happen, because the sliders don't change
            // the number of colors, but as a defensive fallback:
            rebuildGrid();
        }
    }

    private void rebuildGrid() {
        selection.clear();

        List<Color> colors = palette.getColors();
        int newSize = colors.size();
        int oldSize = swatches.size();
        numRows = Math.ceilDiv(newSize, numCols);

        boolean colsChanged = (numCols != laidOutCols);
        laidOutCols = numCols;

        int min = Math.min(oldSize, newSize);
        for (int i = 0; i < min; i++) {
            ColorSwatchButton swatch = swatches.get(i);
            swatch.setColor(colors.get(i));
            if (colsChanged) {
                setSwatchLocation(swatch, i % numCols, i / numCols);
            }
        }

        for (int i = min; i < newSize; i++) {
            addSwatch(i % numCols, i / numCols, colors.get(i));
        }

        while (swatches.size() > newSize) {
            ColorSwatchButton swatch = swatches.removeLast();
            remove(swatch);
        }

        revalidate();
        repaint();
    }

    // creates a swatch for the given color and places it at the given grid position
    private void addSwatch(int col, int row, Color color) {
        var button = new ColorSwatchButton(color, clickHandler, selection);
        setSwatchLocation(button, col, row);
        button.setSize(button.getPreferredSize());
        add(button);
        swatches.add(button);
    }

    private static void setSwatchLocation(ColorSwatchButton button, int col, int row) {
        button.setLocation(GAP + col * STEP, GAP + row * STEP);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = GAP + numCols * STEP;
        int height = GAP + numRows * STEP;
        return new Dimension(width, height);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // scroll by one swatch at a time
        return STEP;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        // scroll by a page (the height of the viewport)
        return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // ensure that a static palette's panel shrinks when the
        // dialog shrinks, triggering a reflow
        return palette instanceof StaticPalette;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // never track viewport height, so the vertical scrollbar can appear
        return false;
    }

    public static void showVariationsDialog(boolean fg) {
        var palette = new VariationsPalette(
            fg ? getFgColor() : getBgColor(),
            fg ? "Foreground Color Variations" : "Background Color Variations"
        );
        showDialog(palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showFilterVariationsDialog(Window owner,
                                                  Color refColor,
                                                  ColorSwatchClickHandler clickHandler) {
        var palette = new VariationsPalette(refColor,
            "Filter Color Variations");
        showDialog(palette, clickHandler, owner);
    }

    public static void showHSBMixDialog(boolean fg) {
        var palette = new HSBColorMixPalette(fg);
        showDialog(palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showRGBMixDialog(boolean fg) {
        var palette = new RGBColorMixPalette(fg);
        showDialog(palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showStaticPaletteDialog(String dialogTitle) {
        // unfinished feature: for the time being it shows 100 random colors
        List<Color> colors = IntStream.range(0, 100)
            .mapToObj(_ -> Rnd.createRandomColor())
            .toList();

        var palette = new StaticPalette(dialogTitle, colors);
        showDialog(palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showDialog(Palette palette,
                                  ColorSwatchClickHandler clickHandler) {
        showDialog(palette, clickHandler, PixelitorWindow.get());
    }

    private static void showDialog(Palette palette,
                                   ColorSwatchClickHandler clickHandler,
                                   Window owner) {
        var palettePanel = new PalettePanel(palette, clickHandler);

        JComponent swatchArea = palettePanel;
        // for static palettes, wrap the panel in a scroll pane
        // (dynamic palettes adapt to the available space)
        if (palette instanceof StaticPalette) {
            JScrollPane scrollPane = new JScrollPane(palettePanel,
                VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            swatchArea = scrollPane;
        }

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(palette.getConfig().createConfigPanel(palettePanel::refreshColors), NORTH);
        contentPanel.add(swatchArea, CENTER);

        new DialogBuilder()
            .title(palette.getDialogTitle())
            .owner(owner)
            .content(contentPanel)
            .modeless()
            .noOKButton()
            .noCancelButton()
            .show();

        Messages.showStatusMessage(palette.getHelpText(clickHandler));
    }

    private static int fittingCells(int pixels) {
        return Math.max(1, (pixels - GAP) / STEP);
    }
}
