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
package pixelitor.utils.test;

import pixelitor.Build;
import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.GlobalKeyboardWatch;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.filters.Brick;
import pixelitor.filters.Fade;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.Invert;
import pixelitor.filters.RandomFilter;
import pixelitor.filters.animation.Interpolation;
import pixelitor.filters.animation.TweenAnimation;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ParamSetState;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.Layers;
import pixelitor.layers.ShapeLayer;
import pixelitor.layers.TextLayer;
import pixelitor.menus.SelectionActions;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.CopyType;
import pixelitor.menus.edit.PasteAction;
import pixelitor.menus.view.ShowHideAllAction;
import pixelitor.menus.view.ShowHideHistogramsAction;
import pixelitor.menus.view.ShowHideLayersAction;
import pixelitor.menus.view.ShowHideStatusBarAction;
import pixelitor.menus.view.ShowHideToolsAction;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.tools.FgBgColorSelector;
import pixelitor.tools.Tool;
import pixelitor.tools.ToolSettingsPanelContainer;
import pixelitor.tools.Tools;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Dialogs;
import pixelitor.utils.GUIUtils;
import pixelitor.utils.MemoryInfo;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.AWTException;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Random;

/**
 * An automatic test using java.awt.Robot.
 * Can be dangerous because of the random native mouse events that can control other apps as well if they escape.
 */
public class RobotTest {
    final static Random rand = new Random();

    private static final Tool preferredTool = null; // set to null to select random tools

    private static final boolean singleImageTest = false;
    private static final boolean noHideSHow = true; // no view operations if set to true

    private static boolean continueRunning = true;

    private static WeightedCaller weightedCaller = new WeightedCaller();
    public static final boolean PRINT_MEMORY = false;

    /**
     * Utility class with static methods
     */
    private RobotTest() {
    }

