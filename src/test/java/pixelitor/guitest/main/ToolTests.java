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

import com.bric.util.JVM;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JCheckBoxFixture;
import org.assertj.swing.fixture.JSliderFixture;
import pixelitor.assertions.PixelitorAssertions;
import pixelitor.colors.FgBgColorSelector;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.EDT;
import pixelitor.guitest.Keyboard;
import pixelitor.guitest.Mouse;
import pixelitor.layers.Drawable;
import pixelitor.menus.view.ZoomControl;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.tools.BrushType;
import pixelitor.tools.Symmetry;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.GradientColorType;
import pixelitor.tools.gradient.GradientTool;
import pixelitor.tools.gradient.GradientType;
import pixelitor.tools.move.MoveMode;
import pixelitor.tools.pen.PathTool;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.TwoPointPaintType;
import pixelitor.tools.transform.TransformBox;
import pixelitor.utils.Geometry;
import pixelitor.utils.Rnd;
import pixelitor.utils.input.Modifiers;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;

import static java.awt.event.KeyEvent.VK_CONTROL;
import static pixelitor.guitest.GUITestUtils.change;
import static pixelitor.guitest.GUITestUtils.checkRandomly;
import static pixelitor.guitest.GUITestUtils.findButtonByText;
import static pixelitor.menus.view.ZoomLevel.zoomLevels;
import static pixelitor.selection.SelectionModifyType.EXPAND;
import static pixelitor.selection.ShapeCombinator.ADD;
import static pixelitor.selection.ShapeCombinator.REPLACE;
import static pixelitor.tools.DragToolState.IDLE;
import static pixelitor.tools.DragToolState.TRANSFORM;

public class ToolTests {
    private static boolean maskIndependentToolsTested = false;

    private final Keyboard keyboard;
    private final Mouse mouse;
    private final AppRunner app;
    private final MaskMode maskMode;
    private final FrameFixture pw;
    private final TestConfig config;

    private final TestContext context;

    public ToolTests(TestContext context) {
        this.context = context;

        this.keyboard = context.keyboard();
        this.mouse = context.mouse();
        this.app = context.app();
        this.maskMode = context.maskMode();
        this.pw = context.pw();
        this.config = context.config();
    }

    /**
     * Tests all tools.
     */
    void start() {
        context.log(0, "tools");

        // make sure we have a big enough canvas for the tool tests
        keyboard.actualPixels();

        // test mask-independent tools only once, or randomly 5% of the time, to save time
        if (!maskIndependentToolsTested || Rnd.nextDouble() < 0.05) {
            testMoveTool();
            testCropTool();
            testSelectionToolsAndMenus();

            testPathTools();
            testHandTool();
            testZooming();
            testColorSelector();
            maskIndependentToolsTested = true;
        }

        testBrushTool();
        testCloneTool();
        testEraserTool();
        testSmudgeTool();
        testGradientTool();
        testPaintBucketTool();
        testColorPickerTool();
        testShapesTool();

        app.reload();

        maskMode.apply(context);
        context.afterTestActions();
    }

    private void testMoveTool() {
        context.log(1, "move tool");

        app.addSelection(); // so that the moving of the selection can be tested
        app.clickTool(Tools.MOVE);

        MoveMode[] moveModes = MoveMode.values();
        for (MoveMode mode : moveModes) {
            pw.comboBox("modeSelector").selectItem(mode.toString());

            testMoveToolDrag(mode, false);
            testMoveToolDrag(mode, true);
            EDT.assertNumLayersInActiveHolderIs(1);

            keyboard.nudge();
            keyboard.undoRedoUndo(mode.getEditName());
            EDT.assertNumLayersInActiveHolderIs(1);

            // check that all move-related edits have been undone
            EDT.assertEditToBeUndoneNameIs("Create Selection");

            testMoveToolClick(mode, Modifiers.NONE);
            testMoveToolClick(mode, Modifiers.CTRL);
            testMoveToolClick(mode, Modifiers.SHIFT);
            testMoveToolClick(mode, Modifiers.ALT);
        }

        maskMode.apply(context);
        context.afterTestActions();
    }

