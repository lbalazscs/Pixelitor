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

package pixelitor;

import com.bric.util.JVM;
import net.jafama.FastMath;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.util.Filters;
import pixelitor.gui.*;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.Theme;
import pixelitor.gui.utils.Themes;
import pixelitor.io.FileIO;
import pixelitor.io.IOTasks;
import pixelitor.tools.util.MeasurementOverlay;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The main enty point for the app.
 */
public class Pixelitor {
    public static final String VERSION = "4.3.2";
    public static Locale SYS_LOCALE;

    private Pixelitor() {
        // should not be instantiated
    }

    public static void main(String[] args) {
        initExceptionHandling();
        initAppMode();
        configureLanguage();
        setupSystemProperties();
        launchGUI(args);
        mainThreadInit();
    }

    // register a global exception handler
    private static void initExceptionHandling() {
        ExceptionHandler.INSTANCE.addHandler((thread, exception) ->
            Messages.showException(exception, thread));
    }

    private static void initAppMode() {
        // the app can be put into development mode by
        // adding -Dpixelitor.development=true to the command line
        if ("true".equals(System.getProperty("pixelitor.development"))) {
            Utils.ensureAssertionsEnabled();
            AppMode.ACTIVE = AppMode.DEVELOPMENT_GUI;
        }
    }

    private static void configureLanguage() {
        // Store system locale for number formatting
        SYS_LOCALE = Locale.getDefault();

        if (!Language.isSupported(SYS_LOCALE.getLanguage())) {
            // if a language is not supported yet, then set English
            // in order to avoid mixed-language problems (see issue #35)
            Locale.setDefault(Locale.US);
        }

        Language.load(); // this also sets the locale for the language
    }

    private static void setupSystemProperties() {
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
    }

