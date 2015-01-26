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

import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.automate.SingleDirChooserPanel;
import pixelitor.filters.Fade;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.comp.CompositionUtils;
import pixelitor.filters.gui.ParametrizedAdjustPanel;
import pixelitor.history.History;
import pixelitor.io.FileChoosers;
import pixelitor.io.OutputFormat;
import pixelitor.utils.Dialogs;
import pixelitor.utils.Optional;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.io.File;

/**
 *
 */
public class OpTests {
    /**
     * Utility class with static methods
     */
    private OpTests() {
    }

    public static void saveTheResultOfEachOp() {

        boolean cancelled = !SingleDirChooserPanel.selectOutputDir(true);
        if (cancelled) {
            return;
        }
        final File selectedDir = FileChoosers.getLastSaveDir();
        final OutputFormat outputFormat = OutputFormat.getLastOutputFormat();

        ParametrizedAdjustPanel.setResetParams(false);
        final ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor("Saving the Results of Each Operation");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {

//                ImageComponent ic = AppLogic.getActiveImageComponent();

                if (selectedDir != null) {
                    Filter[] filters = FilterUtils.getAllFiltersShuffled();
                    progressMonitor.setProgress(0);

                    // TODO resize, crop should be also tested
                    for (int i = 0; i < filters.length; i++) {
                        Filter op = filters[i];
                        if (op instanceof Fade) {
                            continue; // TODO just hangs... (Threads?)
                        }

                        progressMonitor.setProgress((int) ((float) i * 100 / filters.length));


                        if (op.isEnabled()) {
                            progressMonitor.setNote("Running " + op.getMenuName());
                            if (progressMonitor.isCanceled()) {
                                break;
                            }

                            op.randomizeSettings();
                            op.execute(ChangeReason.OP_WITHOUT_DIALOG); // a  reason that makes backup
                            Composition comp = ImageComponents.getActiveComp().get();
                            String fileName = "test_" + Utils.toFileName(op.getMenuName()) + '.' + outputFormat.toString();
                            File f = new File(selectedDir, fileName);
                            outputFormat.saveComposition(comp, f);

                            if (History.canUndo()) {
                                History.undo();
                            }
                        }
                    }
                    progressMonitor.close();
                }
                return null;
            }
        };
        try {
            worker.execute();
        } finally {
            ParametrizedAdjustPanel.setResetParams(true);
        }
    }

    public static void runAllOpsOnCurrentLayer() {
        ParametrizedAdjustPanel.setResetParams(false);
        try {
            ProgressMonitor progressMonitor = new ProgressMonitor(PixelitorWindow.getInstance(),
                    "Run All Operations on Current Layer",
                    "", 0, 100);

            progressMonitor.setProgress(0);

            // It is best to run this on the current EDT thread, using SwingWorker leads to strange things here

            Filter[] allOps = FilterUtils.getAllFiltersShuffled();
            for (int i = 0, allOpsLength = allOps.length; i < allOpsLength; i++) {
                progressMonitor.setProgress((int) ((float) i * 100 / allOpsLength));
                Filter op = allOps[i];

                String msg = "Running " + op.getMenuName();

                progressMonitor.setNote(msg);
                if (progressMonitor.isCanceled()) {
                    break;
                }

                op.randomizeSettings();
                op.actionPerformed(null);
            }
            progressMonitor.close();
        } finally {
            ParametrizedAdjustPanel.setResetParams(true);
        }
    }

    public static void getCompositeImagePerformanceTest() {
        final Composition comp = ImageComponents.getActiveComp().get();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                long startTime = System.nanoTime();
                int times = 100;
                for (int i = 0; i < times; i++) {
                    comp.getCompositeImage();
                }

                long totalTime = (System.nanoTime() - startTime) / 1000000;
                String msg = String.format(
                        "Executing getCompositeImage() %d times took %d ms, average time = %d ms",
                        times, totalTime, totalTime / times);
                Dialogs.showInfoDialog("Test Result", msg);
            }
        };
        Utils.executeWithBusyCursor(task);
    }

    public static void randomResize() {
        Optional<Composition> comp = ImageComponents.getActiveComp();
        if (comp.isPresent()) {
            int targetWidth = 10 + RobotTest.rand.nextInt(1200);
            int targetHeight = 10 + RobotTest.rand.nextInt(800);
            CompositionUtils.resize(comp.get(), targetWidth, targetHeight, false);
        }
    }
}