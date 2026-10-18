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

package pixelitor.colors;

import pixelitor.colors.palette.ColorSwatchButton;
import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.colors.palette.SwatchSelection;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A bounded history of recently used foreground, background,
 * and filter colors, displayed in most-recently-used order.
 */
public class ColorHistory {
    public static final ColorHistory INSTANCE = new ColorHistory();

    private static final int MAX_HISTORY_CAPACITY = 200;
    private static final int SWATCHES_PER_ROW = 10;

    // provides O(1) add/contains and maintains insertion order
    private final SequencedSet<Color> colors = new LinkedHashSet<>();

    // track the active dialog and its content panel
    // to prevent duplication and allow dynamic updates
    private JDialog dialog;
    private JPanel swatchPanel;
    private ColorSwatchClickHandler currentClickHandler;

    // the selection of the history dialog; it is preserved by color
    // across swatch updates and cleared when the dialog is closed,
    // because this singleton must not retain any swatch of a closed dialog
    private final SwatchSelection selection = new SwatchSelection();

    private ColorHistory() {
    }

    /**
     * Adds a color to the history, respecting capacity and avoiding duplicates.
     */
    private void add(Color newColor) {
        assert newColor != null;
        if (!colors.isEmpty() && newColor.equals(colors.getLast())) {
            return; // already the most recent: nothing changes
        }

        colors.addLast(newColor); // moves it to the end if already present
        if (colors.size() > MAX_HISTORY_CAPACITY) {
            colors.removeFirst();
        }

        // dynamically update the modeless dialog whenever history changes
        updateSwatchPanel();
    }

    public static void remember(Color newColor) {
        INSTANCE.add(newColor);
    }

    /**
     * Shows a modeless dialog with the color history.
     */
    public void showDialog(Window owner,
                           ColorSwatchClickHandler clickHandler) {
        assert owner != null;
        assert clickHandler != null;

        // prevent dialog duplication
        if (dialog != null && dialog.isVisible()) {
            // update the click handler just in case the context
            // has changed (e.g. Filter mode vs Standard mode)
            if (currentClickHandler != clickHandler) {
                currentClickHandler = clickHandler;
                updateSwatchPanel();
                showHelpMessage();
            }
            dialog.toFront();
            return;
        }

        cleanup(); // drop leftovers of a dialog that was hidden without the callbacks running

        currentClickHandler = clickHandler;

        swatchPanel = new JPanel(new GridLayout(0, SWATCHES_PER_ROW, 2, 2));
        swatchPanel.setBorder(createEmptyBorder(2, 2, 2, 2));
        updateSwatchPanel(); // initially populate the panel

        showHelpMessage();

        DialogBuilder builder = new DialogBuilder()
            .title("Color History")
            .owner(owner)
            .content(swatchPanel)
            .modeless()
            .noOKButton()
            .noCancelButton()
            .cancelAction(this::cleanup);

        builder.show();
        dialog = builder.getDialog();
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanup();
            }
        });
    }

    private void cleanup() {
        selection.clear();
        dialog = null;
        swatchPanel = null;
        currentClickHandler = null;
    }

    private void showHelpMessage() {
        Messages.showStatusMessage("Color History: " + currentClickHandler.getHelpText());
    }

    /**
     * Clears and repopulates the swatch panel.
     */
    private void updateSwatchPanel() {
        if (swatchPanel == null) {
            return;
        }
        if (dialog != null && !dialog.isVisible()) {
            cleanup();
            return;
        }

        int oldRows = Math.ceilDiv(swatchPanel.getComponentCount(), SWATCHES_PER_ROW);

        Color selectedColor = selection.getSelectedColor();
        selection.clear();
        swatchPanel.removeAll();

        // iterate in reverse order so newest colors appear first at the top-left
        for (Color color : colors.reversed()) {
            var swatch = new ColorSwatchButton(color, currentClickHandler, selection);
            swatchPanel.add(swatch);
            if (color.equals(selectedColor)) {
                selection.select(swatch);
            }
        }

        swatchPanel.revalidate();
        swatchPanel.repaint();

        int newRows = Math.ceilDiv(swatchPanel.getComponentCount(), SWATCHES_PER_ROW);

        // repack the dialog so that its height adjusts dynamically if rows were added
        if (newRows != oldRows && dialog != null && dialog.isVisible()) {
            dialog.pack();
        }
    }
}
