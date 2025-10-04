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

package pixelitor.utils.test;

import com.bric.util.JVM;
import pixelitor.AppMode;
import pixelitor.Composition;
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
import pixelitor.filters.util.Filters;
import pixelitor.gui.*;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TaskAction;
import pixelitor.guides.Guides;
import pixelitor.history.History;
import pixelitor.layers.*;
import pixelitor.menus.edit.CopyAction;
import pixelitor.menus.edit.PasteAction;
import pixelitor.menus.edit.PasteTarget;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.selection.SelectionActions;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.pen.PathActions;
import pixelitor.tools.pen.PathTool;
import pixelitor.utils.*;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.input.Alt;
import pixelitor.utils.input.Ctrl;
import pixelitor.utils.input.MouseButton;
import pixelitor.utils.input.Shift;

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
import static pixelitor.FilterContext.FILTER_WITHOUT_DIALOG;
import static pixelitor.FilterContext.PREVIEWING;
import static pixelitor.colors.FgBgColors.randomizeColors;
import static pixelitor.compactions.FlipDirection.HORIZONTAL;
import static pixelitor.compactions.FlipDirection.VERTICAL;
import static pixelitor.compactions.QuadrantAngle.ANGLE_180;
import static pixelitor.compactions.QuadrantAngle.ANGLE_270;
import static pixelitor.compactions.QuadrantAngle.ANGLE_90;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.ImageArea.Mode.TABS;

/**
 * Automated GUI testing utility that simulates random mouse
 * movements and actions using the AWT {@link Robot} class
 * in order to stress-test the app.
 *
 * WARNING: This test generates native mouse events that may affect
 * other applications if they accidentally receive focus during testing.
 */
public class RandomGUITest {
    public static final char EXIT_KEY_CHAR = 'Q';
    public static final char PAUSE_KEY_CHAR = 'A';

    private static final boolean ENABLE_MEMORY_LOGGING = false;
    private static final boolean ENABLE_COPY_PASTE = false;
    private static final boolean TEST_ADJ_LAYERS = false;

    private static final boolean RUN_FOREVER = true;
    private static final int NUM_TESTS = 8000; // must be > 100 if not running forever

    private final boolean VERBOSE = "true".equals(System.getProperty("verbose"));

    private final DateTimeFormatter timestampFormatter
        = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

    private final Random rand = new Random();

    // set to null to select random tools
    private final Tool preferredTool = null;

    // set to null to select random filters
    private final Filter preferredFilter = null;
    private final ParametrizedFilter preferredTweenFilter = null;

    private boolean running = false;
    private volatile boolean stopRunning = false;

    private final WeightedCaller actionCaller = new WeightedCaller();

    private Rectangle initialWindowBounds;

    private int pastedImagesCount = 0;

    private static RandomGUITest instance = null;

    private RandomGUITest() {
    }

    public static RandomGUITest get() {
        assert Threads.calledOnEDT();
        if (instance == null) {
            //noinspection NonThreadSafeLazyInitialization
            instance = new RandomGUITest();
        }
        return instance;
    }

    public void start() {
        if (!AppMode.isDevelopment()) {
            Messages.showError("Error", "Can only run in development mode");
            return;
        }
        if (running) {
            Messages.showError("Error", "Already running");
            return;
        }
        running = true;
        initialWindowBounds = PixelitorWindow.get().getBounds();

        PixelitorWindow.get().setAlwaysOnTop(true);

        GlobalEvents.enableMouseEventDebugging(true);

        pastedImagesCount = 0;

        registerHotKeys();

        System.out.printf("RandomGUITest started at %s, the '%s' key stops, the '%s' key exits.%n",
            timestampFormatter.format(LocalDateTime.now()), PAUSE_KEY_CHAR, EXIT_KEY_CHAR);

        Robot robot = createRobot();
        setupActions(robot);

        Point p = genSafeRandomPoint();
        robot.mouseMove(p.x, p.y);

        log("initial splash");
        SplashImageCreator.createSplashComp();
        if (ENABLE_COPY_PASTE) {
            randomCopy(); // ensure an image is on the clipboard
        }

        createSwingWorker(robot).execute();
    }

