/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import org.assertj.swing.edt.GuiActionRunner;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.guides.Guides;
import pixelitor.history.History;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PenTool;
import pixelitor.utils.test.Assertions;
import pixelitor.utils.test.Events;

import java.awt.Rectangle;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility methods to execute queries, actions and assertions
 * on the Event Dispatch Thread.
 */
public class EDT {
    private EDT() {
    }

    public static Composition getComp() {
        return GuiActionRunner.execute(ImageComponents::getActiveCompOrNull);
    }

    public static Canvas getCanvas() {
        return GuiActionRunner.execute(() -> {
            ImageComponent ic = ImageComponents.getActiveIC();
            return ic.getCanvas();
        });
    }

    public static Rectangle getVisibleCanvasBoundsOnScreen() {
        return GuiActionRunner.execute(() -> {
            ImageComponent ic = ImageComponents.getActiveIC();
            return ic.getVisibleCanvasBoundsOnScreen();
        });
    }

    public static Selection getSelection() {
        return GuiActionRunner.execute(() -> {
            ImageComponent ic = ImageComponents.getActiveIC();
            return ic.getComp().getSelection();
        });
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
        SelectionInteraction actual = GuiActionRunner.execute(
                Tools.SELECTION::getCurrentInteraction);
        if (expected != actual) {
            throw new AssertionError("expected " + expected + ", found " + actual);
        }
    }

    public static void assertCurrentCompFileIs(File expected) {
        GuiActionRunner.execute(() ->
                assertThat(ImageComponents.getActiveCompOrNull().getFile()).isEqualTo(expected));
    }

    public static void assertActiveToolsIs(Tool expected) {
        Tool actual = GuiActionRunner.execute(Tools::getCurrent);
        if (actual != expected) {
            throw new AssertionError("Expected " + expected
                    + ", found " + actual);
        }
    }

    public static Path getPenToolPath() {
        return GuiActionRunner.execute(() -> PenTool.path);
    }

    public static Guides getGuides() {
        return GuiActionRunner.execute(() -> {
            Composition comp = ImageComponents.getActiveCompOrNull();
            return comp.getGuides();
        });
    }

    public static void undo(String edit) {
        GuiActionRunner.execute(() -> History.undo(edit));
    }

    public static void redo(String edit) {
        GuiActionRunner.execute(() -> History.redo(edit));
    }

    public static void assertEditToBeUndoneNameIs(String expected) {
        GuiActionRunner.execute(
                () -> History.assertEditToBeUndoneNameIs(expected));
    }

    public static void assertEditToBeRedoneNameIs(String expected) {
        GuiActionRunner.execute(
                () -> History.assertEditToBeRedoneNameIs(expected));
    }

    public static void postAssertJEvent(String evt) {
        GuiActionRunner.execute(() -> Events.postAssertJEvent(evt));
    }

    public static void increaseZoom() {
        GuiActionRunner.execute(() ->
                ImageComponents.getActiveIC().increaseZoom());
    }

    public static void decreaseZoom() {
        GuiActionRunner.execute(() ->
                ImageComponents.getActiveIC().decreaseZoom());
    }

    public static ZoomLevel getZoomLevel() {
        return GuiActionRunner.execute(() ->
                ImageComponents.getActiveIC().getZoomLevel());
    }

    public static void assertZoomOfActiveIs(ZoomLevel expected) {
        GuiActionRunner.execute(() -> ImageComponents.assertZoomOfActiveIs(expected));
    }

    public static void assertNumOpenImagesIs(int expected) {
        GuiActionRunner.execute(() -> ImageComponents.assertNumOpenImagesIs(expected));
    }

    public static void assertNumOpenImagesIsAtLeast(int expected) {
        GuiActionRunner.execute(() -> ImageComponents.assertNumOpenImagesIsAtLeast(expected));
    }

    public static void assertNumLayersIs(int expected) {
        GuiActionRunner.execute(() -> Assertions.numLayersIs(expected));
    }

    public static void activate(ImageComponent ic) {
        GuiActionRunner.execute(() ->
                ImageComponents.setActiveIC(ic, true));
    }
}
