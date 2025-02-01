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

package pixelitor.tools.pen;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import java.awt.Cursor;
import java.util.ResourceBundle;

import static pixelitor.tools.pen.PathActions.setActionsEnabled;

/**
 * Base class for path-related tools.
 */
public abstract class PathTool extends Tool {
    protected PathTool(String name, String toolMessage, Cursor cursor) {
        super(name, 'P', toolMessage, cursor);

        pixelSnapping = true;
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        settingsPanel.addButton(PathActions.toSelectionAction, "toSelectionButton",
            "Convert the path to a selection");
        settingsPanel.addButton(PathActions.exportSVGAction, "exportSVGButton",
            "Export the path to an SVG file");
        settingsPanel.addButton(PathActions.traceWithBrushAction, "traceWithBrush",
            "Stroke the path using the current Brush Tool settings");
        settingsPanel.addButton(PathActions.traceWithEraserAction, "traceWithEraser",
            "Stroke the path using the current Eraser Tool settings");
        settingsPanel.addButton(PathActions.traceWithSmudgeAction, "traceWithSmudge",
            "Stroke the path using the current Smudge Tool settings");
        settingsPanel.addButton(PathActions.deletePathAction, "deletePath",
            "Delete the path");
    }

    @Override
    public boolean hasSharedHotkey() {
        return true;
    }

    /**
     * Removes a path from the composition, without adding a history edit.
     */
    public void removePath() {
        Views.setActivePath(null);
        Tools.PEN.setPath(null);
        if (this != Tools.PEN) {
            Tools.PEN.activate();
        }
        setActionsEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        if (this != Tools.PEN && !newView.getComp().hasActivePath()) {
            Tools.PEN.activate();
            return;
        }

        super.viewActivated(oldView, newView);
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        Path compPath = newComp.getActivePath();
        if (compPath == null) {
            if (this != Tools.PEN) {
                Tools.PEN.activate();
                return;
            }
        } else if (this == Tools.PEN) {
            Tools.PEN.setPath(compPath);
        }

        if (this == Tools.TRANSFORM_PATH) {
            Tools.TRANSFORM_PATH.initBoxes(newComp);
        }

        setActionsEnabled(compPath != null);
    }
}
