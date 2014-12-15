/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor;

import pixelitor.filters.animation.TweenWizard;
import pixelitor.io.OpenSaveManager;
import pixelitor.layers.Layers;
import pixelitor.tools.FgBgColorSelector;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.EventQueue;
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

    public static void main(final String[] args) {
        setupForMacintosh();

        ExceptionHandler.INSTANCE.register();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    createAndShowGUI(args);
                } catch (Exception e) {
                    Dialogs.showExceptionDialog(e);
                }
            }
        });
    }

    private static void setupForMacintosh() {
        // this works
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pixelitor");

        // it is respected only by the native Aqua look-and-feel
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    /**
     * This is called on the EDT
     */
    private static void createAndShowGUI(String[] args) {
//        if(JVM.isMac) {
//            MacScreenMenu.saveTrickyUISettings();
//        }

        try {
            UIManager.setLookAndFeel(AppPreferences.getLookAndFeelClass());
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }

//        if(JVM.isMac) {
//            MacScreenMenu.restoreTrickyUISettings();
//        }

        Layers.init();

        PixelitorWindow pw = PixelitorWindow.getInstance();
        Dialogs.setMainWindowInitialized(true);


        if (args.length > 0) {
            // open the files given on the command line
            for (String fileName : args) {
                File f = new File(fileName);
                if (f.exists()) {
                    OpenSaveManager.openFile(f);
                } else {
                    Dialogs.showErrorDialog("File not found", "The file \"" + f.getAbsolutePath() + "\" does not exist");
                }
            }
        } else {
            // ensure that the focus is not grabbed by a textfield so that the keyboard shortcuts work properly
            FgBgColorSelector.INSTANCE.requestFocus();
        }

        TipsOfTheDay.showTips(pw, false);

        afterStartTestActions(pw);
    }

    /**
     * A possibility for automatic debugging or testing
     */
    private static void afterStartTestActions(PixelitorWindow pw) {
        if(Build.CURRENT == Build.FINAL) {
            // in the final builds nothing should run
            return;
        }

        new TweenWizard().show(pw);

//        Transition2D op = new Transition2D();
//        op.actionPerformed(null);

//        Composition comp = AppLogic.getActiveComp();
//        if(comp != null) {
//            String layerName = "text layer";
//            String layerText = "text layer text";
//            TextLayer textLayer = new TextLayer(comp, layerName, layerText);
//            comp.addLayer(textLayer, true, true, false);
//        }

//        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.CTRL_MASK, KeyEvent.VK_T, 'T'));
//        NewImage.addNewImage(NewImage.BgFill.WHITE, 10, 10, "Test");
//        NewImage.addNewImage(NewImage.BgFill.WHITE, 600, 400, "Test 2");

//        History.showHistory();
//        Tools.SELECTION.getButton().doClick();

//          Tools.GRADIENT.getButton().doClick();

//        Tools.CROP.getButton().doClick();

//        Tools.PAINT_BUCKET.getButton().doClick();
//        AppLogic.getActiveImageComponent().setZoom(ZoomLevel.Z6400);

//        JHEmboss op = new JHEmboss();
//        RandomSpheres op = new RandomSpheres();
//        JHCells op = new JHCells();
//        op.actionPerformed(null);

//        LittlePlanet op = new LittlePlanet();
//        LittlePlanet op = new LittlePlanet();
//        GlassTile op = new GlassTile();
//        CircleToSquare op = new CircleToSquare();
//        op.actionPerformed(null);

//        Tools.SHAPES.getButton().doClick();
//        Tools.SHAPES.setAction(ShapesAction.STROKE);
//        Tools.SHAPES.setStrokeType(StrokeType.WOBBLE);

//        ImageTests.createSplashImage();
//        AppLogic.getActiveComp().moveLayerSelectionDown();

//        Starburst starburst = new Starburst();
//        starburst.actionPerformed(null);

    }
}