    private void testMoveToolDrag(MoveMode mode, boolean altDrag) {
        mouse.moveToCanvas(400, 400);

        if (altDrag) {
            app.setMaxUntestedEdits(2);

            // adds 2 edits: "Duplicate Layer", "Move"
            mouse.altDragToCanvas(300, 300);

            keyboard.undo(mode.getEditName());
            if (mode.movesLayer()) {
                keyboard.undoRedo("Duplicate Layer");
            }
            keyboard.redo(mode.getEditName());

            app.setMaxUntestedEdits(1);
        } else {
            View view = EDT.getActiveView();
            Drawable dr = view.getComp().getActiveDrawableOrThrow();
            assert dr.getTx() == 0 : "tx = " + dr.getTx();
            assert dr.getTy() == 0 : "ty = " + dr.getTx();

            mouse.dragToCanvas(200, 300);

            if (mode.movesLayer()) {
                // the translations will have these values only if we are at 100% zoom
                assert view.getZoomLevel() == ZoomLevel.ACTUAL_SIZE : "zoom is " + view.getZoomLevel();
                assert dr.getTx() == -200 : "tx = " + dr.getTx();
                assert dr.getTy() == -100 : "ty = " + dr.getTy();
            } else {
                assert dr.getTx() == 0 : "tx = " + dr.getTx();
                assert dr.getTy() == 0 : "ty = " + dr.getTy();
            }
        }

        keyboard.undoRedoUndo(mode.getEditName());

        if (altDrag && mode.movesLayer()) {
            // The alt-dragged movement creates two history edits:
            // a duplicate and a layer move. Now also undo the duplication.
            keyboard.undo("Duplicate Layer");
        }

        // check that all move-related edits have been undone
        EDT.assertEditToBeUndoneNameIs("Create Selection");
    }

    private void testMoveToolClick(MoveMode mode, Modifiers modifiers) {
        EDT.assertNumLayersInActiveHolderIs(1);
        boolean duplicated = modifiers.alt().isDown() && mode.movesLayer();
        if (duplicated) {
            app.setMaxUntestedEdits(2);
        }

        mouse.click(modifiers);
        keyboard.undoRedoUndo(mode.getEditName());

        if (duplicated) {
            EDT.assertNumLayersInActiveHolderIs(2);
            keyboard.undoRedoUndo("Duplicate Layer");
            EDT.assertNumLayersInActiveHolderIs(1);
            app.setMaxUntestedEdits(1);
        }
    }

    private void testCropTool() {
        context.log(1, "crop tool");

        app.clickTool(Tools.CROP);

        List<Boolean> checkBoxStates = List.of(Boolean.TRUE, Boolean.FALSE);
        for (Boolean allowGrowing : checkBoxStates) {
            for (Boolean deleteCropped : checkBoxStates) {
                pw.checkBox("allowGrowingCB").check(allowGrowing);
                pw.checkBox("deleteCroppedCB").check(deleteCropped);

                cropFromCropTool();
            }
        }

        context.afterTestActions();
    }

    private void cropFromCropTool() {
        checkCropBoxDoesNotExist();

        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(400, 400);
        keyboard.undoRedo("Create Crop Box");
        checkCropBoxExists();

        mouse.dragToCanvas(450, 450);
        keyboard.undoRedo("Modify Crop Box");

        mouse.moveToCanvas(200, 200); // move to the top left corner
        mouse.dragToCanvas(150, 150);
        keyboard.undoRedo("Modify Crop Box");

        keyboard.nudge();
        keyboard.undoRedo("Nudge Crop Box");

        mouse.randomAltClick(); // must be at the end, otherwise it tries to start a rectangle

        findButtonByText(pw, "Crop")
            .requireEnabled()
            .click();
        checkCropBoxDoesNotExist();

        keyboard.undoRedoUndo("Crop");
        // undoing the crop restores the crop box
        checkCropBoxExists();

        findButtonByText(pw, "Cancel")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Dismiss Crop Box");

        checkCropBoxDoesNotExist();
    }

    private static void checkCropBoxExists() {
        assert EDT.call(Tools.CROP::hasCropBox);
    }

    private static void checkCropBoxDoesNotExist() {
        assert !EDT.call(Tools.CROP::hasCropBox);
    }

    private void testSelectionToolsAndMenus() {
        context.log(1, "selection tools and the selection menus");
        boolean hadSelectionAtStart = EDT.hasActiveSelection();

        // make sure we are at 100%
        keyboard.actualPixels();

        app.clickTool(Tools.RECTANGLE_SELECTION);
        EDT.assertSelectionCombinatorIs(Tools.RECTANGLE_SELECTION, REPLACE);

        mouse.randomAltClick();
        if (hadSelectionAtStart) {
            if (EDT.hasActiveSelection()) {
                // this alt-click didn't deselect
                app.deselect();
            } else {
                // this alt-click deselected and added an edit
                keyboard.undoRedo("Deselect");
            }
        }

        // the Alt should change the interaction only temporarily, while the mouse is down
        EDT.assertSelectionCombinatorIs(Tools.RECTANGLE_SELECTION, REPLACE);

        // TODO test poly selection
        testWithSimpleSelection();
        testWithTwoEllipseSelections();

        context.afterTestActions();
    }

