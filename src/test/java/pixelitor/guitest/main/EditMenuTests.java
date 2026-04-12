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

import org.assertj.swing.fixture.DialogFixture;
import pixelitor.Composition;
import pixelitor.gui.ImageArea;
import pixelitor.guides.GuideStrokeType;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.EDT;
import pixelitor.guitest.Keyboard;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.utils.Rnd;

import static org.assertj.core.api.Assertions.assertThat;
import static pixelitor.gui.ImageArea.Mode.FRAMES;

public class EditMenuTests {
    private static boolean preferencesTested = false;

    private final Keyboard keyboard;
    private final AppRunner app;
    private final MaskMode maskMode;

    private final TestContext context;

    public EditMenuTests(TestContext context) {
        this.context = context;

        this.keyboard = context.keyboard();
        this.app = context.app();
        this.maskMode = context.maskMode();
    }

    void start() {
        context.log(0, "edit menu");

        testMenuUndoRedo();
        testFade();
        testCopyPaste();
        testPreferences();

        context.afterTestActions();
    }

    private void testMenuUndoRedo() {
        app.invert();
        app.runMenuCommand("Undo Invert");
        app.runMenuCommand("Redo Invert");
    }

    private void testFade() {
        app.runMenuCommand("Fade Invert...");
        var dialog = app.findFilterDialog();

        dialog.slider().slideTo(75); // set opacity to 75%

        dialog.checkBox("show original").check().uncheck();

        dialog.button("ok").click();

        keyboard.undoRedoUndo("Fade");
    }

    private void testCopyPaste() {
        context.log(1, "copy-paste");

        EDT.assertNumViewsIs(1);
        String existingLayerName = "layer 1";
        String activeCompAtStartName = EDT.queryActiveComp(Composition::getName);
        app.checkLayerNamesAre(existingLayerName);

        app.runMenuCommand("Copy Layer/Mask");

        app.runMenuCommand("Paste as New Layer");
        app.checkLayerNamesAre(existingLayerName, "pasted layer");

        keyboard.undo("New Pasted Layer");
        app.checkLayerNamesAre(existingLayerName);

        keyboard.redo("New Pasted Layer");
        app.checkLayerNamesAre(existingLayerName, "pasted layer");

        app.checkLayerNamesAre(existingLayerName, "pasted layer");

        app.runMenuCommand("Copy Composite");
        app.runMenuCommand("Paste as New Image");
        assertThat(EDT.getActiveCompName()).startsWith("Pasted Image ");

        // close the pasted image
        app.closeCurrentView(AppRunner.ExpectConfirmation.NO);
        EDT.assertOpenCompNamesAre(activeCompAtStartName);

        // delete the pasted layer
        app.checkLayerNamesAre(existingLayerName, "pasted layer");
        assert DeleteActiveLayerAction.INSTANCE.isEnabled();
        app.runMenuCommand("Delete Layer");
        app.checkLayerNamesAre(existingLayerName);

        keyboard.undo("Delete pasted layer");
        app.checkLayerNamesAre(existingLayerName, "pasted layer");

        keyboard.redo("Delete pasted layer");
        app.checkLayerNamesAre(existingLayerName);

        maskMode.apply(context);
    }

    private void testPreferences() {
        if (preferencesTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        context.log(1, "preferences dialog");

        app.runMenuCommand("Preferences...");
        var dialog = app.findDialogByTitle("Preferences");
        if (preferencesTested) {
            dialog.tabbedPane().selectTab("UI");
        }

        // test "Images In"
        testPreferencesUIChooser(dialog);

        // test "Layer/Mask Thumb Sizes"
        var thumbSizeCB = dialog.comboBox("thumbSizeCB");
        thumbSizeCB.selectItem(3);
        thumbSizeCB.selectItem(0);

        // test the Mouse tab

        // test the Guides tab
        dialog.tabbedPane().selectTab("Guides");
        GuideStrokeType[] guideStyles = GuideStrokeType.values();
        dialog.comboBox("guideStyleCB").selectItem(Rnd.chooseFrom(guideStyles).toString());
        dialog.comboBox("cropGuideStyleCB").selectItem(Rnd.chooseFrom(guideStyles).toString());

        // test the Advanced tab
        dialog.tabbedPane().selectTab("Advanced");
        testPreferencesUndoLevels(dialog);

        dialog.button("ok").click();
        // this time the preferences dialog should close
        dialog.requireNotVisible();

        preferencesTested = true;
    }

    private static void testPreferencesUIChooser(DialogFixture dialog) {
        var uiChooser = dialog.comboBox("uiChooser");
        if (EDT.call(() -> ImageArea.isActiveMode(FRAMES))) {
            uiChooser.requireSelection("Internal Windows");
            uiChooser.selectItem("Tabs");
            uiChooser.selectItem("Internal Windows");
        } else {
            uiChooser.requireSelection("Tabs");
            uiChooser.selectItem("Internal Windows");
            uiChooser.selectItem("Tabs");
        }
    }

    private void testPreferencesUndoLevels(DialogFixture dialog) {
        var undoLevelsTF = dialog.textBox("undoLevelsTF");
        boolean undoWas5 = undoLevelsTF.text().equals("5");
        undoLevelsTF.deleteText().enterText("n");

        // try to accept the error dialog
        dialog.button("ok").click();
        app.expectAndCloseErrorDialog();

        // correct the error
        if (undoWas5) {
            undoLevelsTF.deleteText().enterText("6");
        } else {
            undoLevelsTF.deleteText().enterText("5");
        }
    }
}
