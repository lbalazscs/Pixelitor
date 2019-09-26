package pixelitor.guitest;

import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JButtonFixture;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.GlobalEventWatch;
import pixelitor.gui.MappedKey;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.pen.PenToolMode;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.Ansi;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
 * A standalone program which tests the tools with randomly generated
 * assertj-swing GUI actions. Not a unit test.
 */
public class RandomToolTest {
    private File inputDir;
    private final AppRunner app;
    private final Mouse mouse;
    private final Keyboard keyboard;

    private final Object resumeMonitor = new Object();

    private volatile boolean keepRunning = true;
    private long testNr = 1;

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

        mainLoop();
    }

    private void mainLoop() {
        testLoop();

        //noinspection InfiniteLoopStatement
        while (true) {
            // wait for a signal from the EDT indicating that
            // a suspended test run should be resumed
            synchronized (resumeMonitor) {
                try {
                    resumeMonitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            testLoop();
        }
    }

    private void testLoop() {
        Tool[] preferredTools = {};
        while (true) {
            if (preferredTools.length == 0) {
                // there is no preferred tool, each tool gets equal chance
                testTool(Tools.getRandomTool(), testNr++);
            } else {
                // with 50% probability force using a preferred tool
                if (Rnd.nextBoolean()) {
                    testTool(Rnd.chooseFrom(preferredTools), testNr++);
                } else {
                    testTool(Tools.getRandomTool(), testNr++);
                }
            }

            if (!keepRunning) {
                System.out.println("\n" + RandomToolTest.class.getSimpleName() + " paused.");
                break;
            }
        }
    }

    private void setupPauseKey() {
        // make sure it can be paused by pressing a key
        KeyStroke pauseKeyStroke = KeyStroke.getKeyStroke('w');
        GlobalEventWatch.add(MappedKey.fromKeyStroke(pauseKeyStroke, "pauseTest", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (keepRunning) {
                    System.err.println(pauseKeyStroke.getKeyChar() + " pressed, pausing.");
                    keepRunning = false;
                } else {
                    System.err.println(pauseKeyStroke.getKeyChar() + " pressed, starting again.");
                    keepRunning = true;
                    synchronized (resumeMonitor) {
                        // wake up the waiting main thread from the EDT
                        resumeMonitor.notify();
                    }
                }
            }
        }));
    }

    @SuppressWarnings("MethodMayBeStatic")
    private void setupExitKey() {
        // This key not only stops the testing, but also exits the app
        KeyStroke exitKeyStroke = KeyStroke.getKeyStroke('j');
        GlobalEventWatch.add(MappedKey.fromKeyStroke(exitKeyStroke, "exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.err.println("\nexiting because '" + exitKeyStroke
                    .getKeyChar() + "' was pressed");
                System.exit(1);
            }
        }));
    }

    private void testTool(Tool tool, long testNr) {
        Utils.sleep(200, MILLISECONDS);
        activate(tool, testNr);
        randomizeToolSettings(tool);

        // set the source point for the clone tool
        if (tool == CLONE) {
            setSourceForCloneTool();
        }

        clickRandomly(tool);
        dragRandomly(tool);
        clickRandomly(tool);
        pushToolButtons(tool);

        cleanupAfterToolTest(tool);

        Utils.sleep(1, SECONDS);
    }

    private void activate(Tool tool, long testNr) {
        log(tool, "activating, starting test " + Ansi.red(testNr + "."));
        app.clickTool(tool);
    }

    private static void randomizeToolSettings(Tool tool) {
        log(tool, "randomize tool settings");
        EDT.run(ToolSettingsPanelContainer.INSTANCE::randomizeToolSettings);

        if (tool == PEN && PEN.getMode() == PenToolMode.BUILD) {
            // prevent paths getting too large
            log(tool, "removing the path");
            Rnd.withProbability(0.5, () -> EDT.run(PEN::removePath));
        }
    }

    private void clickRandomly(Tool tool) {
        Rnd.withProbability(0.3, () -> click(tool));
        Rnd.withProbability(0.3, () -> ctrlClick(tool));
        Rnd.withProbability(0.3, () -> altClick(tool));
        Rnd.withProbability(0.3, () -> shiftClick(tool));
        Rnd.withProbability(0.3, () -> doubleClick(tool));
        Rnd.withProbability(0.1, () -> pressEnter(tool));
        Rnd.withProbability(0.1, () -> pressEsc(tool));

        possiblyUndoRedo(tool);
    }

    private void pressEnter(Tool tool) {
        log(tool, "pressing Enter");
        keyboard.pressEnter();
    }

    private void pressEsc(Tool tool) {
        log(tool, "pressing Esc");
        keyboard.pressEsc();
    }

    private void cleanupAfterToolTest(Tool tool) {
        if (tool == MOVE || tool == CROP) {
            Composition comp = EDT.getComp();
            if (comp.getNumLayers() > 1) {
                flattenImage(tool);
            }

            ImageLayer layer = (ImageLayer) comp.getActiveLayer();
            int tx = layer.getTX();
            int ty = layer.getTY();

            if (tx < -comp.getCanvasImWidth() || ty < -comp.getCanvasImHeight()) {
                cutBigLayer(tool);
            } else if (tx < 0 || ty < 0) {
                Rnd.withProbability(0.3, () -> cutBigLayer(tool));
            }
        }
        Rnd.withProbability(0.1, () -> deselect(tool));

        if (tool == ZOOM) {
            Rnd.withProbability(0.5, () -> actualPixels(tool));
        }

        Rnd.withProbability(0.05, () -> reload(tool));

        keyboard.randomizeColors();

        setStandardSize();
    }

    private void reload(Tool tool) {
        log(tool, "reloading the image");
        app.runMenuCommand("Reload");
    }

    // might be necessary because of the croppings
    private void setStandardSize() {
        Canvas canvas = EDT.active(Composition::getCanvas);
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();
        if (canvasWidth != 770 || canvasHeight != 600) {
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
        }
    }

    private void possiblyUndoRedo(Tool tool) {
        if (!EDT.call(History::canUndo)) {
            return;
        }

        Utils.sleep(200, MILLISECONDS);
        boolean undone = Rnd.withProbability(0.25, () -> undo(tool));
        if (undone) {
            Utils.sleep(200, MILLISECONDS);
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
        Utils.sleep(200, MILLISECONDS);
        keyboard.undo();
    }

    private void redo(Tool tool) {
        String editName = EDT.call(History::getEditToBeRedoneName);
        log(tool, "random redo " + Ansi.yellow(editName));
        Utils.sleep(200, MILLISECONDS);
        keyboard.redo();
    }

    private void parseCLArguments(String[] args) {
        assert args.length > 0 : "missing CL argument";
        inputDir = new File(args[0]);
        assert inputDir.exists() : "input dir doesn't exist";
        assert inputDir.isDirectory() : "input dir is not a directory";
    }

    private static void log(Tool tool, String msg) {
        System.out.println(Ansi.blue(tool.getName() + ": ") + msg);
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
            Rnd.withProbability(0.2, this::clickPenToolButton);
        } else if (tool == ZOOM || tool == HAND) {
            Rnd.withProbability(0.2, () -> clickZoomOrHandToolButton(tool));
        } else if (tool == CROP) {
            Rnd.withProbability(0.5, this::clickCropToolButton);
        } else if (tool == SELECTION) {
            Rnd.withProbability(0.5, this::clickSelectionToolButton);
        }
    }

    private void changeLazyMouseSetting(Tool tool) {
        app.findButtonByText("Lazy Mouse...").click();
        DialogFixture dialog = app.findDialogByTitle("Lazy Mouse");

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
}
