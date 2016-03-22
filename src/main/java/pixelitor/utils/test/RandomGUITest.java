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

package pixelitor.utils.test;

import com.bric.util.JVM;
import pixelitor.Build;
import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.Fade;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterAction;
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
import pixelitor.gui.Desktop;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.layers.AddLayerMaskAction;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.layers.TextLayer;
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
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tool;
import pixelitor.tools.ToolSettingsPanelContainer;
import pixelitor.tools.Tools;
import pixelitor.utils.AppPreferences;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_180;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_270;
import static pixelitor.filters.comp.Rotate.SpecialAngle.ANGLE_90;

/**
 * An automatic test using java.awt.Robot.
 * Can be dangerous because of the random native mouse events that can control other apps as well if they escape.
 */
public class RandomGUITest {
    final static Random rand = new Random();

    private static final Tool preferredTool = null; // set to null to select random tools

    private static final boolean singleImageTest = false;
    private static final boolean noHideShow = true; // no view operations if set to true
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private static boolean continueRunning = true;

    private static final WeightedCaller weightedCaller = new WeightedCaller();
    private static final boolean PRINT_MEMORY = false;
    private static KeyStroke stopKeyStroke;

    private static int numPastedImages = 0;

    private static boolean running = false;

    /**
     * Utility class with static methods
     */
    private RandomGUITest() {
    }

