/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest.main;

import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import pixelitor.Views;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.EDT;
import pixelitor.history.HistoryChecker;
import pixelitor.io.FileChoosers;
import pixelitor.utils.Texts;
import pixelitor.utils.Utils;

import java.util.Arrays;

import static pixelitor.guitest.AppRunner.getCurrentTimeHM;

/**
 * An automated GUI test which uses AssertJ-Swing.
 * This is not a unit test: the app as a whole is tested from the user
 * perspective, and depending on the configuration, it could run for hours.
 * <p>
 * AssertJ-Swing requires using the following VM option:
 * --add-opens java.base/java.util=ALL-UNNAMED
 */
public class MainGuiTest {

    private final AppRunner app;

    private final TestConfig config;

    /**
     * The entry point for this GUI test.
     */
    public static void main(String[] args) {
        Texts.init();
        Utils.ensureAssertionsEnabled();
        FailOnThreadViolationRepaintManager.install();

//        GlobalKeyboardWatch.registerDebugMouseWatching(false);

        new MainGuiTest(args);
    }

    private MainGuiTest(String[] args) {
        long startMillis = System.currentTimeMillis();

        config = new TestConfig(args);
        app = new AppRunner(new HistoryChecker(), config.getInputDir(), config.getSvgOutputDir(), "a.jpg");

        if (EDT.call(FileChoosers::useNativeDialogs)) {
            // the tests work only with the swing file choosers
            System.out.println("MainGuiTest: native dialogs, exiting");
            System.exit(0);
        }

        app.runMenuCommand("Reset Workspace");
        app.configureRobotDelay();

        runConfiguredTests();

        long totalTimeMillis = System.currentTimeMillis() - startMillis;
        System.out.printf("MainGuiTest: finished at %s after %s.",
            getCurrentTimeHM(), Utils.formatDuration(totalTimeMillis));

        app.exit();
    }

    private void runConfiguredTests() {
        MaskMode[] maskModes = MaskMode.load();
        TestSuite testSuite = TestSuite.load();
        printTestConfiguration(testSuite, maskModes);

        for (int i = 0; i < maskModes.length; i++) {
            MaskMode mode = maskModes[i];
            TestContext context = new TestContext(mode, app, config);
            runTests(testSuite, mode, context);

            if (i < maskModes.length - 1) {
                resetState(context);
            }
        }
    }

    private void printTestConfiguration(TestSuite testSuite, MaskMode[] maskModes) {
        System.out.println("Quick = " + config.isQuick()
            + ", test suite = " + testSuite
            + ", mask modes = " + Arrays.toString(maskModes));
    }

    /**
     * Resets the application state to a known baseline for a new test run.
     */
    private void resetState(TestContext context) {
        if (EDT.call(Views::getNumViews) > 0) {
            app.closeAll();
        }
        context.openFileWithDialog(config.getInputDir(), "a.jpg");

        context.clickAndResetRectSelectTool();
    }

    /**
     * Configures and runs a specific test suite with a given mask mode.
     */
    private void runTests(TestSuite testSuite, MaskMode maskMode, TestContext context) {
        maskMode.apply(context);

        System.out.printf("MainGuiTest: testSuite = %s, testingMode = %s, started at %s%n",
            testSuite, maskMode, getCurrentTimeHM());

        app.runTests(() -> testSuite.run(context));
    }

    /**
     * Runs all tests.
     */
    static void testAll(TestContext context) {
        testTools(context);
        testFileMenu(context);
        testAutoPaint(context);
        testEditMenu(context);
        testImageMenu(context);
        testFilters(context);
        testRest(context);
        testLayers(context);
    }

    static void testTools(TestContext context) {
        new ToolTests(context).start();
    }

    static void testLayers(TestContext context) {
        new LayerTests(context).start();
    }

    static void testFileMenu(TestContext context) {
        new FilesTests(context).start();
    }

    static void testAutoPaint(TestContext context) {
        new AutoPaintTests(context).start();
    }

    static void testEditMenu(TestContext context) {
        new EditMenuTests(context).start();
    }

    static void testImageMenu(TestContext context) {
        new ImageMenuTests(context).start();
    }

    static void testFilters(TestContext context) {
        new FilterTests(context).start();
    }

    static void testRest(TestContext context) {
        new ViewHelpMenusTests(context).start();
    }
}
