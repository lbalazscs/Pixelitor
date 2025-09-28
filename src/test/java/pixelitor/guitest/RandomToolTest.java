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

package pixelitor.guitest;

import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.exception.WaitTimedOutError;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import pixelitor.Composition;
import pixelitor.ExceptionHandler;
import pixelitor.Views;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.HistogramsPanel;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.StatusBar;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayersContainer;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.BrushType;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.pen.Path;
import pixelitor.tools.selection.AbstractSelectionTool;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.StrokeCap;
import pixelitor.tools.util.ArrowKey;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.Ansi;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.input.Modifiers;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.awt.event.KeyEvent.*;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pixelitor.guitest.GUITestUtils.checkRandomly;
import static pixelitor.guitest.GUITestUtils.chooseRandomly;
import static pixelitor.guitest.GUITestUtils.slideRandomly;
import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.CROP;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.HAND;
import static pixelitor.tools.Tools.MOVE;
import static pixelitor.tools.Tools.PEN;
import static pixelitor.tools.Tools.SHAPES;
import static pixelitor.tools.Tools.ZOOM;
import static pixelitor.tools.Tools.getRandomTool;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOn;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.calledOutsideEDT;
import static pixelitor.utils.Threads.threadName;
import static pixelitor.utils.test.RandomGUITest.EXIT_KEY_CHAR;
import static pixelitor.utils.test.RandomGUITest.PAUSE_KEY_CHAR;

/**
 * A standalone program which tests the tools with randomly
 * generated assertj-swing GUI actions. Not a unit test.
 * <p>
 * AssertJ-Swing requires using the following VM option:
 * --add-opens java.base/java.util=ALL-UNNAMED
 */
public class RandomToolTest {
    private File inputDir;
    private final AppRunner app;
    private final Mouse mouse;
    private final Keyboard keyboard;

    private volatile boolean paused = false;
    private volatile boolean stopped = false;

    private final AtomicLong testNr = new AtomicLong(0);

    private List<Runnable> actions;
    private final ArrowKey[] arrowKeys = ArrowKey.values();
    private final List<Double> testDurations = new ArrayList<>();

    private final Object resumeMonitor = new Object();
    private final CountDownLatch mainLoopExitLatch = new CountDownLatch(1);

    private static final String[] simpleMultiLayerEdits = {
        "comp_rot_90", "comp_rot_180", "comp_rot_270",
        "comp_flip_hor", "comp_flip_ver"
    };

    private static final String[] ADD_MASK_MENU_COMMANDS = {
        "Add White (Reveal All)", "Add Black (Hide All)", "Add from Layer"};

    private static final String[] REMOVE_MASK_MENU_COMMANDS = {
        "Delete", "Apply"};

    private static final int[] TOOL_HOTKEYS = {
        VK_V, VK_C, VK_M, VK_B, VK_S, VK_E, VK_K,
        VK_G, VK_N, VK_I, VK_P, VK_U, VK_H, VK_Z,
    };

    public static void main(String[] args) {
        Utils.ensureAssertionsEnabled();
        FailOnThreadViolationRepaintManager.install();

        new RandomToolTest(args);
    }

    private RandomToolTest(String[] args) {
        parseCLArguments(args);

        EDT.run(this::setupPauseKey);
        EDT.run(this::setupExitKey);

        app = new AppRunner(null, inputDir, "b.jpg", "a.jpg");
        keyboard = app.getKeyboard();
        mouse = app.getMouse();
        ExceptionHandler.INSTANCE.addFirstHandler((t, e) -> {
            e.printStackTrace();
            keyboard.releaseModifierKeysFromAnyThread();
        });

        initActions();
        app.runMenuCommand("Fit Space");

        app.runTests(this::mainLoop);

        // It's the final countdown...
        // (signal that the main loop was exited cleanly)
        mainLoopExitLatch.countDown();
    }

