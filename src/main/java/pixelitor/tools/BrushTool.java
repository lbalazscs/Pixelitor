/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools;

import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GUIText;
import pixelitor.layers.Drawable;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * The brush tool.
 */
public class BrushTool extends BlendingModeBrushTool {
    public static final String NAME = GUIText.BRUSH;
    private Color drawingColor;

    public BrushTool() {
        super(NAME, 'B',
            "<b>click</b> or <b>drag</b> to draw with the active brush, " +
                "<b>Shift-click</b> to draw lines, " +
                "<b>right-click</b> or <b>right-drag</b> to draw with the background color.",
            Cursors.CROSSHAIR, true
        );
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        addTypeSelector();
        addBrushSettingsButton();

        settingsPanel.addSeparator();
        addSizeSelector();
        addSymmetrySelector();

        settingsPanel.addSeparator();
        addBlendingModePanel();
        addLazyMouseDialogButton();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        initDrawingColor(e);
        super.mousePressed(e);
    }

    @Override
    protected void initBrushStroke() {
        // reinitialize the color for each stroke
        brushContext.setColor(drawingColor);
    }

    @Override
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        super.prepareProgrammaticBrushStroke(dr, start);
        brushContext.setColor(getFGColor());
    }

    private void initDrawingColor(PMouseEvent e) {
        drawingColor = e.isRight() ? getBGColor() : getFGColor();
    }

    @Override
    public void trace(Drawable dr, Shape shape) {
        drawingColor = getFGColor();
        super.trace(dr, shape);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);

        FgBgColors.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);

        FgBgColors.loadStateFrom(preset);
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintBrushIcon;
    }
}