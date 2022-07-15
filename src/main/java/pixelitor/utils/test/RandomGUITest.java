/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.Composition.LayerAdder;
import pixelitor.ConsistencyChecks;
import pixelitor.Views;
import pixelitor.compactions.EnlargeCanvas;
import pixelitor.compactions.Flip;
import pixelitor.compactions.Rotate;
import pixelitor.filters.*;
import pixelitor.filters.animation.TimeInterpolation;
import pixelitor.filters.animation.TweenAnimation;
import pixelitor.filters.gui.FilterState;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.FilterUtils;
import pixelitor.gui.*;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.PAction;
import pixelitor.guides.Guides;
import pixelitor.history.History;
import pixelitor.layers.*;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.PasteAction;
import pixelitor.menus.edit.PasteDestination;
import pixelitor.menus.view.*;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.pen.PenTool;
import pixelitor.utils.MemoryInfo;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.Debug;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.awt.event.KeyEvent.*;
import static java.lang.String.format;
import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.FilterContext.FILTER_WITHOUT_DIALOG;
import static pixelitor.FilterContext.PREVIEWING;
import static pixelitor.colors.FgBgColors.randomizeColors;
import static pixelitor.compactions.Flip.Direction.HORIZONTAL;
import static pixelitor.compactions.Flip.Direction.VERTICAL;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.ImageArea.Mode.TABS;
import static pixelitor.utils.QuadrantAngle.*;

/**
 * An automatic test using java.awt.Robot, which performs
 * random mouse movements and actions.
 *
 * Can be dangerous because the random native mouse events
 * can control other apps as well if they escape.
 */
public class RandomGUITest {
    public static final char EXIT_KEY_CHAR = 'Q';
    public static final char PAUSE_KEY_CHAR = 'A';
    private static final Random rand = new Random();

    // set to null to select random tools
    private static final Tool preferredTool = null;

    // set to null to select random filters
    private static final Filter preferredFilter = null;
    private static final ParametrizedFilter preferredTweenFilter = null;

    private static final boolean singleImageTest = false;
    private static final boolean noHideShow = true; // no view operations if set to true
    private static final DateTimeFormatter DATE_FORMAT
        = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

    private static volatile boolean stopRunning = false;

    private static final WeightedCaller weightedCaller = new WeightedCaller();
    private static final boolean PRINT_MEMORY = false;

    private static int numPastedImages = 0;

    private static boolean running = false;

    private static final boolean verbose = "true".equals(System.getProperty("verbose"));

    private static Rectangle startBounds;

    private static final boolean enableCopyPaste = false;

    /**
     * Utility class with static methods
     */
    private RandomGUITest() {
    }

    public static void start() {
        if (AppContext.isFinal()) {
            Messages.showError("Error", "Build is FINAL");
            return;
        }
        if (running) {
            System.out.println("RandomGUITest::runTest: already running");
            return;
        }
        running = true;
        startBounds = getWindowBounds();

        PixelitorWindow.get().setAlwaysOnTop(true);

        new PixelitorEventListener().register();
        GlobalEvents.registerDebugMouseWatching(true);

        numPastedImages = 0;

        // make sure it can be stopped by pressing a key
        GlobalEvents.addHotKey(PAUSE_KEY_CHAR, new PAction() {
                @Override
                protected void onClick() {
                    System.err.printf("%nRandomGUITest: '%s' pressed.%n", PAUSE_KEY_CHAR);
                    stopRunning = true;
                }
            }
        );
        stopRunning = false;

        // This key not only stops the testing, but also exits the app
        GlobalEvents.addHotKey(EXIT_KEY_CHAR, new PAction() {
            @Override
            protected void onClick() {
                System.err.printf("%nRandomGUITest: exiting app because '%s' was pressed.%n",
                    EXIT_KEY_CHAR);
                // no need to reset the GUI here, because preferences won't be saved
                System.exit(1);
            }
        });

        System.out.printf("RandomGUITest started at %s, the '%s' key stops, the '%s' key exits.%n",
            DATE_FORMAT.format(LocalDateTime.now()), PAUSE_KEY_CHAR, EXIT_KEY_CHAR);

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
        SplashImageCreator.createSplashComp();
        if (enableCopyPaste) {
            randomCopy(); // ensure an image is on the clipboard
        }

        SwingWorker<Void, Void> worker = createSwingWorker(r);
        worker.execute();
    }

    public static boolean isRunning() {
        return running;
    }

    public static void stop() {
        stopRunning = true;
        var pw = PixelitorWindow.get();
        // the window can be null if an exception is thrown at startup,
        // and we get here from the uncaught exception handler
        if (pw != null) {
            pw.setAlwaysOnTop(false);
        }
    }

