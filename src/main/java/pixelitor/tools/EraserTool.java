/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

import static java.awt.AlphaComposite.DST_OUT;

/**
 * The eraser tool.
 */
public class EraserTool extends AbstractBrushTool {
    public EraserTool() {
        super("Eraser", 'e', "erase_tool_icon.png",
                "<b>click and drag</b> to erase pixels.",
                Cursors.CROSSHAIR);
        drawStrategy = DrawStrategy.DIRECT;
    }

    @Override
    public void initSettingsPanel() {
        addTypeSelector();
        addBrushSettingsButton();
        settingsPanel.addSeparator();
        addSizeSelector();
        addSymmetryCombo();
    }

    @Override
    protected void initializeGraphics(Graphics2D g) {
        // the color does not matter as long as AlphaComposite.DST_OUT is used
        g.setComposite(AlphaComposite.getInstance(DST_OUT));
    }
}