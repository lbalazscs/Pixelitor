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

package pixelitor.utils.test;

import pixelitor.Build;
import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.Desktop;
import pixelitor.FgBgColors;
import pixelitor.GlobalKeyboardWatch;
import pixelitor.ImageComponents;
import pixelitor.ImageDisplay;
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
import pixelitor.filters.comp.EnlargeCanvas;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ParamSetState;
import pixelitor.filters.painters.TextSettings;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.layers.AddLayerMaskAction;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.menus.SelectionActions;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.CopySource;
import pixelitor.menus.edit.PasteAction;
import pixelitor.menus.edit.PasteDestination;
import pixelitor.menus.view.ShowHideAllAction;
import pixelitor.menus.view.ShowHideHistogramsAction;
import pixelitor.menus.view.ShowHideLayersAction;
import pixelitor.menus.view.ShowHideStatusBarAction;
import pixelitor.menus.view.ShowHideToolsAction;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.tools.Tool;
import pixelitor.tools.ToolSettingsPanelContainer;
import pixelitor.tools.Tools;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.GUIUtils;
import pixelitor.utils.MemoryInfo;
import pixelitor.utils.Messages;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.AWTException;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Random;

import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_180;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_270;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_90;

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

    private static final WeightedCaller weightedCaller = new WeightedCaller();
    private static final boolean PRINT_MEMORY = false;
    private static KeyStroke stopKeyStroke;

    private static int numPastedImages = 0;

    /**
     * Utility class with static methods
     */
    private RobotTest() {
    }

    public static void runRobot() {
        if (Build.CURRENT != Build.DEVELOPMENT) {
            Messages.showError("Error", "Build is not DEVELOPMENT");
            return;
        }
        Build.CURRENT.setRobotTest(true);

        numPastedImages = 0;

        // make sure it can be stopped by pressing the u key
        stopKeyStroke = KeyStroke.getKeyStroke('w');
        GlobalKeyboardWatch.addKeyboardShortCut(stopKeyStroke, "stoprobot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("RobotTest: \"" + stopKeyStroke + "\" Pressed");
                continueRunning = false;
            }
        });
        continueRunning = true;

        // and the j key exits the app
        KeyStroke exitKeyStroke = KeyStroke.getKeyStroke('j');
        GlobalKeyboardWatch.addKeyboardShortCut(exitKeyStroke, "exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("RobotTest: exiting app because \"" + exitKeyStroke + "\" was pressed");
                System.exit(1);
            }
        });

        System.out.println("RobotTest.runRobot CALLED at " + new Date() + ", press the '" + stopKeyStroke.getKeyChar() + "' key to stop it");

        Robot r = null;
        try {
            r = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        setupWeightedCaller(r);

        Point p = generateRandomPoint();
        r.mouseMove(p.x, p.y);

        logRobotEvent("initial splash");
        ImageTests.createSplashImage();
        randomCopy(); // ensure an image is on the clipboard

        SwingWorker<Void, Void> worker = createOneRoundSwingWorker(r, true);
        worker.execute();
    }

    private static SwingWorker<Void, Void> createOneRoundSwingWorker(Robot r, boolean forever) {
        return new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                int numTests = 8000;
                int onePercent = numTests / 100;

                int max = forever ? Integer.MAX_VALUE : numTests;

                for (int i = 0; i < max; i++) {
                    if ((i % onePercent) == 0) {
                        int percent = 100 * i / numTests;
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

                    Runnable runnable = () -> {
                        try {
                            weightedCaller.callRandomAction();
                            if (!ImageComponents.getActiveComp().isPresent()) {
                                throw new IllegalStateException("no active composition");
                            }
                            ConsistencyChecks.checkAll(true);
                        } catch (Exception e) {
                            Messages.showException(e);
                        }
                    };
                    try {
                        EventQueue.invokeAndWait(runnable);
                    } catch (InterruptedException | InvocationTargetException e) {
                        Messages.showException(e);
                    }
                }
                System.out.println("\nRobotTest.runRobot FINISHED at " + new Date());
                cleanUp();
                Toolkit.getDefaultToolkit().beep();

                return null;
            } // end of doInBackground()
        };
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
//        System.out.println(msg);
    }

    private static void randomMove(Robot r) {
        Point randomPoint = generateRandomPoint();
        int x = randomPoint.x;
        int y = randomPoint.y;
        logRobotEvent("random move to (" + x + ", " + y + ')');
        r.mouseMove(x, y);
    }

    private static void randomDrag(Robot r) {
        Point randomPoint = generateRandomPoint();
        int x = randomPoint.x;
        int y = randomPoint.y;
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
        FgBgColors.setRandomColors();
    }

    private static void randomOperation() {
        if (!ImageComponents.getActiveIC().activeIsImageLayer()) {
            return;
        }

        Filter op = FilterUtils.getRandomFilter(filter ->
                (!(filter instanceof Fade)) &&
                        (!(filter instanceof Brick)) &&
                        (!(filter instanceof RandomFilter)));

        String opName = op.getName();
        logRobotEvent("random operation: " + opName);

        long runCountBefore = Filter.runCount;

        op.randomizeSettings();

        ImageLayer layer = ImageComponents.getActiveImageLayerOrMask().get();
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

    private static void randomTweenOperation() {
        long runCountBefore = Filter.runCount;

        FilterWithParametrizedGUI filter = getRandomTweenFilter();
        String filterName = filter.getName();

        logRobotEvent("randomTweenOperation: " + filterName);

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

        logRobotEvent("random operation in randomTweenOperation: " + filterName);

//        System.out.println(String.format("RobotTest::randomTweenOperation: " +
//                "filterName = '%s' time=%.2f, interpolation = %s",
//                filterName, randomTime, randomInterpolation.toString()));

        // execute everything without showing a modal dialog

        ImageLayer imageLayer = ImageComponents.getActiveImageLayerOrMaskOrNull();
        if (imageLayer == null) {
            return;
        }

        imageLayer.tweenCalculatingStarted();

        PixelitorWindow busyCursorParent = PixelitorWindow.getInstance();

        try {
            Utils.executeFilterWithBusyCursor(filter, ChangeReason.OP_PREVIEW, busyCursorParent);
        } catch (Throwable e) {
            BufferedImage src = imageLayer.getFilterSourceImage();
            String msg = String.format(
                    "Exception in random tween: filter name = %s, srcWidth = %d, srcHeight = %d, isMaskEditing = %b, params = %s",
                    filterName, src.getWidth(), src.getHeight(), imageLayer.isMaskEditing(), paramSet.toString());
            throw new IllegalStateException(msg, e);
        }

        imageLayer.tweenCalculatingEnded();

        long runCountAfter = Filter.runCount;
        if (runCountAfter != (runCountBefore + 1)) {
            throw new IllegalStateException("runCountBefore = " + runCountBefore + ", runCountAfter = " + runCountAfter);
        }
    }

    private static void randomFitToScreen() {
        if (Math.random() > 0.1) {
            logRobotEvent("fitActiveToScreen");
            ImageComponents.fitActiveToScreen();
        } else {
            logRobotEvent("fitActiveToActualPixels");
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
            KeyEvent.VK_COMMA, KeyEvent.VK_HOME,
            KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_DOWN
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

    private static void randomZoom() {
        ImageDisplay ic = ImageComponents.getActiveIC();
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

    private static void randomZoomOut() {
        logRobotEvent("randomZoomOut");

        ImageDisplay ic = ImageComponents.getActiveIC();
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

            // for some reason, redo might not be available even if we are right
            // after an undo which didn't throw a CannotUndoException
            // This is not a problem in Pixelitor, because the RedoMenuItem
            // also checks History.canRedo() after an undo
            if (!History.canRedo()) {
                return;
            }

//            assert History.canRedo();
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
            Desktop.INSTANCE.tileWindows();
        } else {
            logRobotEvent("arrange windows - cascade");
            Desktop.INSTANCE.cascadeWindows();
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
        ImageComponents.getActiveComp().get().activeLayerToCanvasSize();
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
            logRobotEvent("trace with current eraser");
            SelectionActions.getTraceWithEraser().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomRotateFlip() {
        int r = rand.nextInt(5);
        Action action = null;

        switch (r) {
            case 0:
                logRobotEvent("rotate 90 CW");
                action = new Rotate(ANGLE_90);
                break;
            case 1:
                logRobotEvent("rotate 180");
                action = new Rotate(ANGLE_180);
                break;
            case 2:
                logRobotEvent("rotate 90 CCW");
                action = new Rotate(ANGLE_270);
                break;
            case 3:
                logRobotEvent("flip horizontal");
                action = new Flip(HORIZONTAL);
                break;
            case 4:
                logRobotEvent("flip vertical");
                action = new Flip(VERTICAL);
                break;
        }


        action.actionPerformed(new ActionEvent("", 0, ""));
    }

    private static void layerOrderChange() {
        Composition comp = ImageComponents.getActiveComp().get();
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
                comp.moveActiveLayerUp();
                break;
            case 5:
                logRobotEvent("layer order change: active down");
                comp.moveActiveLayerDown();
                break;
        }
    }

    private static void layerMerge() {
        Composition comp = ImageComponents.getActiveComp().get();

        if (rand.nextBoolean()) {
            logRobotEvent("layer merge down");
            comp.mergeDown(UpdateGUI.YES);
        } else {
            logRobotEvent("layer flatten image");
            comp.flattenImage(UpdateGUI.YES);
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
            new CopyAction(CopySource.LAYER).actionPerformed(new ActionEvent("", 0, ""));
        } else {
            logRobotEvent("random copy composite");
            new CopyAction(CopySource.COMPOSITE).actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomPaste() {
        if (numPastedImages > 3) {
            return;
        }
        int r = rand.nextInt(10);
        if (r == 0) {
            if (singleImageTest) {
                return;
            }
            logRobotEvent("random paste as new image");
            new PasteAction(PasteDestination.NEW_IMAGE).actionPerformed(new ActionEvent("", 0, ""));
            numPastedImages++;
        } else if (r == 1) {
            logRobotEvent("random paste as new layer");
            new PasteAction(PasteDestination.NEW_LAYER).actionPerformed(new ActionEvent("", 0, ""));
            numPastedImages++;
        }
    }

    private static void randomChangeLayerOpacityOrBlending() {
        Layer layer = ImageComponents.getActiveLayer().get();
        if (rand.nextBoolean()) {
            float opacity = layer.getOpacity();
            float f = rand.nextFloat();

            if (f > opacity) {
                // always increase
                logRobotEvent("random increase opacity");
                layer.setOpacity(f, UpdateGUI.YES, AddToHistory.YES, true);
            } else if (rand.nextFloat() > 0.75) { // sometimes decrease
                logRobotEvent("random decrease opacity");
                layer.setOpacity(f, UpdateGUI.YES, AddToHistory.YES, true);
            }
        } else {
            logRobotEvent("random change layer blending mode");
            BlendingMode[] blendingModes = BlendingMode.values();
            BlendingMode randomBlendingMode = blendingModes[rand.nextInt(blendingModes.length)];
            layer.setBlendingMode(randomBlendingMode, UpdateGUI.YES, AddToHistory.YES, true);
        }
    }

    private static void randomChangeLayerVisibility() {
        Layer layer = ImageComponents.getActiveLayer().get();
        boolean visible = layer.isVisible();
        if (rand.nextBoolean()) {
            if (!visible) {
                logRobotEvent("random showing layer");
                layer.setVisible(true, AddToHistory.YES);
            }
        } else {
            if (visible) {
                if (rand.nextFloat() > 0.8) { // sometimes hide
                    logRobotEvent("random hiding layer");
                    layer.setVisible(false, AddToHistory.YES);
                }
            }
        }
    }

    private static void randomTool() {
        Tool tool;
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

    private static void randomException() {
//        logRobotEvent("random exception");
//        throw new IllegalStateException("test");
    }


    private static void setupWeightedCaller(Robot r) {
        // random move
        weightedCaller.registerCallback(10, () -> randomMove(r));

        weightedCaller.registerCallback(70, () -> randomDrag(r));

        weightedCaller.registerCallback(5, () -> click(r));

        weightedCaller.registerCallback(1, () -> randomRightClick(r));

        weightedCaller.registerCallback(1, RobotTest::randomResize);

        weightedCaller.registerCallback(2, RobotTest::repeat);

        weightedCaller.registerCallback(1, RobotTest::randomUndoRedo);

        weightedCaller.registerCallback(1, RobotTest::randomCrop);

        weightedCaller.registerCallback(1, RobotTest::randomFade);

        weightedCaller.registerCallback(2, RobotTest::randomizeToolSettings);

        weightedCaller.registerCallback(1, RobotTest::arrangeWindows);

        weightedCaller.registerCallback(1, RobotTest::randomColors);

        weightedCaller.registerCallback(5, RobotTest::randomOperation);

        weightedCaller.registerCallback(25, RobotTest::randomTweenOperation);

        weightedCaller.registerCallback(10, RobotTest::randomFitToScreen);

        weightedCaller.registerCallback(3, () -> randomKey(r));

        weightedCaller.registerCallback(1, RobotTest::randomZoom);

        weightedCaller.registerCallback(1, RobotTest::randomZoomOut);

        weightedCaller.registerCallback(5, RobotTest::deselect);

        weightedCaller.registerCallback(1, RobotTest::layerToCanvasSize);

        weightedCaller.registerCallback(1, RobotTest::invertSelection);

        weightedCaller.registerCallback(1, RobotTest::traceWithCurrentBrush);

        weightedCaller.registerCallback(1, RobotTest::traceWithCurrentEraser);

        weightedCaller.registerCallback(1, RobotTest::randomRotateFlip);

        weightedCaller.registerCallback(1, RobotTest::layerOrderChange);

        if (Build.advancedLayersEnabled()) {
            weightedCaller.registerCallback(20, RobotTest::layerMerge);
        } else {
            weightedCaller.registerCallback(3, RobotTest::layerMerge);
        }

        weightedCaller.registerCallback(3, RobotTest::layerAddDelete);

        weightedCaller.registerCallback(1, RobotTest::randomHideShow);

        weightedCaller.registerCallback(1, RobotTest::randomCopy);

        weightedCaller.registerCallback(1, RobotTest::randomPaste);

        weightedCaller.registerCallback(1, RobotTest::randomChangeLayerOpacityOrBlending);

        weightedCaller.registerCallback(1, RobotTest::randomChangeLayerVisibility);

        weightedCaller.registerCallback(3, RobotTest::randomTool);

        weightedCaller.registerCallback(1, RobotTest::randomEnlargeLayer);

        if (Build.advancedLayersEnabled()) {
            weightedCaller.registerCallback(7, RobotTest::randomNewTextLayer);
            weightedCaller.registerCallback(7, RobotTest::randomTextLayerRasterize);
            weightedCaller.registerCallback(2, RobotTest::randomNewAdjustmentLayer);
            weightedCaller.registerCallback(7, RobotTest::randomSetLayerMaskEditMode);
            weightedCaller.registerCallback(20, RobotTest::randomLayerMaskAction);
        }

        // Not called now:
//        randomCloseImageWOSaving();
//        randomLoadImage();
//        randomSaveInAllFormats();
//        randomException();
    }

    private static void randomNewTextLayer() {
        Composition comp = ImageComponents.getActiveComp().get();
        TextLayer textLayer = new TextLayer(comp);
        textLayer.setSettings(TextSettings.createRandomSettings(rand));
        comp.addLayer(textLayer, AddToHistory.NO, true, false);
    }

    private static void randomTextLayerRasterize() {
        Layer layer = ImageComponents.getActiveLayer().get();
        if (layer instanceof TextLayer) {
            Composition comp = ImageComponents.getActiveComp().get();
            TextLayer.replaceWithRasterized(comp);
        }
    }

    private static void randomNewAdjustmentLayer() {
        Composition comp = ImageComponents.getActiveComp().get();
        AdjustmentLayer adjustmentLayer = new AdjustmentLayer(comp, "Invert", new Invert());
        comp.addLayer(adjustmentLayer, AddToHistory.YES, true, false);
    }

    private static void randomSetLayerMaskEditMode() {
        Layer layer = ImageComponents.getActiveLayer().get();
        if (!layer.hasMask()) {
            return;
        }
        PixelitorWindow pw = PixelitorWindow.getInstance();
        double d = rand.nextDouble();
        int keyCode;
        char keyChar;
        if (d < 0.33) {
            keyCode = KeyEvent.VK_1;
            keyChar = '1';
        } else if (d < 0.66) {
            keyCode = KeyEvent.VK_2;
            keyChar = '2';
        } else {
            keyCode = KeyEvent.VK_3;
            keyChar = '3';
        }

        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.CTRL_MASK, keyCode, keyChar));
    }

    // (add, delete, apply)
    private static void randomLayerMaskAction() {
        Layer layer = ImageComponents.getActiveLayer().get();
        if (!layer.hasMask()) {
            AddLayerMaskAction.INSTANCE.actionPerformed(null);
        } else {
            if (layer instanceof ImageLayer) {
                double d = rand.nextDouble();
                if (d > 0.5) {
                    ((ImageLayer) layer).applyLayerMask(AddToHistory.YES);
                } else {
                    layer.deleteMask(AddToHistory.YES, true);
                }
            } else {
                layer.deleteMask(AddToHistory.YES, true);
            }
        }
    }

    private static FilterWithParametrizedGUI getRandomTweenFilter() {
        FilterWithParametrizedGUI[] filters = FilterUtils.getAnimationFiltersSorted();
        return filters[(int) (Math.random() * filters.length)];
    }

    private static void randomEnlargeLayer() {
        int north = rand.nextInt(3);
        int east = rand.nextInt(3);
        int south = rand.nextInt(3);
        int west = rand.nextInt(3);
        Composition comp = ImageComponents.getActiveComp().get();
        new EnlargeCanvas(north, east, south, west).process(comp);
    }
}



