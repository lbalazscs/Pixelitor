/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.*;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.Theme;
import pixelitor.gui.utils.Themes;
import pixelitor.io.IO;
import pixelitor.io.IOTasks;
import pixelitor.tools.util.DragDisplay;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Language;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The main class
 */
public class Pixelitor {
    public static final String VERSION_NUMBER = "4.3.1";
    public static Locale SYS_LOCALE;

    private Pixelitor() {
        // should not be instantiated
    }

    public static void main(String[] args) {
        // the app can be put into development mode by
        // adding -Dpixelitor.development=true to the command line
        if ("true".equals(System.getProperty("pixelitor.development"))) {
            Utils.ensureAssertionsEnabled();
            GUIMode.CURRENT = GUIMode.DEVELOPMENT_GUI;
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

        Messages.setHandler(new GUIMessageHandler());

//        GlobalKeyboardWatch.showEventsSlowerThan(100, TimeUnit.MILLISECONDS);

        Theme theme = AppPreferences.loadTheme();
        Themes.install(theme, false, true);
        loadUIFonts(theme);

        var pw = PixelitorWindow.get();
        Dialogs.setMainWindowInitialized(true);

        // Make sure that at the end of GUI
        // initialization the focus isn't grabbed by
        // a textfield and the keyboard shortcuts work properly
        FgBgColors.getGUI().requestFocus();

        TipsOfTheDay.showTips(pw, false);

        MouseZoomMethod.load();
        PanMethod.load();

        // The IO-intensive preloading of fonts is scheduled
        // to run after all the files have been opened,
        // and on the same IO thread
        openCLFilesAsync(args)
            .exceptionally(throwable -> null) // recover
            .thenAcceptAsync(v -> afterStartTestActions(), onEDT)
            .thenRunAsync(Utils::preloadFontNames, onIOThread)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    private static void loadUIFonts(Theme theme) {
        int uiFontSize = AppPreferences.loadUIFontSize();
        String uiFontType = AppPreferences.loadUIFontType();
        if (uiFontSize == 0 || uiFontType.isEmpty()) {
            // no saved settings found
            return;
        }

        Font defaultFont = UIManager.getFont("defaultFont");
        if (defaultFont == null) {
            // if null, we don't know how to set the font
            return;
        }

        Font newFont;
        if (!uiFontType.isEmpty()) {
            newFont = new Font(uiFontType, Font.PLAIN, uiFontSize);
        } else {
            newFont = defaultFont.deriveFont((float) uiFontSize);
        }

        FontUIResource fontUIResource = new FontUIResource(newFont);
        UIManager.put("defaultFont", fontUIResource);

        if (theme.isNimbus()) {
            UIManager.getLookAndFeel().getDefaults().put("defaultFont", fontUIResource);
        }
    }

    /**
     * Schedules the opening of the files given as command-line arguments
     */
    private static CompletableFuture<Void> openCLFilesAsync(String[] args) {
        List<CompletableFuture<Composition>> openedFiles = new ArrayList<>();

        for (String fileName : args) {
            File file = new File(fileName);
            if (file.exists()) {
                openedFiles.add(IO.openFileAsync(file, false));
            } else {
                Messages.showError("File not found",
                    format("The file \"%s\" doesn't exist.", file.getAbsolutePath()));
            }
        }

        return Utils.allOf(openedFiles);
    }

    public static void warnAndExit(PixelitorWindow pw) {
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
                    EventQueue.invokeLater(() -> warnAndExit(pw));
                }).start();

                return;
            }
        }

        List<Composition> unsaved = Views.getUnsavedComps();
        if (!unsaved.isEmpty()) {
            boolean yesClicked = Dialogs.showYesNoWarningDialog(pw,
                "Unsaved Changes", createUnsavedChangesMsg(unsaved));
            if (yesClicked) {
                exit(pw);
            }
        } else {
            exit(pw);
        }
    }

    private static void exit(PixelitorWindow pw) {
        pw.setVisible(false);
        AppPreferences.savePreferences();
        System.exit(0);
    }

    private static String createUnsavedChangesMsg(List<Composition> unsavedComps) {
        String msg;
        if (unsavedComps.size() == 1) {
            msg = format("<html>There are unsaved changes in <b>%s</b>." +
                    "<br>Are you sure you want to exit?",
                unsavedComps.getFirst().getName());
        } else {
            msg = "<html>There are unsaved changes. Are you sure you want to exit?" +
                "<br>Unsaved images:<ul>";
            for (Composition comp : unsavedComps) {
                msg += "<li>" + comp.getName();
            }
        }
        return msg;
    }

    /**
     * A possibility for automatic debugging or testing
     */
    private static void afterStartTestActions() {
        if (GUIMode.isFinal()) {
            // in the final builds nothing should run
            return;
        }

        if (Views.getNumViews() > 0) {
            AutoZoom.FIT_SPACE_ACTION.actionPerformed(null);
        }

//        NewImage.addNewImage(FillType.WHITE, 700, 500, "Test");
//        Debug.startFilter(ConcentricShapes.NAME);

//        GradientFillLayer.createNew();

//        Views.onActiveLayer(Layer::replaceWithSmartObject);
//        Debug.startFilter(ColorBalance.NAME);

//        new Flip(HORIZONTAL).actionPerformed(null);
    }
}
