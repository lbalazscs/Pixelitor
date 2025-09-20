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

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.History;
import pixelitor.io.FileIO;
import pixelitor.selection.SelectionChangeResult;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.ConvertPathToSelectionEdit;
import pixelitor.utils.Threads;

import javax.swing.*;
import java.awt.Shape;

public class PathActions {
    public static final Action toSelectionAction = new TaskAction(
        "Convert to Selection", PathActions::convertToSelection);

    private static boolean enabled = true;

    private PathActions() {
    }

    public static void convertToSelection() {
        Composition comp = Views.getActiveComp();
        Path oldPath = comp.getActivePath();

        Shape shape = oldPath.toImageSpaceShape();

        SelectionChangeResult result = comp.changeSelection(shape);
        if (!result.isSuccess()) {
            result.showInfoDialog("path");
            return;
        }

        Tools.PEN.removePath();
        comp.pathChanged(true);
        History.add(new ConvertPathToSelectionEdit(
            comp, oldPath, result.getEdit(), (PathTool) Tools.getActive()));

        Tools.LASSO_SELECTION.activate();
    }

    public static final Action exportSVGAction = new TaskAction(
        "Export SVG...", PathActions::exportSVG);

    private static void exportSVG() {
        Path path = Views.getActivePath();
        FileIO.saveSVG(path.toImageSpaceShape(), null, "path.svg");
    }

    public static final Action deletePathAction = new TaskAction(
        "Delete Path", PathActions::deletePath);

    private static void deletePath() {
        Views.getActivePath().delete();
    }

    public static final Action traceWithBrushAction = new TraceAction(
        "Stroke with Brush", Tools.BRUSH);
    public static final Action traceWithEraserAction = new TraceAction(
        "Stroke with Eraser", Tools.ERASER);
    public static final Action traceWithSmudgeAction = new TraceAction(
        "Stroke with Smudge", Tools.SMUDGE);

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setActionsEnabled(boolean newEnabled) {
        assert Threads.calledOnEDT() || AppMode.isUnitTesting() : Threads.callInfo();

        if (enabled == newEnabled) {
            return;
        }
        enabled = newEnabled;

        toSelectionAction.setEnabled(newEnabled);
        exportSVGAction.setEnabled(newEnabled);
        traceWithBrushAction.setEnabled(newEnabled);
        traceWithEraserAction.setEnabled(newEnabled);
        traceWithSmudgeAction.setEnabled(newEnabled);
        deletePathAction.setEnabled(newEnabled);
    }
}
