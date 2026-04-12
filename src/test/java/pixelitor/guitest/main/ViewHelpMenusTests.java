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

import org.assertj.swing.exception.ComponentLookupException;
import pixelitor.gui.HistogramsPanel;
import pixelitor.gui.ImageArea;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.StatusBar;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.EDT;
import pixelitor.guitest.Keyboard;
import pixelitor.guitest.Mouse;
import pixelitor.layers.LayersContainer;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.tools.Tools;
import pixelitor.utils.Rnd;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import static java.awt.event.KeyEvent.VK_F6;
import static java.awt.event.KeyEvent.VK_F7;
import static org.assertj.core.api.Assertions.assertThat;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.guitest.GUITestUtils.findButtonByText;

/**
 * Tests for the "View" and "Help" menus.
 */
public class ViewHelpMenusTests {
    private static boolean helpMenuTested = false;
    private static boolean viewMenuTested = false;
    private static boolean colorsTested = false;

    private final Keyboard keyboard;
    private final Mouse mouse;
    private final AppRunner app;

    private final TestContext context;

    public ViewHelpMenusTests(TestContext context) {
        this.context = context;

        this.keyboard = context.keyboard();
        this.mouse = context.mouse();
        this.app = context.app();
    }

    public void start() {
        testViewMenu();
        testColors();
        testHelpMenu();
    }

    void testViewMenu() {
        if (viewMenuTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        context.log(0, "view menu");

        EDT.assertNumViewsIs(1);
        EDT.assertNumLayersInActiveHolderIs(1);

        testZoomCommands();
        testHistory();
        testHideShowUI();
        testGuides();

        if (ImageArea.isActiveMode(FRAMES)) {
            app.runMenuCommand("Cascade");
            app.runMenuCommand("Tile");
        }

        context.afterTestActions();
        viewMenuTested = true;
    }

    private void testZoomCommands() {
        context.log(1, "zoom commands");

        ZoomLevel startingZoom = EDT.getActiveZoomLevel();

        app.runMenuCommand("Zoom In");
        EDT.assertActiveZoomIs(startingZoom.zoomIn());

        app.runMenuCommand("Zoom Out");
        EDT.assertActiveZoomIs(startingZoom.zoomIn().zoomOut());

        app.runMenuCommand("Fit Space");
        app.runMenuCommand("Fit Width");
        app.runMenuCommand("Fit Height");

        app.runMenuCommand("Actual Pixels");
        EDT.assertActiveZoomIs(ZoomLevel.ACTUAL_SIZE);
    }

    private void testHideShowUI() {
        context.log(1, "hide/show UI elements");

        app.runMenuCommand("Reset Workspace");
        assert EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert EDT.call(LayersContainer::areLayersShown);
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());

        app.runMenuCommand("Hide Status Bar");
        assert !EDT.call(StatusBar::isShown);

        app.runMenuCommand("Show Status Bar");
        assert EDT.call(StatusBar::isShown);

        app.runMenuCommand("Show Histograms");
        assert EDT.call(HistogramsPanel::isShown);

        app.runMenuCommand("Hide Histograms");
        assert !EDT.call(HistogramsPanel::isShown);

        keyboard.press(VK_F6);
        assert EDT.call(HistogramsPanel::isShown);

        keyboard.press(VK_F6);
        assert !EDT.call(HistogramsPanel::isShown);

        app.runMenuCommand("Hide Layers");
        assert !EDT.call(LayersContainer::areLayersShown);

        app.runMenuCommand("Show Layers");
        assert EDT.call(LayersContainer::areLayersShown);

        keyboard.press(VK_F7);
        assert !EDT.call(LayersContainer::areLayersShown);

        keyboard.press(VK_F7);
        assert EDT.call(LayersContainer::areLayersShown);

        app.runMenuCommand("Hide Tools");
        assert !EDT.call(() -> PixelitorWindow.get().areToolsShown());

        app.runMenuCommand("Show Tools");
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());

        app.runMenuCommand("Hide All");
        assert !EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert !EDT.call(LayersContainer::areLayersShown);
        assert !EDT.call(() -> PixelitorWindow.get().areToolsShown());