    private static SwingWorker<Void, Void> createSwingWorker(Robot r) {
        return new SwingWorker<>() {
            @Override
            public Void doInBackground() {
                return backgroundRunner(r);
            }
        };
    }

    private static Void backgroundRunner(Robot r) {
        int numTests = 8000;  // must be > 100
        int onePercent = numTests / 100;

        boolean forever = true;
        int max = forever ? Integer.MAX_VALUE : numTests;

        for (int roundNr = 0; roundNr < max; roundNr++) {
            printProgressPercentage(numTests, onePercent, roundNr);

            if (!GUIUtils.appHasFocus()) {
                tryToRegainWindowFocus(3);

                if (!GUIUtils.appHasFocus()) {
                    System.out.println("\nRandomGUITest app focus lost.");
                    finishRunning();
                    break;
                }
            }

            if (stopRunning) {
                System.out.println("\nRandomGUITest stopped.");
                finishRunning();
                break;
            }

            r.delay(Rnd.intInRange(100, 500));

            GUIUtils.invokeAndWait(() -> {
                try {
                    weightedCaller.callRandomAction();
                    var comp = Views.getActiveCompOpt().orElseThrow(() ->
                        new IllegalStateException("no active composition"));
                    ConsistencyChecks.checkAll(comp, true);
                } catch (Throwable e) {
                    Messages.showException(e);
                }
            });
        }
        System.out.println("\nRandomGUITest.runTest FINISHED at " + LocalDateTime.now());
        finishRunning();
        Toolkit.getDefaultToolkit().beep();

        return null;
    }

    private static void printProgressPercentage(int numTests, int onePercent, int roundNr) {
        if (roundNr % onePercent == 0) {
            int percent = 100 * roundNr / numTests;
            System.out.print(percent + "% ");
            if (PRINT_MEMORY) {
                System.out.println(new MemoryInfo());
            } else {
                if ((percent + 1) % 20 == 0) {
                    System.out.println();
                }
            }
        }
    }

    private static void tryToRegainWindowFocus(int attempts) {
        if (attempts <= 0) {
            return;
        }

        System.out.println("RandomGUITest: trying to regain window focus");
        GUIUtils.invokeAndWait(() -> {
            var pw = PixelitorWindow.get();
            pw.toFront();
            pw.repaint();
        });

        if (!GUIUtils.appHasFocus()) {
            Utils.sleep(1, TimeUnit.SECONDS);
            tryToRegainWindowFocus(attempts - 1);
        }
    }

    private static Rectangle getWindowBounds() {
        return PixelitorWindow.get().getBounds();
    }

    // generates a random point within the main window relative to the screen
    private static Point generateRandomPoint() {
        Rectangle windowBounds = getWindowBounds();
        if (!windowBounds.equals(startBounds)) {
            // Window moved. Shouldn't happen, but as a workaround
            // restore it to the starting state
            System.out.println("Restoring the original window bounds " + startBounds);
            PixelitorWindow.get().setBounds(startBounds);
        }

        int safetyGapLeft = 10;
        int safetyGapRight = 10;
        int safetyGapTop = 130;
        int safetyGapBottom = 10;

        int minX = windowBounds.x + safetyGapLeft;
        int minY = windowBounds.y + safetyGapTop;
        int maxX = windowBounds.x + windowBounds.width - safetyGapRight;
        int maxY = windowBounds.y + windowBounds.height - safetyGapBottom;

        if (maxX <= 0 || maxY <= 0) {
            // probably the mouse was moved, and the window is too small
            System.out.printf("RandomGUITest::generateRandomPoint: " +
                    "minX = %d, minY = %d, maxX = %d, maxY = %d, " +
                    "windowBounds = %s%n",
                minX, minY, maxX, maxY,
                windowBounds);
            stop();
            throw new IllegalStateException("small window");
        }

        return new Point(
            Rnd.intInRange(minX, maxX),
            Rnd.intInRange(minY, maxY));
    }

    private static void finishRunning() {
        resetGUI();
        running = false;
    }

    private static void resetGUI() {
        WorkSpace.resetDefaults(PixelitorWindow.get());
        resetTabsUI();
        PixelitorWindow.get().setAlwaysOnTop(false);
    }

    private static void log(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
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
        Tool tool = Tools.getCurrent();
        Point randomPoint = generateRandomPoint();
        int x = randomPoint.x;
        int y = randomPoint.y;
        String stateInfo = tool.getStateInfo();
        if (stateInfo == null) {
            stateInfo = "";
        } else {
            stateInfo = " (" + stateInfo + ")";
        }
        String modifiers = doWithModifiers(r, () -> r.mouseMove(x, y));
        log(tool.getName() + stateInfo + " " + modifiers
            + "drag to (" + x + ", " + y + ')');
    }