    private void testWithSimpleSelection() {
        EDT.requireNoSelection();

        mouse.moveToCanvas(200, 100);
        mouse.dragToCanvas(400, 300);

        EDT.requireSelection();

        keyboard.undo("Create Selection");
        EDT.requireNoSelection();

        keyboard.redo("Create Selection");
        EDT.requireSelection();

        keyboard.nudge();
        EDT.requireSelection();

        keyboard.undoRedoUndo("Nudge Selection");
        EDT.requireSelection();

        app.deselect();
        EDT.requireNoSelection();

        keyboard.undo("Deselect");
        EDT.requireSelection();
    }

    private void testWithTwoEllipseSelections() {
        app.clickTool(Tools.ELLIPSE_SELECTION);
        EDT.assertActiveToolIs(Tools.ELLIPSE_SELECTION);

        pw.comboBox("combinatorCB").selectItem("Replace");
        EDT.assertSelectionCombinatorIs(Tools.ELLIPSE_SELECTION, REPLACE);

        // replace current selection with the first ellipse
        int e1X = 200;
        int e1Y = 100;
        int e1Width = 200;
        int e1Height = 200;
        mouse.moveToCanvas(e1X, e1Y);
        mouse.dragToCanvas(e1X + e1Width, e1Y + e1Height);
        keyboard.undoRedo("Replace Selection");
        EDT.requireSelection();

        // add second ellipse
        pw.comboBox("combinatorCB").selectItem("Add");
        EDT.assertSelectionCombinatorIs(Tools.ELLIPSE_SELECTION, ADD);

        int e2X = 400;
        int e2Y = 100;
        int e2Width = 100;
        int e2Height = 100;
        mouse.moveToCanvas(e2X, e2Y);
        mouse.dragToCanvas(e2X + e2Width, e2Y + e2Height);
        keyboard.undoRedo("Add Selection");

        // test crop selection by clicking on the button
        context.testCropSelection(() -> findButtonByText(pw, "Crop Selection").requireEnabled().click(),
            true, 300.0, 200.0);

        if (!config.isQuick()) {
            // test crop selection by using the menu
            context.testCropSelection(() -> app.runMenuCommand("Crop Selection"),
                true, 300.0, 200.0);
        }

        testSelectionModifyMenu();
        EDT.requireSelection();

        app.runMenuCommand("Invert Selection");
        keyboard.undoRedo("Invert Selection");
        EDT.requireSelection();

        app.runMenuCommand("Deselect");
        EDT.requireNoSelection();

        keyboard.undo("Deselect");
        EDT.requireSelection();

        keyboard.redo("Deselect");
        EDT.requireNoSelection();
    }

    private void testSelectionModifyMenu() {
        app.runModifySelection(EXPAND, 24);
        keyboard.undo("Modify Selection");
    }

    private void testPathTools() {
        testPenTool();
        testNodeTool();
        testTransformPathTool();

        context.afterTestActions();
    }

    private void testPenTool() {
        context.log(1, "pen tool");
        app.clickTool(Tools.PEN);

        pw.button("toSelectionButton").requireDisabled();

        mouse.moveToCanvas(200, 200);
        mouse.dragToCanvas(200, 400);
        keyboard.undoRedo("Subpath Start");

        mouse.moveToCanvas(300, 400);
        mouse.dragToCanvas(300, 200);
        keyboard.undoRedo("Add Anchor Point");

        mouse.moveToCanvas(200, 200);
        mouse.click();
        keyboard.undoRedo("Close Subpath");

        PixelitorAssertions.assertThat(EDT.getActivePath())
            .isNotNull()
            .numSubPathsIs(1)
            .numAnchorsIs(2);

        keyboard.undo("Close Subpath");
        keyboard.undo("Add Anchor Point");
        keyboard.undo("Subpath Start");

        PixelitorAssertions.assertThat(EDT.getActivePath()).isNull();

        keyboard.redo("Subpath Start");
        keyboard.redo("Add Anchor Point");
        keyboard.redo("Close Subpath");

        // add a second subpath, this one will be open and
        // consists of straight segments
        mouse.clickCanvas(600, 200);
        keyboard.undoRedo("Subpath Start");

        mouse.clickCanvas(600, 300);
        keyboard.undoRedo("Add Anchor Point");

        mouse.clickCanvas(700, 300);
        keyboard.undoRedo("Add Anchor Point");

        mouse.clickCanvas(700, 200);
        keyboard.undoRedo("Add Anchor Point");

        mouse.ctrlClickCanvas(700, 150);
        keyboard.undoRedo("Finish Subpath");

        PixelitorAssertions.assertThat(EDT.getActivePath())
            .isNotNull()
            .numSubPathsIs(2)
            .numAnchorsIs(6);

        testTraceButtons(Tools.PEN);
    }

