/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.GUIMessageHandler;
import pixelitor.gui.MouseZoomMethod;
import pixelitor.gui.PanMethod;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.Themes;
import pixelitor.io.IO;
import pixelitor.io.IOTasks;
import pixelitor.menus.file.ProjectIntegrationFilesMenu;
import pixelitor.tools.util.DragDisplay;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Language;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static pixelitor.utils.Threads.*;

/**
 * The main class
 */
public class Pixelitor {
    public static final String VERSION_NUMBER = "4.2.4";
    public static Locale SYS_LOCALE;

    private Pixelitor() {
        // should not be instantiated
    }

    public static void main(String[] args) {
        // the app can be put into development mode by
        // adding -Dpixelitor.development=true to the command line
        if ("true".equals(System.getProperty("pixelitor.development"))) {
            Utils.makeSureAssertionsAreEnabled();
            AppContext.CURRENT = AppContext.DEVELOPMENT_GUI;
        }

        // Force using English locale, because using the default system
        // settings leads to mixed-language problems (see issue #35),
        // but keep the system locale for number formatting
        SYS_LOCALE = Locale.getDefault();
        String sysLangCode = SYS_LOCALE.getLanguage();
        if (!Language.isCodeSupported(sysLangCode)) { // except if supported
            Locale.setDefault(Locale.US);
        }
        Language.load();

        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pixelitor");

//        System.setProperty("sun.java2d.uiScale", "1.5");

        if (JVM.isLinux) {
            // doesn't seem to pick up good defaults
            System.setProperty("awt.useSystemAAFontSettings", "lcd");
            System.setProperty("swing.aatext", "true");

            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("Pixelitor can't be used in headless mode");
                System.exit(1);
            }
        }

        ExceptionHandler.INSTANCE.addHandler((t, e) -> Messages.showException(e, t));

        EventQueue.invokeLater(() -> {
            try {
                createAndShowGUI(args);
            } catch (Exception e) {
                Dialogs.showExceptionDialog(e);
            }
        });

        DragDisplay.initializeFont();

        // Force the initialization of FastMath look-up tables now
        // on the main thread, so that later no unexpected delays happen.
        // This is OK because static initializers are thread safe.
        FastMath.cos(0.1);
    }

    private static void createAndShowGUI(String[] args) {
        assert calledOnEDT() : threadInfo();

        Messages.setMsgHandler(new GUIMessageHandler());

//        GlobalKeyboardWatch.showEventsSlowerThan(100, TimeUnit.MILLISECONDS);

        Themes.install(AppPreferences.getDefaultTheme(),
                false, true);

        var pw = PixelitorWindow.get();
        Dialogs.setMainWindowInitialized(true);

        // Just to make 100% sure that at the end of GUI
        // initialization the focus is not grabbed by
        // a textfield and the keyboard shortcuts work properly
        FgBgColors.getGUI().requestFocus();

        TipsOfTheDay.showTips(pw, false);

        MouseZoomMethod.load();
        PanMethod.load();

        // The IO-intensive pre-loading of fonts is scheduled
        // to run after all the files have been opened,
        // and on the same IO thread
        openCLFilesAsync(args)
                .exceptionally(throwable -> null) // recover
                .thenAcceptAsync(v -> afterStartTestActions(), onEDT)
                .thenRunAsync(Utils::preloadFontNames, onIOThread)
                .exceptionally(Messages::showExceptionOnEDT);
    }

    /**
     * Schedules the opening of the files given as command-line arguments
     */
    private static CompletableFuture<Void> openCLFilesAsync(String[] args) {
        List<CompletableFuture<Composition>> openedFiles = new ArrayList<>();

        // This marker is used to indicate is the upcoming files have to
        // be loaded and opened, or if '-PI' marker is used, to be loaded
        // in a separate menu.
        boolean projectIntegrationFlag = false;

        for (String fileName : args) {

            if (fileName.equals("-PI")) {
                projectIntegrationFlag = true;
                continue;

            }


            File f = new File(fileName);
            if (f.exists()) {

                if (projectIntegrationFlag)
//                    IO.openFileForPI(f, true);
                    ProjectIntegrationFilesMenu.INSTANCE.addFile(f);

                else
                    openedFiles.add(IO.openFileAsync(f, false));

            } else {
                Messages.showError("File not found",
                        format("The file \"%s\" does not exist", f.getAbsolutePath()));
            }
        }

        return Utils.allOf(openedFiles);
    }

    public static void exitApp(PixelitorWindow pw) {
        assert calledOnEDT() : threadInfo();

        var paths = IOTasks.getCurrentWritePaths();
        if (!paths.isEmpty()) {
            String msg = "<html>The writing of the following files is not finished yet. Exit anyway?<br><ul>";
            for (String path : paths) {
                msg += "<li>" + path;
            }

            String[] options = {"Wait 10 seconds", "Exit now"};
            boolean wait = Dialogs.showOKCancelWarningDialog(
                    msg, "Warning", options, 0);

            if (wait && IOTasks.isBusyWriting()) {
                // wait on another thread so that the status bar
                // can be updated while waiting
                new Thread(() -> {
                    Utils.sleep(10, TimeUnit.SECONDS);
                    EventQueue.invokeLater(() -> exitApp(pw));
                }).start();

                return;
            }
        }

        var unsavedComps = OpenImages.getUnsavedComps();
        if (!unsavedComps.isEmpty()) {
            String msg;
            if (unsavedComps.size() == 1) {
                msg = format("<html>There are unsaved changes in <b>%s</b>." +
                                "<br>Are you sure you want to exit?",
                        unsavedComps.get(0).getName());
            } else {
                msg = "<html>There are unsaved changes. Are you sure you want to exit?" +
                        "<br>Unsaved images:<ul>";
                for (Composition comp : unsavedComps) {
                    msg += "<li>" + comp.getName();
                }
            }

            if (Dialogs.showYesNoWarningDialog(pw, "Unsaved changes", msg)) {
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
    private static void afterStartTestActions() {
        if (AppContext.isFinal()) {
            // in the final builds nothing should run
            return;
        }

//        SplashImageCreator.saveManySplashImages();

//        Debug.keepSwitchingToolsRandomly();
//        Debug.startFilter(new Marble());

//        Navigator.showInDialog(pw);

//        Tools.PEN.activate();
//        Debug.addTestPath();
//        Tools.PEN.startRestrictedMode(PenToolMode.TRANSFORM, false);
//        Debug.addMaskAndShowIt();

//        Debug.showAddTextLayerDialog();

//        AutoPaint.showDialog();

//        Tests3x3.addStandardImage(false);

//        ImageComponents.getActiveCV().setZoom(ZoomLevel.Z6400, true);

//        GlobalKeyboardWatch.registerDebugMouseWatching(false);

//        new TweenWizard().start(pw);

//        Debug.dispatchKeyPress(pw, true, KeyEvent.VK_T, 'T');

//        Debug.addNewImageWithMask();
    }
}