    private static void randomClick(Robot r) {
        String modifiers = doWithModifiers(r, null);
        String msg = Tools.getCurrent().getName() + " " + modifiers + "click";
        log(msg);
    }

    private static String doWithModifiers(Robot r, Runnable action) {
        boolean shiftDown = rand.nextBoolean();
        if (shiftDown) {
            r.keyPress(VK_SHIFT);
            r.delay(50);
        }
        // don't generate Alt-movements on Linux, because it can drag the window
        boolean altDown = JVM.isLinux ? false : rand.nextBoolean();
        if (altDown) {
            r.keyPress(VK_ALT);
            r.delay(50);
        }
        boolean ctrlDown = rand.nextBoolean();
        if (ctrlDown) {
            r.keyPress(VK_CONTROL);
            r.delay(50);
        }

        boolean rightMouse = rand.nextFloat() < 0.2;
        if (rightMouse) {
            r.mousePress(BUTTON3_DOWN_MASK);
        } else {
            r.mousePress(BUTTON1_DOWN_MASK);
        }
        r.delay(50);

        if (action != null) {
            action.run();
            r.delay(50);
        }

        if (rightMouse) {
            r.mouseRelease(BUTTON3_DOWN_MASK);
        } else {
            r.mouseRelease(BUTTON1_DOWN_MASK);
        }
        r.delay(50);
        if (ctrlDown) {
            r.keyRelease(VK_CONTROL);
            r.delay(50);
        }
        if (altDown) {
            r.keyRelease(VK_ALT);
            r.delay(50);
        }
        if (shiftDown) {
            r.keyRelease(VK_SHIFT);
            r.delay(50);
        }

        return Debug.modifiersAsString(ctrlDown, altDown, shiftDown, rightMouse, false);
    }

    private static void randomColors() {
        log("randomize colors");
        randomizeColors();
    }

    private static void randomFilter() {
        Drawable dr = Views.getActiveDrawable();
        if (dr == null) {
            return;
        }

        Filter filter;
        if (preferredFilter == null) {
            filter = FilterUtils.getRandomFilter(f ->
                (!(f instanceof RandomFilter)
                 && !(f instanceof FlowField))
            );
        } else {
            filter = preferredFilter;
        }

        String filterName = filter.getName();
        log("filter: " + filterName);

        long runCountBefore = Filter.runCount;

        if (filter instanceof FilterWithGUI guiFilter) {
            guiFilter.randomizeSettings();
            dr.startPreviewing();

            try {
                dr.startFilter(filter, PREVIEWING);
            } catch (Throwable e) {
                BufferedImage src = dr.getFilterSourceImage();
                if (guiFilter instanceof ParametrizedFilter pf) {
                    ParamSet paramSet = pf.getParamSet();
                    System.out.printf(
                        "RandomGUITest::randomFilter: filterName = %s, " +
                        "src.width = %d, src.height = %d, params = %s%n",
                        filterName, src.getWidth(), src.getHeight(), paramSet);
                } else {
                    System.out.printf(
                        "RandomGUITest::randomFilter: filterName = %s, " +
                        "src.width = %d, src.height = %d%n",
                        filterName, src.getWidth(), src.getHeight());
                }
                throw e;
            }

            if (Math.random() > 0.3) {
                dr.onFilterDialogAccepted(filterName);
            } else {
                dr.onFilterDialogCanceled();
            }
        } else {
            BufferedImage src = dr.getFilterSourceImage();
            try {
                dr.startFilter(filter, FILTER_WITHOUT_DIALOG);
            } catch (Throwable e) {
                System.out.printf(
                    "RandomGUITest::randomFilter: name = %s, width = %d, height = %d%n",
                    filterName, src.getWidth(), src.getHeight());
                throw e;
            }
        }
        long runCountAfter = Filter.runCount;
        if (runCountAfter != runCountBefore + 1) {
            throw new IllegalStateException(
                "runCountBefore = " + runCountBefore
                    + ", runCountAfter = " + runCountAfter);
        }
    }

