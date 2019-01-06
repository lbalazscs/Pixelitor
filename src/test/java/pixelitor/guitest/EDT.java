/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Composition;
import pixelitor.gui.CompositionView;
import pixelitor.gui.OpenComps;
import pixelitor.guides.Guides;
import pixelitor.history.History;
import pixelitor.layers.Layer;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PathTransformer;
import pixelitor.tools.pen.PenTool;
import pixelitor.tools.shapes.ShapesToolState;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.utils.test.Assertions;
import pixelitor.utils.test.Events;

import java.awt.Point;
import java.util.concurrent.Callable;
import java.util.function.Function;

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

    public static CompositionView getActiveView() {
        return call(OpenComps::getActiveView);
    }

    public static Composition getComp() {
        return call(OpenComps::getActiveCompOrNull);
    }

    /**
     * Returns the given property of the active composition
     */
    public static <T> T active(Function<Composition, ? extends T> fun) {
        return call(() -> fun.apply(OpenComps.getActiveCompOrNull()));
    }

    public static <T> T activeTool(Function<Tool, ? extends T> fun) {
        return call(() -> fun.apply(Tools.getCurrent()));
    }

    public static Selection getSelection() {
        return active(Composition::getSelection);
    }

    public static Guides getGuides() {
        return active(Composition::getGuides);
    }

    public static Layer getActiveLayer() {
        return call(OpenComps::getActiveLayerOrNull);
    }

    public static void assertThereIsSelection() {
        if (getSelection() == null) {
            throw new AssertionError("no selection found");
        }
    }

    public static void assertThereIsNoSelection() {
        if (getSelection() != null) {
            throw new AssertionError("selection found");
        }
    }

    public static void assertSelectionInteractionIs(SelectionInteraction expected) {
        SelectionInteraction actual = call(Tools.SELECTION::getCurrentInteraction);
        if (expected != actual) {
            throw new AssertionError("expected " + expected + ", found " + actual);
        }
    }

    public static void assertActiveToolIs(Tool expected) {
        Tool actual = call(Tools::getCurrent);
        if (actual != expected) {
            throw new AssertionError("Expected " + expected
                + ", found " + actual);
        }
    }

    public static Path getPenToolPath() {
        return call(() -> PenTool.path);
    }

    public static Point getPenToolBoxPos(int boxIndex,
                                         Function<TransformBox, DraggablePoint> pointFun) {
        return call(() -> {
            PathTransformer mode = (PathTransformer) Tools.PEN.getMode();
            TransformBox box = mode.getBox(boxIndex);
            return pointFun.apply(box).getScreenCoords();
        });
    }

    public static void undo(String edit) {
        run(() -> History.undo(edit));
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

    public static void postAssertJEvent(String evt) {
        run(() -> Events.postAssertJEvent(evt));
    }

    public static void increaseZoom() {
        run(() -> OpenComps.getActiveView().increaseZoom());
    }

    public static void decreaseZoom() {
        run(() -> OpenComps.getActiveView().decreaseZoom());
    }

    public static ZoomLevel getZoomLevelOfActive() {
        return call(() -> OpenComps.getActiveView().getZoomLevel());
    }

    public static void assertZoomOfActiveIs(ZoomLevel expected) {
        run(() -> OpenComps.assertZoomOfActiveIs(expected));
    }

    public static void assertNumOpenImagesIs(int expected) {
        run(() -> OpenComps.assertNumOpenImagesIs(expected));
    }

    public static void assertNumOpenImagesIsAtLeast(int expected) {
        run(() -> OpenComps.assertNumOpenImagesIsAtLeast(expected));
    }

    public static void assertNumLayersIs(int expected) {
        run(() -> Assertions.numLayersIs(expected));
    }

    public static void assertShapesToolStateIs(ShapesToolState expected) {
        ShapesToolState actual = call(Tools.SHAPES::getState);
        if (actual != expected) {
            throw new AssertionError("expected " + expected + ", found " + actual);
        }
    }

    public static void activate(CompositionView cv) {
        run(() -> OpenComps.setActiveIC(cv, true));
    }

    /**
     * Returns the given property of the active layer.
     */
    public static <T> T activeLayer(Function<Layer, T> fun) {
        return call(() -> fun.apply(OpenComps.getActiveLayerOrNull()));
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
}