    // the main loop is the test loop with pause-resume support
    private void mainLoop() {
        assert calledOn("main") : callInfo();

        while (true) {
            try {
                testLoop();
            } catch (StoppedException e) {
                assert stopped;
                System.out.println("\n" + RandomToolTest.class.getSimpleName() + " stopped.");
                printDurationStatistics();

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

    private void printDurationStatistics() {
        var stats = testDurations.stream()
            .mapToDouble(s -> s)
            .summaryStatistics();
        System.out.printf("duration stats: count = %d, min = %.2fs, max = %.2fs, avg = %.2fs%n",
            stats.getCount(), stats.getMin(), stats.getMax(), stats.getAverage());
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
        // exit this infinite loop only by throwing an exception
        //noinspection InfiniteLoopStatement
        while (true) {
            Tool tool = selectNextToolForTesting();

            long startTime = System.nanoTime();
            testNr.incrementAndGet();

            testWithTimeout(tool);

            double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            testDurations.add(estimatedSeconds);
        }
    }

    private static Tool selectNextToolForTesting() {
        // this can't be a field because the tools
        // are initialized after the fields of this class
        List<Tool> preferredTools = List.of();

        Tool selectedTool;
        if (preferredTools.isEmpty()) {
            // there is no preferred tool, each tool gets equal chance
            selectedTool = getRandomTool(RandomToolTest::canBeTested);
        } else {
            // with 80% probability force using a preferred tool
            if (Rnd.nextDouble() < 0.8) {
                selectedTool = Rnd.chooseFrom(preferredTools);
            } else {
                selectedTool = getRandomTool(RandomToolTest::canBeTested);
            }
        }
        return selectedTool;
    }

    private static boolean canBeTested(Tool tool) {
        if (tool == Tools.NODE || tool == Tools.TRANSFORM_PATH) {
            return EDT.getActivePath() != null;
        }
        return true;
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
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            Utils.sleep(1, HOURS);
        }
    }

    private void test(Tool tool) {
        Utils.sleep(200, MILLISECONDS);
        activate(tool);

        EDT.assertModalDialogCountIs(0);
        randomizeToolSettings(tool);
        EDT.assertModalDialogCountIs(0);

        // set the source point for the clone tool
        if (tool == CLONE) {
            setSourceForCloneTool();
        }

        randomActions(tool);
        dragRandomly(tool);

        randomActions(tool);
        clickToolButtons();

        randomActions(tool);

        cleanupAfterTool();
        EDT.assertModalDialogCountIs(0);

        Utils.sleep(200, MILLISECONDS);
        checkControlVariables();
    }

    // close a possible randomly shown dialog
    private void closeToolDialog(Tool tool) {
        if (EDT.getModalDialogCount() == 0) {
            return;
        }
        waitForIdleEDT();

        JOptionPaneFixture optionPane;
        try {
            optionPane = app.findJOptionPane(null);
        } catch (WaitTimedOutError e) {
            throw new IllegalStateException("No option pane");
        }
        String title = optionPane.title();
        switch (title) {
            case "Nothing Selected", "No Selection" -> optionPane.okButton().click();
            case "Non-Rectangular Selection Crop" -> optionPane.buttonWithText("Crop and Hide").click();
            case "Existing Selection" -> optionPane.buttonWithText("Replace").click();
            default -> System.out.println("RandomToolTest::closeToolDialog: tool = "
                + tool + ", title = " + title);
        }
        Utils.sleep(200, MILLISECONDS);
        EDT.assertModalDialogCountIs(0);
    }

    private void activate(Tool tool) {
        boolean toolsShown = EDT.call(() -> PixelitorWindow.get().areToolsShown());
        if (toolsShown) {
            log("activating " + tool.getName() + " by clicking on the button");
            app.clickTool(tool);
        } else {
            log("activating " + tool.getName() + " with keyboard");
            keyboard.pressChar(tool.getHotkey());
        }
    }

    private void randomizeToolSettings(Tool tool) {
        log("randomize the settings of " + tool.getName());
        EDT.run(ToolSettingsPanelContainer.get()::randomizeToolSettings);
    }

    private void initActions() {
        actions = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            addClickAction();
        }

        addAction(this::doubleClick, "doubleClick");
        addAction(this::pressEnter, "pressEnter");
        addAction(this::pressEsc, "pressEsc");
        addAction(this::pressTab, "pressTab");
        addAction(this::pressCtrlTab, "pressCtrlTab");
        addAction(this::nudge, "nudge");
        addAction(this::possiblyUndoRedo, "possiblyUndoRedo");
        addAction(this::randomMultiLayerEdit, "randomMultiLayerEdit");
        addAction(this::randomShowHide, "randomShowHide");
        addAction(this::randomMaskAction, "randomMaskEvent");

//        addAction(this::randomKeyboardToolSwitch, "randomKeyboardToolSwitch");
//        addAction(this::changeUI, "changeUI");

        // breaks assertj?
//        addAction(this::changeMaskView, "changeMaskView");
    }

    private void addAction(Runnable event, String name) {
        actions.add(new MeasuredTask(event, name));
    }

    private void addClickAction() {
        actions.add(new MeasuredTask(() ->
            click(Modifiers.randomly(new Random()))));
    }

    private void randomActions(Tool tool) {
        Collections.shuffle(actions);
        closeToolDialog(tool);
        for (Runnable action : actions) {
            Rnd.runWithProbability(action, 0.2);
            keyboard.assertModifiersReleased();
            closeToolDialog(tool);

            waitForIdleEDT();
        }
    }

    private static void waitForIdleEDT() {
        // wait until things started with invokeLater also finish
        CountDownLatch latch = new CountDownLatch(1);
        EventQueue.invokeLater(latch::countDown);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
        Tool tool = EDT.call(Tools::getActive);
        log("cleaning up after " + tool.getName());

        if (EDT.getActiveSelection() != null) {
            Rnd.runWithProbability(this::deselect, 0.2);
        }

        if (tool == MOVE || tool == CROP) {
            flattenImage();
        } else if (tool == ZOOM) {
            Rnd.runWithProbability(this::actualPixels, 0.5);
        } else if (tool == PEN) {
            // prevent paths getting too large
            log("removing the path");
            Rnd.runWithProbability(() -> EDT.run(PEN::removePath), 0.5);
        }

        Rnd.runWithProbability(this::reload, 0.05);
        randomizeColors();

        cutBigLayersIfNecessary();
        setStandardSize();

        // this shouldn't be necessary
        keyboard.releaseModifierKeys();
    }

    private void reload() {
        log("reloading the image");
        app.reload();
    }

    private void randomizeColors() {
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
        app.deselect();
    }

    private void actualPixels() {
        log("actual pixels");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Actual Pixels");
    }

    private void dragRandomly(Tool tool) {
        EDT.assertModalDialogCountIs(0);

        int numDrags = Rnd.intInRange(1, 5);
        for (int i = 0; i < numDrags; i++) {
            Utils.sleep(200, MILLISECONDS);
            mouse.moveRandomlyWithinCanvas();

            boolean ctrlPressed = Rnd.runWithProbability(keyboard::pressCtrl, 0.25);
            boolean altPressed = Rnd.runWithProbability(keyboard::pressAlt, 0.25);
            boolean shiftPressed = Rnd.runWithProbability(keyboard::pressShift, 0.25);

            String msg = "random " + Debug.modifiersToString(ctrlPressed, altPressed,
                shiftPressed, false, false) + "drag";
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

            closeToolDialog(tool);

            possiblyUndoRedo();
            keyboard.assertModifiersReleased();
        }
    }

    private void possiblyUndoRedo() {
        if (!EDT.call(History::canUndo)) {
            return;
        }

        boolean undone = Rnd.runWithProbability(this::undo, 0.5);
        if (undone) {
            Utils.sleep(200, MILLISECONDS);
            assert EDT.call(History::canRedo);
            Rnd.runWithProbability(this::redo, 0.5);
        }
    }

    private String click(Modifiers modifiers) {
        String name = "random " + Debug.modifiersToString(modifiers, false) + "click";
        log(name);

        Utils.sleep(200, MILLISECONDS);
        mouse.randomClick(modifiers);
        return name;
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
        app.runMenuCommandByName(command);
    }

    private void parseCLArguments(String[] args) {
        assert args.length == 1 : "missing inputDir argument";
        inputDir = new File(args[0]);
        assert inputDir.exists() : "input dir doesn't exist";
        assert inputDir.isDirectory() : "input dir is not a directory";
    }

    private void log(String msg) {
        checkControlVariables();

        Tool tool = EDT.call(Tools::getActive);
        String toolInfo = tool.getShortName();

        String stateInfo = EDT.call(tool::getStateInfo);
        toolInfo += (" [" + stateInfo + "]");

        String printed = Ansi.red(testNr + ".") + " " + Ansi.blue(toolInfo + ": ") + msg;
        if (EDT.getActiveSelection() != null) {
            printed += Ansi.red(" SEL");
        }
        if (EDT.active(Composition::getDraftSelection) != null) {
            printed += Ansi.red(" IP SEL");
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

    private void cutBigLayersIfNecessary() {
        Rectangle imgSize = EDT.call(() -> calcMaxImageSize(Views.getActiveComp()));
        Dimension canvasSize = EDT.active(comp -> comp.getCanvas().getSize());

        if (imgSize.width > 3 * canvasSize.width || imgSize.height > 3 * canvasSize.height) {
            // needs to be cut, otherwise there is a risk that
            // the image will grow too large during the next resize
            cutBigLayers();
        } else if (imgSize.width > canvasSize.width || imgSize.height > canvasSize.height) {
            Rnd.runWithProbability(this::cutBigLayers, 0.3);
        }
    }

    private static Rectangle calcMaxImageSize(Composition comp) {
        Rectangle max = new Rectangle(0, 0, 0, 0);
        int numLayers = comp.getNumLayers();
        for (int i = 0; i < numLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer imageLayer) {
                Rectangle layerBounds = imageLayer.getContentBounds();
                max.add(layerBounds);
            }
        }
        return max;
    }

    private void cutBigLayers() {
        log("layers to canvas size");
        Utils.sleep(200, MILLISECONDS);

        EDT.run(() -> Views.onActiveComp(
            Composition::allImageLayersToCanvasSize));
    }

    private void flattenImage() {
        if (EDT.active(Composition::getNumLayers) == 1) {
            return;
        }
        log("flatten image");
        Utils.sleep(200, MILLISECONDS);
        app.runMenuCommand("Flatten Image");
    }

    private void setSourceForCloneTool() {
        log("setting source point");
        Utils.sleep(200, MILLISECONDS);
        mouse.randomAltClick();
    }

    private void clickToolButtons() {
        boolean toolsShown = EDT.call(() -> PixelitorWindow.get().areToolsShown());
        if (!toolsShown) {
            return;
        }

        Tool tool = EDT.call(Tools::getActive);

        if (tool instanceof AbstractBrushTool) {
            Rnd.runWithProbability(() -> randomizeLazyMouse(tool), 0.2);
        }

        if (tool == CROP) {
            Rnd.runWithProbability(this::clickCropToolButton, 0.5);
        } else if (tool == BRUSH || tool == ERASER) {
            Rnd.runWithProbability(() -> randomizeBrushSettings(tool), 0.2);
        } else if (tool == CLONE) {
            Rnd.runWithProbability(this::randomizeCloneTransform, 0.2);
        } else if (tool == PEN) {
            Rnd.runWithProbability(this::clickPenToolButton, 0.4);
        } else if (tool == ZOOM || tool == HAND) {
            Rnd.runWithProbability(this::clickZoomOrHandToolButton, 0.2);
        } else if (tool instanceof AbstractSelectionTool) {
            Rnd.runWithProbability(this::clickSelectionToolButton, 0.5);
        } else if (tool == SHAPES) {
            Rnd.runWithProbability(this::randomizeShapeTypeSettings, 0.2);
            Rnd.runWithProbability(this::randomizeShapeStrokeSettings, 0.2);
            Rnd.runWithProbability(this::randomizeShapeEffects, 0.2);
            Rnd.runWithProbability(this::clickConvertShapeToSelection, 0.2);
        }
    }

    private void randomizeShapeTypeSettings() {
        ShapeType shapeType = EDT.call(SHAPES::getSelectedType);
        if (!shapeType.hasSettings()) {
            return;
        }
        log("randomizing the shape type setting for " + shapeType);

        app.withToolDialog("shapeSettingsButton",
            "Settings for " + shapeType.toString(),
            dialog -> randomizeShapeSettingsDialog(dialog, shapeType));
    }

    private static void randomizeShapeSettingsDialog(DialogFixture dialog, ShapeType shapeType) {
        //noinspection EnumSwitchStatementWhichMissesCases
        switch (shapeType) {
            case RECTANGLE -> slideRandomly(dialog.slider("Rounding Radius (px)"));
            case LINE -> {
                slideRandomly(dialog.slider("Width (px)"));
                chooseRandomly(dialog.comboBox(StrokeCap.NAME));
            }
            case STAR -> {
                slideRandomly(dialog.slider("Number of Branches"));
                slideRandomly(dialog.slider("Inner/Outer Radius Ratio (%)"));
            }
            // other shape types should have no settings dialog
            default -> throw new IllegalStateException("shapeType is " + shapeType);
        }
    }

    private void randomizeShapeStrokeSettings() {
        log("randomizing the shapes stroke setting");
        app.withToolDialog("strokeSettingsButton",
            "Stroke Settings",
            RandomToolTest::randomizeShapeStrokeDialog);
    }

    private static void randomizeShapeStrokeDialog(DialogFixture dialog) {
        slideRandomly(dialog.slider("width"));
        chooseRandomly(dialog.comboBox("cap"));
        chooseRandomly(dialog.comboBox("join"));
        chooseRandomly(dialog.comboBox("strokeType"));

        var shapeType = dialog.comboBox("shapeType");
        if (shapeType.isEnabled()) {
            chooseRandomly(shapeType);
        }

        var dashed = dialog.checkBox("dashed");
        if (dashed.isEnabled()) {
            checkRandomly(dashed);
        }
    }

    private void randomizeShapeEffects() {
        log("randomizing the shapes effects");
        app.withToolDialog("effectsButton", "Effects",
            RandomToolTest::randomizeEffectsDialog);
    }

    private static void randomizeEffectsDialog(DialogFixture dialog) {
        int selectedTabIndex = Rnd.nextInt(4);
        String[] tabNames = {
            EffectsPanel.GLOW_TAB_NAME,
            EffectsPanel.INNER_GLOW_TAB_NAME,
            EffectsPanel.NEON_BORDER_TAB_NAME,
            EffectsPanel.DROP_SHADOW_TAB_NAME};

        for (int i = 0; i < tabNames.length; i++) {
            String tabName = tabNames[i];
            if (i == selectedTabIndex) {
                dialog.checkBox(tabName).check();
            } else {
                dialog.checkBox(tabName).uncheck();
            }
        }
    }

    private void clickConvertShapeToSelection() {
        clickRandomToolButton(new String[]{"Convert to Selection"});
    }

    private void randomizeCloneTransform() {
        log("randomizing the clone transform setting");
        app.withToolDialog("transformButton", "Clone Transform", dialog -> {
            slideRandomly(dialog.slider("scale"));
            slideRandomly(dialog.slider("rotate"));
            chooseRandomly(dialog.comboBox("mirror"));
        });
    }

    private void randomizeLazyMouse(Tool tool) {
        log("randomizing the lazy mouse on " + tool.getName());
        app.withToolDialog("lazyMouseDialogButton", "Lazy Mouse Settings", dialog -> {
            var enabledCB = dialog.checkBox();
            enabledCB.click();
            if (enabledCB.target().isSelected()) {
                slideRandomly(dialog.slider("distSlider"));
            }

            Utils.sleep(200, MILLISECONDS);
        });
    }

    private void randomizeBrushSettings(Tool tool) {
        BrushType brushType = getActiveBrushType(tool);
        log("randomizing the brush setting for " + tool + ", brushType = " + brushType);
        app.testBrushSettings(tool, brushType);
    }

    private static BrushType getActiveBrushType(Tool tool) {
        return EDT.call(() -> {
            if (tool == BRUSH) {
                return BRUSH.getBrushType();
            } else if (tool == ERASER) {
                return ERASER.getBrushType();
            }
            throw new IllegalStateException("tool = " + tool);
        });
    }

    private void clickPenToolButton() {
        Path path = EDT.active(Composition::getActivePath);
        if (path == null) {
            return;
        }
        var canvas = EDT.getCanvas();
        if (!canvas.getBounds().contains(path.getImBounds())) {
            // if the path is outside, then it can potentially take a very long time
            return;
        }

        String[] texts = {
            "Stroke with Brush",
            "Stroke with Eraser",
            "Stroke with Smudge",
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

    private void randomShowHide(String target, Callable<Boolean> visibilityCheck) {
        boolean shownBefore = EDT.call(visibilityCheck);
        String command = shownBefore ? "Hide " + target : "Show " + target;
        log(command);
        app.runMenuCommand(command);

        boolean shownAfter = EDT.call(visibilityCheck);
        assert shownAfter == !shownBefore;
    }

    private void randomKeyboardToolSwitch() {
        int keyCode = Rnd.chooseFrom(TOOL_HOTKEYS);
        log("random keyboard tool switch using " + Ansi.cyan(getKeyText(keyCode)));
        cleanupAfterTool();
        keyboard.press(keyCode);
    }

    private void toggleUI() {
        log("toggling the UI (Ctrl-K)");
        keyboard.ctrlPress(VK_K);
    }

    private void randomizeMaskView() {
        if (!EDT.activeLayerHasMask()) {
            return;
        }
        int mode = Rnd.nextInt(4) + 1;
        log("randomizing the mask view mode: Ctrl-" + mode);
        switch (mode) {
            case 1 -> keyboard.pressCtrlOne();
            case 2 -> keyboard.pressCtrlTwo();
            case 3 -> keyboard.pressCtrlThree();
            case 4 -> keyboard.pressCtrlFour();
        }
    }

    private void randomMaskAction() {
        String command = EDT.activeLayerHasMask()
            ? Rnd.chooseFrom(REMOVE_MASK_MENU_COMMANDS)
            : Rnd.chooseFrom(ADD_MASK_MENU_COMMANDS);
        log("randomMaskAction: " + command);
        app.runMenuCommand(command);
    }

    private void setupPauseKey() {
        GlobalEvents.registerHotkey(PAUSE_KEY_CHAR, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseKeyPressed();
            }
        });
    }

    private void pauseKeyPressed() {
        assert calledOnEDT() : callInfo();
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

    private void setupExitKey() {
        // This key not only pauses the testing, but also exits the app
        GlobalEvents.registerHotkey(EXIT_KEY_CHAR, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitKeyPressed();
            }
        });
    }

    private void exitKeyPressed() {
        assert calledOnEDT() : callInfo();
        System.err.println("\nexiting because '" + EXIT_KEY_CHAR + "' was pressed");

        // we are on the EDT now, and before exiting
        // we want to wait until the modifier keys are released
        exitInNewThread();
    }

    private void exitInNewThread() {
        new Thread(this::exitGracefully).start();
    }

    private void exitGracefully() {
        // avoid blocking the EDT
        assert calledOutsideEDT() : "on EDT";
        // this should also not be called from the main thread
        assert !threadName().equals("main");

        if (paused) {
            // if already paused, then exit immediately
            keyboard.releaseModifierKeys();
            System.exit(0);
        }

        // signal the main thread to finish ASAP
        stopped = true;

        // wait for the main thread to complete in a consistent state,
        // (with the modifier keys released), and finish
        try {
            boolean ok = mainLoopExitLatch.await(30, SECONDS);
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
    private String name;

    public MeasuredTask(Runnable task, String name) {
        this.task = task;
        this.name = name;
    }

    // the name of the task is given at runtime by the given supplier
    public MeasuredTask(Supplier<String> supplier) {
        task = () -> name = supplier.get();
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
//            System.out.printf("%s was running for %.2f s%n", name, seconds);
        }
    }
}