    private void testNodeTool() {
        context.log(1, "node tool");
        app.clickTool(Tools.NODE);

        mouse.moveToCanvas(600, 300);
        mouse.dragToCanvas(500, 400);
        keyboard.undoRedo("Move Anchor Point");

        var popupMenu = mouse.showPopupAtCanvas(500, 400);
        AppRunner.clickPopupMenu(popupMenu, "Delete Point");
        keyboard.undoRedoUndo("Delete Anchor Point");

        popupMenu = mouse.showPopupAtCanvas(500, 400);
        AppRunner.clickPopupMenu(popupMenu, "Delete Subpath");
        keyboard.undoRedoUndo("Delete Subpath");

        popupMenu = mouse.showPopupAtCanvas(500, 400);
        AppRunner.clickPopupMenu(popupMenu, "Delete Path");
        keyboard.undoRedoUndo("Delete Path");

        // drag out handle
        mouse.moveToCanvas(500, 400);
        mouse.altDragToCanvas(600, 500);
        keyboard.undoRedo("Move Control Handle");

        popupMenu = mouse.showPopupAtCanvas(500, 400);
        AppRunner.clickPopupMenu(popupMenu, "Retract Handles");
        keyboard.undoRedo("Retract Handles");

        // test convert to selection
        pw.button("toSelectionButton")
            .requireEnabled()
            .click();
        EDT.assertActiveToolIs(Tools.LASSO_SELECTION);

        keyboard.undo("Convert Path to Selection");
        EDT.assertActiveToolIs(Tools.NODE);

        keyboard.redo("Convert Path to Selection");
        EDT.assertActiveToolIs(Tools.LASSO_SELECTION);

        app.invert();

        pw.button("toPathButton")
            .requireEnabled()
            .click();
        EDT.assertActiveToolIs(Tools.NODE);
        PixelitorAssertions.assertThat(EDT.getActivePath()).isNotNull();

        keyboard.undo("Convert Selection to Path");
        EDT.assertActiveToolIs(Tools.LASSO_SELECTION);
        PixelitorAssertions.assertThat(EDT.getActivePath()).isNull();

        keyboard.redo("Convert Selection to Path");
        EDT.assertActiveToolIs(Tools.NODE);
        PixelitorAssertions.assertThat(EDT.getActivePath()).isNotNull();

        testTraceButtons(Tools.NODE);
    }

    private void testTransformPathTool() {
        context.log(1, "transform path tool");
        app.clickTool(Tools.TRANSFORM_PATH);

        Point nw = EDT.getTransformPathToolBoxPos(0, TransformBox::getNW);
        mouse.moveToScreen(nw.x, nw.y);
        mouse.dragToScreen(nw.x - 100, nw.y - 50);
        keyboard.undoRedo("Change Transform Box");

        Point rot = EDT.getTransformPathToolBoxPos(1, TransformBox::getRot);
        mouse.moveToScreen(rot.x, rot.y);
        mouse.dragToScreen(rot.x + 100, rot.y + 100);
        keyboard.undoRedo("Change Transform Box");

        testTraceButtons(Tools.TRANSFORM_PATH);
    }

    // tests the trace buttons in one of the path tools
    private void testTraceButtons(PathTool tool) {
        context.log(2, "test tracing in " + tool.getName());

        pw.button("traceWithSmudge")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Smudge Tool");

        pw.button("traceWithEraser")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Eraser Tool");

        pw.button("traceWithBrush")
            .requireEnabled()
            .click();
        keyboard.undoRedo("Brush Tool");
    }

    private void testHandTool() {
        context.log(1, "hand tool");

        app.clickTool(Tools.HAND);

        mouse.randomAltClick();

        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();

        testAutoZoomButtons();

        context.afterTestActions();
    }

