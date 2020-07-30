/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest;

import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import pixelitor.Composition;
import pixelitor.ExceptionHandler;
import pixelitor.OpenImages;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.HistogramsPanel;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.StatusBar;
import pixelitor.history.History;
import pixelitor.layers.LayersContainer;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PenTool;
import pixelitor.tools.pen.PenToolMode;
import pixelitor.tools.util.ArrowKey;
import pixelitor.utils.Rnd;
import pixelitor.utils.Threads;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.Ansi;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static java.awt.event.KeyEvent.*;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.*;
import static pixelitor.tools.Tools.*;
import static pixelitor.utils.Threads.calledOutsideEDT;
import static pixelitor.utils.test.RandomGUITest.EXIT_KEY_CHAR;
import static pixelitor.utils.test.RandomGUITest.PAUSE_KEY_CHAR;

/**
 * A standalone program which tests the tools with randomly
 * generated assertj-swing GUI actions. Not a unit test.
 */
public class RandomToolTest {
    private File inputDir;
    private final AppRunner app;
    private final Mouse mouse;
    private final Keyboard keyboard;

    private final Object resumeMonitor = new Object();
    private final CountDownLatch mainThreadExitLatch = new CountDownLatch(1);

    private volatile boolean paused = false;
    private volatile boolean stopped = false;

    private long testNr = 1;

    private List<Runnable> events;
    private final ArrowKey[] arrowKeys = ArrowKey.values();
    private final List<Double> testTimes = new ArrayList<>();

    private static final String[] simpleMultiLayerEdits = {
        "Rotate 90° CW", "Rotate 180°", "Rotate 90° CCW",
        "Flip Horizontal", "Flip Vertical"
    };

    private static final int[] TOOL_HOTKEYS = {
        VK_V,
        VK_C,
        VK_M,
        VK_B,
        VK_S,
        VK_E,
        VK_K,
        VK_G,
        VK_N,
        VK_I,
        VK_P,
        VK_U,
        VK_H,
        VK_Z,
    };
    private static final String[] ADD_MASK_MENU_COMMANDS = {
        "Add White (Reveal All)", "Add Black (Hide All)", "Add from Layer"};
    private static final String[] REMOVE_MASK_MENU_COMMANDS = {
        "Delete", "Apply"};

    public static void main(String[] args) {
        Utils.makeSureAssertionsAreEnabled();
        FailOnThreadViolationRepaintManager.install();
        RandomGUITest.setRunning(true);

        new RandomToolTest(args);
    }

    private RandomToolTest(String[] args) {
        parseCLArguments(args);

        EDT.run(this::setupPauseKey);
        EDT.run(this::setupExitKey);

        app = new AppRunner(inputDir, "b.jpg", "a.jpg");
        keyboard = app.getKeyboard();
        mouse = app.getMouse();
        ExceptionHandler.INSTANCE.addFirstHandler((t, e) -> {
            e.printStackTrace();
            keyboard.releaseModifierKeysFromAnyThread();
        });

        initEventList();

        app.runMenuCommand("Fit Space");
        app.runTests(this::mainLoop);

        // It's the final countdown...
        mainThreadExitLatch.countDown();
    }

    // the main loop is the test loop with pause-resume support
    private void mainLoop() {
        while (true) {
            try {
                testLoop();
            } catch (StoppedException e) {
                assert stopped;
                System.out.println("\n" + RandomToolTest.class.getSimpleName() + " stopped.");

                var stats = testTimes.stream()
                    .mapToDouble(s -> s)
                    .summaryStatistics();
                System.out.printf("time stats: count = %d, min = %.2fs, max = %.2fs, avg = %.2fs%n",
                    stats.getCount(), stats.getMin(), stats.getMax(), stats.getAverage());

                return; // if stopped, then exit the main loop
            } catch (PausedException e) {
                // do nothing
            }

            assert paused;
            keyboard.releaseModifierKeys();
            System.out.println("\n" + RandomToolTest.class.getSimpleName() + " paused.");

            // stay paused until a signal from the EDT indicates
            // that the paused test should be resumed
            waitForResumeSignal();
            assert !paused;
        }
    }

