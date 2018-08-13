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

package pixelitor;

import com.bric.util.JVM;
import net.jafama.FastMath;
import pixelitor.colors.FgBgColors;
import pixelitor.colors.FillType;
import pixelitor.filters.Filter;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.io.IOThread;
import pixelitor.io.OpenSave;
import pixelitor.layers.AddLayerMaskAction;
import pixelitor.layers.AddTextLayerAction;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMaskAddType;
import pixelitor.layers.MaskViewMode;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * The main class
 */
public class Pixelitor {

    private Pixelitor() {
        // should not be instantiated
    }

    public static void main(String[] args) {
        // the app can be put into development mode by
        // adding -Dpixelitor.development=true to the command line
        if ("true".equals(System.getProperty("pixelitor.development"))) {
            Utils.makeSureAssertionsAreEnabled();
            Build.CURRENT = Build.DEVELOPMENT;
        }

        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pixelitor");

        if (JVM.isLinux) {
            // doesn't seem to pick up good defaults
            System.setProperty("awt.useSystemAAFontSettings", "lcd");
            System.setProperty("swing.aatext", "true");
        }

        ExceptionHandler.INSTANCE.initialize();
        EventQueue.invokeLater(() -> {
            try {
                createAndShowGUI(args);
            } catch (Exception e) {
                Dialogs.showExceptionDialog(e);
            }
        });

        // Force the initialization of FastMath look-up tables now
        // on the main thread, so that later no unexpected delays happen.
        // This is OK because static initializers are thread safe.
        FastMath.cos(0.1);
    }

    private static void createAndShowGUI(String[] args) {
        assert EventQueue.isDispatchThread() : "not EDT thread";

        setLookAndFeel();

        PixelitorWindow pw = PixelitorWindow.getInstance();
        Dialogs.setMainWindowInitialized(true);

        // Just to make 100% sure that at the end of GUI
        // initialization the focus is not grabbed by
        // a textfield and the keyboard shortcuts work properly
        FgBgColors.getGUI().requestFocus();

        TipsOfTheDay.showTips(pw, false);

        // The IO-intensive pre-loading of fonts is scheduled
        // to run after all the files have been opened,
        // and on the same IO thread
        openCLFilesAsync(args)
                .thenRunAsync(Utils::preloadFontNames,
                        IOThread.getExecutor())
                .exceptionally(Messages::showExceptionOnEDT);

        afterStartTestActions(pw);
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(
                    "javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }
    }

    /**
     * Schedules the opening of the files given as command-line arguments
     */
    private static CompletableFuture<Void> openCLFilesAsync(String[] args) {
        if (args.length == 0) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<?>> openedFiles = new ArrayList<>();

        for (String fileName : args) {
            File f = new File(fileName);
            if (f.exists()) {
                openedFiles.add(OpenSave.openFileAsync(f));
            } else {
                Messages.showError("File not found",
                        format("The file \"%s\" does not exist", f.getAbsolutePath()));
            }
        }

        return Utils.allOfList(openedFiles);
    }

    public static void exitApp(PixelitorWindow pw) {
        if (ImageComponents.thereAreUnsavedChanges()) {
            String msg = "There are unsaved changes. Are you sure you want to exit?";
            if (Dialogs.showYesNoWarningDialog(pw, "Confirmation", msg)) {
                pw.setVisible(false);
                AppPreferences.savePrefsAndExit();
            }
        } else {
            pw.setVisible(false);
            AppPreferences.savePrefsAndExit();
        }
    }

    /**
     * A possibility for automatic debugging or testing
     */
    @SuppressWarnings("UnusedParameters")
    private static void afterStartTestActions(PixelitorWindow pw) {
        if (Build.CURRENT == Build.FINAL) {
            // in the final builds nothing should run
            return;
        }

//        SplashImageCreator.saveManySplashImages();

//        addTestPath();

//        keepSwitchingToolsRandomly();
//        startFilter(new Marble());

//        Navigator.showInDialog(pw);

//        clickTool(Tools.PEN);
//        addMaskAndShowIt();

//        showAddTextLayerDialog();

//        AutoPaint.showDialog();

//        Tests3x3.addStandardImage(false);

//        ImageComponents.getActiveIC().setZoom(ZoomLevel.Z6400, true);

//        GlobalKeyboardWatch.registerDebugMouseWatching();

//        new TweenWizard().start(pw);

//        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.CTRL_MASK, KeyEvent.VK_T, 'T'));
    }

    private static void addTestPath() {
        Rectangle2D.Double shape = new Rectangle2D.Double(100, 100, 300, 100);

        Path path = Shapes.shapeToPath(shape, ImageComponents.getActiveIC());
        Tools.PEN.setPath(path);
        Tools.PEN.startEditing(false);
        Tools.PEN.getButton().doClick();
    }

    private static void showAddTextLayerDialog() {
        AddTextLayerAction.INSTANCE.actionPerformed(null);
    }

    private static void addMaskAndShowIt() {
        AddLayerMaskAction.INSTANCE.actionPerformed(null);
        ImageComponent ic = ImageComponents.getActiveIC();
        Layer layer = ic.getComp()
                .getActiveLayer();
        MaskViewMode.SHOW_MASK.activate(ic, layer, "after-start test");
    }

    private static void clickTool(Tool tool) {
        tool.getButton().doClick();
    }

    private static void startFilter(Filter filter) {
        filter.startOn(ImageComponents.getActiveDrawableOrThrow());
    }

    private static void addNewImage() {
        NewImage.addNewImage(FillType.WHITE, 600, 400, "Test");
        ImageComponents.getActiveLayerOrNull()
                .addMask(LayerMaskAddType.PATTERN);
    }

    private static void keepSwitchingToolsRandomly() {
        Runnable backgroundTask = () -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                Utils.sleep(1, TimeUnit.SECONDS);

                Runnable changeToolOnEDTTask = () -> clickTool(Tools.getRandomTool());
                GUIUtils.invokeAndWait(changeToolOnEDTTask);
            }
        };
        new Thread(backgroundTask).start();
    }
}
