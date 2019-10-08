/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JButtonFixture;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ExceptionHandler;
import pixelitor.gui.GlobalEvents;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PenTool;
import pixelitor.tools.pen.PenToolMode;
import pixelitor.tools.util.ArrowKey;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.Ansi;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.CROP;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.HAND;
import static pixelitor.tools.Tools.MOVE;
import static pixelitor.tools.Tools.PEN;
import static pixelitor.tools.Tools.SELECTION;
import static pixelitor.tools.Tools.SMUDGE;
import static pixelitor.tools.Tools.ZOOM;

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

    private List<Consumer<Tool>> events;
    private final ArrowKey[] arrowKeys = ArrowKey.values();
    private final Robot robot;

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

        app = new AppRunner(inputDir);
        keyboard = app.getKeyboard();
        mouse = app.getMouse();
        robot = app.getRobot();
        ExceptionHandler.INSTANCE.addFirstHandler((t, e) -> {
            e.printStackTrace();
            keyboard.releaseModifierKeysFromAnyThread();
        });

        initEventList();

        app.runTests(this::mainLoop);

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
        Tool[] preferredTools = {};

        // exit this infinite loop only by throwing an exception
        while (true) {
            if (preferredTools.length == 0) {
                // there is no preferred tool, each tool gets equal chance
                testToolWithTimeout(Tools.getRandomTool(), testNr++);
            } else {
                // with 50% probability force using a preferred tool
                if (Rnd.nextBoolean()) {
                    testToolWithTimeout(Rnd.chooseFrom(preferredTools), testNr++);
                } else {
                    testToolWithTimeout(Tools.getRandomTool(), testNr++);
                }
            }
        }
    }

    private void testToolWithTimeout(Tool tool, long testNr) {
        CompletableFuture<Void> cf = CompletableFuture.runAsync(
                () -> testToolWithCleanup(tool, testNr));
        try {
            cf.get(1, MINUTES);
        } catch (InterruptedException e) {
            stopped = true;
            System.err.println("task unexpectedly interrupted, exiting");
            exitInNewThread();
        } catch (TimeoutException e) {
            stopped = true;
            System.err.println("task unexpectedly timed out, exiting");
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

    private void testToolWithCleanup(Tool tool, long testNr) {
        try {
            testTool(tool, testNr);
        } catch (PausedException e) {
            // do the cleanup if it is paused to that
            // it starts in a clean state if resumed later
            assert paused;

            paused = false; // prevent throwing another exception during the cleanup
            cleanupAfterToolTest(tool);
            paused = true;

            throw e;
        }
    }

    private void testTool(Tool tool, long testNr) {
        Utils.sleep(200, MILLISECONDS);
        activate(tool, testNr);
        randomizeToolSettings(tool);

        // set the source point for the clone tool
        if (tool == CLONE) {
            setSourceForCloneTool();
        }

        randomEvents(tool);
        dragRandomly(tool);
        randomEvents(tool);
        pushToolButtons(tool);
        cleanupAfterToolTest(tool);

        Utils.sleep(200, MILLISECONDS);
        checkControlVariables();
    }

    private void activate(Tool tool, long testNr) {
        log(tool, "activating, starting test " + Ansi.red(testNr + "."));
        app.clickTool(tool);
    }

    private void randomizeToolSettings(Tool tool) {
        log(tool, "randomize tool settings");
        EDT.run(ToolSettingsPanelContainer.INSTANCE::randomizeToolSettings);

        if (tool == PEN && PEN.getMode() == PenToolMode.BUILD) {
            // prevent paths getting too large
            log(tool, "removing the path");
            Rnd.withProbability(0.5, () -> EDT.run(PEN::removePath));
        }
    }

    private void initEventList() {
        events = new ArrayList<>();

        events.add(this::click);
        events.add(this::ctrlClick);
        events.add(this::altClick);
        events.add(this::shiftClick);
        events.add(this::doubleClick);
        events.add(this::pressEnter);
        events.add(this::pressEsc);
        events.add(this::nudge);
        events.add(this::possiblyUndoRedo);
    }

    private void randomEvents(Tool tool) {
        Collections.shuffle(events);
        for (Consumer<Tool> event : events) {
            Rnd.withProbability(0.3, () -> event.accept(tool));
            keyboard.assertModifiersAreReleased();
        }
    }

    private void pressEnter(Tool tool) {
        log(tool, "pressing Enter");
        keyboard.pressEnter();
    }

    private void pressEsc(Tool tool) {
        log(tool, "pressing Esc");
        keyboard.pressEsc();
    }

    private void nudge(Tool tool) {
        ArrowKey randomArrowKey = Rnd.chooseFrom(arrowKeys);
        log(tool, "nudging: " + randomArrowKey);

        keyboard.nudge(randomArrowKey);
    }

    private void cleanupAfterToolTest(Tool tool) {
        log(tool, "starting final cleanup");

        Composition comp = EDT.getComp();
        if (tool == MOVE || tool == CROP) {
            if (comp.getNumLayers() > 1) {
                flattenImage(tool);
            }
        }
        if (EDT.getActiveSelection() != null) {
            Rnd.withProbability(0.2, () -> deselect(tool));
        }

        if (tool == ZOOM) {
            Rnd.withProbability(0.5, () -> actualPixels(tool));
        }

        Rnd.withProbability(0.05, () -> reload(tool));
        randomizeColors(tool);
        cutBigLayerIfNecessary(tool, comp);
        setStandardSize(tool);

        // this shouldn't be necessary
        keyboard.releaseModifierKeys();
    }

    private void reload(Tool tool) {
        log(tool, "reloading the image");
        app.runMenuCommand("Reload");
    }

    private void randomizeColors(Tool tool) {
        if (tool == ZOOM || tool == HAND || tool == CROP || tool == SELECTION || tool == PEN) {
            return;
        }
        log(tool, "randomizing colors");
        keyboard.randomizeColors();
    }

    // might be necessary because of the croppings
    private void setStandardSize(Tool tool) {
        Canvas canvas = EDT.active(Composition::getCanvas);
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();
        if (canvasWidth != 770 || canvasHeight != 600) {
            log(tool, "resizing back to 770x600");
            app.resize(770, 600);
        }
    }

    private void deselect(Tool tool) {
        log(tool, "deselecting");
        Utils.sleep(200, MILLISECONDS);
        keyboard.deselect();
    }

    private void actualPixels(Tool tool) {
        log(tool, "actual pixels");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Actual Pixels");
    }

    private void dragRandomly(Tool tool) {
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
            log(tool, msg);

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

            possiblyUndoRedo(tool);
            keyboard.assertModifiersAreReleased();
        }
    }

    private void possiblyUndoRedo(Tool tool) {
        if (!EDT.call(History::canUndo)) {
            return;
        }

        boolean undone = Rnd.withProbability(0.5, () -> undo(tool));
        if (undone) {
            Rnd.withProbability(0.5, () -> redo(tool));
        }
    }

    private void click(Tool tool) {
        log(tool, "random click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomClick();
    }

    private void ctrlClick(Tool tool) {
        log(tool, "random ctrl-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomCtrlClick();
    }

    private void altClick(Tool tool) {
        log(tool, "random alt-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomAltClick();
    }

    private void shiftClick(Tool tool) {
        log(tool, "random shift-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomShiftClick();
    }

    private void doubleClick(Tool tool) {
        log(tool, "random double click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomDoubleClick();
    }

    private void undo(Tool tool) {
        String editName = EDT.call(History::getEditToBeUndoneName);
        log(tool, "random undo " + Ansi.yellow(editName));

        keyboard.undo();
    }

    private void redo(Tool tool) {
        String editName = EDT.call(History::getEditToBeRedoneName);
        log(tool, "random redo " + Ansi.yellow(editName));

        keyboard.redo();
    }

    private void parseCLArguments(String[] args) {
        assert args.length > 0 : "missing CL argument";
        inputDir = new File(args[0]);
        assert inputDir.exists() : "input dir doesn't exist";
        assert inputDir.isDirectory() : "input dir is not a directory";
    }

    private void log(Tool tool, String msg) {
        checkControlVariables();

        String toolInfo = tool.getName();
        String stateInfo = EDT.call(tool::getStateInfo);
        if (stateInfo != null) {
            toolInfo += (" [" + stateInfo + "]");
        }

        String printed = Ansi.blue(toolInfo + ": ") + msg;
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

    private void cutBigLayerIfNecessary(Tool tool, Composition comp) {
        ImageLayer layer = (ImageLayer) comp.getActiveLayer();
        int tx = layer.getTX();
        int ty = layer.getTY();
        if (tx < -comp.getCanvasImWidth() || ty < -comp.getCanvasImHeight()) {
            cutBigLayer(tool);
        } else if (tx < 0 || ty < 0) {
            Rnd.withProbability(0.3, () -> cutBigLayer(tool));
        }
    }

    private void cutBigLayer(Tool tool) {
        log(tool, "layer to canvas size");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Layer to Canvas Size");
    }

    private void flattenImage(Tool tool) {
        log(tool, "merge layers");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Flatten Image");
    }

    private void setSourceForCloneTool() {
        log(CLONE, "setting source point");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomAltClick();
    }

    private void pushToolButtons(Tool tool) {
        Tool actual = EDT.call(Tools::getCurrent);
        if (actual != tool) {
            // this can happen in rare cases,
            // if an undo/redo also changes the tool
            return;
        }

        // TODO Clone Transform
        // TODO Shape: "Stroke Settings...", "Effects...", "Convert to Selection"
        if (tool == BRUSH || tool == ERASER) {
            Rnd.withProbability(0.2, () -> changeLazyMouseSetting(tool));
            Rnd.withProbability(0.2, () -> changeBrushSetting(tool));
        } else if (tool == CLONE || tool == SMUDGE) {
            Rnd.withProbability(0.2, () -> changeLazyMouseSetting(tool));
        } else if (tool == PEN) {
            Rnd.withProbability(0.4, this::clickPenToolButton);
        } else if (tool == ZOOM || tool == HAND) {
            Rnd.withProbability(0.2, () -> clickZoomOrHandToolButton(tool));
        } else if (tool == CROP) {
            Rnd.withProbability(0.5, this::clickCropToolButton);
        } else if (tool == SELECTION) {
            Rnd.withProbability(0.5, this::clickSelectionToolButton);
        }
    }

    private void changeLazyMouseSetting(Tool tool) {
        app.findButton("lazyMouseDialogButton").click();
        DialogFixture dialog = app.findDialogByTitle("Lazy Mouse Settings");

        log(tool, "changing the lazy mouse setting");
        Utils.sleep(200, MILLISECONDS);
        dialog.checkBox().click();
        Utils.sleep(200, MILLISECONDS);

        dialog.button("ok").click();
    }

    private void changeBrushSetting(Tool tool) {
        JButtonFixture button = app.findButtonByText("Settings...");
        if (!button.isEnabled()) {
            return;
        }

        log(tool, "changing the brush setting");
        button.click();
        DialogFixture dialog = app.findDialogByTitleStartingWith("Settings for the");

        // TODO

        dialog.button("ok").click();
    }

    private void clickPenToolButton() {
        Path path = EDT.call(PenTool::getPath);
        if (path == null) {
            return;
        }
        Canvas canvas = EDT.getCanvas();
        if (!canvas.getImBounds().contains(path.getImBounds())) {
            // if the path is outside, then it can potentially take a very long time
            return;
        }

        String[] texts = {
                "Stroke with Current Brush",
                "Stroke with Current Eraser",
                "Stroke with Current Smudge",
                "Convert to Selection"
        };
        clickRandomToolButton(PEN, texts);
    }

    private void clickZoomOrHandToolButton(Tool tool) {
        String[] texts = {
                "Actual Pixels",
                "Fit Space",
                "Fit Width",
                "Fit Height"
        };
        clickRandomToolButton(tool, texts);
    }

    private void clickCropToolButton() {
        String[] texts = {
                "Crop",
                "Cancel",
        };
        clickRandomToolButton(CROP, texts);
    }

    private void clickSelectionToolButton() {
        String[] texts = {
                "Crop Selection",
                "Convert to Path",
        };
        clickRandomToolButton(SELECTION, texts);
    }

    private void clickRandomToolButton(Tool tool, String[] texts) {
        String text = Rnd.chooseFrom(texts);
        JButtonFixture button = app.findButtonByText(text);
        if (button.isEnabled()) {
            log(tool, "Clicking " + Ansi.cyan(text));
            Utils.sleep(200, MILLISECONDS);
            button.click();
            Utils.sleep(500, MILLISECONDS);
        }
    }

    private void setupPauseKey() {
        char pauseKey = 'a';
        GlobalEvents.addHotKey(pauseKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (paused) {
                    System.err.println(pauseKey + " pressed, starting again.");
                    paused = false;
                    synchronized (resumeMonitor) {
                        // wake up the waiting main thread from the EDT
                        resumeMonitor.notify();
                    }
                } else {
                    System.err.println(pauseKey + " pressed, pausing.");
                    paused = true;
                }
            }
        });
    }

    private void setupExitKey() {
        // This key not only pauses the testing, but also exits the app
        char exitKey = 'j';
        GlobalEvents.addHotKey(exitKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.err.println("\nexiting because '" + exitKey + "' was pressed");

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
        assert !EventQueue.isDispatchThread();
        // this should also not be called from the main thread
        assert !Thread.currentThread().getName().equals("main");

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
            System.exit(1);
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