    private static Robot createRobot() {
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            Messages.showException(e);
        }
        return robot;
    }

    private void registerHotKeys() {
        // make sure it can be stopped by pressing a key
        GlobalEvents.registerHotkey(PAUSE_KEY_CHAR, new TaskAction(() -> {
                System.err.printf("%nRandomGUITest: '%s' pressed.%n", PAUSE_KEY_CHAR);
                stopRunning = true;
            })
        );
        stopRunning = false;

        // this key not only stops the testing, but also exits the app
        GlobalEvents.registerHotkey(EXIT_KEY_CHAR, new TaskAction(() -> {
            System.err.printf("%nRandomGUITest: exiting app because '%s' was pressed.%n",
                EXIT_KEY_CHAR);
            // no need to reset the GUI here, because preferences won't be saved
            System.exit(1);
        }));
    }

    public static boolean isRunning() {
        if (instance == null) {
            return false;
        }
        return instance.running;
    }

    public static void stop() {
        if (instance == null) {
            return;
        }
        instance.stopRunning = true;

        var pw = PixelitorWindow.get();
        // the window can be null if an exception is thrown at startup,
        // and we get here from the uncaught exception handler
        if (pw != null) {
            pw.setAlwaysOnTop(false);
        }
    }

    private SwingWorker<Void, Void> createSwingWorker(Robot robot) {
        return new SwingWorker<>() {
            @Override
            public Void doInBackground() {
                return runTestLoop(robot);
            }
        };
    }

    private Void runTestLoop(Robot robot) {
        int onePercent = NUM_TESTS / 100;
        int maxRounds = RUN_FOREVER ? Integer.MAX_VALUE : NUM_TESTS;

        for (int roundNr = 0; roundNr < maxRounds; roundNr++) {
            printProgressPercentage(onePercent, roundNr);

            if (!GUIUtils.isAppFocused()) {
                tryRegainingWindowFocus(3);

                if (!GUIUtils.isAppFocused()) {
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

            robot.delay(Rnd.intInRange(100, 500));

            GUIUtils.invokeAndWait(() -> {
                try {
                    actionCaller.executeRandomAction();
                    ConsistencyChecks.checkAll(Views.getActiveComp());
                } catch (Throwable e) {
                    Messages.showException(e);
                }
            });
        }
        System.out.println("\nRandomGUITest.runTestLoop FINISHED at " + LocalDateTime.now());
        finishRunning();
        Toolkit.getDefaultToolkit().beep();

        return null;
    }

    private static void printProgressPercentage(int onePercent, int roundNr) {
        if (roundNr % onePercent == 0) {
            int percent = 100 * roundNr / NUM_TESTS;
            System.out.print(percent + "% ");
            if (ENABLE_MEMORY_LOGGING) {
                System.out.println(new MemoryInfo());
            } else {
                // add a newline every 20 percent
                if ((percent + 1) % 20 == 0) {
                    System.out.println();
                }
            }
        }
    }

    private static void tryRegainingWindowFocus(int maxAttempts) {
        if (maxAttempts <= 0) {
            return; // give up
        }

        System.out.println("RandomGUITest: trying to regain window focus");
        GUIUtils.invokeAndWait(() -> {
            var pw = PixelitorWindow.get();
            pw.toFront();
            pw.repaint();
        });

        if (!GUIUtils.isAppFocused()) {
            Utils.sleep(1, TimeUnit.SECONDS);
            //noinspection TailRecursion
            tryRegainingWindowFocus(maxAttempts - 1);
        }
    }

    /**
     * Generates a random point in screen coordinates
     * within safe boundaries of the main window.
     */
    private Point genSafeRandomPoint() {
        Rectangle currentBounds = PixelitorWindow.get().getBounds();
        if (!currentBounds.equals(initialWindowBounds)) {
            // The main frame was moved. Shouldn't happen, but
            // as a workaround restore it to the starting bounds.
            System.out.println("Restoring the original window bounds " + initialWindowBounds);
            PixelitorWindow.get().setBounds(initialWindowBounds);
        }

        int safetyMarginLeft = 10;
        int safetyMarginRight = 10;
        int safetyMarginTop = 130;
        int safetyMarginBottom = 10;

        int minX = currentBounds.x + safetyMarginLeft;
        int minY = currentBounds.y + safetyMarginTop;
        int maxX = currentBounds.x + currentBounds.width - safetyMarginRight;
        int maxY = currentBounds.y + currentBounds.height - safetyMarginBottom;

        if (maxX <= 0 || maxY <= 0) {
            // probably the mouse was moved, and the window is too small
            stop();
            throw new IllegalStateException(String.format("small window: " +
                    "minX = %d, minY = %d, maxX = %d, maxY = %d, currentBounds = %s",
                minX, minY, maxX, maxY, currentBounds));
        }

        return Rnd.pointInRect(minX, maxX, minY, maxY);
    }

    private void finishRunning() {
        resetGUI();
        running = false;
    }

    private static void resetGUI() {
        PixelitorWindow.get().resetDefaultWorkspace();
        if (ImageArea.isActiveMode(FRAMES)) {
            ImageArea.toggleUI();
        }
        PixelitorWindow.get().setAlwaysOnTop(false);
    }

    private void log(String msg) {
        if (VERBOSE) {
            System.out.println(msg);
        }
    }

    private void randomMove(Robot robot) {
        Point randomPoint = genSafeRandomPoint();
        int x = randomPoint.x;
        int y = randomPoint.y;
        log("move to (" + x + ", " + y + ')');
        robot.mouseMove(x, y);
    }

    private static boolean canUseTool(Tool tool) {
        if (tool.allowOnlyDrawables()) {
            return (Views.getActiveLayer() instanceof Drawable);
        }
        return true;
    }

    private void randomDrag(Robot robot) {
        Tool tool = Tools.getActive();
        if (!canUseTool(tool)) {
            return;
        }

        Point randomPoint = genSafeRandomPoint();
        int x = randomPoint.x;
        int y = randomPoint.y;
        String modifiers = runWithModifiers(robot, () -> robot.mouseMove(x, y));
        log("%s (%s) %s drag to (%d, %d)".formatted(
            tool.getName(), tool.getStateInfo(), modifiers, x, y));
    }

    private void randomClick(Robot robot) {
        Tool tool = Tools.getActive();
        if (!canUseTool(tool)) {
            return;
        }

        String modifiers = runWithModifiers(robot, null);
        String msg = tool.getName() + " " + modifiers + "click";
        log(msg);
    }

    private String runWithModifiers(Robot robot, Runnable task) {
        Shift shift = Shift.randomly(rand).press(robot);
        // don't generate Alt-movements on Linux, because it can drag the window
        Alt alt = JVM.isLinux ? Alt.RELEASED : Alt.randomly(rand);
        alt.press(robot);

        Ctrl ctrl = Ctrl.randomly(rand).press(robot);
        MouseButton button = MouseButton.randomly(rand).press(robot);

        if (task != null) {
            task.run();
            robot.delay(50);
        }

        button.release(robot);
        ctrl.release(robot);
        alt.release(robot);
        shift.release(robot);

        return Debug.modifiersToString(ctrl, alt, shift, button, false);
    }

    private void randomColors() {
        log("randomize colors");
        randomizeColors();
    }

    private void randomFilter() {
        Drawable dr = Views.getActiveDrawable();
        if (dr == null) {
            return;
        }

        Filter filter = chooseTestedFilter();

        log("filter: " + filter.getName());

        long executionsBefore = Filter.executionCount;

        if (filter instanceof FilterWithGUI guiFilter) {
            runGUIFilter(guiFilter, dr);
        } else {
            runNonGUIFilter(filter, dr);
        }

        if (Filter.executionCount != executionsBefore + 1) {
            throw new IllegalStateException("%s: before = %d, after = %d"
                .formatted(filter.getName(), executionsBefore, Filter.executionCount));
        }
    }

    private static void runGUIFilter(FilterWithGUI filter, Drawable dr) {
        filter.randomize();
        dr.startPreviewing();

        try {
            dr.startFilter(filter, PREVIEWING);
        } catch (Throwable e) {
            BufferedImage src = dr.getFilterSourceImage();
            if (filter instanceof ParametrizedFilter pf) {
                ParamSet paramSet = pf.getParamSet();
                System.out.printf(
                    "RandomGUITest::runGUIFilter: filterName = %s, " +
                        "src.width = %d, src.height = %d, params = %s%n",
                    filter.getName(), src.getWidth(), src.getHeight(), paramSet);
            } else {
                System.out.printf(
                    "RandomGUITest::runGUIFilter: filterName = %s, " +
                        "src.width = %d, src.height = %d%n",
                    filter.getName(), src.getWidth(), src.getHeight());
            }
            throw e;
        }

        if (Math.random() > 0.3) {
            dr.onFilterDialogAccepted(filter.getName());
        } else {
            dr.onFilterDialogCanceled();
        }
    }

    private static void runNonGUIFilter(Filter filter, Drawable dr) {
        BufferedImage src = dr.getFilterSourceImage();
        try {
            dr.startFilter(filter, FILTER_WITHOUT_DIALOG);
        } catch (Throwable e) {
            System.out.printf(
                "RandomGUITest::runNonGUIFilter: name = %s, width = %d, height = %d%n",
                filter.getName(), src.getWidth(), src.getHeight());
            throw e;
        }
    }

    private Filter chooseTestedFilter() {
        Filter filter;
        if (preferredFilter == null) {
            filter = Filters.getRandomFilter(f ->
                (!(f instanceof RandomFilter)
                    && !(f instanceof FlowField))
            );
        } else {
            filter = preferredFilter;
        }
        return filter;
    }

    private void randomTween() {
        Drawable dr = Views.getActiveDrawable();
        if (dr == null) {
            return;
        }

        long executionsBefore = Filter.executionCount;

        ParametrizedFilter filter = getRandomTweenFilter();
        String filterName = filter.getName();

        log("tween: " + filterName);

        var animation = new TweenAnimation();
        animation.setFilter(filter);
        animation.setInterpolation(Rnd.chooseFrom(TimeInterpolation.values()));

        ParamSet paramSet = filter.getParamSet();
        paramSet.randomize();
        animation.captureInitialState();

        paramSet.randomize();
        animation.captureFinalState();

        double randomTime = Math.random();
        FilterState intermediateState = animation.tween(randomTime);
        paramSet.setState(intermediateState, true);

        // run everything without showing a modal dialog
        dr.startPreviewing();

        try {
            dr.startFilter(filter, PREVIEWING);
        } catch (Throwable e) {
            BufferedImage src = dr.getFilterSourceImage();
            String msg = String.format(
                "Exception in random tween: filter name = %s, " +
                    "srcWidth = %d, srcHeight = %d, " +
                    "isMaskEditing = %b, params = %s",
                filterName, src.getWidth(), src.getHeight(),
                dr.isMaskEditing(), paramSet);
            throw new IllegalStateException(msg, e);
        }

        dr.stopPreviewing();

        if (Filter.executionCount != executionsBefore + 1) {
            throw new IllegalStateException(
                "filter = %s, before = %d, after = %d".formatted(
                    filterName, executionsBefore, Filter.executionCount));
        }
    }

    private void randomFitTo() {
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

    private final int[] keyCodes = {VK_1,
        VK_ENTER, VK_ESCAPE, VK_BACK_SPACE,
        // skip A, because it's the stop keystroke
        VK_B, VK_C, VK_D, VK_E, VK_F,
        VK_G, VK_H, VK_I, VK_J, VK_K,
        VK_L, VK_M, VK_N, VK_O, VK_P,
        // skip Q, because it's the exit keystroke
        VK_R, VK_S,
        // skip T, because it brings up the text layer dialog
        VK_U,
        // skip V, because too much Move Tool consumes all the memory
        VK_W, VK_Z, VK_X, VK_Y,
        VK_TAB, VK_COMMA, VK_HOME,
        VK_RIGHT, VK_LEFT, VK_UP, VK_DOWN
    };

    private void randomKey(Robot robot) {
        int randomIndex = rand.nextInt(keyCodes.length);
        int keyCode = keyCodes[randomIndex];

        log("random key: " + getKeyText(keyCode));
        pressKey(robot, keyCode);
    }

    private static void pressKey(Robot robot, int keyCode) {
        robot.keyPress(keyCode);
        robot.delay(50);
        robot.keyRelease(keyCode);
    }

    private static void pressCtrlKey(Robot robot, int keyCode) {
        robot.keyPress(VK_CONTROL);
        robot.delay(50);
        robot.keyPress(keyCode);
        robot.delay(50);
        robot.keyRelease(keyCode);
        robot.delay(50);
        robot.keyRelease(VK_CONTROL);
    }

    private void randomZoom() {
        Views.onActive(this::setRandomZoom);
    }

    private void setRandomZoom(View view) {
        ZoomLevel randomZoomLevel = calcRandomZoomLevel();
        log("zoom " + view.getName() + ", zoom level = " + randomZoomLevel);

        if (rand.nextBoolean()) {
            view.setZoom(randomZoomLevel);
        } else {
            Point focusPos = randomCoPointOn(view);
            view.setZoom(randomZoomLevel, focusPos);
        }
    }

    private static ZoomLevel calcRandomZoomLevel() {
        double percentValue = 0;
        ZoomLevel level = null;
        while (percentValue < 49) {
            level = ZoomLevel.getRandomZoomLevel();
            percentValue = level.getPercent();
        }
        return level;
    }

    /**
     * Generates a random component-space point inside the visible region of the given view.
     */
    private static Point randomCoPointOn(View view) {
        return Rnd.pointInRect(view.getVisibleRegion());
    }

    private void randomZoomOut() {
        View view = Views.getActive();
        if (view != null) {
            log("zoom out " + view.getName());
            ZoomLevel newZoom = view.getZoomLevel().zoomOut();
            if (rand.nextBoolean()) {
                view.setZoom(newZoom);
            } else {
                Point mousePos = randomCoPointOn(view);
                view.setZoom(newZoom, mousePos);
            }
        }
    }

    private void repeatLastFilter() {
        log("repeat (dispatch Ctrl-F)");
        dispatchKey(VK_F, 'F', Ctrl.PRESSED);
    }

    private void randomUndoRedo() {
        if (History.canUndo()) {
            log("undo " + History.getEditToBeUndoneName());

            History.undo();

            // For some reason, redo might not be available even if we are right
            // after an undo which didn't throw a CannotUndoException.
            // This isn't a problem normally, because the RedoMenuItem
            // also checks History.canRedo() after an undo.
            if (!History.canRedo()) {
                return;
            }

            if (rand.nextInt(10) > 3) {
                log("redo " + History.getEditToBeRedoneName());
                History.redo();
            }
        }
    }

    private void randomCrop() {
        boolean enabled = SelectionActions.areEnabled();
        if (enabled) {
            runAction(SelectionActions.getCrop());
        }
    }

    private void randomFade() {
        if (!History.canFade()) {
            return;
        }
        int opacity = rand.nextInt(100);

        log("fade, opacity = " + opacity + " %");

        Fade fade = new Fade();
        fade.setOpacity(opacity);

        Drawable dr = Views.getActiveDrawable();
        dr.startFilter(fade, FILTER_WITHOUT_DIALOG);
    }

    private void randomizeToolSettings() {
        log("randomize tool settings for " + Tools.getActive());
        ToolSettingsPanelContainer.get().randomizeToolSettings();
    }

    private void arrangeWindows() {
        if (ImageArea.isActiveMode(TABS)) {
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

    private void toggleImageAreaMode() {
        log("toggle image area from " + ImageArea.getMode());
        ImageArea.toggleUI();
    }

    private void deselect() {
        if (SelectionActions.areEnabled()) {
            runAction(SelectionActions.getDeselect());
        }
    }

    private void showHideSelection(Robot robot) {
        log("showHideSelection");
        pressCtrlKey(robot, VK_H);
    }

    private void layerToCanvasSize() {
        log("layer to canvas size");
        Views.onActiveComp(Composition::activeLayerToCanvasSize);
    }

    private void fitCanvasToLayers() {
        log("fit canvas to layers");
        Views.onActiveComp(Composition::fitCanvasToLayers);
    }

    private void invertSelection() {
        if (SelectionActions.areEnabled()) {
            runAction(SelectionActions.getInvert());
        }
    }

    private void traceWithCurrentBrush() {
        if (canTrace()) {
            runAction(PathActions.traceWithBrushAction);
        }
    }

    private void traceWithCurrentEraser() {
        if (canTrace()) {
            runAction(PathActions.traceWithEraserAction);
        }
    }

    private void traceWithCurrentSmudge() {
        if (canTrace()) {
            runAction(PathActions.traceWithSmudgeAction);
        }
    }

    private static boolean canTrace() {
        Composition comp = Views.getActiveComp();
        if (comp == null) {
            return false;
        }
        return comp.hasActivePath() && comp.canDrawOnActiveTarget();
    }

    private void randomRotateFlip() {
        Action[] actions = {
            new Rotate(ANGLE_90),
            new Rotate(ANGLE_180),
            new Rotate(ANGLE_270),
            new Flip(HORIZONTAL),
            new Flip(VERTICAL)
        };
        runAction(Rnd.chooseFrom(actions));
    }

    private void activateRandomView() {
        View view = Views.activateRandomView();
        if (view != null) {
            log("activated random view " + view.getName());
        }
    }

    private void randomLayerOrderChange() {
        var comp = Views.getActiveComp();
        Runnable[] actions = {
            () -> moveActiveLayerToTop(comp),
            () -> moveActiveLayerToBottom(comp),
            () -> raiseLayerSelection(comp),
            () -> lowerLayerSelection(comp),
            () -> moveActiveLayerUp(comp),
            () -> moveActiveLayerDown(comp)
        };
        Rnd.chooseFrom(actions).run();
    }

    private void moveActiveLayerToTop(Composition comp) {
        log("layer order change: active to top");
        comp.getActiveHolder().moveActiveLayerToTop();
    }

    private void moveActiveLayerToBottom(Composition comp) {
        log("layer order change: active to bottom");
        comp.getActiveHolder().moveActiveLayerToBottom();
    }

    private void raiseLayerSelection(Composition comp) {
        log("layer selection change: raise selection");
        comp.getActiveHolder().raiseLayerSelection();
    }

    private void lowerLayerSelection(Composition comp) {
        log("layer selection change: lower selection");
        comp.getActiveHolder().lowerLayerSelection();
    }

    private void moveActiveLayerUp(Composition comp) {
        log("layer order change: active up");
        comp.getActiveHolder().reorderActiveLayer(true);
    }

    private void moveActiveLayerDown(Composition comp) {
        log("layer order change: active down");
        comp.getActiveHolder().reorderActiveLayer(false);
    }

    private void randomMergeDown() {
        var comp = Views.getActiveComp();
        Layer layer = comp.getActiveLayer();
        if (layer.getHolder().canMergeDown(layer)) {
            log("merge down " + layer.getName() + " in " + comp.getName());
            comp.mergeActiveLayerDown();
        }
    }

    private void randomFlattenImage() {
        var comp = Views.getActiveComp();
        log("flatten image " + comp.getName());
        comp.flattenImage();
    }

    private void randomLayerAddOrDelete() {
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

    private void randomlyTogglePanelVisibility() {
        WorkSpace workSpace = PixelitorWindow.get().getWorkSpace();
        Action[] actions = {
            workSpace.getHistogramsAction(),
            workSpace.getToolsAction(),
            workSpace.getLayersAction(),
            workSpace.getStatusBarAction(),
            workSpace.getAllAction()
        };
        runAction(Rnd.chooseFrom(actions));
    }

    private void randomCopy() {
        if (rand.nextBoolean()) {
            runAction(CopyAction.COPY_LAYER);
        } else {
            runAction(CopyAction.COPY_COMPOSITE);
        }
    }

    private void runAction(Action action) {
        String msg = String.format("action \"%s\" (class: \"%s\")",
            action.getValue(Action.NAME),
            action.getClass().getSimpleName());
        log(msg);

        action.actionPerformed(new ActionEvent("", 0, ""));
    }

    private void randomPaste() {
        if (pastedImagesCount > 3) {
            return;
        }
        PasteTarget target = switch (rand.nextInt(10)) {
            case 0 -> PasteTarget.NEW_IMAGE;
            case 1 -> PasteTarget.NEW_LAYER;
            case 2 -> Views.getActiveLayer().hasMask() ? PasteTarget.MASK : null;
            default -> null;
        };
        if (target != null) {
            runAction(new PasteAction(target));
            pastedImagesCount++;
        }
    }

    private void randomChangeLayerOpacityOrBlending() {
        Layer layer = Views.getActiveLayer();
        if (rand.nextBoolean()) {
            float opacity = layer.getOpacity();
            float f = rand.nextFloat();

            if (f > opacity) {
                // always increase
                log("increase opacity for " + layer.getName());
                layer.setOpacity(f, true, true);
            } else if (rand.nextFloat() > 0.75) { // sometimes decrease
                log("decrease opacity for " + layer.getName());
                layer.setOpacity(f, true, true);
            }
        } else {
            log("change layer blending mode for " + layer.getName());

            BlendingMode randomBM = Rnd.chooseFrom(layer.isGroup() ?
                BlendingMode.ALL_MODES : BlendingMode.LAYER_MODES);
            layer.setBlendingMode(randomBM, true, true);
        }
    }

    private void randomChangeLayerVisibility() {
        Layer layer = Views.getActiveLayer();
        boolean visible = layer.isVisible();
        if (rand.nextBoolean()) {
            if (!visible) {
                log("show layer");
                layer.setVisible(true, true, true);
            }
        } else {
            if (visible) {
                if (rand.nextFloat() > 0.8) { // sometimes hide
                    log("hide layer");
                    layer.setVisible(false, true, true);
                }
            }
        }
    }

    private void randomTool() {
        Tool tool;
        if (preferredTool != null) {
            if (rand.nextBoolean()) {
                tool = preferredTool;
            } else {
                tool = Tools.getRandomTool();
            }
        } else {
            tool = Tools.getRandomTool();

            // the move tool can cause out of memory errors, so don't test it
            if (tool == Tools.MOVE) {
                return;
            }
        }
        Tool activeTool = Tools.getActive();
        if (activeTool != tool) {
            log("tool click on " + activeTool);
            tool.activate();
        }
    }

    private void newRandomTextLayer() {
        Composition comp = Views.getActiveComp();
        MaskViewMode oldMaskViewMode = comp.getView().getMaskViewMode();
        Layer activeLayerBefore = comp.getActiveLayer();

        TextSettings settings = new TextSettings();
        settings.randomize();
        TextLayer textLayer = TextLayer.createNew(comp, settings);

        // has to be called explicitly, since no dialog will be shown
        textLayer.finalizeCreation(activeLayerBefore, oldMaskViewMode);

        log("new text layer: " + textLayer.getName());
    }

    private void newColorFillLayer() {
        log("new color fill layer");
        ColorFillLayer.createNew(Views.getActiveComp());
    }

    private void newGradientFillLayer() {
        log("new gradient fill layer");
        GradientFillLayer.createNew(Views.getActiveComp());
    }

    private void newShapesLayer() {
        log("new shapes layer");
        ShapesLayer.createNew(Views.getActiveComp());
    }

    private void newEmptyLayerGroup() {
        log("new empty layer group");
        Views.getActiveComp().getHolderForNewLayers().addEmptyGroup();
    }

    private void convertVisibleToGroup() {
        log("convert visible to group");
        Views.getActiveComp().getHolderForGrouping().convertVisibleLayersToGroup();
    }

    private void convertToSmartObject() {
        Layer layer = Views.getActiveLayer();
        if (!layer.isConvertibleToSmartObject()) {
            return;
        }
        log(String.format("Convert %s to Smart Object", layer.getTypeString()));
        layer.replaceWithSmartObject();
    }

    private void randomRasterizeLayer() {
        Layer layer = Views.getActiveLayer();
        if (layer.isRasterizable()) {
            log("rasterize " + layer.getTypeStringLC() + " " + layer.getName());
            layer.replaceWithRasterized();
        }
    }

    private void randomNewAdjustmentLayer() {
        log("new adj layer");
        var comp = Views.getActiveComp();
        var adjustmentLayer = new AdjustmentLayer(comp, "Invert", new Invert());
        comp.addWithHistory(adjustmentLayer, "New Random Adj Layer");
    }

    private void randomSetLayerMaskEditMode() {
        if (!Views.getActiveLayer().hasMask()) {
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

        dispatchKey(keyCode, keyChar, Ctrl.PRESSED);
    }

    // a random action on a layer mask (add, delete, apply, link/unlink)
    private void randomLayerMaskAction() {
        Layer layer = Views.getActiveLayer();
        if (!layer.hasMask()) {
            assert AddLayerMaskAction.INSTANCE.isEnabled();
            runAction(AddLayerMaskAction.INSTANCE);
        } else {
            assert !AddLayerMaskAction.INSTANCE.isEnabled();
            if (rand.nextFloat() < 0.2 && layer instanceof ContentLayer) {
                // 20% chance to toggle link status
                LayerMask mask = layer.getMask();
                if (mask.isLinked()) {
                    log("unlink layer mask");
                    mask.setLinked(false, true);
                } else {
                    log("re-link layer mask");
                    mask.setLinked(true, true);
                }
            } else if (layer instanceof ImageLayer imageLayer) {
                // for image layers, either apply or delete the mask
                if (rand.nextBoolean()) {
                    log("apply layer mask");
                    imageLayer.applyLayerMask(true);
                } else {
                    log("delete layer mask");
                    imageLayer.deleteMask(true);
                }
            } else {
                // for other layer types, just delete the mask
                log("delete layer mask");
                layer.deleteMask(true);
            }
        }
    }

    private ParametrizedFilter getRandomTweenFilter() {
        if (preferredTweenFilter != null) {
            assert preferredTweenFilter.isAnimatable();
            return preferredTweenFilter;
        }
        FilterAction filterAction = Filters.getRandomAnimationFilter();
        return (ParametrizedFilter) filterAction.getFilter();
    }

    private void randomEnlargeCanvas() {
        int north = rand.nextInt(3);
        int east = rand.nextInt(3);
        int south = rand.nextInt(3);
        int west = rand.nextInt(3);
        log(String.format("enlarge canvas north = %d, east = %d, south = %d, west = %d",
            north, east, south, west));
        var comp = Views.getActiveComp();
        new EnlargeCanvas(north, east, south, west).process(comp);
    }

    private void randomGuides() {
        var comp = Views.getActiveComp();
        float v = rand.nextFloat();
        if (v < 0.2) {
            log("clear guides");
            comp.clearGuides();
            return;
        }
        new Guides.Builder(comp.getView(), false)
            .build(false, this::randomGuidesSetup);
    }

    private void randomGuidesSetup(Guides guides) {
        if (rand.nextBoolean()) {
            log("add relative horizontal guide");
            guides.addHorRelative(rand.nextFloat());
        } else {
            log("add relative vertical guide");
            guides.addVerRelative(rand.nextFloat());
        }
    }

    private void reload(Robot robot) {
        if (rand.nextFloat() < 0.1) {
            var comp = Views.getActiveComp();
            if (comp.getFile() != null) {
                log("f12 reload");
                pressKey(robot, VK_F12);
            }
        }
    }

    private void deletePath() {
        if (!Tools.activeIsPathTool()) {
            return;
        }
        if (PathActions.deletePathAction.isEnabled()) {
            log("delete path");
            PathActions.deletePathAction.actionPerformed(null);
        }
    }

    /**
     * Dispatches a key-pressed event to the main window.
     */
    private static void dispatchKey(int keyCode, char keyChar, Ctrl ctrl) {
        int modifiers = ctrl.modify(0);
        var pw = PixelitorWindow.get();
        //noinspection MagicConstant
        pw.dispatchEvent(new KeyEvent(pw, KEY_PRESSED,
            System.currentTimeMillis(), modifiers, keyCode, keyChar));
    }

    private void setupActions(Robot robot) {
        // random move
        actionCaller.registerAction(10, () -> randomMove(robot));
        actionCaller.registerAction(20, () -> randomDrag(robot));
        actionCaller.registerAction(5, () -> randomClick(robot));
        actionCaller.registerAction(2, this::repeatLastFilter);
        actionCaller.registerAction(5, this::randomUndoRedo);
        actionCaller.registerAction(1, this::randomCrop);
        actionCaller.registerAction(1, this::randomFade);
        actionCaller.registerAction(2, this::randomizeToolSettings);
        actionCaller.registerAction(1, this::arrangeWindows);
        actionCaller.registerAction(3, this::toggleImageAreaMode);
        actionCaller.registerAction(1, this::randomColors);
        actionCaller.registerAction(3, this::randomFilter);
        actionCaller.registerAction(1, this::randomTween);
        actionCaller.registerAction(1, this::randomFitTo);
        actionCaller.registerAction(3, () -> randomKey(robot));
        actionCaller.registerAction(1, () -> reload(robot));
        actionCaller.registerAction(1, this::randomZoom);
        actionCaller.registerAction(1, this::randomZoomOut);
        actionCaller.registerAction(10, this::deselect);
        actionCaller.registerAction(1, () -> showHideSelection(robot));
        actionCaller.registerAction(1, this::layerToCanvasSize);
        actionCaller.registerAction(1, this::fitCanvasToLayers);
        actionCaller.registerAction(1, this::invertSelection);
        actionCaller.registerAction(1, this::traceWithCurrentBrush);
        actionCaller.registerAction(1, this::traceWithCurrentEraser);
        actionCaller.registerAction(1, this::traceWithCurrentSmudge);
        actionCaller.registerAction(1, this::randomRotateFlip);
        actionCaller.registerAction(5, this::activateRandomView);
        actionCaller.registerAction(1, this::randomLayerOrderChange);
        actionCaller.registerAction(5, this::randomMergeDown);
        actionCaller.registerAction(5, this::randomFlattenImage);
        actionCaller.registerAction(3, this::randomLayerAddOrDelete);
        actionCaller.registerAction(1, this::randomlyTogglePanelVisibility);
        if (ENABLE_COPY_PASTE) {
            actionCaller.registerAction(1, this::randomCopy);
            actionCaller.registerAction(1, this::randomPaste);
        }
        actionCaller.registerAction(1, this::randomChangeLayerOpacityOrBlending);
        actionCaller.registerAction(1, this::randomChangeLayerVisibility);
        actionCaller.registerAction(5, this::randomTool);
        actionCaller.registerAction(1, this::randomEnlargeCanvas);
        actionCaller.registerAction(2, this::newRandomTextLayer);
        actionCaller.registerAction(1, this::newColorFillLayer);
        actionCaller.registerAction(1, this::newGradientFillLayer);
        actionCaller.registerAction(1, this::newShapesLayer);
        actionCaller.registerAction(1, this::newEmptyLayerGroup);
        actionCaller.registerAction(1, this::convertVisibleToGroup);
        actionCaller.registerAction(1, this::convertToSmartObject);
        actionCaller.registerAction(5, this::randomRasterizeLayer);
        actionCaller.registerAction(4, this::randomGuides);
        actionCaller.registerAction(4, this::deletePath);

        if (TEST_ADJ_LAYERS) {
            actionCaller.registerAction(2, this::randomNewAdjustmentLayer);
        }

        actionCaller.registerAction(7, this::randomSetLayerMaskEditMode);
        actionCaller.registerAction(10, this::randomLayerMaskAction);
    }
}