    private void waitForResumeSignal() {
        synchronized (resumeMonitor) {
            try {
                while (paused) {
                    resumeMonitor.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void testLoop() {
        List<Tool> preferredTools = List.of();

        // exit this infinite loop only by throwing an exception
        while (true) {
            Tool selectedTool;
            if (preferredTools.isEmpty()) {
                // there is no preferred tool, each tool gets equal chance
                selectedTool = getRandomTool();
            } else {
                // with 50% probability force using a preferred tool
                if (Rnd.nextBoolean()) {
                    selectedTool = Rnd.chooseFrom(preferredTools);
                } else {
                    selectedTool = getRandomTool();
                }
            }
            long startTime = System.nanoTime();

            testNr++;
            testWithTimeout(selectedTool);

            double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf("Test \u001B[31m%d\u001B[0m (%s) took %.2f s%n",
                testNr, selectedTool.getName(), estimatedSeconds);
            testTimes.add(estimatedSeconds);
        }
    }

    private void testWithTimeout(Tool tool) {
        var future = CompletableFuture.runAsync(() -> testWithCleanup(tool));
        try {
            future.get(10, MINUTES);
        } catch (InterruptedException e) {
            stopped = true;
            System.err.println(AppRunner.getCurrentTimeHMS() + ": task unexpectedly interrupted, exiting");
            exitInNewThread();
        } catch (TimeoutException e) {
            stopped = true;
            System.err.printf("%s: task unexpectedly timed out, exiting.%n" +
                    "Active comp is: %s%n",
                AppRunner.getCurrentTimeHMS(),
                EDT.active(Composition::toString));
            exitInNewThread();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TestControlException) {
                // it's OK
                throw (TestControlException) cause;
            }

            // something bad happened
            cause.printStackTrace();
            stopped = true;
            exitInNewThread();
        }
    }

    private void testWithCleanup(Tool tool) {
        try {
            test(tool);
        } catch (PausedException e) {
            // do the cleanup if it is paused to that
            // it starts in a clean state if resumed later
            assert paused;

            paused = false; // prevent throwing another exception during the cleanup
            cleanupAfterTool();
            paused = true;

            throw e;
        }
    }

    private void test(Tool tool) {
        Utils.sleep(200, MILLISECONDS);
        activate(tool);
        randomizeToolSettings(tool);
        GlobalEvents.assertDialogNestingIs(0);

        // set the source point for the clone tool
        if (tool == CLONE) {
            setSourceForCloneTool();
        }

        randomEvents();
        dragRandomly();
        randomEvents();
        pushToolButtons();
        GlobalEvents.assertDialogNestingIs(0);
        randomEvents();

        cleanupAfterTool();
        GlobalEvents.assertDialogNestingIs(0);

        Utils.sleep(200, MILLISECONDS);
        checkControlVariables();
    }

    private void activate(Tool tool) {
        boolean toolsShown = EDT.call(() -> PixelitorWindow.get().areToolsShown());
        if (toolsShown) {
            log("activating " + tool.getName() + " by clicking on the button");
            app.clickTool(tool);
        } else {
            log("activating " + tool.getName() + " with keyboard");
            keyboard.pressChar(tool.getActivationKey());
        }
    }

    private void randomizeToolSettings(Tool tool) {
        log("randomize the settings of " + tool.getName());
        EDT.run(ToolSettingsPanelContainer.get()::randomizeToolSettings);

        if (tool == PEN && PEN.getMode() == PenToolMode.BUILD) {
            // prevent paths getting too large
            log("removing the path");
            Rnd.withProbability(0.5, () -> EDT.run(PEN::removePath));
        }
    }

    private void initEventList() {
        events = new ArrayList<>();

        addEvent(this::click, "click");

        addEvent(this::ctrlClick, "ctrlClick");
        addEvent(this::altClick, "altClick");
        addEvent(this::shiftClick, "shiftClick");
        addEvent(this::doubleClick, "doubleClick");
        addEvent(this::pressEnter, "pressEnter");
        addEvent(this::pressEsc, "pressEsc");
        addEvent(this::pressTab, "pressTab");
        addEvent(this::pressCtrlTab, "pressCtrlTab");
        addEvent(this::nudge, "nudge");
        addEvent(this::possiblyUndoRedo, "possiblyUndoRedo");
        addEvent(this::randomMultiLayerEdit, "randomMultiLayerEdit");
        addEvent(this::randomShowHide, "randomShowHide");
        addEvent(this::randomMaskEvent, "randomMaskEvent");

//        addEvent(this::randomKeyboardToolSwitch, "randomKeyboardToolSwitch");
//        addEvent(this::changeUI, "changeUI");

        // breaks assertj?
//        addEvent(this::changeMaskView, "changeMaskView");
    }

    private void addEvent(Runnable event, String name) {
        events.add(new MeasuredTask(event, name));
    }

    private void randomEvents() {
        Collections.shuffle(events);
        for (Runnable event : events) {
            Rnd.withProbability(0.2, event);
            keyboard.assertModifiersAreReleased();
        }
    }

    private void pressEnter() {
        log("pressing Enter");
        keyboard.pressEnter();
    }

    private void pressEsc() {
        log("pressing Esc");
        keyboard.pressEsc();
    }

    private void pressCtrlTab() {
        log("pressing Ctrl-Tab");
        keyboard.pressCtrlTab();
    }

    private void pressTab() {
        log("pressing Tab");
        keyboard.pressTab();
    }

    private void nudge() {
        ArrowKey randomArrowKey = Rnd.chooseFrom(arrowKeys);
        log("nudging: " + randomArrowKey);

        keyboard.nudge(randomArrowKey);
    }

    private void cleanupAfterTool() {
        Tool tool = EDT.call(Tools::getCurrent);
        log("cleaning up after " + tool.getName());

        var comp = EDT.getComp();
        if (tool == MOVE || tool == CROP) {
            if (comp.getNumLayers() > 1) {
                flattenImage();
            }
        }
        if (EDT.getActiveSelection() != null) {
            Rnd.withProbability(0.2, this::deselect);
        }

        if (tool == ZOOM) {
            Rnd.withProbability(0.5, this::actualPixels);
        }

        Rnd.withProbability(0.05, this::reload);
        randomizeColors();
        cutBigLayersIfNecessary(comp);
        setStandardSize();

        // this shouldn't be necessary
        keyboard.releaseModifierKeys();
    }

    private void reload() {
        log("reloading the image");
        app.runMenuCommand("Reload");
    }

    private void randomizeColors() {
        Tool tool = EDT.call(Tools::getCurrent);
        if (tool == ZOOM || tool == HAND || tool == CROP || tool == SELECTION || tool == PEN) {
            return;
        }
        log("randomizing colors");
        keyboard.randomizeColors();
    }

    // might be necessary because of the croppings
    private void setStandardSize() {
        var canvas = EDT.active(Composition::getCanvas);
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        boolean standardSize = canvasWidth == 770 && canvasHeight == 600;
        if (standardSize) {
            return;
        }

        boolean rotatedSize = canvasWidth == 600 && canvasHeight == 770;
        if (rotatedSize) {
            return;
        }

        log(format("resizing from %dx%d to 770x600",
            canvasWidth, canvasHeight));
        app.resize(770, 600);
    }

    private void deselect() {
        log("deselecting");
        Utils.sleep(200, MILLISECONDS);
        keyboard.deselect();
    }

    private void actualPixels() {
        log("actual pixels");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Actual Pixels");
    }

    private void dragRandomly() {
        int numDrags = Rnd.intInRange(1, 5);
        for (int i = 0; i < numDrags; i++) {
            Utils.sleep(200, MILLISECONDS);
            mouse.moveRandomlyWithinCanvas();

            boolean ctrlPressed = Rnd.withProbability(0.25, keyboard::pressCtrl);
            boolean altPressed = Rnd.withProbability(0.25, keyboard::pressAlt);
            boolean shiftPressed = Rnd.withProbability(0.25, keyboard::pressShift);
            String msg = "random ";
            if (ctrlPressed) {
                msg += "ctrl-";
            }
            if (altPressed) {
                msg += "alt-";
            }
            if (shiftPressed) {
                msg += "shift-";
            }
            msg += "drag";
            log(msg);

            Utils.sleep(200, MILLISECONDS);
            mouse.dragRandomlyWithinCanvas();

            if (ctrlPressed) {
                keyboard.releaseCtrl();
            }
            if (altPressed) {
                keyboard.releaseAlt();
            }
            if (shiftPressed) {
                keyboard.releaseShift();
            }

            possiblyUndoRedo();
            keyboard.assertModifiersAreReleased();
        }
    }

    private void possiblyUndoRedo() {
        if (!EDT.call(History::canUndo)) {
            return;
        }

        boolean undone = Rnd.withProbability(0.5, this::undo);
        if (undone) {
            Utils.sleep(200, MILLISECONDS);
            assert EDT.call(History::canRedo);
            Rnd.withProbability(0.5, this::redo);
        }
    }

    private void click() {
        log("random click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomClick();
    }

    private void ctrlClick() {
        log("random ctrl-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomCtrlClick();
    }

    private void altClick() {
        log("random alt-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomAltClick();
    }

    private void shiftClick() {
        log("random shift-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomShiftClick();
    }

    private void doubleClick() {
        log("random double click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomDoubleClick();
    }

    private void undo() {
        String editName = EDT.call(History::getEditToBeUndoneName);
        log("random undo " + Ansi.yellow(editName));

        keyboard.undo();
    }

    private void redo() {
        String editName = EDT.call(History::getEditToBeRedoneName);
        log("random redo " + Ansi.yellow(editName));

        keyboard.redo();
    }

    private void randomMultiLayerEdit() {
        String command = Rnd.chooseFrom(simpleMultiLayerEdits);
        log(command);
        app.runMenuCommand(command);
    }

    private void parseCLArguments(String[] args) {
        assert args.length > 0 : "missing CL argument";
        inputDir = new File(args[0]);
        assert inputDir.exists() : "input dir doesn't exist";
        assert inputDir.isDirectory() : "input dir is not a directory";
    }

    private void log(String msg) {
        checkControlVariables();

        Tool tool = EDT.call(Tools::getCurrent);
        String toolInfo = tool.getName();

        String stateInfo = EDT.call(tool::getStateInfo);
        if (stateInfo != null) {
            toolInfo += (" [" + stateInfo + "]");
        }

        String printed = Ansi.red(testNr + ".") + " " + Ansi.blue(toolInfo + ": ") + msg;
        if (EDT.getActiveSelection() != null) {
            printed += Ansi.red(" SEL");
        }
        if (EDT.active(Composition::getBuiltSelection) != null) {
            printed += Ansi.red(" BuiltSEL");
        }
        System.out.println(printed);
    }

    private void checkControlVariables() {
        if (paused) {
            throw new PausedException();
        }
        if (stopped) {
            throw new StoppedException();
        }
    }

    private void cutBigLayersIfNecessary(Composition comp) {
        Rectangle imgSize = EDT.call(comp::getMaxImageSize);
        Dimension canvasSize = EDT.call(() ->
            comp.getCanvas().getSize());

        if (imgSize.width > 3 * canvasSize.width || imgSize.height > 3 * canvasSize.height) {
            // needs to be cut, otherwise there is a risk that
            // the image will grow too large during the next resize
            cutBigLayers();
        } else if (imgSize.width > canvasSize.width || imgSize.height > canvasSize.height) {
            Rnd.withProbability(0.3, this::cutBigLayers);
        }
    }

    private void cutBigLayers() {
        log("layers to canvas size");
        Utils.sleep(200, MILLISECONDS);

        int numImageLayers = EDT.call(() -> OpenImages.fromActiveComp(
            Composition::getNumImageLayers));
        if (numImageLayers == 1) {
            app.runMenuCommand("Layer to Canvas Size");
        } else if (numImageLayers > 1) {
            EDT.run(() -> OpenImages.onActiveComp(
                Composition::allImageLayersToCanvasSize));
        }
    }

    private void flattenImage() {
        log("merge layers");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Flatten Image");
    }

    private void setSourceForCloneTool() {
        log("setting source point");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomAltClick();
    }

    private void pushToolButtons() {
        boolean toolsShown = EDT.call(() -> PixelitorWindow.get().areToolsShown());
        if (!toolsShown) {
            return;
        }

        Tool tool = EDT.call(Tools::getCurrent);

        // TODO Clone Transform
        // TODO Shape: "Stroke Settings...", "Effects...", "Convert to Selection"
        if (tool == BRUSH || tool == ERASER) {
            Rnd.withProbability(0.2, this::changeLazyMouseSetting);
            Rnd.withProbability(0.2, this::changeBrushSetting);
        } else if (tool == CLONE || tool == SMUDGE) {
            Rnd.withProbability(0.2, this::changeLazyMouseSetting);
        } else if (tool == PEN) {
            Rnd.withProbability(0.4, this::clickPenToolButton);
        } else if (tool == ZOOM || tool == HAND) {
            Rnd.withProbability(0.2, this::clickZoomOrHandToolButton);
        } else if (tool == CROP) {
            Rnd.withProbability(0.5, this::clickCropToolButton);
        } else if (tool == SELECTION) {
            Rnd.withProbability(0.5, this::clickSelectionToolButton);
        }
    }

    private void changeLazyMouseSetting() {
        app.findButton("lazyMouseDialogButton").click();
        var dialog = app.findDialogByTitle("Lazy Mouse Settings");

        log("changing the lazy mouse setting");
        Utils.sleep(200, MILLISECONDS);
        dialog.checkBox().click();
        Utils.sleep(200, MILLISECONDS);

        dialog.button("ok").click();
    }

    private void changeBrushSetting() {
        var settingsButton = app.findButtonByText("Settings...");
        if (!settingsButton.isEnabled()) {
            return;
        }

        log("changing the brush setting");
        settingsButton.click();
        var settingsDialog = app.findDialogByTitleStartingWith("Settings for the");

        // TODO

        settingsDialog.button("ok").click();
    }

    private void clickPenToolButton() {
        Path path = EDT.call(PenTool::getPath);
        if (path == null) {
            return;
        }
        var canvas = EDT.getCanvas();
        if (!canvas.getBounds().contains(path.getImBounds())) {
            // if the path is outside, then it can potentially take a very long time
            return;
        }

        String[] texts = {
            "Stroke with Current Brush",
            "Stroke with Current Eraser",
            "Stroke with Current Smudge",
            "Convert to Selection"
        };
        clickRandomToolButton(texts);
    }

    private void clickZoomOrHandToolButton() {
        String[] texts = {
            "Actual Pixels",
            "Fit Space",
            "Fit Width",
            "Fit Height"
        };
        clickRandomToolButton(texts);
    }

    private void clickCropToolButton() {
        String[] texts = {"Crop", "Cancel",};
        clickRandomToolButton(texts);
    }

    private void clickSelectionToolButton() {
        String[] texts = {
            "Crop Selection",
            "Convert to Path",
        };
        clickRandomToolButton(texts);
    }

    private void clickRandomToolButton(String[] texts) {
        String text = Rnd.chooseFrom(texts);
        var button = app.findButtonByText(text);
        if (button.isEnabled()) {
            log("Clicking " + Ansi.cyan(text));
            Utils.sleep(200, MILLISECONDS);
            button.click();
            Utils.sleep(500, MILLISECONDS);
        }
    }

    // The Tab hotkey is tested separately,
    // this is for hiding/showing with menu shortcuts
    private void randomShowHide() {
        int randomNumber = Rnd.nextInt(10);
        switch (randomNumber) {
            case 0 -> randomShowHide("Tools", () -> PixelitorWindow.get().areToolsShown());
            case 1 -> randomShowHide("Layers", LayersContainer::areLayersShown);
            case 2 -> randomShowHide("Histograms", HistogramsPanel::isShown);
            case 3 -> randomShowHide("Status Bar", StatusBar::isShown);
            // by default do nothing, this doesn't have to be tested all the time
        }
    }

    private void randomShowHide(String name, Callable<Boolean> checkCurrent) {
        boolean shownBefore = EDT.call(checkCurrent);
        String cmd;
        if (shownBefore) {
            cmd = "Hide " + name;
        } else {
            cmd = "Show " + name;
        }
        log(cmd);
        app.runMenuCommand(cmd);

        boolean shownAfter = EDT.call(checkCurrent);
        assert shownAfter == !shownBefore;
    }

    private void randomKeyboardToolSwitch() {
        int keyCode = Rnd.chooseFrom(TOOL_HOTKEYS);
        log("random keyboard tool switch using " + Ansi.cyan(getKeyText(keyCode)));
        cleanupAfterTool();
        keyboard.press(keyCode);
    }

    private void changeUI() {
        log("changing the UI (Ctrl-K)");
        keyboard.ctrlPress(VK_K);
    }

    private void changeMaskView() {
        if (!EDT.activeLayerHasMask()) {
            return;
        }
        int num = Rnd.nextInt(4) + 1;
        log("changing the mask view mode: Ctrl-" + num);
        switch (num) {
            case 1 -> keyboard.pressCtrlOne();
            case 2 -> keyboard.pressCtrlTwo();
            case 3 -> keyboard.pressCtrlThree();
            case 4 -> keyboard.pressCtrlFour();
        }
    }

    private void randomMaskEvent() {
        String command;
        if (EDT.activeLayerHasMask()) {
            command = Rnd.chooseFrom(REMOVE_MASK_MENU_COMMANDS);
        } else {
            command = Rnd.chooseFrom(ADD_MASK_MENU_COMMANDS);
        }
        log("randomMaskEvent: " + command);
        app.runMenuCommand(command);
    }

    private void setupPauseKey() {
        GlobalEvents.addHotKey(PAUSE_KEY_CHAR, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (paused) {
                    System.err.println(PAUSE_KEY_CHAR + " pressed, starting again.");
                    paused = false;
                    synchronized (resumeMonitor) {
                        // wake up the waiting main thread from the EDT
                        resumeMonitor.notify();
                    }
                } else {
                    System.err.println(PAUSE_KEY_CHAR + " pressed, pausing.");
                    paused = true;
                }
            }
        });
    }

    private void setupExitKey() {
        // This key not only pauses the testing, but also exits the app
        GlobalEvents.addHotKey(EXIT_KEY_CHAR, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.err.println("\nexiting because '" + EXIT_KEY_CHAR + "' was pressed");

                // we are on the EDT now, and before exiting
                // we want to wait until the modifier keys are released
                exitInNewThread();
            }
        });
    }

    private void exitInNewThread() {
        new Thread(this::exitGracefully).start();
    }

    private void exitGracefully() {
        // avoid blocking the EDT
        assert calledOutsideEDT() : "on EDT";
        // this should also not be called from the main thread
        assert !Threads.threadName().equals("main");

        if (paused) {
            // if already paused, then we can exit immediately
            keyboard.releaseModifierKeys();
            System.exit(0);
        }

        // signal the main thread to finish ASAP
        stopped = true;

        // wait for the main thread to complete in a consistent state,
        // (with the modifier keys released), and finish
        try {
            boolean ok = mainThreadExitLatch.await(30, SECONDS);
            if (!ok) {
                System.err.println("Timed out waiting for the main thread to finish");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            System.err.println("Unexpected InterruptedException");
            System.exit(2);
        }

        // The EDT is still running => force the exit
        System.exit(0);
    }
}

class TestControlException extends RuntimeException {
}

class PausedException extends TestControlException {
}

class StoppedException extends TestControlException {
}

class MeasuredTask implements Runnable {
    private static final boolean TRACK_MEMORY = false;
    private final Runnable task;
    private final String name;

    public MeasuredTask(Runnable task, String name) {
        this.task = task;
        this.name = name;
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();
        long startFree;
        if (TRACK_MEMORY) {
            startFree = Runtime.getRuntime().freeMemory();
        }

        task.run();

        double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        long allocatedKB;
        if (TRACK_MEMORY) {
            allocatedKB = (startFree - Runtime.getRuntime().freeMemory()) / 1024;
            System.out.printf("%s was running for %.2f s, allocated %d kBytes%n", name, seconds, allocatedKB);
            if (allocatedKB > 100 * 1024) { // more than 100 mega: why?
                System.exit(1);
            }
        } else {
            System.out.printf("%s was running for %.2f s%n", name, seconds);
        }
    }
}