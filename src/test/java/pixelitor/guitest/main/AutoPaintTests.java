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

package pixelitor.guitest.main;

import pixelitor.automate.AutoPaint;
import pixelitor.automate.AutoPaintPanel;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.Keyboard;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

public class AutoPaintTests {
    private final Keyboard keyboard;
    private final AppRunner app;

    private final TestContext context;

    public AutoPaintTests(TestContext context) {
        this.context = context;

        this.keyboard = context.keyboard();
        this.app = context.app();
    }

    void start() {
        context.log(0, "testing AutoPaint");

        app.runWithSelectionTranslationCombinations(this::testAutoPaintTask, context);

        context.afterTestActions();
    }

    private void testAutoPaintTask() {
        for (Tool tool : AutoPaint.SUPPORTED_TOOLS) {
            if (context.skip()) {
                continue;
            }
            if (tool == Tools.BRUSH) {
                for (String colorMode : AutoPaintPanel.COLOR_MODES) {
                    testAutoPaintWithTool(tool, colorMode);
                }
            } else {
                testAutoPaintWithTool(tool, null);
            }
        }
    }

    private void testAutoPaintWithTool(Tool tool, String colorMode) {
        app.runMenuCommand("Auto Paint...");
        var dialog = app.findDialogByTitle("Auto Paint");

        var toolSelector = dialog.comboBox("toolSelector");
        toolSelector.selectItem(tool.toString());

        var strokeCountTF = dialog.textBox("strokeCountTF");
        String testNumStrokes = "111";
        if (!strokeCountTF.text().equals(testNumStrokes)) {
            strokeCountTF.deleteText();
            strokeCountTF.enterText(testNumStrokes);
        }

        var colorsCB = dialog.comboBox("colorsCB");
        if (colorMode != null) {
            colorsCB.requireEnabled();
            colorsCB.selectItem(colorMode);
        } else {
            colorsCB.requireDisabled();
        }

        dialog.button("ok").click();
        dialog.requireNotVisible();

        keyboard.undoRedoUndo("Auto Paint");
    }
}