    public static void runTest() {
        if (Build.CURRENT != Build.DEVELOPMENT) {
            Messages.showError("Error", "Build is not DEVELOPMENT");
            return;
        }
        running = true;

        PixelitorWindow.getInstance().setAlwaysOnTop(true);

        new PixelitorEventListener().register();

        numPastedImages = 0;

        // make sure it can be stopped by pressing the u key
        stopKeyStroke = KeyStroke.getKeyStroke('w');
        GlobalKeyboardWatch.addKeyboardShortCut(stopKeyStroke, "stopTest", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("RandomGUITest: \"" + stopKeyStroke + "\" pressed");
                continueRunning = false;
            }
        });
        continueRunning = true;

        // and the j key exits the app
        KeyStroke exitKeyStroke = KeyStroke.getKeyStroke('j');
        GlobalKeyboardWatch.addKeyboardShortCut(exitKeyStroke, "exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("RandomGUITest: exiting app because '" + exitKeyStroke.getKeyChar() + "' was pressed");
                System.exit(1);
            }
        });

        System.out.printf("RandomGUITest.runTest CALLED at %s, the '%s' key stops, the '%s' key exits.%n",
                DATE_FORMAT.format(new Date()),
                stopKeyStroke.getKeyChar(), exitKeyStroke.getKeyChar());

        Robot r = null;
        try {
            r = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        setupWeightedCaller(r);

        Point p = generateRandomPoint();
        r.mouseMove(p.x, p.y);

        log("initial splash");
        SplashImageCreator.createSplashImage();
        randomCopy(); // ensure an image is on the clipboard

        SwingWorker<Void, Void> worker = createOneRoundSwingWorker(r, true);
        worker.execute();
    }

    public static boolean isRunning() {
        return running;
    }

    public static void stop() {
        continueRunning = false;
        PixelitorWindow.getInstance().setAlwaysOnTop(false);
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
                        if (JVM.isWindows) { // might be some "upgrade to windows 10" window
                            tryToActivateWindow(3);
                        }

                        if (!GUIUtils.appIsActive()) {
                            System.out.println("\nRandomGUITest app focus lost.");
                            cleanUp();
                            break;
                        }
                    }

                    if (!continueRunning) {
                        System.out.println("\nRandomGUITest stopped.");
                        cleanUp();
                        break;
                    }

                    r.delay(100 + rand.nextInt(400));

                    Runnable runnable = () -> {
                        try {
                            weightedCaller.callRandomAction();
                            Composition comp = ImageComponents.getActiveComp().orElseThrow(() ->
                                    new IllegalStateException("no active composition"));
                            ConsistencyChecks.checkAll(
                                    comp,
                                    true);
                        } catch (Throwable e) {
                            Messages.showException(e);
                        }
                    };
                    try {
                        EventQueue.invokeAndWait(runnable);
                    } catch (InterruptedException | InvocationTargetException e) {
                        Messages.showException(e);
                    }
                }
                System.out.println("\nRandomGUITest.runTest FINISHED at " + new Date());
                cleanUp();
                Toolkit.getDefaultToolkit().beep();

                return null;
            } // end of doInBackground()
        };
    }

    private static void tryToActivateWindow(int attempts) {
        if (attempts <= 0) {
            return;
        }

        System.out.println("RandomGUITest: trying to regain window focus");
        try {
            SwingUtilities.invokeAndWait(() -> {
                PixelitorWindow pw = PixelitorWindow.getInstance();
                pw.toFront();
                pw.repaint();
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }

        if (GUIUtils.appIsActive()) { // success
            return;
        } else {
            Utils.sleep(1, TimeUnit.SECONDS);
            tryToActivateWindow(attempts - 1);
        }
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
        AppPreferences.WorkSpace.setDefault(PixelitorWindow.getInstance());
        running = false;
    }

    private static void randomResize() {
        log("resize");
        FilterTests.randomResize();
    }

    private static void log(String msg) {
        Events.postRandomTestEvent(msg);
    }

    private static void randomMove(Robot r) {
        Point randomPoint = generateRandomPoint();
        int x = randomPoint.x;
        int y = randomPoint.y;
        log("move to (" + x + ", " + y + ')');
        r.mouseMove(x, y);
    }

    private static void randomDrag(Robot r) {
        Point randomPoint = generateRandomPoint();
        int x = randomPoint.x;
        int y = randomPoint.y;
        Tool tool = Tools.getCurrentTool();
        String stateInfo = tool.getStateInfo();
        if (stateInfo == null) {
            stateInfo = "";
        } else {
            stateInfo = "(" + stateInfo + ")";
        }

        log(tool.getName() + " Tool " + stateInfo + " drag to (" + x + ", " + y + ')');

        r.mousePress(InputEvent.BUTTON1_MASK);
        r.mouseMove(x, y);
        r.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static void click(Robot r) {
        log("click");

        r.mousePress(InputEvent.BUTTON1_MASK);
        r.delay(50);
        r.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static void randomRightClick(Robot r) {
        log("right click");

        r.mousePress(InputEvent.BUTTON3_MASK);
        r.delay(50);
        r.mouseRelease(InputEvent.BUTTON3_MASK);
    }

    private static void randomColors() {
        log("randomize colors");
        FgBgColors.randomizeColors();
    }

    private static void randomFilter() {
        if (!ImageComponents.getActiveIC().activeIsImageLayerOrMask()) {
            return;
        }

        Filter f = FilterUtils.getRandomFilter(filter ->
                (!(filter instanceof Fade)) &&
                        (!(filter instanceof RandomFilter)));

        String filterName = f.getName();
        log("filter: " + filterName);

        long runCountBefore = Filter.runCount;

        f.randomizeSettings();

        ImageLayer layer = ImageComponents.getActiveImageLayerOrMaskOrNull();
        if (f instanceof FilterWithGUI) {
            layer.startPreviewing();

            try {
                f.randomizeSettings();
                f.execute(layer, ChangeReason.OP_PREVIEW);
            } catch (Throwable e) {
                BufferedImage src = layer.getFilterSourceImage();
                if (f instanceof FilterWithParametrizedGUI) {
                    ParamSet paramSet = ((FilterWithParametrizedGUI) f).getParamSet();
                    System.out.println(String.format(
                            "RandomGUITest::randomFilter: name = %s, width = %d, height = %d, params = %s",
                            filterName, src.getWidth(), src.getHeight(), paramSet.toString()));
                } else {
                    System.out.println(String.format(
                            "RandomGUITest::randomFilter: name = %s, width = %d, height = %d",
                            filterName, src.getWidth(), src.getHeight()));
                }
                throw e;
            }

            if (Math.random() > 0.3) {
                layer.okPressedInDialog(filterName);
            } else {
                layer.cancelPressedInDialog();
            }
        } else {
            BufferedImage src = layer.getFilterSourceImage();
            try {
                f.execute(layer, ChangeReason.OP_WITHOUT_DIALOG);
            } catch (Throwable e) {
                System.out.println(String.format(
                        "RandomGUITest::randomFilter: name = %s, width = %d, height = %d",
                        filterName, src.getWidth(), src.getHeight()));
                throw e;
            }
        }
        long runCountAfter = Filter.runCount;
        if (runCountAfter != (runCountBefore + 1)) {
            throw new IllegalStateException("runCountBefore = " + runCountBefore + ", runCountAfter = " + runCountAfter);
        }
    }

    private static void randomTween() {
        ImageLayer imageLayer = ImageComponents.getActiveImageLayerOrMaskOrNull();
        if (imageLayer == null) {
            return;
        }

        long runCountBefore = Filter.runCount;

        FilterWithParametrizedGUI filter = getRandomTweenFilter();
        String filterName = filter.getName();

        log("tween: " + filterName);

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

        // execute everything without showing a modal dialog
        imageLayer.tweenCalculatingStarted();

        PixelitorWindow busyCursorParent = PixelitorWindow.getInstance();

        try {
            filter.executeFilterWithBusyCursor(imageLayer, ChangeReason.OP_PREVIEW, busyCursorParent);
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
            log("fitActiveToScreen");
            ImageComponents.fitActiveToScreen();
        } else {
            log("fitActiveToActualPixels");
            ImageComponents.fitActiveToActualPixels();
        }
    }

    private static final int[] keyEvents = {KeyEvent.VK_1, KeyEvent.VK_A,
            KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE,
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

        log("key = " + keyEvent);
        pressKey(r, keyEvent);
    }

    private static void pressKey(Robot r, int keyEvent) {
        r.keyPress(keyEvent);
        r.delay(50);
        r.keyRelease(keyEvent);
    }

    private static void pressCtrlKey(Robot r, int keyEvent) {
        r.keyPress(KeyEvent.VK_CONTROL);
        r.delay(50);
        r.keyPress(keyEvent);
        r.delay(50);
        r.keyRelease(keyEvent);
        r.delay(50);
        r.keyRelease(KeyEvent.VK_CONTROL);
    }

    private static void randomZoom() {
        ImageComponent ic = ImageComponents.getActiveIC();
        if (ic != null) {
            ZoomLevel randomZoomLevel = null;

            double percentValue = 0;
            while (percentValue < 49) {
                randomZoomLevel = ZoomLevel.getRandomZoomLevel(rand);
                percentValue = randomZoomLevel.getPercentValue();
            }

            log("zoom zoomLevel = " + randomZoomLevel);

            if (rand.nextBoolean()) {
                ic.setZoom(randomZoomLevel, false, null);
            } else {
                Point mousePos = getRandomPointOnIC(ic);
                ic.setZoom(randomZoomLevel, false, mousePos);
            }
        }
    }

    private static Point getRandomPointOnIC(ImageComponent ic) {
        int randX = rand.nextInt(ic.getWidth());
        int randY = rand.nextInt(ic.getWidth());
        return new Point(randX, randY);
    }

    private static void randomZoomOut() {
        log("zoomOut");

        ImageComponent ic = ImageComponents.getActiveIC();
        if (ic != null) {
            ZoomLevel newZoom = ic.getZoomLevel().zoomOut();
            if (rand.nextBoolean()) {
                ic.setZoom(newZoom, false, null);
            } else {
                Point mousePos = getRandomPointOnIC(ic);
                ic.setZoom(newZoom, false, mousePos);
            }
        }
    }

    private static void repeat() {
        log("repeat");

        PixelitorWindow pw = PixelitorWindow.getInstance();
        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.CTRL_MASK, KeyEvent.VK_F, 'F'));
    }

    private static void randomUndoRedo() {
        if (History.canUndo()) {
            log("undo");

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
                log("redo");
                History.redo();
            }
        }
    }

    private static void randomCrop() {
        boolean enabled = SelectionActions.areEnabled();
        if (enabled) {
            log("crop");
            SelectionActions.getCropAction().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomFade() {
        if (!History.canFade()) {
            return;
        }
        int opacity = rand.nextInt(100);

        log("fade, opacity = " + opacity + " %");

        Fade fade = new Fade();
        fade.setOpacity(opacity);

        ImageLayer layer = ImageComponents.getActiveImageLayerOrMaskOrNull();
        fade.execute(layer, ChangeReason.OP_WITHOUT_DIALOG);
    }

    private static void randomizeToolSettings() {
        log("randomize tool settings");
        ToolSettingsPanelContainer.INSTANCE.randomizeToolSettings();
    }

    private static void arrangeWindows() {
        double r = Math.random();
        if (r < 0.8) {
            log("arrange windows - tile");
            Desktop.INSTANCE.tileWindows();
        } else {
            log("arrange windows - cascade");
            Desktop.INSTANCE.cascadeWindows();
        }
    }

    private static void deselect() {
        if (SelectionActions.areEnabled()) {
            log("deselect");
            SelectionActions.getDeselectAction().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void showHideSelection(Robot robot) {
        log("showHideSelection");
        pressCtrlKey(robot, KeyEvent.VK_H);
    }

    private static void layerToCanvasSize() {
        log("layer to canvas size");
        ImageComponents.getActiveCompOrNull().activeLayerToCanvasSize();
    }

    private static void invertSelection() {
        if (SelectionActions.areEnabled()) {
            log("invert selection");
            SelectionActions.getInvertSelectionAction().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void traceWithCurrentBrush() {
        if (SelectionActions.areEnabled()) {
            log("trace with current brush");
            SelectionActions.getTraceWithBrush().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void traceWithCurrentEraser() {
        if (SelectionActions.areEnabled()) {
            log("trace with current eraser");
            SelectionActions.getTraceWithEraser().actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomRotateFlip() {
        int r = rand.nextInt(5);
        Action action = null;

        switch (r) {
            case 0:
                log("rotate 90 CW");
                action = new Rotate(ANGLE_90);
                break;
            case 1:
                log("rotate 180");
                action = new Rotate(ANGLE_180);
                break;
            case 2:
                log("rotate 90 CCW");
                action = new Rotate(ANGLE_270);
                break;
            case 3:
                log("flip horizontal");
                action = new Flip(HORIZONTAL);
                break;
            case 4:
                log("flip vertical");
                action = new Flip(VERTICAL);
                break;
        }


        action.actionPerformed(new ActionEvent("", 0, ""));
    }

    private static void layerOrderChange() {
        Composition comp = ImageComponents.getActiveCompOrNull();
        int r = rand.nextInt(6);
        switch (r) {
            case 0:
                log("layer order change: active to top");
                comp.moveActiveLayerToTop();
                break;
            case 1:
                log("layer order change: active to bottom");
                comp.moveActiveLayerToBottom();
                break;
            case 2:
                log("layer order change: selection up");
                comp.moveLayerSelectionUp();
                break;
            case 3:
                log("layer order change: selection down");
                comp.moveLayerSelectionDown();
                break;
            case 4:
                log("layer order change: active up");
                comp.moveActiveLayerUp();
                break;
            case 5:
                log("layer order change: active down");
                comp.moveActiveLayerDown();
                break;
        }
    }

    private static void layerMerge() {
        Composition comp = ImageComponents.getActiveCompOrNull();

        if (rand.nextBoolean()) {
            log("layer merge down");
            comp.mergeDown(UpdateGUI.YES);
        } else {
            log("layer flatten image");
            comp.flattenImage(UpdateGUI.YES);
        }
    }

    private static void layerAddDelete() {
        if (rand.nextBoolean()) {
            if (AddNewLayerAction.INSTANCE.isEnabled()) {
                log("add new layer");
                AddNewLayerAction.INSTANCE.actionPerformed(new ActionEvent("", 0, ""));
            }
        } else {
            if (DeleteActiveLayerAction.INSTANCE.isEnabled()) {
                log("delete active layer");
                DeleteActiveLayerAction.INSTANCE.actionPerformed(new ActionEvent("", 0, ""));
            }
        }
    }

    private static void randomHideShow() {
        if (noHideShow) {
            return;
        }

        int r = rand.nextInt(5);
        if (r == 0) {
            log("show-hide histograms");
            new ShowHideHistogramsAction().actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 1) {
            log("show-hide layers");
            new ShowHideLayersAction().actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 2) {
            log("show-hide tools");
            new ShowHideToolsAction().actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 4) {
            log("show-hide status bar");
            new ShowHideStatusBarAction().actionPerformed(new ActionEvent("", 0, ""));
        } else if (r == 5) {
            log("show-hide all");
            ShowHideAllAction.INSTANCE.actionPerformed(new ActionEvent("", 0, ""));
        }
    }

    private static void randomCopy() {
        if (rand.nextBoolean()) {
            log("copy layer");
            new CopyAction(CopySource.LAYER).actionPerformed(new ActionEvent("", 0, ""));
        } else {
            log("copy composite");
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
            log("paste as new image");
            new PasteAction(PasteDestination.NEW_IMAGE).actionPerformed(new ActionEvent("", 0, ""));
            numPastedImages++;
        } else if (r == 1) {
            log("paste as new layer");
            new PasteAction(PasteDestination.NEW_LAYER).actionPerformed(new ActionEvent("", 0, ""));
            numPastedImages++;
        }
    }

    private static void randomChangeLayerOpacityOrBlending() {
        Layer layer = ImageComponents.getActiveLayerOrNull();
        if (rand.nextBoolean()) {
            float opacity = layer.getOpacity();
            float f = rand.nextFloat();

            if (f > opacity) {
                // always increase
                log("increase opacity");
                layer.setOpacity(f, UpdateGUI.YES, AddToHistory.YES, true);
            } else if (rand.nextFloat() > 0.75) { // sometimes decrease
                log("decrease opacity");
                layer.setOpacity(f, UpdateGUI.YES, AddToHistory.YES, true);
            }
        } else {
            log("change layer blending mode");
            BlendingMode[] blendingModes = BlendingMode.values();
            BlendingMode randomBlendingMode = blendingModes[rand.nextInt(blendingModes.length)];
            layer.setBlendingMode(randomBlendingMode, UpdateGUI.YES, AddToHistory.YES, true);
        }
    }

    private static void randomChangeLayerVisibility() {
        Layer layer = ImageComponents.getActiveLayerOrNull();
        boolean visible = layer.isVisible();
        if (rand.nextBoolean()) {
            if (!visible) {
                log("show layer");
                layer.setVisible(true, AddToHistory.YES);
            }
        } else {
            if (visible) {
                if (rand.nextFloat() > 0.8) { // sometimes hide
                    log("hide layer");
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
        Tool currentTool = Tools.getCurrentTool();
        if (currentTool != tool) {
            log("tool click on " + currentTool);
            tool.getButton().doClick();
        }
    }

    private static void randomException() {
//        logRobotEvent("random exception");
//        throw new IllegalStateException("test");
    }

    private static void randomNewTextLayer() {
        log("new text layer");
        Composition comp = ImageComponents.getActiveCompOrNull();
        TextLayer textLayer = new TextLayer(comp);
        TextSettings randomSettings = TextSettings.createRandomSettings(rand);
        textLayer.setSettings(randomSettings);
        comp.addLayer(textLayer, AddToHistory.YES, "New Random Text Layer", true, false);
        textLayer.setName(randomSettings.getText(), AddToHistory.YES);
    }

    private static void randomTextLayerRasterize() {
        Layer layer = ImageComponents.getActiveLayerOrNull();
        if (layer instanceof TextLayer) {
            log("text layer rasterize");

            ((TextLayer) layer).replaceWithRasterized();
        }
    }

    private static void randomNewAdjustmentLayer() {
        log("new adj layer");
        Composition comp = ImageComponents.getActiveCompOrNull();
        AdjustmentLayer adjustmentLayer = new AdjustmentLayer(comp, "Invert", new Invert());
        comp.addLayer(adjustmentLayer, AddToHistory.YES, "New Random Adj Layer", true, false);
    }

    private static void randomSetLayerMaskEditMode() {
        Layer layer = ImageComponents.getActiveLayerOrNull();
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
            log("Ctrl-1");
        } else if (d < 0.66) {
            keyCode = KeyEvent.VK_2;
            keyChar = '2';
            log("Ctrl-2");
        } else {
            keyCode = KeyEvent.VK_3;
            keyChar = '3';
            log("Ctrl-3");
        }

        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.CTRL_MASK, keyCode, keyChar));
    }

    // (add, delete, apply, link)
    private static void randomLayerMaskAction() {
        Layer layer = ImageComponents.getActiveLayerOrNull();
        if (!layer.hasMask()) {
            log("add layer mask");
            AddLayerMaskAction.INSTANCE.actionPerformed(null);
        } else {
            if (rand.nextFloat() < 0.2 && layer instanceof ContentLayer) {
                LayerMask mask = layer.getMask();
                if (mask.isLinked()) {
                    log("unlink layer mask");
                    mask.setLinked(false, AddToHistory.YES);
                } else {
                    log("re-link layer mask");
                    mask.setLinked(true, AddToHistory.YES);
                }
            } else if (layer instanceof ImageLayer) {
                double d = rand.nextDouble();
                if (d > 0.5) {
                    log("apply layer mask");
                    ((ImageLayer) layer).applyLayerMask(AddToHistory.YES);
                } else {
                    log("delete layer mask");
                    layer.deleteMask(AddToHistory.YES);
                }
            } else {
                log("delete layer mask");
                layer.deleteMask(AddToHistory.YES);
            }
        }
    }

    private static FilterWithParametrizedGUI getRandomTweenFilter() {
        FilterAction[] filterActions = FilterUtils.getAnimationFiltersSorted();
        FilterAction filterAction = filterActions[(int) (Math.random() * filterActions.length)];
        return (FilterWithParametrizedGUI) filterAction.getFilter();
    }

    private static void randomEnlargeCanvas() {
        int north = rand.nextInt(3);
        int east = rand.nextInt(3);
        int south = rand.nextInt(3);
        int west = rand.nextInt(3);
        log(String.format("enlargeCanvas north = %d, east = %d, south = %d, west = %d",
                north, east, south, west));
        Composition comp = ImageComponents.getActiveCompOrNull();
        new EnlargeCanvas(north, east, south, west).process(comp);
    }

    private static void reload(Robot r) {
        if (rand.nextFloat() < 0.1) {
            Composition comp = ImageComponents.getActiveCompOrNull();
            if (comp.getFile() != null) {
                log("f5 reload");
                pressKey(r, KeyEvent.VK_F5);
            }
        }
    }

    private static void setupWeightedCaller(Robot r) {
        // random move
        weightedCaller.registerCallback(10, () -> randomMove(r));

        weightedCaller.registerCallback(70, () -> randomDrag(r));

        weightedCaller.registerCallback(5, () -> click(r));

        weightedCaller.registerCallback(1, () -> randomRightClick(r));

        weightedCaller.registerCallback(3, RandomGUITest::randomResize);

        weightedCaller.registerCallback(2, RandomGUITest::repeat);

        weightedCaller.registerCallback(1, RandomGUITest::randomUndoRedo);

        weightedCaller.registerCallback(1, RandomGUITest::randomCrop);

        weightedCaller.registerCallback(1, RandomGUITest::randomFade);

        weightedCaller.registerCallback(2, RandomGUITest::randomizeToolSettings);

        weightedCaller.registerCallback(1, RandomGUITest::arrangeWindows);

        weightedCaller.registerCallback(1, RandomGUITest::randomColors);

        weightedCaller.registerCallback(5, RandomGUITest::randomFilter);

        weightedCaller.registerCallback(25, RandomGUITest::randomTween);

        weightedCaller.registerCallback(10, RandomGUITest::randomFitToScreen);

        weightedCaller.registerCallback(3, () -> randomKey(r));

        weightedCaller.registerCallback(1, () -> reload(r));

        weightedCaller.registerCallback(1, RandomGUITest::randomZoom);

        weightedCaller.registerCallback(1, RandomGUITest::randomZoomOut);

        weightedCaller.registerCallback(3, RandomGUITest::deselect);

        weightedCaller.registerCallback(1, () -> showHideSelection(r));

        weightedCaller.registerCallback(1, RandomGUITest::layerToCanvasSize);

        weightedCaller.registerCallback(1, RandomGUITest::invertSelection);

        weightedCaller.registerCallback(1, RandomGUITest::traceWithCurrentBrush);

        weightedCaller.registerCallback(1, RandomGUITest::traceWithCurrentEraser);

        weightedCaller.registerCallback(1, RandomGUITest::randomRotateFlip);

        weightedCaller.registerCallback(1, RandomGUITest::layerOrderChange);

        weightedCaller.registerCallback(5, RandomGUITest::layerMerge);

        weightedCaller.registerCallback(3, RandomGUITest::layerAddDelete);

        weightedCaller.registerCallback(1, RandomGUITest::randomHideShow);

        weightedCaller.registerCallback(1, RandomGUITest::randomCopy);

        weightedCaller.registerCallback(1, RandomGUITest::randomPaste);

        weightedCaller.registerCallback(1, RandomGUITest::randomChangeLayerOpacityOrBlending);

        weightedCaller.registerCallback(1, RandomGUITest::randomChangeLayerVisibility);

        weightedCaller.registerCallback(3, RandomGUITest::randomTool);

        weightedCaller.registerCallback(1, RandomGUITest::randomEnlargeCanvas);

        weightedCaller.registerCallback(7, RandomGUITest::randomNewTextLayer);
        weightedCaller.registerCallback(7, RandomGUITest::randomTextLayerRasterize);

        if (Build.enableAdjLayers) {
            weightedCaller.registerCallback(2, RandomGUITest::randomNewAdjustmentLayer);
        }

        weightedCaller.registerCallback(7, RandomGUITest::randomSetLayerMaskEditMode);
        weightedCaller.registerCallback(20, RandomGUITest::randomLayerMaskAction);

        // Not called now:
//        randomCloseImageWOSaving();
//        randomLoadImage();
//        randomSaveInAllFormats();
//        randomException();
    }
}