        app.runMenuCommand("Restore Workspace");
        assert EDT.call(StatusBar::isShown);
        assert !EDT.call(HistogramsPanel::isShown);
        assert EDT.call(LayersContainer::areLayersShown);
        assert EDT.call(() -> PixelitorWindow.get().areToolsShown());
    }

    private void testGuides() {
        context.log(1, "guides");

        app.runMenuCommand("Add Horizontal Guide...");
        var dialog = app.findDialogByTitle("Add Horizontal Guide");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getActiveGuides().getHorizontals()).containsExactly(0.5);
        assertThat(EDT.getActiveGuides().getVerticals()).isEmpty();
        keyboard.undoRedo("Create Guides");

        app.runMenuCommand("Add Vertical Guide...");
        dialog = app.findDialogByTitle("Add Vertical Guide");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getActiveGuides().getHorizontals()).containsExactly(0.5);
        assertThat(EDT.getActiveGuides().getVerticals()).containsExactly(0.5);
        keyboard.undoRedo("Change Guides");

        app.runMenuCommand("Add Grid Guides...");
        dialog = app.findDialogByTitle("Add Grid Guides");
        dialog.button("ok").click();
        dialog.requireNotVisible();
        assertThat(EDT.getActiveGuides().getHorizontals()).containsExactly(0.25, 0.5, 0.75);
        assertThat(EDT.getActiveGuides().getVerticals()).containsExactly(0.25, 0.5, 0.75);
        keyboard.undoRedo("Change Guides");

        app.runMenuCommand("Clear Guides");
        keyboard.undoRedo("Clear Guides");
        assertThat(EDT.getActiveGuides()).isNull();
    }

    private void testHistory() {
        context.log(1, "history");

        // before testing make sure that we have something
        // in the history even if this is running alone
        app.clickTool(Tools.BRUSH);
        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();
        keyboard.undoRedo("Brush Tool");

        app.clickTool(Tools.ERASER);
        mouse.moveRandomlyWithinCanvas();
        mouse.dragRandomlyWithinCanvas();
        keyboard.undoRedo("Eraser Tool");

        // now start testing the history window
        app.runMenuCommand("Show History...");
        var dialog = app.findDialogByTitle("History");

        var undoButton = dialog.button("undo");
        var redoButton = dialog.button("redo");

        undoButton.requireEnabled();
        redoButton.requireDisabled();

        undoButton.click();
        redoButton.requireEnabled();
        redoButton.click();

        var list = dialog.list();

        // after clicking the first item,
        // we have one last undo
        list.clickItem(0);
        undoButton.requireEnabled();
        redoButton.requireEnabled();
        undoButton.click();
        // no more undo, the list should contain no selection
        list.requireNoSelection();
        undoButton.requireDisabled();
        redoButton.requireEnabled();

        // after clicking the last item,
        // we have a selection and undo, but no redo
        String[] contents = list.contents();
        int lastIndex = contents.length - 1;
        list.clickItem(lastIndex);
        list.requireSelection(lastIndex);
        undoButton.requireEnabled();
        redoButton.requireDisabled();

        dialog.close();
        dialog.requireNotVisible();
    }

    void testHelpMenu() {
        if (helpMenuTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        context.log(0, "help menu");

        testTipOfTheDay();
        testInternalState();
        testCheckForUpdate();
        testAbout();

        context.afterTestActions();
        helpMenuTested = true;
    }

    private void testTipOfTheDay() {
        var laf = EDT.call(UIManager::getLookAndFeel);

        app.runMenuCommand("Tip of the Day");
        var dialog = app.findDialogByTitle("Tip of the Day");
        if (laf instanceof NimbusLookAndFeel) {
            findButtonByText(dialog, "Next >").click();
            findButtonByText(dialog, "Next >").click();
            findButtonByText(dialog, "< Back").click();
        } else {
            findButtonByText(dialog, "Next Tip").click();
            findButtonByText(dialog, "Next Tip").click();
        }
        findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

    private void testInternalState() {
        app.runMenuCommand("Internal State...");
        var dialog = app.findDialogByTitle("Internal State");
        findButtonByText(dialog, "Copy as JSON").click();
        findButtonByText(dialog, "Close").click();
        dialog.requireNotVisible();
    }

    private void testCheckForUpdate() {
        app.runMenuCommand("Check for Updates...");
        try {
            // the title is either "Pixelitor Is Up to Date"
            // or "New Version Available"
            app.findJOptionPane(null).buttonWithText("Close").click();
        } catch (ComponentLookupException e) {
            // if a close button was not found, then it must be the up-to-date dialog
            app.findJOptionPane("Pixelitor Is Up to Date").okButton().click();
        }
    }

    private void testAbout() {
        app.runMenuCommand("About Pixelitor");
        var dialog = app.findDialogByTitle("About Pixelitor");

        var tabbedPane = dialog.tabbedPane();
        tabbedPane.requireTabTitles("About", "Credits", "System Info");
        tabbedPane.selectTab("Credits");
        tabbedPane.selectTab("System Info");
        tabbedPane.selectTab("About");

        dialog.button("ok").click();
        dialog.requireNotVisible();
    }

    void testColors() {
        if (colorsTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        context.log(0, "colors");

        testColorPaletteMenu("Foreground...", "Foreground Color Variations");
        testColorPaletteMenu("Background...", "Background Color Variations");

        testColorPaletteMenu("HSB Mix Foreground with Background...", "HSB Mix with Background");
        testColorPaletteMenu("RGB Mix Foreground with Background...", "RGB Mix with Background");
        testColorPaletteMenu("HSB Mix Background with Foreground...", "HSB Mix with Foreground");
        testColorPaletteMenu("RGB Mix Background with Foreground...", "RGB Mix with Foreground");

        testColorPaletteMenu("Color Palette...", "Color Palette");

        context.afterTestActions();
        colorsTested = true;
    }

    private void testColorPaletteMenu(String menuName, String dialogTitle) {
        app.runMenuCommand(menuName);
        context.testColorPaletteDialog(dialogTitle);
    }
}