    private static void randomTween() {
        Drawable dr = Views.getActiveDrawable();
        if (dr == null) {
            return;
        }

        long runCountBefore = Filter.runCount;

        ParametrizedFilter filter = getRandomTweenFilter();
        String filterName = filter.getName();

        log("tween: " + filterName);

        var animation = new TweenAnimation();
        animation.setFilter(filter);
        animation.setInterpolation(Rnd.chooseFrom(TimeInterpolation.values()));

        ParamSet paramSet = filter.getParamSet();
        paramSet.randomize();
        animation.copyInitialStateFromCurrent();

        paramSet.randomize();
        animation.copyFinalStateFromCurrent();

        double randomTime = Math.random();
        FilterState intermediateState = animation.tween(randomTime);
        paramSet.setState(intermediateState, true);

        // run everything without showing a modal dialog
        dr.startTweening();

        PixelitorWindow busyCursorParent = PixelitorWindow.get();

        try {
            dr.startFilter(filter, PREVIEWING, busyCursorParent);
        } catch (Throwable e) {
            BufferedImage src = dr.getFilterSourceImage();
            String msg = format(
                "Exception in random tween: filter name = %s, " +
                    "srcWidth = %d, srcHeight = %d, " +
                    "isMaskEditing = %b, params = %s",
                filterName, src.getWidth(), src.getHeight(),
                dr.isMaskEditing(), paramSet);
            throw new IllegalStateException(msg, e);
        }

        dr.endTweening();

        long runCountAfter = Filter.runCount;
        if (runCountAfter != runCountBefore + 1) {
            throw new IllegalStateException(
                "runCountBefore = " + runCountBefore
                    + ", runCountAfter = " + runCountAfter);
        }
    }

    private static void randomFitTo() {
        double r = Math.random();
        if (r > 0.75) {
            log("fit active to space");
            Views.fitActive(AutoZoom.FIT_SPACE);
        } else if (r > 0.5) {
            log("fit active to width");
            Views.fitActive(AutoZoom.FIT_WIDTH);
        } else if (r > 0.25) {
            log("fit active to height");
            Views.fitActive(AutoZoom.FIT_HEIGHT);
        } else {
            log("fit active to actual pixels");
            Views.fitActive(AutoZoom.ACTUAL_PIXELS);
        }
    }

    private static final int[] keyCodes = {VK_1,
        VK_ENTER, VK_ESCAPE, VK_BACK_SPACE,
        // skip A, because it is the stop keystroke
        VK_B, VK_C,
        VK_D, VK_E, VK_F,
        VK_G, VK_H, VK_I,
        VK_J, VK_K, VK_L,
        VK_M, VK_N, VK_O,
        VK_P,
        // skip Q, because it is the exit keystroke
        VK_R, VK_S,
        // skip T, because it brings up the text layer dialog
        VK_U,
        // skip V, because too much Move Tool consumes all the memory
        VK_W,
        VK_Z,
        VK_X,
        VK_Y,
        VK_TAB,
        VK_COMMA, VK_HOME,
        VK_RIGHT, VK_LEFT, VK_UP, VK_DOWN
    };

    private static void randomKey(Robot r) {
        int randomIndex = rand.nextInt(keyCodes.length);
        int keyCode = keyCodes[randomIndex];

        log("random key: " + getKeyText(keyCode));
        pressKey(r, keyCode);
    }

    private static void pressKey(Robot r, int keyCode) {
        r.keyPress(keyCode);
        r.delay(50);
        r.keyRelease(keyCode);
    }

    private static void pressCtrlKey(Robot r, int keyCode) {
        r.keyPress(VK_CONTROL);
        r.delay(50);
        r.keyPress(keyCode);
        r.delay(50);
        r.keyRelease(keyCode);
        r.delay(50);
        r.keyRelease(VK_CONTROL);
    }

    private static void randomZoom() {
        Views.onActiveView(RandomGUITest::setRandomZoom);
    }

    private static void setRandomZoom(View view) {
        ZoomLevel randomZoomLevel = calcRandomZoomLevel();
        log("zoom " + view.getName() + ", zoom level = " + randomZoomLevel);

        if (rand.nextBoolean()) {
            view.setZoom(randomZoomLevel);
        } else {
            Point mousePos = pickRandomPointOn(view);
            view.setZoom(randomZoomLevel, mousePos);
        }
    }

    private static ZoomLevel calcRandomZoomLevel() {
        double percentValue = 0;
        ZoomLevel level = null;
        while (percentValue < 49) {
            level = ZoomLevel.getRandomZoomLevel();
            percentValue = level.asPercent();
        }
        return level;
    }

    private static Point pickRandomPointOn(View view) {
        Rectangle vp = view.getVisiblePart();
        int randX = vp.x;
        if (vp.width >= 2) {
            randX = Rnd.intInRange(vp.x, vp.x + vp.width);
        }
        int randY = vp.y;
        if (vp.height >= 2) {
            randY = Rnd.intInRange(vp.y, vp.y + vp.height);
        }
        return new Point(randX, randY);
    }

    private static void randomZoomOut() {
        View view = Views.getActive();
        if (view != null) {
            log("zoom out " + view.getName());
            ZoomLevel newZoom = view.getZoomLevel().zoomOut();
            if (rand.nextBoolean()) {
                view.setZoom(newZoom);
            } else {
                Point mousePos = pickRandomPointOn(view);
                view.setZoom(newZoom, mousePos);
            }
        }
    }

    private static void repeat() {
        log("repeat (dispatch Ctrl-F)");

        dispatchKey(VK_F, 'F', CTRL_DOWN_MASK);
    }