    private static void launchGUI(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                createAndShowGUI(args);
            } catch (Exception e) {
                Dialogs.showExceptionDialog(e);
            }
        });
    }

    private static void createAndShowGUI(String[] args) {
        assert calledOnEDT() : threadInfo();

        Messages.setHandler(new GUIMessageHandler());

//        GlobalKeyboardWatch.showEventsSlowerThan(100, TimeUnit.MILLISECONDS);

        Theme theme = AppPreferences.loadTheme();
        Themes.apply(theme, false, true);
        loadUIFonts(theme);

        PixelitorWindow mainWindow = PixelitorWindow.get();
        Dialogs.setMainWindowInitialized(true);

        // Make sure that at the end of GUI
        // initialization the focus isn't grabbed by
        // a textfield and the keyboard shortcuts work properly
        FgBgColors.getGUI().requestFocus();

        TipsOfTheDay.showTips(mainWindow, false);

        MouseZoomMethod.loadFromPreferences();
        PanMethod.loadFromPreferences();

        // The IO-intensive preloading of fonts is scheduled
        // to run after all the files have been opened,
        // and on the same IO thread
        openCommanLineFilesAsync(args)
            .exceptionally(throwable -> null) // recover
            .thenAcceptAsync(v -> doPostStartupActions(), onEDT)
            .thenRunAsync(Utils::preloadFontNames, onIOThread)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    // after launching the GUI on the EDT, do the less urgent
    // initializations concurrently on the main thread
    private static void mainThreadInit() {
        MeasurementOverlay.initializeFont();

        // Force the initialization of FastMath look-up tables now
        // on the main thread, so that later no unexpected delays happen.
        // This is OK because static initializers are thread safe.
        FastMath.cos(0.1);
    }

    private static void loadUIFonts(Theme theme) {
        int uiFontSize = AppPreferences.loadUIFontSize();
        String uiFontType = AppPreferences.loadUIFontType();

        if (uiFontSize == 0 || uiFontType.isEmpty()) {
            // no saved settings found, use default font settings
            return;
        }

        Font defaultFont = UIManager.getFont("defaultFont");
        if (defaultFont == null) {
            // if null, we don't know how to set the font
            return;
        }

        Font customFont = uiFontType.isEmpty()
            ? defaultFont.deriveFont((float) uiFontSize)
            : new Font(uiFontType, Font.PLAIN, uiFontSize);

        applyCustomFont(theme, customFont);
    }

    private static void applyCustomFont(Theme theme, Font customFont) {
        FontUIResource fontUIResource = new FontUIResource(customFont);
        UIManager.put("defaultFont", fontUIResource);

        if (theme.isNimbus()) {
            UIManager.getLookAndFeel().getDefaults().put("defaultFont", fontUIResource);
        }
    }

    /**
     * Schedules the opening of the files given as command-line arguments
     */
    private static CompletableFuture<Void> openCommanLineFilesAsync(String[] args) {
        List<CompletableFuture<Composition>> fileOpeningTasks = new ArrayList<>();

        for (String fileName : args) {
            File file = new File(fileName);
            if (file.exists()) {
                fileOpeningTasks.add(FileIO.openFileAsync(file, false));
            } else {
                Messages.showError("File Not Found",
                    format("<html>Unable to locate file: <b>%s</b>", file.getAbsolutePath()));
            }
        }

        return Utils.allOf(fileOpeningTasks);
    }

    public static void exitApp(PixelitorWindow mainWindow) {
        assert calledOnEDT() : threadInfo();

        if (hasOngoingWrite(mainWindow)) {
            return;
        }

        checkUnsavedChangesAndExit(mainWindow);
    }

    private static boolean hasOngoingWrite(PixelitorWindow mainWindow) {
        var writePaths = IOTasks.getActiveWritePaths();
        if (writePaths.isEmpty()) {
            return false;
        }

        boolean waitRequested = showOngoingWriteWarning(writePaths);
        if (waitRequested && IOTasks.hasActiveWrites()) {
            scheduleExitRetry(mainWindow);
            return true;
        }

        return false;
    }

    private static boolean showOngoingWriteWarning(Set<String> writePaths) {
        StringBuilder msg = new StringBuilder(
            "<html>The following files are still being written. Exit anyway?<br><ul>");
        for (String path : writePaths) {
            msg.append("<li>").append(path);
        }

        String warningMessage = msg.toString();
        String[] options = {"Wait 10 seconds", "Exit now"};
        return Dialogs.showOKCancelWarningDialog(
            warningMessage, "Warning", options, 0);
    }

    private static void scheduleExitRetry(PixelitorWindow mainWindow) {
        // wait on another thread so that the status bar
        // can be updated while waiting
        new Thread(() -> {
            Utils.sleep(10, TimeUnit.SECONDS);
            EventQueue.invokeLater(() -> exitApp(mainWindow));
        }).start();
    }

    private static void checkUnsavedChangesAndExit(PixelitorWindow mainWindow) {
        List<Composition> unsavedWork = Views.getUnsavedComps();
        if (unsavedWork.isEmpty()) {
            exit(mainWindow);
        } else {
            boolean proceedWithExit = Dialogs.showYesNoWarningDialog(mainWindow,
                "Unsaved Changes", createUnsavedChangesMsg(unsavedWork));
            if (proceedWithExit) {
                exit(mainWindow);
            }
        }
    }

    private static void exit(PixelitorWindow mainWindow) {
        mainWindow.setVisible(false);
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
     * Executes development mode actions after application startup.
     */
    private static void doPostStartupActions() {
        if (!AppMode.isDevelopment()) {
            return;
        }

        if (Views.getNumViews() > 0) {
            AutoZoom.FIT_SPACE_ACTION.actionPerformed(null);

            // automatically start a filter specified with -DautoFilter
            String autoFilterName = System.getProperty("autoFilter");
            if (autoFilterName != null) {
                Filters.startFilter(autoFilterName);
            }
        }

//        NewImage.addNewImage(FillType.BLACK, 700, 500, "Test");

//        GradientFillLayer.createNew();

//        Views.onActiveLayer(Layer::replaceWithSmartObject);

//        new Flip(HORIZONTAL).actionPerformed(null);
    }
}
