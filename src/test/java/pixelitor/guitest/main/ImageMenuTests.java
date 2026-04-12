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

import org.assertj.swing.fixture.FrameFixture;
import pixelitor.guitest.*;
import pixelitor.tools.gradient.GradientType;
import pixelitor.tools.shapes.ShapeType;

import java.awt.Color;

public class ImageMenuTests {
    private final Keyboard keyboard;
    private final Mouse mouse;
    private final AppRunner app;
    private final MaskMode maskMode;
    private final FrameFixture pw;

    private final TestContext context;

    public ImageMenuTests(TestContext context) {
        this.context = context;

        this.keyboard = context.keyboard();
        this.mouse = context.mouse();
        this.app = context.app();
        this.maskMode = context.maskMode();
        this.pw = context.pw();
    }

    void start() {
        context.log(0, "image menu");

        // image from the previous tests
        EDT.assertNumViewsIs(1);
        EDT.assertNumLayersInActiveHolderIs(1);

        testCropSelection();

        // add more layer types
        // TODO set to false, because currently not all layer types
        //   create undo edits when used with the move tool
        boolean addExtraLayers = false;
        if (addExtraLayers) {
            app.addGradientFillLayer(GradientType.ANGLE);
            app.addColorFillLayer(Color.BLUE);
            app.addShapesLayer(ShapeType.BAT, CanvasDrag.diagonal(20, 380, 100));
        }

        testDuplicateImage();

        // crop is tested with the crop tool

        app.runWithSelectionTranslationCombinations(() -> {
            testResize();
            testEnlargeCanvas();
            context.testRotateFlip(true);
        }, context);

        if (addExtraLayers) {
            // delete the 3 extra layers
            pw.button("deleteLayer")
                .requireEnabled()
                .click()
                .click()
                .click();
            EDT.assertNumLayersInActiveHolderIs(1);
        }

        maskMode.apply(context);
        maskMode.check();

        context.afterTestActions();
    }

    private void testCropSelection() {
        // create the selection that will be cropped
        context.clickAndResetRectSelectTool();

        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(400, 400);
        keyboard.undoRedo("Create Selection");
        EDT.requireSelection();

        context.testCropSelection(() -> app.runMenuCommand("Crop Selection"),
            false, 200.0, 200.0);

        app.deselect();
    }

    private void testDuplicateImage() {
        int numLayers = EDT.getNumLayersInActiveHolder();
        context.log(1, "image duplication, num layers = " + numLayers);

        EDT.assertNumViewsIs(1);

        app.runMenuCommand("Duplicate");
        EDT.assertNumViewsIs(2);
        EDT.assertNumLayersInActiveHolderIs(numLayers);

        context.closeOneOfTwoViews();
        EDT.assertNumViewsIs(1);
    }

    private void testResize() {
        context.log(2, "resize");
        app.resize(622);

        keyboard.undo("Resize");
    }

    private void testEnlargeCanvas() {
        context.log(2, "enlarge canvas");
        app.enlargeCanvas(100, 100, 100, 100);
        keyboard.undoRedoUndo("Enlarge Canvas");
    }
}
