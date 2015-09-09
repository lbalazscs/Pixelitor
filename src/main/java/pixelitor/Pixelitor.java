/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import net.jafama.FastMath;
import pixelitor.io.OpenSaveManager;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Dialogs;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
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

        setupForMacintosh();

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

    private static void setupForMacintosh() {
        // this works
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pixelitor");

        // this is respected only by the native Aqua look-and-feel
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    private static void createAndShowGUI(String[] args) {
        assert SwingUtilities.isEventDispatchThread();

        setLookAndFeel();

        PixelitorWindow pw = PixelitorWindow.getInstance();
        Dialogs.setMainWindowInitialized(true);

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

//        NewImage.addNewImage(FillType.WHITE, 100, 100, "Test");


//        Tests3x3.addStandardImage(false);

//        ImageComponents.getActiveIC().setZoom(ZoomLevel.Z6400, true);
//
//        Tools.MOVE.getButton().doClick();
//
//        NewImage.addNewImage(FillType.WHITE, 600, 400, "Test");
//        ImageComponents.getActiveLayer().get()
//                .addMask(LayerMaskAddType.PATTERN);
//
//        TextLayer.createNew(pw);


//        ImageComponents.getActiveLayer().get()
//                .addMask(LayerMaskAddType.REVEAL_ALL_INT_ARGB);
//
//        NewImage.addNewImage(FillType.WHITE, 600, 400, "Test INT_RGB");
//        ImageComponents.getActiveLayer().get()
//                .addMask(LayerMaskAddType.REVEAL_ALL_INT_RGB);


//        // show and edit layer mask
//        ImageComponent ic = ImageComponents.getActiveIC();
//        Layer activeLayer = ic.getComp().getActiveLayer();
//        ic.setShowLayerMask(true);
//        FgBgColorSelector.INSTANCE.setMaskEditing(true);
//        activeLayer.setMaskEditing(true);

//        new DrunkVision().actionPerformed(null);

//        new ChannelToTransparency().actionPerformed(null);

//        Tools.SELECTION.getButton().doClick();

//        new BatchFilterWizard().start(pw);

//        GlobalKeyboardWatch.registerDebugMouseWatching();

//        new TweenWizard().start(pw);

//        CircleToSquare op = new CircleToSquare();
//        op.actionPerformed(null);

//        Composition comp = AppLogic.getActiveComp();
//        if(comp != null) {
//            String layerName = "text layer";
//            String layerText = "text layer text";
//            TextLayer textLayer = new TextLayer(comp, layerName, layerText);
//            comp.addLayer(textLayer, true, true, false);
//        }

//        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.CTRL_MASK, KeyEvent.VK_T, 'T'));

//        History.showHistory();

//        Tools.SHAPES.setAction(ShapesAction.STROKE);
//        Tools.SHAPES.setStrokeType(StrokeType.WOBBLE);

//        ImageTests.createSplashImage();
//        AppLogic.getActiveComp().moveLayerSelectionDown();
    }
}
