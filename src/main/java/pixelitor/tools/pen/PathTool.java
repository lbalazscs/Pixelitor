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

import com.bric.geom.ShapeStringUtils;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.utils.Messages;

import java.awt.Cursor;
import java.awt.Shape;
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

    /**
     * Removes a path from the composition, without adding a history edit.
     */
    public void removePath() {
        Views.setActivePath(null);
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
        }

        if (this == Tools.TRANSFORM_PATH) {
            Tools.TRANSFORM_PATH.initBoxes(newComp);
        }

        setActionsEnabled(compPath != null);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        Path path = Views.getActivePath();
        if (path != null) {
            preset.put("Path", ShapeStringUtils.toString(path.toImageSpaceShape()));
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        String pathString = preset.get("Path");
        if (pathString != null) {
            View view = Views.getActive();
            if (view != null) {
                Shape pathShape = ShapeStringUtils.createGeneralPath(pathString);
                Composition comp = view.getComp();
                if (comp.hasActivePath()) {
                    boolean confirmed = Messages.showYesNoQuestion("Confirmation",
                        "Overwrite existing path with the path loaded from the preset?");
                    if (!confirmed) {
                        return;
                    }
                }
                comp.createPathFromShape(pathShape, false, false);
                view.repaint();
            }
        }
    }
}