    public static void runRobot() throws AWTException {
        if (Build.CURRENT != Build.DEVELOPMENT) {
            Dialogs.showErrorDialog("Error", "Build is not DEVELOPMENT");
            return;
        }
        Build.CURRENT.setRobotTest(true);

        // make sure it can be stopped by pressing the u key
        final KeyStroke stopKeyStroke = KeyStroke.getKeyStroke('u');
        GlobalKeyboardWatch.addKeyboardShortCut(stopKeyStroke, "stoprobot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("RobotTest: \"" + stopKeyStroke + "\" Pressed");
                continueRunning = false;
            }
        });
        continueRunning = true;

        // and the j key exits the app
        final KeyStroke exitKeyStroke = KeyStroke.getKeyStroke('j');
        GlobalKeyboardWatch.addKeyboardShortCut(exitKeyStroke, "exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("RobotTest: exiting app because \"" + exitKeyStroke + "\" was pressed");
                System.exit(1);
            }
        });

        System.out.println("RobotTest.runRobot CALLED at " + new Date() + ", press the 'u' key to stop it");

        final Robot r = new Robot();
        setupWeightedCaller(r);

        Point p = generateRandomPoint();
        r.mouseMove(p.x, p.y);

        if (ImageComponents.getActiveComp() == null) {
            logRobotEvent("initial splash");
            ImageTests.createSplashImage();
        }
        randomCopy(); // ensure an image is on the clipboard

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
//                int maxTests = 8000;
                int maxTests = 10000;
                int onePercent = maxTests / 100;

                for (int i = 0; i < maxTests; i++) {
                    if ((i % onePercent) == 0) {
                        int percent = 100 * i / maxTests;
                        System.out.print(percent + "% ");
                        if (PRINT_MEMORY) {
                            String memoryString = new MemoryInfo().toString();
                            System.out.println(memoryString);
                        } else {
                            if (((percent + 1) % 20) == 0) {
                                System.out.println();
                            }
                        }
                    }

                    if (!GUIUtils.appIsActive()) {
                        System.out.println("\nRobotTest app focus lost");
                        cleanUp();
                        break;
                    }

                    if (!continueRunning) {
                        System.out.println("\nStopped with \"" + stopKeyStroke + '"');
                        cleanUp();
                        break;
                    }

                    r.delay(100 + rand.nextInt(400));

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                weightedCaller.callRandomAction();
                                if (ImageComponents.getActiveComp() == null) {
                                    throw new IllegalStateException("no active composition");
                                }
                                ConsistencyChecks.checkAll();
                            } catch (Exception e) {
                                Dialogs.showExceptionDialog(e);
                            }
                        } // end of run
                    };
                    try {
                        EventQueue.invokeAndWait(runnable);
                    } catch (InterruptedException | InvocationTargetException e) {
                        Dialogs.showExceptionDialog(e);
                    }
                }
                System.out.println("\nRobotTest.runRobot FINISHED at " + new Date());
                cleanUp();
                Toolkit.getDefaultToolkit().beep();

                return null;
            } // end of doInBackground()
        };
        worker.execute();
    }

    private static Point generateRandomPoint() {
        Container contentPane = PixelitorWindow.getInstance().getContentPane();
        Dimension winDim = contentPane.getSize();
        Point locationOnScreen = contentPane.getLocationOnScreen();

        int safetyGapX = 5;
        int safetyGapY = 5;

        int minX = locationOnScreen.x + safetyGapX;
        int minY = locationOnScreen.y + safetyGapY;
        int maxX = locationOnScreen.x + winDim.width - 2 * minX;
        int maxY = locationOnScreen.y + winDim.height - 2 * minY;

        Point randomPoint = new Point(minX + rand.nextInt(maxX), minY + rand.nextInt(maxY));
        return randomPoint;
    }

    private static void cleanUp() {
        AppPreferences.WorkSpace.setDefault();
        Build.CURRENT.setRobotTest(false);
    }

    private static void randomResize() {
        logRobotEvent("random resize");
        OpTests.randomResize();
    }

    private static void logRobotEvent(String msg) {
        DebugEventQueue.post(new RobotEvent(msg));
    }

    private static void move(Robot r, int x, int y) {
        logRobotEvent("random move to (" + x + ", " + y + ')');
        r.mouseMove(x, y);
    }

    private static void drag(Robot r, int x, int y) {
        logRobotEvent("random \"" + Tools.getCurrentTool().getName() + " Tool\" drag to (" + x + ", " + y + ')');
        r.mousePress(InputEvent.BUTTON1_MASK);
        r.mouseMove(x, y);
        r.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static void click(Robot r) {
        logRobotEvent("random click");

        r.mousePress(InputEvent.BUTTON1_MASK);
        r.delay(50);
        r.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static void randomRightClick(Robot r) {
        logRobotEvent("random right click");

        r.mousePress(InputEvent.BUTTON3_MASK);
        r.delay(50);
        r.mouseRelease(InputEvent.BUTTON3_MASK);
    }

    private static void randomColors() {
        logRobotEvent("random colors");
        FgBgColorSelector.setRandomColors();
    }

    private static void randomOperation(Robot r) {
        if (!Layers.activeIsImageLayer()) {
            return;
        }

        Filter op = getRandomFilter();
        if (op instanceof Fade) {
            return;
        }
        if (op instanceof Brick) {
            return;
        }
        if (op instanceof RandomFilter) {
            return;
        }

        long runCountBefore = Filter.runCount;

        String opName = op.getName();
//        System.out.println(String.format("RobotTest::randomOperation: opName = '%s'", opName));

        logRobotEvent("random operation: " + opName);

        op.randomizeSettings();

        ImageLayer layer = ImageComponents.getActiveComp().getActiveImageLayer();
        if (op instanceof FilterWithGUI) {
            FilterWithGUI fg = (FilterWithGUI) op;

            layer.startPreviewing();

            try {
                op.randomizeSettings();
                op.execute(ChangeReason.OP_PREVIEW);
            } catch (Exception e) {
                BufferedImage src = layer.getFilterSourceImage();
                if (op instanceof FilterWithParametrizedGUI) {
                    ParamSet paramSet = ((FilterWithParametrizedGUI) op).getParamSet();
                    System.out.println(String.format(
                            "RobotTest::randomOperation: name = %s, width = %d, height = %d, params = %s",
                            opName, src.getWidth(), src.getHeight(), paramSet.toString()));
                } else {
                    System.out.println(String.format(
                            "RobotTest::randomOperation: name = %s, width = %d, height = %d",
                            opName, src.getWidth(), src.getHeight()));
                }
                throw e;
            }

            if (Math.random() > 0.3) {
                layer.okPressedInDialog(opName);
            } else {
                layer.cancelPressedInDialog();
            }

            fg.endDialogSession();
        } else {
            BufferedImage src = layer.getFilterSourceImage();
            try {
                op.execute(ChangeReason.OP_WITHOUT_DIALOG);
            } catch (Exception e) {
                System.out.println(String.format(
                        "RobotTest::randomOperation: name = %s, width = %d, height = %d",
                        opName, src.getWidth(), src.getHeight()));
                throw e;
            }
        }
        long runCountAfter = Filter.runCount;
        if (runCountAfter != (runCountBefore + 1)) {
            throw new IllegalStateException("runCountBefore = " + runCountBefore + ", runCountAfter = " + runCountAfter);
        }
    }

    private static void randomTweenOperation(Robot r) {
        long runCountBefore = Filter.runCount;

        FilterWithParametrizedGUI filter = getRandomTweenFilter();
        String filterName = filter.getName();
//        System.out.println(String.format("RobotTest::randomTweenOperation: filterName = '%s'", filterName));

        TweenAnimation animation = new TweenAnimation();
        animation.setFilter(filter);

        Interpolation[] interpolations = Interpolation.values();
        Interpolation randomInterpolation = interpolations[(int) (Math.random() * interpolations.length)];
        animation.setInterpolation(randomInterpolation);

        ParamSet paramSet = filter.getParamSet();
        paramSet.randomize();
        animation.copyInitialStateFromCurrent();

        paramSet.randomize();
        animation.copyFinalStateFromCurrent();

        double randomTime = Math.random();
        ParamSetState intermediateState = animation.tween(randomTime);
        paramSet.setState(intermediateState);

        logRobotEvent("random operation: " + filterName);

//        System.out.println(String.format("RobotTest::randomTweenOperation: " +
//                "filterName = '%s' time=%.2f, interpolation = %s",
//                filterName, randomTime, randomInterpolation.toString()));

        // execute everything without showing a modal dialog
        ImageLayer layer = ImageComponents.getActiveComp().getActiveImageLayer();
        layer.tweenCalculatingStarted();

        PixelitorWindow busyCursorParent = PixelitorWindow.getInstance();

        try {
            Utils.executeFilterWithBusyCursor(filter, ChangeReason.OP_PREVIEW, busyCursorParent);
        } catch (Exception e) {
            BufferedImage src = layer.getFilterSourceImage();
            System.out.println(String.format(
                    "RobotTest::randomTweenOperation: name = %s, width = %d, height = %d, params = %s",
                    filterName, src.getWidth(), src.getHeight(), paramSet.toString()));
            throw e;
        }

        filter.endDialogSession();
        layer.tweenCalculatingEnded();

        long runCountAfter = Filter.runCount;
        if (runCountAfter != (runCountBefore + 1)) {
            throw new IllegalStateException("runCountBefore = " + runCountBefore + ", runCountAfter = " + runCountAfter);
        }
    }

    private static void randomFitToScreen(Robot r) {
        if (Math.random() > 0.1) {
            ImageComponents.fitActiveToScreen();
        } else {
            ImageComponents.fitActiveToActualPixels();
        }
    }

    private static final int[] keyEvents = {KeyEvent.VK_1, KeyEvent.VK_A,
            KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE,
            KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
            KeyEvent.VK_M, KeyEvent.VK_C, KeyEvent.VK_Z,
            KeyEvent.VK_G, KeyEvent.VK_B,
            KeyEvent.VK_E, KeyEvent.VK_I,
            KeyEvent.VK_S, KeyEvent.VK_Q,
            KeyEvent.VK_R, KeyEvent.VK_D,
            KeyEvent.VK_X, KeyEvent.VK_H,
            KeyEvent.VK_P, KeyEvent.VK_B,
            KeyEvent.VK_ALT, KeyEvent.VK_TAB,
            KeyEvent.VK_COMMA, KeyEvent.VK_HOME, KeyEvent.VK_RIGHT
//            , KeyEvent.VK_V, // do not trigger move tool
    };

    private static void randomKey(Robot r) {
        int randomIndex = rand.nextInt(keyEvents.length);
        int keyEvent = keyEvents[randomIndex];

        logRobotEvent("random key keyEvent = " + keyEvent);

        r.keyPress(keyEvent);
        r.delay(50);
        r.keyRelease(keyEvent);
    }

    private static void randomZoom(Random rand) {
        ImageComponent ic = ImageComponents.getActiveImageComponent();
        if (ic != null) {
            ZoomLevel randomZoomLevel = null;
            double percentValue = 0;
            while (percentValue < 49) {
                randomZoomLevel = ZoomLevel.getRandomZoomLevel(rand);
                percentValue = randomZoomLevel.getPercentValue();
            }

            logRobotEvent("random zoom zoomLevel = " + randomZoomLevel);
            ic.setZoom(randomZoomLevel, false);
        }
    }

    private static void randomZoomOut(Random rand) {
        ImageComponent ic = ImageComponents.getActiveImageComponent();
        if (ic != null) {
            ZoomLevel previous = ic.getZoomLevel().zoomOut();
            ic.setZoom(previous, false);
        }
    }

    private static void repeat() {
        logRobotEvent("repeat");
        PixelitorWindow pw = PixelitorWindow.getInstance();
        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.CTRL_MASK, KeyEvent.VK_F, 'F'));
    }

    private static void randomUndoRedo() {
        if (History.canUndo()) {
            logRobotEvent("randomUndoRedo");
            History.undo();

            if (!History.canRedo()) {
                PixelitorEdit lastEdit = History.getLastEdit();
                System.out.println("RobotTest::randomUndoRedo: lastEdit = " + (lastEdit == null ? "null" : (lastEdit.toString() + ", class = " + lastEdit.getClass().getName())));
            }

            assert History.canRedo();
            if (rand.nextInt(10) > 3) {
                History.redo();
            }
        }
    }

    private static void randomCrop() {
        boolean enabled = SelectionActions.areEnabled();
        if (enabled) {
            logRobotEvent("randomCrop");
            SelectionActions.getCropAction().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomFade() {
        boolean b = History.canFade();
        if (!b) {
            return;
        }

        logRobotEvent("randomFade");

        Fade fade = new Fade();
        fade.setOpacity(rand.nextInt(100));

        fade.execute(ChangeReason.OP_WITHOUT_DIALOG);
    }

    private static void randomizeToolSettings() {
        logRobotEvent("randomize tool settings");
        ToolSettingsPanelContainer.INSTANCE.randomizeToolSettings();
    }

    private static void arrangeWindows() {
        double r = Math.random();
        if (r < 0.8) {
            logRobotEvent("arrange windows - tile");
            PixelitorWindow.getInstance().tileWindows();
        } else {
            logRobotEvent("arrange windows - cascade");
            PixelitorWindow.getInstance().cascadeWindows();
        }
    }

    private static void deselect() {
        if (SelectionActions.areEnabled()) {
            logRobotEvent("deselect");
            SelectionActions.getDeselectAction().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void layerToCanvasSize() {
        logRobotEvent("layer to canvas size");
        ImageComponents.getActiveComp().layerToCanvasSize();
    }

    private static void invertSelection() {
        if (SelectionActions.areEnabled()) {
            logRobotEvent("invert selection");
            SelectionActions.getInvertSelectionAction().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void traceWithCurrentBrush() {
        if (SelectionActions.areEnabled()) {
            logRobotEvent("trace with current brush");
            SelectionActions.getTraceWithBrush().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void traceWithCurrentEraser() {
        if (SelectionActions.areEnabled()) {
            logRobotEvent("trace with current easer");
            SelectionActions.getTraceWithEraser().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomRotateFlip() {
        int r = rand.nextInt(5);
        Action action = null;

        switch (r) {
            case 0:
                logRobotEvent("roate 90 CW");
                action = new Rotate(90, "Rotate 90\u00B0 CW");
                break;
            case 1:
                logRobotEvent("roate 180");
                action = new Rotate(180, "Rotate 180\u00B0");
                break;
            case 2:
                logRobotEvent("roate 90 CCW");
                action = new Rotate(270, "Rotate 90\u00B0 CCW");
                break;
            case 3:
                logRobotEvent("flip horizontal");
                action = Flip.createFlipOp(Flip.Direction.HORIZONTAL);
                break;
            case 4:
                logRobotEvent("flip vertical");
                action = Flip.createFlipOp(Flip.Direction.VERTICAL);
                break;
        }


        action.actionPerformed(new ActionEvent("", 0, ""));
    }

    private static void layerOrderChange() {
        Composition comp = ImageComponents.getActiveComp();
        int r = rand.nextInt(6);
        switch (r) {
            case 0:
                logRobotEvent("layer order change: active to top");
                comp.moveActiveLayerToTop();
                break;
            case 1:
                logRobotEvent("layer order change: active to bottom");
                comp.moveActiveLayerToBottom();
                break;
            case 2:
                logRobotEvent("layer order change: selection up");
                comp.moveLayerSelectionUp();
                break;
            case 3:
                logRobotEvent("layer order change: selection down");
                comp.moveLayerSelectionDown();
                break;
            case 4:
                logRobotEvent("layer order change: active up");
                comp.moveActiveLayer(true);
                break;
            case 5:
                logRobotEvent("layer order change: active down");
                comp.moveActiveLayer(false);
                break;
        }
    }

    private static void layerMerge() {
        Composition comp = ImageComponents.getActiveComp();

        if (rand.nextBoolean()) {
            logRobotEvent("layer merge down");
            comp.mergeDown();
        } else {
            logRobotEvent("layer flatten image");
            comp.flattenImage();
        }
    }

    private static void layerAddDelete() {
        if (rand.nextBoolean()) {
            if (AddNewLayerAction.INSTANCE.isEnabled()) {
                logRobotEvent("add new layer");
                AddNewLayerAction.INSTANCE.actionPerformed(new ActionEvent("", 0, ""));
            }
        } else {
            if (DeleteActiveLayerAction.INSTANCE.isEnabled()) {
                logRobotEvent("delete active layer");
                DeleteActiveLayerAction.INSTANCE.actionPerformed(new ActionEvent("", 0, ""));
            }
        }
    }

    private static void randomHideShow() {
        if (noHideSHow) {
            return;
        }

        int r = rand.nextInt(5);
        if (r == 0) {
            logRobotEvent("random show-hide histograms");
            new ShowHideHistogramsAction().actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 1) {
            logRobotEvent("random show-hide layers");
            new ShowHideLayersAction().actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 2) {
            logRobotEvent("random show-hide tools");
            new ShowHideToolsAction().actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 4) {
            logRobotEvent("random show-hide statusbar");
            new ShowHideStatusBarAction().actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 5) {
            logRobotEvent("random show-hide all");
            ShowHideAllAction.INSTANCE.actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomCopy() {
        if (rand.nextBoolean()) {
            logRobotEvent("random copy layer");
            new CopyAction(CopyType.COPY_LAYER).actionPerformed(new ActionEvent("", 0, ""));
        } else {
            logRobotEvent("random copy composite");
            new CopyAction(CopyType.COPY_COMPOSITE).actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomPaste() {
        int r = rand.nextInt(10);
        if (r == 0) {
            if (singleImageTest) {
                return;
            }
            logRobotEvent("random paste as new image");
            new PasteAction(false).actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 1) {
            logRobotEvent("random paste as new layer");
            new PasteAction(true).actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomChangeLayerOpacityOrBlending() {
        Layer layer = ImageComponents.getActiveLayer();
        if (rand.nextBoolean()) {
            float opacity = layer.getOpacity();
            float f = rand.nextFloat();

            if (f > opacity) {
                // always increase
                logRobotEvent("random increase opacity");
                layer.setOpacity(f, true, true, true);
            } else if (rand.nextFloat() > 0.75) { // sometimes decrease
                logRobotEvent("random decrease opacity");
                layer.setOpacity(f, true, true, true);
            }
        } else {
            logRobotEvent("random change layer blending mode");
            BlendingMode[] blendingModes = BlendingMode.values();
            BlendingMode randomBlendingMode = blendingModes[rand.nextInt(blendingModes.length)];
            layer.setBlendingMode(randomBlendingMode, true, true, true);
        }
    }

    private static void randomChangeLayerVisibility() {
        Layer layer = ImageComponents.getActiveLayer();
        boolean visible = layer.isVisible();
        if (rand.nextBoolean()) {
            if (!visible) {
                logRobotEvent("random showing layer");
                layer.setVisible(true, true);
            }
        } else {
            if (visible) {
                if (rand.nextFloat() > 0.8) { // sometimes hide
                    logRobotEvent("random hiding layer");
                    layer.setVisible(false, true);
                }
            }
        }
    }

    private static void randomTool() {
        Tool tool = null;
        if (preferredTool != null) {
            tool = preferredTool;
        } else {
            tool = Tools.getRandomTool(rand);

            // The move tool can cause out of memory errors, so don't test it
            if (tool == Tools.MOVE) {
                return;
            }
        }
        if (Tools.getCurrentTool() != tool) {
            tool.getButton().doClick();
        }
    }

    private static void randomCloseImageWOSaving() {
        if (singleImageTest) {
            return;
        }
//        logRobotEvent("random close image without saving");
//        ImageComponent ic = AppLogic.getActiveImageComponent();
//        if(ic != null) {
//            ic.close();
//        }
    }

    private static void randomSaveInAllFormats() {
//        logRobotEvent("random save in all formats");
    }

    private static void randomLoadImage() {
        if (singleImageTest) {
            return;
        }
//        logRobotEvent("random load image");
    }

    private static void randomException() {
//        logRobotEvent("random exception");
//        throw new IllegalStateException("test");
    }

    private static void randomSpecialLayer() {
        int r = rand.nextInt(3);
        Composition comp = ImageComponents.getActiveComp();
        Layer newLayer = null;

        if (r == 0) {
            logRobotEvent("random text layer");
            String layerName = "text layer";
            String layerText = "text layer text";
            newLayer = new TextLayer(comp, layerName, layerText);
        } else if (r == 1) {
            logRobotEvent("random shape layer");
            String layerName = "shape layer";
            Shape shape = new RoundRectangle2D.Float(10, 10, 100, 100, 20, 20);
            newLayer = new ShapeLayer(comp, layerName, shape);
        } else if (r == 2) {
            logRobotEvent("random adjustment layer");
            newLayer = new AdjustmentLayer(comp, "invert adjustment", new Invert());
        }

        comp.addLayer(newLayer, true, true, false);
    }

    private static void randomLayerMask() {
        logRobotEvent("random layer mask");

        ImageComponents.getActiveComp().getActiveLayer().addTestLayerMask();
    }

    private static void setupWeightedCaller(final Robot r) {
        // random move
        weightedCaller.registerCallback(10, new Runnable() {
            @Override
            public void run() {
                Point randomPoint = generateRandomPoint();
                final int randomX = randomPoint.x;
                final int randomY = randomPoint.y;
                move(r, randomX, randomY);
            }
        });

        // random drag
        weightedCaller.registerCallback(70, new Runnable() {
            @Override
            public void run() {
                Point randomPoint = generateRandomPoint();
                final int randomX = randomPoint.x;
                final int randomY = randomPoint.y;
                drag(r, randomX, randomY);
            }
        });

        weightedCaller.registerCallback(5, new Runnable() {
            @Override
            public void run() {
                click(r);
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomRightClick(r);
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomResize();
            }
        });

        weightedCaller.registerCallback(2, new Runnable() {
            @Override
            public void run() {
                repeat();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomUndoRedo();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomCrop();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomFade();
            }
        });

        weightedCaller.registerCallback(2, new Runnable() {
            @Override
            public void run() {
                randomizeToolSettings();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                arrangeWindows();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomColors();
            }
        });

        weightedCaller.registerCallback(5, new Runnable() {
            @Override
            public void run() {
                randomOperation(r);
            }
        });

        weightedCaller.registerCallback(25, new Runnable() {
            @Override
            public void run() {
                randomTweenOperation(r);
            }
        });

        weightedCaller.registerCallback(10, new Runnable() {
            @Override
            public void run() {
                randomFitToScreen(r);
            }
        });

        weightedCaller.registerCallback(3, new Runnable() {
            @Override
            public void run() {
                randomKey(r);
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomZoom(rand);
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomZoomOut(rand);
            }
        });

        weightedCaller.registerCallback(5, new Runnable() {
            @Override
            public void run() {
                deselect();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                layerToCanvasSize();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                invertSelection();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                traceWithCurrentBrush();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                traceWithCurrentEraser();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomRotateFlip();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                layerOrderChange();
            }
        });

        weightedCaller.registerCallback(3, new Runnable() {
            @Override
            public void run() {
                layerMerge();
            }
        });

        weightedCaller.registerCallback(3, new Runnable() {
            @Override
            public void run() {
                layerAddDelete();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomHideShow();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomCopy();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomPaste();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomChangeLayerOpacityOrBlending();
            }
        });

        weightedCaller.registerCallback(1, new Runnable() {
            @Override
            public void run() {
                randomChangeLayerVisibility();
            }
        });

        weightedCaller.registerCallback(3, new Runnable() {
            @Override
            public void run() {
                randomTool();
            }
        });

        // Not called now:
//        randomCloseImageWOSaving();
//        randomLoadImage();
//        randomSaveInAllFormats();
//        randomException();
//        randomSpecialLayer();
//        randomLayerMask();
    }

    private static FilterWithParametrizedGUI getRandomTweenFilter() {
//        return canny;
        FilterWithParametrizedGUI[] filters = FilterUtils.getAnimationFiltersSorted();
        return filters[(int) (Math.random() * filters.length)];
    }

    private static Filter getRandomFilter() {
        return FilterUtils.getRandomFilter();
    }

//    private static Canny canny = new Canny();
}