    private void testZooming() {
        context.log(1, "zoom tool");

        app.clickTool(Tools.ZOOM);

        ZoomLevel startingZoom = EDT.getActiveZoomLevel();

        mouse.moveToActiveCanvasCenter();

        mouse.click();
        EDT.assertActiveZoomIs(startingZoom.zoomIn());
        mouse.click();
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn());
        mouse.click(Modifiers.ALT);
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn().zoomOut());
        mouse.click(Modifiers.ALT);
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn().zoomOut().zoomOut());

        testMouseWheelZooming();
        testControlPlusMinusZooming();
        testZoomControlAndNavigatorZooming();
        testNavigatorRightClickPopupMenu();
        testAutoZoomButtons();

        context.afterTestActions();
    }

    private void testControlPlusMinusZooming() {
        ZoomLevel startingZoom = EDT.getActiveZoomLevel();

        Keyboard.pressCtrlPlus(pw, 2);
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn());

        Keyboard.pressCtrlMinus(pw, 2);
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomIn().zoomOut().zoomOut());
    }

    private void testZoomControlAndNavigatorZooming() {
        var slider = findZoomControlSlider();

        slider.slideToMinimum();
        EDT.assertActiveZoomIs(zoomLevels[0]);

        findButtonByText(pw, "100%").click();
        EDT.assertActiveZoomIs(ZoomLevel.ACTUAL_SIZE);

        slider.slideToMaximum();
        EDT.assertActiveZoomIs(zoomLevels[zoomLevels.length - 1]);

        findButtonByText(pw, "Fit").click();

        app.runMenuCommand("Show Navigator...");
        var navigator = app.findDialogByTitle("Navigator");
        navigator.resizeTo(new Dimension(500, 400));

        ZoomLevel startingZoom = EDT.getActiveZoomLevel();

        Keyboard.pressCtrlPlus(navigator, 4);
        ZoomLevel expectedZoomIn = startingZoom.zoomIn().zoomIn().zoomIn().zoomIn();
        EDT.assertActiveZoomIs(expectedZoomIn);

        Keyboard.pressCtrlMinus(navigator, 2);
        ZoomLevel expectedZoomOut = expectedZoomIn.zoomOut().zoomOut();
        EDT.assertActiveZoomIs(expectedZoomOut);
        findButtonByText(pw, "Fit").click();

        // navigate
        int mouseStartX = navigator.target().getWidth() / 2;
        int mouseStartY = navigator.target().getHeight() / 2;

        mouse.moveTo(navigator, mouseStartX, mouseStartY);
        mouse.dragTo(navigator, mouseStartX - 30, mouseStartY + 30);
        mouse.dragTo(navigator, mouseStartX, mouseStartY);

        navigator.close();
        navigator.requireNotVisible();
    }

    private JSliderFixture findZoomControlSlider() {
        return pw.slider(new GenericTypeMatcher<>(JSlider.class) {
            @Override
            protected boolean isMatching(JSlider s) {
                return s.getParent() == ZoomControl.get();
            }

            @Override
            public String toString() {
                return "Matcher for the ZoomControl's slider ";
            }
        });
    }

    private void testNavigatorRightClickPopupMenu() {
        app.runMenuCommand("Show Navigator...");
        var navigator = app.findDialogByTitle("Navigator");
        navigator.resizeTo(new Dimension(500, 400));

        var popupMenu = navigator.showPopupMenu();
        AppRunner.clickPopupMenu(popupMenu, "Navigator Zoom: 100%");

        popupMenu = navigator.showPopupMenu();
        AppRunner.clickPopupMenu(popupMenu, "Navigator Zoom: 50%");

        popupMenu = navigator.showPopupMenu();
        AppRunner.clickPopupMenu(popupMenu, "Navigator Zoom: 25%");

        popupMenu = navigator.showPopupMenu();
        AppRunner.clickPopupMenu(popupMenu, "Navigator Zoom: 12.5%");

        navigator.resizeTo(new Dimension(500, 400));
        popupMenu = navigator.showPopupMenu();
        AppRunner.clickPopupMenu(popupMenu, "View Box Color...");

        testColorSelectorDialog("Navigator");

        navigator.close();
        navigator.requireNotVisible();
    }

    private void testAutoZoomButtons() {
        findButtonByText(pw, "Fit Space").click();
        findButtonByText(pw, "Fit Width").click();
        findButtonByText(pw, "Fit Height").click();
        findButtonByText(pw, "Actual Pixels").click();
    }

    private void testMouseWheelZooming() {
        pw.pressKey(VK_CONTROL);
        ZoomLevel startingZoom = EDT.getActiveZoomLevel();
        View view = EDT.getActiveView();

        mouse.rotateWheel(view, 2);
        if (JVM.isLinux) {
            EDT.assertActiveZoomIs(startingZoom.zoomOut().zoomOut());
        } else {
            EDT.assertActiveZoomIs(startingZoom.zoomOut());
        }

        mouse.rotateWheel(view, -2);

        if (JVM.isLinux) {
            EDT.assertActiveZoomIs(startingZoom.zoomOut().zoomOut().zoomIn().zoomIn());
        } else {
            EDT.assertActiveZoomIs(startingZoom.zoomOut().zoomIn());
        }

        pw.releaseKey(VK_CONTROL);
    }

    private void testColorSelector() {
        context.log(1, "color selector");

        app.setDefaultColors();
        app.swapColors();
        app.randomizeColors();

        var fgButton = pw.button(FgBgColorSelector.FG_BUTTON_NAME);
        fgButton.click();
        testColorSelectorDialog(GUIText.FG_COLOR);

        var bgButton = pw.button(FgBgColorSelector.BG_BUTTON_NAME);
        bgButton.click();
        testColorSelectorDialog(GUIText.BG_COLOR);

        if (!config.isQuick()) {
            testColorSelectorPopup(fgButton, true);
            testColorSelectorPopup(bgButton, false);
        }

        context.afterTestActions();
    }

    private void testColorSelectorDialog(String title) {
        var colorSelector = app.findDialogByTitle(title);
        mouse.moveTo(colorSelector, 100, 150);
        mouse.dragTo(colorSelector, Rnd.intInRange(110, 200), Rnd.intInRange(160, 300));
        findButtonByText(colorSelector, "OK").click();
    }

    private void testColorSelectorPopup(JButtonFixture button, boolean isFg) {
        testColorPaletteDialogWithPopup(button,
            isFg ? "Foreground Color Variations"
                : "Background Color Variations");

        testColorPaletteDialogWithPopup(button,
            isFg ? "HSB Mix with Background"
                : "HSB Mix with Foreground");

        testColorPaletteDialogWithPopup(button,
            isFg ? "RGB Mix with Background"
                : "RGB Mix with Foreground");

        testColorPaletteDialogWithPopup(button, "Color History");

        AppRunner.clickPopupMenu(button.showPopupMenu(), "Copy Color");
        AppRunner.clickPopupMenu(button.showPopupMenu(), "Paste Color");
    }

    private void testColorPaletteDialogWithPopup(JButtonFixture button, String dialogName) {
        AppRunner.clickPopupMenu(button.showPopupMenu(), dialogName + "...");
        context.testColorPaletteDialog(dialogName);
    }

    private void testBrushTool() {
        context.log(1, "brush tool");

        app.clickTool(Tools.BRUSH);

        enableLazyMouse(false);
        testBrushStrokes(Tools.BRUSH);

        // TODO this freezes when running with coverage??
        // sometimes also without coverage??
//        enableLazyMouse(true);
//        testBrushStrokes();

        context.afterTestActions();
    }

    private void enableLazyMouse(boolean b) {
        pw.button("lazyMouseDialogButton")
            .requireEnabled()
            .click();
        var dialog = app.findDialogByTitle("Lazy Mouse Settings");
        if (b) {
            dialog.checkBox().check();
            dialog.slider("distSlider")
                .requireEnabled()
                .slideToMinimum();
        } else {
            dialog.checkBox().uncheck();
            dialog.slider("distSlider").requireDisabled();
        }
        findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

    private void testBrushStrokes(Tool tool) {
        mouse.randomAltClick();

        for (BrushType brushType : BrushType.values()) {
            pw.comboBox("typeCB").selectItem(brushType.toString());
            app.testBrushSettings(tool, brushType);

            for (Symmetry symmetry : Symmetry.values()) {
                if (context.skip()) {
                    continue;
                }
                pw.comboBox("symmetrySelector").selectItem(symmetry.toString());
                keyboard.randomizeColors();
                mouse.moveRandomlyWithinCanvas();
                mouse.dragRandomlyWithinCanvas();
                keyboard.undoRedo(tool == Tools.BRUSH ? "Brush Tool" : "Eraser Tool");
            }
        }
    }

    private void testEraserTool() {
        context.log(1, "eraser tool");

        app.clickTool(Tools.ERASER);

        testBrushStrokes(Tools.ERASER);

        context.afterTestActions();
    }


    private void testSmudgeTool() {
        context.log(1, "smudge tool");

        app.clickTool(Tools.SMUDGE);

        mouse.randomAltClick(); // adds no history entry

        mouse.randomClick(); // adds a history entry
        keyboard.undoRedo("Smudge Tool");

        for (int i = 0; i < 3; i++) {
            mouse.shiftMoveClickRandom();
            keyboard.undoRedo("Smudge Tool");

            mouse.moveRandomlyWithinCanvas();
            mouse.dragRandomlyWithinCanvas();
            keyboard.undoRedo("Smudge Tool");
        }

        context.afterTestActions();
    }

    private void testCloneTool() {
        context.log(1, "clone tool");

        app.clickTool(Tools.CLONE);

        testClone(false, false, 100);
        testClone(false, true, 200);
        testClone(true, false, 300);
        testClone(true, true, 400);

        context.afterTestActions();
    }

    private void testClone(boolean aligned, boolean sampleAllLayers, int startX) {
        pw.checkBox("alignedCB").check(aligned);

        pw.checkBox("sampleAllLayersCB").check(sampleAllLayers);

        // set the source point
        mouse.moveToCanvas(300, 300);
        mouse.click(Modifiers.ALT);

        // do some cloning
        mouse.moveToCanvas(startX, 300);
        for (int i = 1; i <= 2; i++) {
            int x = startX + i * 10;
            mouse.dragToCanvas(x, 300);
            keyboard.undoRedo("Clone Stamp Tool");
            mouse.dragToCanvas(x, 400);
            keyboard.undoRedo("Clone Stamp Tool");
        }
    }

    private void testGradientTool() {
        context.log(1, "gradient tool");

        app.clickTool(Tools.GRADIENT);

        if (maskMode.isMaskEditing()) {
            // reset the default colors, otherwise it might be all gray
            keyboard.fgBgDefaults();
        }

        mouse.randomAltClick(); // TODO this adds a history entry
        keyboard.undoRedo("Hide Gradient Handles");

        boolean gradientCreated = false;

        for (GradientType gradientType : GradientType.values()) {
            if (change(pw.comboBox("typeCB"), gradientType.toString()) && gradientCreated) {
                keyboard.undoRedo("Change Gradient Type");
            }

            for (String cycleMethod : GradientTool.CYCLE_METHODS) {
                if (change(pw.comboBox("cycleMethodCB"), cycleMethod) && gradientCreated) {
                    keyboard.undoRedo("Change Gradient Cycling");
                }

                GradientColorType[] gradientColorTypes = GradientColorType.values();
                for (GradientColorType colorType : gradientColorTypes) {
                    if (context.skip()) {
                        continue;
                    }
                    if (change(pw.comboBox("colorTypeCB"), colorType.toString()) && gradientCreated) {
                        keyboard.undoRedo("Change Gradient Colors");
                    }

                    if (checkRandomly(pw.checkBox("reverseCB")) && gradientCreated) {
                        keyboard.undoRedo("Reverse Gradient");
                    }

                    // drag the gradient
                    Point start = mouse.moveRandomlyWithinCanvas();
                    Point end = mouse.dragRandomlyWithinCanvas();
                    if (!gradientCreated) { // this was the first
                        keyboard.undoRedo("Create Gradient");
                    } else {
                        keyboard.undoRedo("Change Gradient");
                    }

                    // test the handle movement
                    double rd = Rnd.nextDouble();
                    if (rd < 0.33) {
                        // drag the end handle
                        mouse.moveToScreen(end.x, end.y);
                        mouse.dragRandomlyWithinCanvas();
                    } else if (rd > 0.66) {
                        // drag the start handle
                        mouse.moveToScreen(start.x, start.y);
                        mouse.dragRandomlyWithinCanvas();
                    } else {
                        // drag the middle handle
                        Point2D c = Geometry.midPoint(start, end);
                        mouse.moveToScreen((int) c.getX(), (int) c.getY());
                        mouse.dragRandomlyWithinCanvas();
                    }
                    keyboard.undoRedo("Change Gradient");
                    gradientCreated = true;
                }
            }
        }

        if (gradientCreated) { // pretty likely
            keyboard.pressEsc(); // hide the gradient handles
            keyboard.undoRedo("Hide Gradient Handles");
        }
        context.afterTestActions();
    }

    private void testPaintBucketTool() {
        context.log(1, "paint bucket tool");
        app.clickTool(Tools.PAINT_BUCKET);

        mouse.randomAltClick();
        keyboard.undoRedoUndo("Paint Bucket Tool");

        mouse.moveToCanvas(300, 300);
        pw.click();
        keyboard.undoRedoUndo("Paint Bucket Tool");

        context.afterTestActions();
    }

    private void testColorPickerTool() {
        context.log(1, "color picker tool");

        app.clickTool(Tools.COLOR_PICKER);

        mouse.randomAltClick();

        mouse.moveToCanvas(300, 300);
        pw.click();
        mouse.dragToCanvas(400, 400);

        context.afterTestActions();
    }

    private void testShapesTool() {
        context.log(1, "shapes tool");
        app.clickTool(Tools.SHAPES);

        keyboard.randomizeColors();

        // reset defaults
        pw.comboBox("shapeTypeCB").selectItem(ShapeType.RECTANGLE.toString());
        pw.comboBox("fillPaintCB").selectItem(TwoPointPaintType.RADIAL_GRADIENT.toString());
        pw.comboBox("strokePaintCB").selectItem(TwoPointPaintType.NONE.toString());

        EDT.assertShapesToolStateIs(IDLE);
        pw.button("convertToSelection").requireDisabled();

        mouse.randomCtrlClick();
        mouse.randomAltClick();
        mouse.randomShiftClick();

        mouse.moveToCanvas(50, 50);
        mouse.dragToCanvas(150, 100);
        keyboard.undoRedo("Create Shape");

        changeShapesToolEffects();
        keyboard.undoRedo("Change Shape Effects");

        pw.comboBox("strokePaintCB").selectItem(TwoPointPaintType.FOREGROUND.toString());
        keyboard.undoRedo("Change Shape Stroke");

        setupStrokeSettingsDialog();
        keyboard.undoRedo("Change Shape Stroke Settings");

        mouse.moveToCanvas(200, 50);
        app.setMaxUntestedEdits(2); // the drag will create two edits
        mouse.dragToCanvas(300, 100);
        keyboard.undoRedo("Rasterize Shape", "Create Shape");
        app.setMaxUntestedEdits(1); // reset

        pw.comboBox("shapeTypeCB").selectItem(ShapeType.CAT.toString());
        keyboard.undoRedo("Change Shape Type");

        // resize the transform box by the SE handle
        mouse.moveToCanvas(300, 100);
        mouse.dragToCanvas(500, 300);
        keyboard.undoRedo("Change Transform Box");

        ShapeType[] shapeTypes = ShapeType.values();
        for (ShapeType shapeType : shapeTypes) {
            if (shapeType == ShapeType.CAT || context.skip()) {
                continue;
            }
            pw.comboBox("shapeTypeCB").selectItem(shapeType.toString());
            keyboard.undoRedo("Change Shape Type");
        }

        for (TwoPointPaintType paintType : TwoPointPaintType.values()) {
            if (context.skip()) {
                continue;
            }
            if (change(pw.comboBox("fillPaintCB"), paintType.toString())) {
                keyboard.undoRedo("Change Shape Fill");
            }
            if (change(pw.comboBox("strokePaintCB"), paintType.toString())) {
                keyboard.undoRedo("Change Shape Stroke");
            }
        }

        EDT.assertShapesToolStateIs(TRANSFORM);
        pw.button("convertToSelection").requireEnabled();

        mouse.clickCanvas(50, 300);

        EDT.assertShapesToolStateIs(IDLE);
        pw.button("convertToSelection").requireDisabled();

        keyboard.undoRedoUndo("Rasterize Shape");

        // test convert to selection
        pw.button("convertToSelection").requireEnabled().click();
        keyboard.undoRedo("Convert Path to Selection");
        app.deselect();

        context.afterTestActions();
    }

    private void changeShapesToolEffects() {
        findButtonByText(pw, "Effects...")
            .requireEnabled()
            .click();

        var dialog = app.findDialogByTitle("Effects");
        var tabbedPane = dialog.tabbedPane();
        tabbedPane.requireTabTitles(
            EffectsPanel.GLOW_TAB_NAME,
            EffectsPanel.INNER_GLOW_TAB_NAME,
            EffectsPanel.NEON_BORDER_TAB_NAME,
            EffectsPanel.DROP_SHADOW_TAB_NAME);
        tabbedPane.selectTab(EffectsPanel.INNER_GLOW_TAB_NAME);
        tabbedPane.selectTab(EffectsPanel.NEON_BORDER_TAB_NAME);
        tabbedPane.selectTab(EffectsPanel.DROP_SHADOW_TAB_NAME);
        tabbedPane.selectTab(EffectsPanel.GLOW_TAB_NAME);

        JCheckBoxFixture enabledCB = dialog.checkBox("enabledCB");
        boolean activeTabEnabled = enabledCB.target().isSelected();
        enabledCB.check(!activeTabEnabled); // toggle in order to force a change

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    private void setupStrokeSettingsDialog() {
        findButtonByText(pw, "Stroke Settings...")
            .requireEnabled()
            .click();
        var dialog = app.findDialogByTitle("Stroke Settings");

        // force a change in the stroke width
        var strokeWidthSlider = dialog.slider();
        int value = strokeWidthSlider.target().getValue();
        if (value == 5) { // 5 is the default stroke width
            strokeWidthSlider.slideTo(20);
        } else {
            strokeWidthSlider.slideTo(5);
        }

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }
}
