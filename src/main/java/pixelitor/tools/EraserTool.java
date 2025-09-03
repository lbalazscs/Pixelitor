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

import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * The eraser tool.
 */
public class EraserTool extends AbstractBrushTool {
    public EraserTool() {
        super("Eraser", 'E',
            "<b>click and drag</b> to erase pixels. <b>Shift-click</b> to erase lines.",
            Cursors.CROSSHAIR, true);
        drawTarget = DrawTarget.DIRECT;
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        addTypeSelector();
        addBrushSettingsButton();

        settingsPanel.addSeparator();
        addSizeSelector();
        addSymmetrySelector();
        addLazyMouseDialogButton();
    }

    @Override
    protected void initBrushStroke() {
        brushContext.setErasing();
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintEraserIcon;
    }
}