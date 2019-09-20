package pixelitor.guitest;

import org.assertj.swing.core.Robot;
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

public class RandomToolTest {
    private static File inputDir;
    private static AppRunner app;
    private static Mouse mouse;
    private static Robot robot;
    private static Keyboard keyboard;

    private static volatile boolean keepRunning = true;
    private static long testNr = 1;

    public static void main(String[] args) {
        Utils.makeSureAssertionsAreEnabled();
        FailOnThreadViolationRepaintManager.install();
        RandomGUITest.setRunning(true);

        parseCLArguments(args);

        EDT.run(RandomToolTest::setupPauseKey);
        EDT.run(RandomToolTest::setupExitKey);

        app = new AppRunner(inputDir);
        robot = app.getRobot();
        keyboard = app.getKeyboard();
        mouse = app.getMouse();

        mainLoop();
    }

    private static void mainLoop() {
        testLoop();

        //noinspection InfiniteLoopStatement
        while (true) {
            // wait for a signal from the EDT indicating that
            synchronized (app) {
                try {
                    app.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            testLoop();
        }
    }

    private static void testLoop() {
        while (true) {
            Tool tool = Tools.getRandomTool();
            testTool(tool, testNr++);

            if (!keepRunning) {
                System.out.println("\n" + RandomToolTest.class.getSimpleName() + " paused.");
                break;
            }
        }
    }

    private static void setupPauseKey() {
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
                    synchronized (app) {
                        app.notify();
                    }
                }
            }
        }));
    }

    private static void setupExitKey() {
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

    private static void testTool(Tool tool, long testNr) {
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

    private static void activate(Tool tool, long testNr) {
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

    private static void clickRandomly(Tool tool) {
        Rnd.withProbability(0.3, () -> click(tool));
        Rnd.withProbability(0.3, () -> ctrlClick(tool));
        Rnd.withProbability(0.3, () -> altClick(tool));
        Rnd.withProbability(0.3, () -> shiftClick(tool));
        Rnd.withProbability(0.3, () -> doubleClick(tool));

        possiblyUndoRedo(tool);
    }

    private static void cleanupAfterToolTest(Tool tool) {
        if (tool == MOVE) {
            Composition comp = EDT.getComp();
            if (comp.getNumLayers() > 1) {
                flattenImage(tool);
            }

            ImageLayer layer = (ImageLayer) comp.getActiveLayer();
            int tx = layer.getTX();
            int ty = layer.getTY();
            if (tx < 1000 || ty < 1000) {
                cutBigLayer(tool);
            } else {
                Rnd.withProbability(0.3, () -> cutBigLayer(tool));
            }
        }
        Rnd.withProbability(0.1, () -> deselect(tool));

        if (tool == ZOOM) {
            Rnd.withProbability(0.5, () -> actualPixels(tool));
        }

        keyboard.randomizeColors();

        setStandardSize();
    }

    // might be necessary because of the croppings
    private static void setStandardSize() {
        Canvas canvas = EDT.active(Composition::getCanvas);
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();
        if (canvasWidth != 770 || canvasHeight != 600) {
            app.resize(770, 600);
        }
    }

    private static void deselect(Tool tool) {
        log(tool, "deselecting");
        Utils.sleep(200, MILLISECONDS);
        keyboard.deselect();
    }

    private static void actualPixels(Tool tool) {
        log(tool, "actual pixels");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Actual Pixels");
    }

    private static void dragRandomly(Tool tool) {
        int numDrags = Rnd.intInRange(1, 5);
        for (int i = 0; i < numDrags; i++) {
            Utils.sleep(200, MILLISECONDS);
            mouse.moveRandomlyWithinCanvas();

            boolean ctrlPressed = Rnd.withProbability(0.25, () -> keyboard.pressCtrl());
            boolean altPressed = Rnd.withProbability(0.25, () -> keyboard.pressAlt());
            boolean shiftPressed = Rnd.withProbability(0.25, () -> keyboard.pressShift());
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

    private static void possiblyUndoRedo(Tool tool) {
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

    private static void click(Tool tool) {
        log(tool, "random click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomClick();
    }

    private static void ctrlClick(Tool tool) {
        log(tool, "random ctrl-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomCtrlClick();
    }

    private static void altClick(Tool tool) {
        log(tool, "random alt-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomAltClick();
    }

    private static void shiftClick(Tool tool) {
        log(tool, "random shift-click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomShiftClick();
    }

    private static void doubleClick(Tool tool) {
        log(tool, "random double click");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomDoubleClick();
    }

    private static void undo(Tool tool) {
        String editName = EDT.call(History::getEditToBeUndoneName);
        log(tool, "random undo " + Ansi.yellow(editName));
        Utils.sleep(200, MILLISECONDS);
        keyboard.undo();
    }

    private static void redo(Tool tool) {
        String editName = EDT.call(History::getEditToBeRedoneName);
        log(tool, "random redo " + Ansi.yellow(editName));
        Utils.sleep(200, MILLISECONDS);
        keyboard.redo();
    }

    private static void parseCLArguments(String[] args) {
        assert args.length > 0 : "missing CL argument";
        inputDir = new File(args[0]);
        assert inputDir.exists() : "input dir doesn't exist";
        assert inputDir.isDirectory() : "input dir is not a directory";
    }

    private static void log(Tool tool, String msg) {
        System.out.println(Ansi.blue(tool.getName() + ": ") + msg);
    }

    private static void cutBigLayer(Tool tool) {
        log(tool, "layer to canvas size");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Layer to Canvas Size");
    }

    private static void flattenImage(Tool tool) {
        log(tool, "merge layers");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Flatten Image");
    }

    private static void setSourceForCloneTool() {
        log(CLONE, "setting source point");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomAltClick();
    }

    private static void pushToolButtons(Tool tool) {
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
            Rnd.withProbability(0.2, RandomToolTest::clickPenToolButton);
        } else if (tool == ZOOM || tool == HAND) {
            Rnd.withProbability(0.2, () -> clickZoomOrHandToolButton(tool));
        } else if (tool == CROP) {
            Rnd.withProbability(0.5, RandomToolTest::clickCropToolButton);
        } else if (tool == SELECTION) {
            Rnd.withProbability(0.5, RandomToolTest::clickSelectionToolButton);
        }
    }

    private static void changeLazyMouseSetting(Tool tool) {
        app.findButtonByText("Lazy Mouse...").click();
        DialogFixture dialog = app.findDialogByTitle("Lazy Mouse");

        log(tool, "changing the lazy mouse setting");
        Utils.sleep(200, MILLISECONDS);
        dialog.checkBox().click();
        Utils.sleep(200, MILLISECONDS);

        dialog.button("ok").click();
    }

    private static void changeBrushSetting(Tool tool) {
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

    private static void clickPenToolButton() {
        String[] texts = {
            "Stroke with Current Brush",
            "Stroke with Current Eraser",
            "Stroke with Current Smudge",
            "Convert to Selection"
        };
        clickRandomToolButton(PEN, texts);
    }

    private static void clickZoomOrHandToolButton(Tool tool) {
        String[] texts = {
            "Actual Pixels",
            "Fit Space",
            "Fit Width",
            "Fit Height"
        };
        clickRandomToolButton(tool, texts);
    }

    private static void clickCropToolButton() {
        String[] texts = {
            "Crop",
            "Cancel",
        };
        clickRandomToolButton(CROP, texts);
    }

    private static void clickSelectionToolButton() {
        String[] texts = {
            "Crop Selection",
            "Convert to Path",
        };
        clickRandomToolButton(SELECTION, texts);
    }

    private static void clickRandomToolButton(Tool tool, String[] texts) {
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
