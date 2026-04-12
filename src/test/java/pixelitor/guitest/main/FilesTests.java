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

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.EDT;
import pixelitor.io.FileFormat;
import pixelitor.io.RecentDirs;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.core.matcher.JButtonMatcher.withText;
import static pixelitor.guitest.GUITestUtils.findButtonByText;

public class FilesTests {
    private static boolean fileMenuTested = false;

    private final AppRunner app;
    private final MaskMode maskMode;
    private final TestConfig config;

    private final TestContext context;

    public FilesTests(TestContext context) {
        this.context = context;

        this.app = context.app();
        this.maskMode = context.maskMode();
        this.config = context.config();
    }

    void start() {
        if (fileMenuTested && Rnd.nextDouble() > 0.05) {
            return;
        }
        context.log(0, "file menu");

        config.cleanOutputs();

        testNewImage();
        testSave("png");
        testSave("pxc");
        context.closeOneOfTwoViews();
        testFileOpen();
        context.closeOneOfTwoViews();
        testExportOptimizedJPEG();
        testMagick();
        testExportLayerAnimation();
        testExportTweeningAnimation();
        testReload();
        testShowMetadata();
        testBatchResize();
        testBatchFilter();
        testExportLayerToPNG();
        testScreenCapture();
        testCloseAll();

        // open an image for the next test
        context.openFileWithDialog(config.getInputDir(), "a.jpg");

        fileMenuTested = true;
        context.afterTestActions();
    }

    private void testNewImage() {
        context.log(1, "new image");

        app.createNewImage(611, 411, null);

        app.closeCurrentView(AppRunner.ExpectConfirmation.NO);
    }

    private void testFileOpen() {
        context.log(1, "file open");

        app.runMenuCommand("Open...");
        var openDialog = app.findOpenFileChooser();
        openDialog.cancel();

        context.openFileWithDialog(config.getInputDir(), "b.jpg");

        context.afterTestActions();
    }

    private void testSave(String extension) {
        context.log(1, "save, ext = " + extension);

        // create a new image to be saved
        app.createNewImage(400, 400, null);
        maskMode.apply(context);

        // the new image is unsaved => has no file
        assertThat(EDT.queryActiveComp(Composition::getFile)).isNull();

        String fileName = "saved." + extension;
        app.runMenuCommand("Save");
        // new unsaved image, will be saved with a file chooser
        app.acceptSaveDialog(config.getBaseDir(), fileName);

        // now that the file is saved, save again:
        // no file chooser should appear
        app.runMenuCommand("Save");
        Utils.sleep(500, MILLISECONDS);

        // test "Save As"
        app.runMenuCommand("Save As...");
        // there is always a dialog for "Save As"
        app.acceptSaveDialog(config.getBaseDir(), fileName);

        app.closeCurrentView(AppRunner.ExpectConfirmation.NO);

        context.openFileWithDialog(config.getBaseDir(), fileName);
        maskMode.apply(context);

        // can be dirty if a masked mask mode is set
        app.closeCurrentView(AppRunner.ExpectConfirmation.UNKNOWN);

        maskMode.apply(context);
        context.afterTestActions();
    }

    private void testExportOptimizedJPEG() {
        context.log(1, "testing export optimized jpeg");

        app.runMenuCommand("Export Optimized JPEG...");

        // wait for the preview to be calculated
        Utils.sleep(2, SECONDS);

        app.findDialogByTitle("Export Optimized JPEG").button("ok").click();
        app.acceptSaveDialog(config.getBaseDir(), "saved.jpg");

        context.afterTestActions();
    }

    private void testMagick() {
        context.log(1, "testing ImageMagick export-import");

        // test importing
        app.openFileWithDialog("Import...", config.getBaseDir(), "webp_image.webp");

        // test exporting
        app.runMenuCommand("Export...");
        String exportFileName = "saved_image.webp";
        app.acceptSaveDialog(config.getBaseDir(), exportFileName);
        app.findJOptionPane("WebP Export Options for " + exportFileName)
            .buttonWithText("Export").click();

        app.closeCurrentView(AppRunner.ExpectConfirmation.NO);
        context.afterTestActions();
    }

    private void testExportLayerAnimation() {
        context.log(1, "testing exporting layer animation");

        // precondition: the active image has only 1 layer
        EDT.assertNumLayersInActiveHolderIs(1);

        app.runMenuCommand("Export Layer Animation...");
        // error dialog, because there is only one layer
        app.findJOptionPane("Not Enough Layers")
            .okButton().click();

        app.duplicateLayer(ImageLayer.class);
        app.invert();

        // this time it should work
        app.runMenuCommand("Export Layer Animation...");
        app.findDialogByTitle("Export Animated GIF").button("ok").click();

        app.acceptSaveDialog(config.getBaseDir(), "layeranim.gif");

        context.afterTestActions();
    }