    private static void randomUndoRedo() {
        if (History.canUndo()) {
            log("undo " + History.getEditToBeUndoneName());

            History.undo();

            // for some reason, redo might not be available even if we are right
            // after an undo which didn't throw a CannotUndoException
            // This is not a problem normally, because the RedoMenuItem
            // also checks History.canRedo() after an undo
            if (!History.canRedo()) {
                return;
            }

            if (rand.nextInt(10) > 3) {
                log("redo " + History.getEditToBeRedoneName());
                History.redo();
            }
        }
    }

    private static void randomCrop() {
        boolean enabled = SelectionActions.areEnabled();
        if (enabled) {
            runAction(SelectionActions.getCrop());
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

        Drawable dr = Views.getActiveDrawableOrThrow();
        dr.startFilter(fade, FILTER_WITHOUT_DIALOG);
    }

    private static void randomizeToolSettings() {
        log("randomize tool settings for " + Tools.getCurrent());
        ToolSettingsPanelContainer.get().randomizeToolSettings();
    }

    private static void arrangeWindows() {
        if (ImageArea.currentModeIs(TABS)) {
            return;
        }
        double r = Math.random();
        if (r < 0.8) {
            log("arrange windows - tile");
            ImageArea.tileWindows();
        } else {
            log("arrange windows - cascade");
            ImageArea.cascadeWindows();
        }
    }

    private static void changeImageArea() {
        log("change image area from " + ImageArea.getMode());
        ImageArea.changeUI();
    }

    private static void deselect() {
        if (SelectionActions.areEnabled()) {
            runAction(SelectionActions.getDeselect());
        }
    }

    private static void showHideSelection(Robot robot) {
        log("showHideSelection");
        pressCtrlKey(robot, VK_H);
    }

    private static void layerToCanvasSize() {
        log("layer to canvas size");
        Views.onActiveComp(Composition::activeLayerToCanvasSize);
    }

    private static void fitCanvasToLayers() {
        log("fit canvas to layers");
        Views.onActiveComp(Composition::fitCanvasToLayers);
    }

    private static void invertSelection() {
        if (SelectionActions.areEnabled()) {
            runAction(SelectionActions.getInvert());
        }
    }

    private static void traceWithCurrentBrush() {
        if (canTrace()) {
            runAction(PenTool.getTraceWithBrushAction());
        }
    }

    private static void traceWithCurrentEraser() {
        if (canTrace()) {
            runAction(PenTool.getTraceWithEraserAction());
        }
    }

    private static void traceWithCurrentSmudge() {
        if (canTrace()) {
            runAction(PenTool.getTraceWithSmudgeAction());
        }
    }

    private static boolean canTrace() {
        Composition comp = Views.getActiveComp();
        if (comp == null) {
            return false;
        }
        return comp.hasActivePath() && comp.activeAcceptsToolDrawing();
    }

    private static void randomRotateFlip() {
        int r = rand.nextInt(5);
        switch (r) {
            case 0 -> runAction(new Rotate(ANGLE_90));
            case 1 -> runAction(new Rotate(ANGLE_180));
            case 2 -> runAction(new Rotate(ANGLE_270));
            case 3 -> runAction(new Flip(HORIZONTAL));
            case 4 -> runAction(new Flip(VERTICAL));
            default -> throw new IllegalStateException("r = " + r);
        }
    }

    private static void activateRandomView() {
        View view = Views.activateRandomView();
        if (view != null) {
            log("activated random view " + view.getName());
        }
    }

    private static void layerOrderChange() {
        var comp = Views.getActiveComp();
        int r = rand.nextInt(6);
        switch (r) {
            case 0 -> moveActiveLayerToTop(comp);
            case 1 -> moveActiveLayerToBottom(comp);
            case 2 -> raiseLayerSelection(comp);
            case 3 -> lowerLayerSelection(comp);
            case 4 -> moveActiveLayerUp(comp);
            case 5 -> moveActiveLayerDown(comp);
            default -> throw new IllegalStateException("Unexpected value: " + r);
        }
    }

    private static void moveActiveLayerToTop(Composition comp) {
        log("layer order change: active to top");
        comp.moveActiveLayerToTop();
    }

    private static void moveActiveLayerToBottom(Composition comp) {
        log("layer order change: active to bottom");
        comp.moveActiveLayerToBottom();
    }

    private static void raiseLayerSelection(Composition comp) {
        log("layer selection change: raise selection");
        comp.raiseLayerSelection();
    }

    private static void lowerLayerSelection(Composition comp) {
        log("layer selection change: lower selection");
        comp.lowerLayerSelection();
    }

    private static void moveActiveLayerUp(Composition comp) {
        log("layer order change: active up");
        comp.moveActiveLayerUp();
    }

    private static void moveActiveLayerDown(Composition comp) {
        log("layer order change: active down");
        comp.moveActiveLayerDown();
    }

    private static void layerMerge() {
        var comp = Views.getActiveComp();

        if (rand.nextBoolean()) {
            Layer layer = comp.getActiveLayer();
            if (comp.canMergeDown(layer)) {
                log("merge down " + layer.getName() + " in " + comp.getName());
                comp.mergeActiveLayerDown();
            }
        } else {
            log("flatten image " + comp.getName());
            comp.flattenImage();
        }
    }

    private static void layerAddDelete() {
        if (rand.nextBoolean()) {
            if (AddNewLayerAction.INSTANCE.isEnabled()) {
                runAction(AddNewLayerAction.INSTANCE);
            }
        } else {
            if (DeleteActiveLayerAction.INSTANCE.isEnabled()) {
                runAction(DeleteActiveLayerAction.INSTANCE);
            }
        }
    }

    private static void randomHideShow() {
        if (noHideShow) {
            return;
        }

        int r = rand.nextInt(5);
        switch (r) {
            case 0 -> runAction(ShowHideHistogramsAction.INSTANCE);
            case 1 -> runAction(ShowHideLayersAction.INSTANCE);
            case 2 -> runAction(ShowHideToolsAction.INSTANCE);
            case 4 -> runAction(ShowHideStatusBarAction.INSTANCE);
            case 5 -> runAction(ShowHideAllAction.INSTANCE);
            default -> throw new IllegalStateException("r = " + r);
        }
    }

    private static void randomCopy() {
        if (rand.nextBoolean()) {
            runAction(CopyAction.COPY_LAYER);
        } else {
            runAction(CopyAction.COPY_COMPOSITE);
        }
    }

    private static void runAction(Action action) {
        String msg = format("action \"%s\" (class: \"%s\")",
            action.getValue(Action.NAME),
            action.getClass().getSimpleName());
        log(msg);

        action.actionPerformed(new ActionEvent("", 0, ""));
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
            runAction(new PasteAction(PasteDestination.NEW_IMAGE));
            numPastedImages++;
        } else if (r == 1) {
            runAction(new PasteAction(PasteDestination.NEW_LAYER));
            numPastedImages++;
        }
        // paste as mask?
    }

