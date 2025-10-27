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
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.History;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Utility methods to execute queries, actions and assertions
 * on the Event Dispatch Thread (EDT).
 */
public class EDT {
    private EDT() {
    }

    /**
     * Executes the given callable on the EDT and returns its result.
     */
    public static <T> T call(Callable<T> callable) {
        return GuiActionRunner.execute(callable);
    }

    /**
     * Executes the given runnable on the EDT.
     */
    public static void run(GuiActionRunnable runnable) {
        GuiActionRunner.execute(runnable);
    }

    public static View getActiveView() {
        return call(Views::getActive);
    }

    public static Composition getActiveComp() {
        return call(Views::getActiveComp);
    }

    private static <U, T> T query(Supplier<U> supplier, Function<U, ? extends T> fun) {
        return call(() -> fun.apply(supplier.get()));
    }

    /**
     * Returns the given property of the active composition.
     */
    public static <T> T queryActiveComp(Function<Composition, ? extends T> fun) {
        return query(Views::getActiveComp, fun);
    }

    /**
     * Returns the given property of the active layer.
     */
    public static <T> T queryActiveLayer(Function<Layer, T> fun) {
        return query(Views::getActiveLayer, fun);
    }

    /**
     * Returns the given property of the active view.
     */
    public static <T> T queryActiveView(Function<View, T> fun) {
        return query(Views::getActive, fun);
    }

    public static Selection getActiveSelection() {
        return call(Views::getActiveSelection);
    }

    public static Guides getActiveGuides() {
        return queryActiveComp(Composition::getGuides);
    }

    public static Layer getActiveLayer() {
        return call(Views::getActiveLayer);
    }

    public static boolean isActiveLayerType(Class<? extends Layer> type) {
        return call(() -> Views.getActiveLayer().getClass().equals(type));
    }

    /**
     * Asserts that the active composition has a selection.
     */
    public static void requireSelection() {
        assertThat(getActiveSelection()).withFailMessage("no selection found").isNotNull();
    }

    /**
     * Asserts that the active composition has no selection.
     */
    public static void requireNoSelection() {
        assertThat(getActiveSelection()).withFailMessage("selection found").isNull();
    }

    /**
     * Asserts that the active content layer has zero translation offset.
     */
    public static void assertNoTranslation() {
        Point translation = queryActiveLayer(layer -> {
            assertThat(layer).isInstanceOf(ContentLayer.class);
            ContentLayer contentLayer = (ContentLayer) layer;
            return new Point(contentLayer.getTx(), contentLayer.getTy());
        });
        assertThat(translation).isEqualTo(new Point(0, 0));
    }

    /**
     * Asserts that the given selection tool is using the expected shape combinator.
     */
    public static void assertSelectionCombinatorIs(AbstractSelectionTool tool, ShapeCombinator expected) {
        ShapeCombinator actual = call(tool::getCombinator);
        assertThat(actual).isSameAs(expected);
    }

    public static void assertActiveToolIs(Tool expected) {
        Tool actual = call(Tools::getActive);
        assertThat(actual).isSameAs(expected);
    }

    /**
     * Asserts that the names of all open compositions match the given names in order.
     */
    public static void assertOpenCompNamesAre(String... expectedNames) {
        List<String> actual = call(Views::getOpenCompNames);
        assertThat(actual).containsExactly(expectedNames);
    }

    public static String getActiveCompName() {
        return queryActiveComp(Composition::getName);
    }

    /**
     * Returns the active path from the active composition.
     */
    public static Path getActivePath() {
        return queryActiveComp(Composition::getActivePath);
    }

    /**
     * Returns the screen coordinates of a specific handle on a transform box of the Transform Path tool.
     */
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

    public static List<String> getEditNames() {
        return call(History::getEditNames);
    }

    public static void zoomIn() {
        run(() -> Views.getActive().zoomIn());
    }

    public static void zoomOut() {
        run(() -> Views.getActive().zoomOut());
    }

    /**
     * Returns the current zoom level of the active view.
     */
    public static ZoomLevel getActiveZoomLevel() {
        return call(() -> Views.getActive().getZoomLevel());
    }

    public static void assertActiveZoomIs(ZoomLevel expected) {
        assertThat(getActiveZoomLevel()).isSameAs(expected);
    }

    public static int getNumViews() {
        return call(Views::getNumViews);
    }

    public static void assertNumViewsIs(int expected) {
        int actual = call(Views::getNumViews);
        assertThat(actual).isEqualTo(expected);
    }

    public static int getNumLayersInActiveHolder() {
        return call(Views::getNumLayersInActiveHolder);
    }

    public static void assertNumLayersInActiveHolderIs(int expected) {
        Integer found = call(Views::getNumLayersInActiveHolder);
        assertThat(found).isEqualTo(expected);
    }

    public static void assertShapesToolStateIs(DragToolState expected) {
        DragToolState actual = call(Tools.SHAPES::getState);
        assertThat(actual).isEqualTo(expected);
    }

    public static void activate(View view) {
        run(() -> Views.setActiveView(view, true));
    }

    /**
     * Returns the given property of the layer with the given name in the active composition.
     */
    public static <T> T queryLayerWithName(String layerName, Function<Layer, T> fun) {
        return call(() ->
            fun.apply(Views.findFirstLayerWhere(layer ->
                layer.getName().equals(layerName), false)));
    }

    public static void assertActiveLayerTypeIs(Class<? extends Layer> expected) {
        Class<? extends Layer> actual = queryActiveLayer(Layer::getClass);
        assertThat(actual).isEqualTo(expected);
    }

    public static boolean activeLayerHasMask() {
        return queryActiveLayer(Layer::hasMask);
    }

    public static void assertActiveLayerHasMask(boolean expected) {
        boolean actual = activeLayerHasMask();
        assertThat(actual).isEqualTo(expected);
    }

    public static void assertActiveLayerHasMask() {
        assertActiveLayerHasMask(true);
    }

    public static void assertActiveLayerHasNoMask() {
        assertActiveLayerHasMask(false);
    }

    public static boolean activeLayerIsMaskEditing() {
        return queryActiveLayer(Layer::isMaskEditing);
    }

    public static void assertActiveLayerIsMaskEditing(boolean expected) {
        boolean actual = activeLayerIsMaskEditing();
        assertThat(actual).isEqualTo(expected);
    }

    public static void assertActiveLayerIsMaskEditing() {
        assertActiveLayerIsMaskEditing(true);
    }

    public static void assertActiveLayerIsNotMaskEditing() {
        assertActiveLayerIsMaskEditing(false);
    }

    public static void assertMaskViewModeIs(MaskViewMode expected) {
        MaskViewMode actual = queryActiveView(View::getMaskViewMode);
        assertThat(actual).isSameAs(expected);
    }

    /**
     * Asserts that the canvas of the active composition has the given width and height.
     */
    public static void assertCanvasSizeIs(int expectedWidth, int expectedHeight) {
        assertThat(queryActiveComp(Composition::getCanvas))
            .hasSize(expectedWidth, expectedHeight);
    }

    public static int getModalDialogNesting() {
        return call(GlobalEvents::getModalDialogNesting);
    }

    public static void assertNoModalDialogs() {
        assertThat(getModalDialogNesting()).isEqualTo(0);
    }

    /**
     * Sets the global foreground and background colors.
     */
    public static void setFgBgColors(Color fgColor, Color bgColor) {
        run(() -> {
            FgBgColors.setFGColor(fgColor);
            FgBgColors.setBGColor(bgColor);
        });
    }
}
