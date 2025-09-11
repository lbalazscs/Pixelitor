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

package pixelitor.guitest;

import org.assertj.swing.edt.GuiActionRunnable;
import org.assertj.swing.edt.GuiActionRunner;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.History;
import pixelitor.layers.Layer;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.Selection;
import pixelitor.selection.ShapeCombinator;
import pixelitor.tools.DragToolState;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.selection.AbstractSelectionTool;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.DraggablePoint;

import java.awt.Color;
import java.awt.Point;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Utility methods to execute queries, actions and assertions
 * on the Event Dispatch Thread.
 */
public class EDT {
    private EDT() {
    }

    public static <T> T call(Callable<T> callable) {
        return GuiActionRunner.execute(callable);
    }

    public static void run(GuiActionRunnable runnable) {
        GuiActionRunner.execute(runnable);
    }

    public static View getActiveView() {
        return call(Views::getActive);
    }

    public static Composition getComp() {
        return call(Views::getActiveComp);
    }

    public static Canvas getCanvas() {
        return call(() -> Views.getActiveComp().getCanvas());
    }

    /**
     * Returns the given property of the active composition
     */
    public static <T> T active(Function<Composition, ? extends T> fun) {
        return call(() -> fun.apply(Views.getActiveComp()));
    }

    public static Selection getActiveSelection() {
        return call(Views::getActiveSelection);
    }

    public static Guides getGuides() {
        return active(Composition::getGuides);
    }

    public static Layer getActiveLayer() {
        return call(Views::getActiveLayer);
    }

    public static boolean isActiveLayerType(Class<? extends Layer> type) {
        return call(() -> Views.getActiveLayer().getClass().equals(type));
    }

    public static void assertThereIsSelection() {
        if (getActiveSelection() == null) {
            throw new AssertionError("no selection found");
        }
    }

    public static void assertThereIsNoSelection() {
        if (getActiveSelection() != null) {
            throw new AssertionError("selection found");
        }
    }

    public static void assertSelectionCombinatorIs(AbstractSelectionTool tool, ShapeCombinator expected) {
        ShapeCombinator actual = call(tool::getCombinator);
        if (expected != actual) {
            throw new AssertionError("expected " + expected + ", found " + actual);
        }
    }

    public static void assertActiveToolIs(Tool expected) {
        Tool actual = call(Tools::getActive);
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + ", found " + actual + ".");
        }
    }

    public static Path getActivePath() {
        return active(Composition::getActivePath);
    }

    public static Point getTransformPathToolBoxPos(int boxIndex,
                                                   Function<TransformBox, DraggablePoint> handleSelector) {
        return call(() -> {
            TransformBox box = Tools.TRANSFORM_PATH.getBox(boxIndex);
            return handleSelector.apply(box).getScreenCoords();
        });
    }

    public static void undo() {
        run(History::undo);
    }

    public static void undo(String edit) {
        run(() -> History.undo(edit));
    }

    public static void redo() {
        run(History::redo);
    }

    public static void redo(String edit) {
        run(() -> History.redo(edit));
    }

    public static void assertEditToBeUndoneNameIs(String expected) {
        run(() -> History.assertEditToBeUndoneNameIs(expected));
    }

    public static void assertEditToBeRedoneNameIs(String expected) {
        run(() -> History.assertEditToBeRedoneNameIs(expected));
    }

    public static void zoomIn() {
        run(() -> Views.getActive().zoomIn());
    }

    public static void zoomOut() {
        run(() -> Views.getActive().zoomOut());
    }

    public static ZoomLevel getZoomLevelOfActive() {
        return call(() -> Views.getActive().getZoomLevel());
    }

    public static void assertZoomOfActiveIs(ZoomLevel expected) {
        run(() -> Views.assertZoomOfActiveIs(expected));
    }

    public static void assertNumOpenImagesIs(int expected) {
        run(() -> Views.assertNumViewsIs(expected));
    }

    public static void assertNumOpenImagesIsAtLeast(int expected) {
        run(() -> Views.assertNumViewsIsAtLeast(expected));
    }

    public static int getNumLayersInActiveHolder() {
        return call(Views::getNumLayersInActiveHolder);
    }

    public static void assertNumLayersIs(int expected) {
        Integer found = call(Views::getNumLayersInActiveHolder);
        if (found != expected) {
            throw new AssertionError("expected " + expected + ", found = " + found);
        }
    }

    public static void assertShapesToolStateIs(DragToolState expected) {
        DragToolState actual = call(Tools.SHAPES::getState);
        if (actual != expected) {
            throw new AssertionError("expected " + expected + ", found " + actual);
        }
    }

    public static void activate(View view) {
        run(() -> Views.setActiveView(view, true));
    }

    /**
     * Returns the given property of the active layer.
     */
    public static <T> T activeLayer(Function<Layer, T> fun) {
        return call(() -> fun.apply(Views.getActiveLayer()));
    }

    public static <T> T activeView(Function<View, T> fun) {
        return call(() -> fun.apply(Views.getActive()));
    }

    public static void assertActiveLayerTypeIs(Class<? extends Layer> expected) {
        Class<? extends Layer> actual = activeLayer((Function<Layer, Class<? extends Layer>>) Layer::getClass);
        if (expected != actual) {
            throw new AssertionError("expected " + expected.getSimpleName()
                + ", found " + actual.getSimpleName());
        }
    }

    public static String activeLayerName() {
        return activeLayer(Layer::getName);
    }

    public static boolean activeLayerIsMaskEditing() {
        return activeLayer(Layer::isMaskEditing);
    }

    public static boolean activeLayerHasMask() {
        return activeLayer(Layer::hasMask);
    }

    public static void assertCanvasSizeIs(int expectedWidth, int expectedHeight) {
        assertThat(active(Composition::getCanvas))
            .hasSize(expectedWidth, expectedHeight);
    }

    public static int getModalDialogCount() {
        return call(GlobalEvents::getModalDialogCount);
    }

    public static void assertModalDialogCountIs(int expected) {
        int actual = getModalDialogCount();
        if (actual != expected) {
            throw new AssertionError("expected " + expected + ", found " + actual);
        }
    }

    public static void setFgBgColors(Color fgColor, Color bgColor) {
        run(() -> {
            FgBgColors.setFGColor(fgColor);
            FgBgColors.setBGColor(bgColor);
        });
    }
}