    private void testExportTweeningAnimation() {
        context.log(1, "testing export tweening animation");

        assertThat(EDT.getNumViews()).isGreaterThan(0);

        app.runMenuCommand("Export Tweening Animation...");
        var dialog = app.findDialogByTitle("Export Tweening Animation");
        String[] searchTexts = {"wav", "kalei"};
        dialog.textBox("searchTF").enterText(Rnd.chooseFrom(searchTexts));
        dialog.pressKey(VK_DOWN).releaseKey(VK_DOWN)
            .pressKey(VK_DOWN).releaseKey(VK_DOWN);
        dialog.button("ok").click(); // next
        dialog.requireVisible();

        dialog.button(withText("Randomize Settings")).click();
        dialog.button("ok").click(); // next
        dialog.requireVisible();

        findButtonByText(dialog, "Randomize Settings").click();
        dialog.button("ok").click(); // next
        dialog.requireVisible();

        if (config.isQuick()) {
            dialog.textBox("numSecondsTF").deleteText().enterText("1");
            dialog.textBox("fpsTF").deleteText().enterText("4");
            dialog.label("numFramesLabel").requireText("4");
        } else {
            dialog.textBox("numSecondsTF").deleteText().enterText("3");
            dialog.textBox("fpsTF").deleteText().enterText("5");
            dialog.label("numFramesLabel").requireText("15");
        }

        dialog.button("ok").click(); // render button
        dialog.requireVisible(); // still visible because of the validation error

        app.findJOptionPane("Folder Not Empty")
            .yesButton().click();
        dialog.requireNotVisible();

        app.waitForProgressMonitorEnd();

        context.afterTestActions();
    }

    private void testCloseAll() {
        context.log(1, "testing close all");

        assertThat(EDT.getNumViews()).isGreaterThan(0);

        app.closeAll();
        EDT.assertNumViewsIs(0);

        context.afterTestActions();
    }

    private void testShowMetadata() {
        context.log(1, "testing show metadata");

        app.runMenuCommand("Show Metadata...");
        String title = "Metadata for "
            + EDT.queryActiveComp(Composition::getName);
        var dialog = app.findDialogByTitle(title);

        dialog.button("expandAllButton").click();
        dialog.button("collapseAllButton").click();

        dialog.button("ok").click();
        dialog.requireNotVisible();

        context.afterTestActions();
    }

    private void testBatchResize() {
        context.log(1, "testing batch resize");
        maskMode.apply(context);

        EDT.run(() -> {
            RecentDirs.setLastOpen(config.getInputDir());
            RecentDirs.setLastSave(config.getBatchResizeOutputDir());
            FileFormat.setLastSaved(FileFormat.JPG);
        });

        app.runMenuCommand("Batch Resize...");
        var dialog = app.findDialogByTitle("Batch Resize");

        dialog.textBox("widthTF").setText("200");
        dialog.textBox("heightTF").setText("200");
        dialog.button("ok").click();
        dialog.requireNotVisible();

        Utils.sleep(5, SECONDS);

        config.checkBatchResizeOutputWasCreated();
        context.afterTestActions();
    }

    private void testBatchFilter() {
        context.log(1, "testing batch filter");

        RecentDirs.setLastOpen(config.getInputDir());
        RecentDirs.setLastSave(config.getBatchFilterOutputDir());

        assertThat(EDT.getNumViews()).isGreaterThan(0);
        maskMode.apply(context);

        app.runMenuCommand("Batch Filter...");
        var dialog = app.findDialogByTitle("Batch Filter");
        dialog.textBox("searchTF").enterText("wav");
        dialog.pressKey(VK_DOWN).releaseKey(VK_DOWN)
            .pressKey(VK_DOWN).releaseKey(VK_DOWN);
        dialog.button("ok").click(); // next

        findButtonByText(dialog, "Randomize Settings").click();
        dialog.button("ok").click(); // start processing
        dialog.requireNotVisible();

        app.waitForProgressMonitorEnd();

        context.afterTestActions();

        config.checkBatchFilterOutputWasCreated();
    }

    private void testExportLayerToPNG() {
        context.log(1, "testing export layer to png");

        RecentDirs.setLastSave(config.getBaseDir());

        app.duplicateLayer(ImageLayer.class);
        app.invert();
        maskMode.apply(context);

        app.runMenuCommand("Export Layers to PNG...");
        app.findDialogByTitle("Select Output Folder").button("ok").click();
        Utils.sleep(2, SECONDS);

        context.afterTestActions();
    }

    private void testScreenCapture() {
        context.log(1, "testing screen capture");

        View prevView = EDT.getActiveView();
        testScreenCapture(true);
        testScreenCapture(false);

        EDT.activate(prevView);

        context.afterTestActions();
    }

    private void testScreenCapture(boolean hidePixelitor) {
        app.runMenuCommand("Screen Capture...");
        var dialog = app.findDialogByTitle("Screen Capture");
        var cb = dialog.checkBox();
        if (hidePixelitor) {
            cb.check();
        } else {
            cb.uncheck();
        }
        dialog.button("ok").click();
        dialog.requireNotVisible();

        maskMode.apply(context);

        context.afterTestActions();
    }

    private void testReload() {
        context.log(1, "testing reload");

        app.reload();
        maskMode.apply(context);

        context.afterTestActions();
    }
}
