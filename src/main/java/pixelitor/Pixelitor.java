/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.gui.GUIMessageHandler;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.io.OpenSaveManager;
import pixelitor.layers.AddLayerMaskAction;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMaskAddType;
import pixelitor.layers.MaskViewMode;
import pixelitor.tools.Tool;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.plaf.MenuBarUI;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.io.File;

/**
 * The main class
 */
public class Pixelitor {
    /**
     * Utility class with static methods
     */
    private Pixelitor() {
    }

    public static void main(String[] args) {
        // allows to put the app into development mode by
        // adding -Dpixelitor.development=true to the command line
        if ("true".equals(System.getProperty("pixelitor.development"))) {
            Utils.checkThatAssertionsAreEnabled();
            Build.CURRENT = Build.DEVELOPMENT;
        }

        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pixelitor");

        if (JVM.isLinux) {
            // doesn't seem to pick up good defaults
            System.setProperty("awt.useSystemAAFontSettings", "lcd");
            System.setProperty("swing.aatext", "true");
        }

        ExceptionHandler.INSTANCE.register();
        EventQueue.invokeLater(() -> {
            try {
                createAndShowGUI(args);

                // this will run on a different thread, but call it
                // here because it is IO-intensive and it should not
                // slow down the loading of the GUI
                preloadFontNames();
            } catch (Exception e) {
                Dialogs.showExceptionDialog(e);
            }
        });

        // Force the initialization of FastMath look-up tables now
        // so that later no unexpected delays happen.
        // This is OK because static initializers are thread safe.
        FastMath.cos(0.1);
    }

    private static void preloadFontNames() {
        // The initial loading of font names can take some noticeable time.
        // Preload them to speed up the first start of the text creation dialog
        Runnable loadFontsTask = () -> {
            GraphicsEnvironment localGE = GraphicsEnvironment.getLocalGraphicsEnvironment();
            // the results are cached, no need to cached them here
            localGE.getAvailableFontFamilyNames();
        };
        new Thread(loadFontsTask).start();
    }

    private static void createAndShowGUI(String[] args) {
        assert SwingUtilities.isEventDispatchThread();

        setLookAndFeel();
        Messages.setMessageHandler(new GUIMessageHandler());

        PixelitorWindow pw = PixelitorWindow.getInstance();
        Dialogs.setMainWindowInitialized(true);

//        if (JVM.isMac) {
//            setupMacMenuBar(pw);
//        }

        if (args.length > 0) {
            openFilesGivenAsProgramArguments(args);
        } else {
            // ensure that the focus is not grabbed by a textfield
            // so that the keyboard shortcuts work properly
            FgBgColors.getGUI().requestFocus();
        }

        TipsOfTheDay.showTips(pw, false);

        afterStartTestActions(pw);
    }

    private static void setupMacMenuBar(PixelitorWindow pw) {
        JMenuBar menuBar = pw.getJMenuBar();
        try {
            // this property is respected only by the Aqua look-and-feel...
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            // ...so set the look-and-feel for the menu only to Aqua

            //noinspection ClassNewInstance
            menuBar.setUI((MenuBarUI) Class.forName("com.apple.laf.AquaMenuBarUI").newInstance());
        } catch (Exception e) {
            // ignore
        }
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(AppPreferences.getLookAndFeelClass());
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }
    }

    private static void openFilesGivenAsProgramArguments(String[] args) {
        for (String fileName : args) {
            File f = new File(fileName);
            if (f.exists()) {
                OpenSaveManager.openFile(f);
            } else {
                Messages.showError("File not found", "The file \"" + f.getAbsolutePath() + "\" does not exist");
            }
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

//        startFilter(new Marble());

//        Navigator.showInDialog(pw);

//        clickTool(Tools.SELECTION);
//        addMaskAndShowIt();

//        AddTextLayerAction.INSTANCE.actionPerformed(null);

//        AutoPaint.showDialog();

//        Tests3x3.addStandardImage(false);

//        ImageComponents.getActiveIC().setZoom(ZoomLevel.Z6400, true);

//        GlobalKeyboardWatch.registerDebugMouseWatching();

//        new TweenWizard().start(pw);

//        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.CTRL_MASK, KeyEvent.VK_T, 'T'));
    }

    private static void addMaskAndShowIt() {
        AddLayerMaskAction.INSTANCE.actionPerformed(null);
        ImageComponent ic = ImageComponents.getActiveIC();
        Layer layer = ic.getComp().getActiveLayer();
        MaskViewMode.SHOW_MASK.activate(ic, layer);
    }

    private static void clickTool(Tool tool) {
        tool.getButton().doClick();
    }

    private static void startFilter(Filter filter) {
        filter.execute(ImageComponents.getActiveImageLayerOrMaskOrNull());
    }

    private static void addNewImage() {
        NewImage.addNewImage(FillType.WHITE, 600, 400, "Test");
        ImageComponents.getActiveLayerOrNull()
                .addMask(LayerMaskAddType.PATTERN);
    }
}
