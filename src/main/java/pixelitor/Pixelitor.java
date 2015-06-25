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
import pixelitor.layers.Layers;
import pixelitor.tools.FgBgColorSelector;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Dialogs;
import pixelitor.utils.Utils;

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
            } catch (Exception e) {
                Dialogs.showExceptionDialog(e);
            }
        });

        // Force the initialization of FastMath look-up tables now
        // so that later no unexpected delays happen.
        // This is OK because static initializers are thread safe.
        FastMath.cos(0.1);
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
        Layers.init();

        PixelitorWindow pw = PixelitorWindow.getInstance();
        Dialogs.setMainWindowInitialized(true);

        if (args.length > 0) {
            openFilesGivenAsProgramArguments(args);
        } else {
            // ensure that the focus is not grabbed by a textfield
            // so that the keyboard shortcuts work properly
            FgBgColorSelector.INSTANCE.requestFocus();
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
                Dialogs.showErrorDialog("File not found", "The file \"" + f.getAbsolutePath() + "\" does not exist");
            }
        }
    }

    /**
     * A possibility for automatic debugging or testing
     */
    private static void afterStartTestActions(PixelitorWindow pw) {
        if(Build.CURRENT == Build.FINAL) {
            // in the final builds nothing should run
            return;
        }

//        // add layer mask
//        Layer layer = ImageComponents.getActiveLayer().get();
//        layer.addLayerMask(LayerMaskAddType.REVEAL_ALL);
//        // show and edit layer mask
//        ImageComponent ic = ImageComponents.getActiveImageComponent();
//        Layer activeLayer = ic.getComp().getActiveLayer();
//        ic.setShowLayerMask(true);
//        FgBgColorSelector.INSTANCE.setLayerMaskEditing(true);
//        activeLayer.setLayerMaskEditing(true);



//        NewImage.addNewImage(FillType.WHITE, 500, 500, "Test");
//        ImageComponents.getActiveImageComponent().setZoom(ZoomLevel.Z100, false);
//        new DrunkVision().actionPerformed(null);
//        Tools.GRADIENT.getButton().doClick();
//        new Voronoi().actionPerformed(null);

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

//        Tools.SHAPES.getButton().doClick();
//        Tools.SHAPES.setAction(ShapesAction.STROKE);
//        Tools.SHAPES.setStrokeType(StrokeType.WOBBLE);

//        ImageTests.createSplashImage();
//        AppLogic.getActiveComp().moveLayerSelectionDown();
    }
}