    private static void randomChangeLayerOpacityOrBlending() {
        Layer layer = Views.getActiveLayer();
        if (rand.nextBoolean()) {
            float opacity = layer.getOpacity();
            float f = rand.nextFloat();

            if (f > opacity) {
                // always increase
                log("increase opacity for " + layer.getName());
                layer.setOpacity(f, true);
            } else if (rand.nextFloat() > 0.75) { // sometimes decrease
                log("decrease opacity for " + layer.getName());
                layer.setOpacity(f, true);
            }
        } else {
            log("change layer blending mode for " + layer.getName());
            BlendingMode randomBM = Rnd.chooseFrom(BlendingMode.values());
            layer.setBlendingMode(randomBM, true);
        }
    }

    private static void randomChangeLayerVisibility() {
        Layer layer = Views.getActiveLayer();
        boolean visible = layer.isVisible();
        if (rand.nextBoolean()) {
            if (!visible) {
                log("show layer");
                layer.setVisible(true, true);
            }
        } else {
            if (visible) {
                if (rand.nextFloat() > 0.8) { // sometimes hide
                    log("hide layer");
                    layer.setVisible(false, true);
                }
            }
        }
    }

    private static void randomTool() {
        Tool tool;
        if (preferredTool != null) {
            if (rand.nextBoolean()) {
                tool = preferredTool;
            } else {
                tool = Tools.getRandomTool();
            }
        } else {
            tool = Tools.getRandomTool();

            // The move tool can cause out of memory errors, so don't test it
            if (tool == Tools.MOVE) {
                return;
            }
        }
        Tool currentTool = Tools.getCurrent();
        if (currentTool != tool) {
            log("tool click on " + currentTool);
            tool.activate();
        }
    }

    private static void newRandomTextLayer() {
        Composition comp = Views.getActiveComp();
        MaskViewMode oldMaskViewMode = comp.getView().getMaskViewMode();
        Layer activeLayerBefore = comp.getActiveLayer();

        TextSettings settings = new TextSettings();
        settings.randomize();
        TextLayer textLayer = TextLayer.createNew(settings);

        // has to be called explicitly, since no dialog will be shown
        textLayer.finalizeCreation(comp, activeLayerBefore, oldMaskViewMode);

        log("new text layer: " + textLayer.getName());
    }

    private static void newColorFillLayer() {
        log("new color fill layer");
        ColorFillLayer.createNew();
    }

    private static void newGradientFillLayer() {
        log("new gradient fill layer");
        GradientFillLayer.createNew();
    }

