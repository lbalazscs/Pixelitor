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
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * A panel that displays color swatches in a grid. It can display both
 * dynamically generated palettes and static, fixed-size palettes.
 */
public class PalettePanel extends JPanel implements Scrollable {
    private static final int GAP = 2; // spacing between swatches
    private final Palette palette;
    private final ColorSwatchClickHandler clickHandler;

    private int numCols;
    private int numRows;

    // a horizontal list of columns, where each column is a vertical list of buttons
    private final List<List<ColorSwatchButton>> grid = new ArrayList<>();

    private PalettePanel(Palette palette, ColorSwatchClickHandler clickHandler) {
        this.palette = palette;
        this.clickHandler = clickHandler;

        setLayout(null); // manual button positioning

        // initial setup based on palette type
        if (palette instanceof DynamicPalette dynamicPalette) {
            numCols = dynamicPalette.getColumnCount();
            numRows = dynamicPalette.getRowCount();
        } else {
            // For static palettes, start with a reasonable default column count.
            // It will be immediately recalculated on first display/resize.
            numCols = 10;
            numRows = (int) Math.ceil((double) palette.getColors().size() / numCols);
        }

        updateGrid();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize();
            }
        });
    }

    private void handleResize() {
        switch (palette) {
            case StaticPalette s -> handleStaticPaletteResize();
            case DynamicPalette d -> handleDynamicPaletteResize();
        }
    }

    // static palette: reflow the existing colors
    private void handleStaticPaletteResize() {
        int newNumCols = Math.max(1, (getWidth() - GAP) / (ColorSwatchButton.SIZE + GAP));
        if (newNumCols != numCols) {
            numCols = newNumCols;
            int totalColors = palette.getColors().size();
            numRows = (totalColors == 0) ? 0 : (int) Math.ceil((double) totalColors / numCols);
            updateGrid();
            revalidate(); // update preferred size for scrollbars
        }
    }

    // dynamic palette: regenerate colors based on new grid size
    private void handleDynamicPaletteResize() {
        int newNumRows = Math.max(1, (getHeight() - GAP) / (ColorSwatchButton.SIZE + GAP));
        int newNumCols = Math.max(1, (getWidth() - GAP) / (ColorSwatchButton.SIZE + GAP));

        if (newNumRows != numRows || newNumCols != numCols) {
            numRows = newNumRows;
            numCols = newNumCols;
            ((DynamicPalette) palette).setGridSize(newNumRows, newNumCols);
            updateGrid();
        }
    }

    public void onConfigChanged() {
        palette.onConfigChanged();
        updateGrid();
    }

    private void updateGrid() {
        ColorSwatchButton.lastClickedSwatch = null;
        // clear the panel and the internal grid structure
        removeAll();
        grid.clear();

        List<Color> colors = palette.getColors();
        if (colors.isEmpty()) {
            repaint();
            return;
        }

        // populate the grid with new buttons
        for (int i = 0; i < colors.size(); i++) {
            int col = i % numCols;
            int row = i / numCols;
            addButton(col, row, colors.get(i));
        }

        revalidate();
        repaint();
    }

    // adds a new button
    private void addButton(int col, int row, Color c) {
        ColorSwatchButton button = new ColorSwatchButton(c, clickHandler, col, row);

        // ensure column list exists
        while (grid.size() <= col) {
            grid.add(new ArrayList<>());
        }
        List<ColorSwatchButton> column = grid.get(col);
        // buttons should be added in order
        assert row == column.size();
        column.add(button);

        int x = GAP + col * (ColorSwatchButton.SIZE + GAP);
        int y = GAP + row * (ColorSwatchButton.SIZE + GAP);
        button.setLocation(x, y);
        button.setSize(button.getPreferredSize());

        add(button);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = GAP + numCols * (ColorSwatchButton.SIZE + GAP);
        int height = GAP + numRows * (ColorSwatchButton.SIZE + GAP);
        return new Dimension(width, height);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // scroll by one swatch at a time
        return ColorSwatchButton.SIZE + GAP;
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

    public static void showVariationsDialog(PixelitorWindow pw, boolean fg) {
        var palette = new VariationsPalette(
            fg ? getFGColor() : getBGColor(),
            fg ? "Foreground Color Variations" : "Background Color Variations"
        );
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

    public static void showStaticPaletteDialog(Window window, String title) {
        List<Color> colors = IntStream.range(0, 100)
            .mapToObj(i -> Rnd.createRandomColor())
            .toList();

        var palette = new StaticPalette(title, colors);
        showDialog(window, palette, ColorSwatchClickHandler.STANDARD);
    }

    public static void showDialog(Window window, Palette palette,
                                  ColorSwatchClickHandler clickHandler) {
        assert window != null;

        var palettePanel = new PalettePanel(palette, clickHandler);

        JComponent content = palettePanel;
        // for static palettes, wrap the panel in a scroll pane
        // (dynamic palettes adapt to the available space)
        if (palette instanceof StaticPalette) {
            JScrollPane scrollPane = new JScrollPane(palettePanel,
                VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            content = scrollPane;
        }

        JPanel fullPanel = new JPanel(new BorderLayout());
        fullPanel.add(palette.getConfig().createConfigPanel(palettePanel), NORTH);
        fullPanel.add(content, CENTER);

        new DialogBuilder()
            .title(palette.getDialogTitle())
            .owner(window)
            .content(fullPanel)
            .modeless()
            .noOKButton()
            .noCancelButton()
            .show();

        Messages.showStatusMessage(palette.getHelpText());
    }
}
