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

import pixelitor.filters.gui.UserPreset;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * The hand tool.
 */
public class HandTool extends Tool {
    private final HandToolSupport handToolSupport = new HandToolSupport();

    HandTool() {
        super("Hand", 'H',
            "<b>drag</b> to move the view (if there are scrollbars).",
            Cursors.HAND);
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        settingsPanel.addAutoZoomButtons();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        handToolSupport.mousePressed(e.getOrigEvent(), e.getViewport());
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        handToolSupport.mouseDragged(e.getOrigEvent(), e.getViewport());
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
    }

    @Override
    public boolean hasHandToolForwarding() {
        return false;
    }

    @Override
    public boolean supportsUserPresets() {
        return false;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintHandIcon;
    }
}