    private static void newShapesLayer() {
        log("new shapes layer");
        ShapesLayer.createNew();
    }

    private static void convertToSmartObject() {
        Layer layer = Views.getActiveLayer();
        if (!(layer instanceof SmartObject)) {
            log("Convert Layer to Smart Object");
            layer.replaceWithSmartObject();
        }
    }

    private static void randomRasterizeLayer() {
        Layer layer = Views.getActiveLayer();
        if (layer.isRasterizable()) {
            log("rasterize " + layer.getTypeStringLC() + " " + layer.getName());
            layer.replaceWithRasterized();
        }
    }

    private static void randomNewAdjustmentLayer() {
        log("new adj layer");
        var comp = Views.getActiveComp();
        var adjustmentLayer = new AdjustmentLayer(comp, "Invert", new Invert());
        new LayerAdder(comp)
            .withHistory("New Random Adj Layer")
            .atPosition(ABOVE_ACTIVE)
            .add(adjustmentLayer);
    }

    private static void randomSetLayerMaskEditMode() {
        Layer layer = Views.getActiveLayer();
        if (!layer.hasMask()) {
            return;
        }
        double d = rand.nextDouble();
        int keyCode;
        char keyChar;
        if (d < 0.33) {
            keyCode = VK_1;
            keyChar = '1';
            log("Ctrl-1");
        } else if (d < 0.66) {
            keyCode = VK_2;
            keyChar = '2';
            log("Ctrl-2");
        } else {
            keyCode = VK_3;
            keyChar = '3';
            log("Ctrl-3");
        }

        dispatchKey(keyCode, keyChar, CTRL_DOWN_MASK);
    }

    // (add, delete, apply, link)
    private static void randomLayerMaskAction() {
        Layer layer = Views.getActiveLayer();
        if (!layer.hasMask()) {
            assert AddLayerMaskAction.INSTANCE.isEnabled();
            runAction(AddLayerMaskAction.INSTANCE);
        } else {
            assert !AddLayerMaskAction.INSTANCE.isEnabled();
            if (rand.nextFloat() < 0.2 && layer instanceof ContentLayer) {
                LayerMask mask = layer.getMask();
                if (mask.isLinked()) {
                    log("unlink layer mask");
                    mask.setLinked(false, true);
                } else {
                    log("re-link layer mask");
                    mask.setLinked(true, true);
                }
            } else if (layer instanceof ImageLayer imageLayer) {
                double d = rand.nextDouble();
                if (d > 0.5) {
                    log("apply layer mask");
                    imageLayer.applyLayerMask(true);
                } else {
                    log("delete layer mask");
                    imageLayer.deleteMask(true);
                }
            } else {
                log("delete layer mask");
                layer.deleteMask(true);
            }
        }
    }

    private static ParametrizedFilter getRandomTweenFilter() {
        if (preferredTweenFilter != null) {
            assert !preferredTweenFilter.excludedFromAnimation();
            assert preferredTweenFilter.getParamSet().canBeAnimated();
            return preferredTweenFilter;
        }
        FilterAction[] filterActions = FilterUtils.getAnimationFilters();
        FilterAction filterAction = Rnd.chooseFrom(filterActions);
        return (ParametrizedFilter) filterAction.getFilter();
    }

    private static void randomEnlargeCanvas() {
        int north = rand.nextInt(3);
        int east = rand.nextInt(3);
        int south = rand.nextInt(3);
        int west = rand.nextInt(3);
        log(format("enlarge canvas north = %d, east = %d, south = %d, west = %d",
            north, east, south, west));
        var comp = Views.getActiveComp();
        new EnlargeCanvas(north, east, south, west).process(comp);
    }

    private static void randomGuides() {
        var comp = Views.getActiveComp();
        float v = rand.nextFloat();
        if (v < 0.2) {
            log("clear guides");
            comp.clearGuides();
            return;
        }
        new Guides.Builder(comp.getView(), false)
            .build(false, RandomGUITest::randomGuidesSetup);
    }

    private static void randomGuidesSetup(Guides guides) {
        if (rand.nextBoolean()) {
            log("add relative horizontal guide");
            guides.addHorRelative(rand.nextFloat());
        } else {
            log("add relative vertical guide");
            guides.addVerRelative(rand.nextFloat());
        }
    }

    private static void reload(Robot r) {
        if (rand.nextFloat() < 0.1) {
            var comp = Views.getActiveComp();
            if (comp.getFile() != null) {
                log("f12 reload");
                pressKey(r, VK_F12);
            }
        }
    }

    // to prevent paths growing too large
    private static void setPathsToNull() {
        log("set paths to null");
        Views.forEachView(view -> {
            // don't touch the active, as its path might be edited just now
            if (!view.isActive()) {
                view.getComp().setActivePath(null);
            }
        });
        // history is in an inconsistent state now
        History.clear();
    }

    private static void dispatchKey(int keyCode, char keyChar, int mask) {
        var pw = PixelitorWindow.get();
        pw.dispatchEvent(new KeyEvent(pw, KEY_PRESSED,
            System.currentTimeMillis(), mask, keyCode, keyChar));
    }

    private static void resetTabsUI() {
        if (ImageArea.currentModeIs(FRAMES)) {
            ImageArea.changeUI();
        }
    }

    private static void setupWeightedCaller(Robot r) {
        // random move
        weightedCaller.registerCallback(10, () -> randomMove(r));
        weightedCaller.registerCallback(20, () -> randomDrag(r));
        weightedCaller.registerCallback(5, () -> randomClick(r));
        weightedCaller.registerCallback(2, RandomGUITest::repeat);
        weightedCaller.registerCallback(5, RandomGUITest::randomUndoRedo);
        weightedCaller.registerCallback(1, RandomGUITest::randomCrop);
        weightedCaller.registerCallback(1, RandomGUITest::randomFade);
        weightedCaller.registerCallback(2, RandomGUITest::randomizeToolSettings);
        weightedCaller.registerCallback(1, RandomGUITest::arrangeWindows);
        weightedCaller.registerCallback(3, RandomGUITest::changeImageArea);
        weightedCaller.registerCallback(1, RandomGUITest::randomColors);
        weightedCaller.registerCallback(3, RandomGUITest::randomFilter);
        weightedCaller.registerCallback(1, RandomGUITest::randomTween);
        weightedCaller.registerCallback(1, RandomGUITest::randomFitTo);
        weightedCaller.registerCallback(3, () -> randomKey(r));
        weightedCaller.registerCallback(1, () -> reload(r));
        weightedCaller.registerCallback(1, RandomGUITest::randomZoom);
        weightedCaller.registerCallback(1, RandomGUITest::randomZoomOut);
        weightedCaller.registerCallback(10, RandomGUITest::deselect);
        weightedCaller.registerCallback(1, () -> showHideSelection(r));
        weightedCaller.registerCallback(1, RandomGUITest::layerToCanvasSize);
        weightedCaller.registerCallback(1, RandomGUITest::fitCanvasToLayers);
        weightedCaller.registerCallback(1, RandomGUITest::invertSelection);
        weightedCaller.registerCallback(1, RandomGUITest::traceWithCurrentBrush);
        weightedCaller.registerCallback(1, RandomGUITest::traceWithCurrentEraser);
        weightedCaller.registerCallback(1, RandomGUITest::traceWithCurrentSmudge);
        weightedCaller.registerCallback(1, RandomGUITest::randomRotateFlip);
        weightedCaller.registerCallback(5, RandomGUITest::activateRandomView);
        weightedCaller.registerCallback(1, RandomGUITest::layerOrderChange);
        weightedCaller.registerCallback(5, RandomGUITest::layerMerge);
        weightedCaller.registerCallback(3, RandomGUITest::layerAddDelete);
        weightedCaller.registerCallback(1, RandomGUITest::randomHideShow);
        if (enableCopyPaste) {
            weightedCaller.registerCallback(1, RandomGUITest::randomCopy);
            weightedCaller.registerCallback(1, RandomGUITest::randomPaste);
        }
        weightedCaller.registerCallback(1, RandomGUITest::randomChangeLayerOpacityOrBlending);
        weightedCaller.registerCallback(1, RandomGUITest::randomChangeLayerVisibility);
        weightedCaller.registerCallback(5, RandomGUITest::randomTool);
        weightedCaller.registerCallback(1, RandomGUITest::randomEnlargeCanvas);
        weightedCaller.registerCallback(2, RandomGUITest::newRandomTextLayer);
        weightedCaller.registerCallback(1, RandomGUITest::newColorFillLayer);
        weightedCaller.registerCallback(1, RandomGUITest::newGradientFillLayer);
        weightedCaller.registerCallback(1, RandomGUITest::newShapesLayer);
        weightedCaller.registerCallback(1, RandomGUITest::convertToSmartObject);
        weightedCaller.registerCallback(5, RandomGUITest::randomRasterizeLayer);
        weightedCaller.registerCallback(4, RandomGUITest::randomGuides);
        weightedCaller.registerCallback(4, RandomGUITest::setPathsToNull);

        if (AppContext.enableExperimentalFeatures) {
            weightedCaller.registerCallback(2, RandomGUITest::randomNewAdjustmentLayer);
        }

        weightedCaller.registerCallback(7, RandomGUITest::randomSetLayerMaskEditMode);
        weightedCaller.registerCallback(10, RandomGUITest::randomLayerMaskAction);
    }
}



