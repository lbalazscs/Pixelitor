/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.automate.SingleDirChooserPanel;
import pixelitor.filters.Canny;
import pixelitor.filters.Fade;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.RandomFilter;
import pixelitor.filters.comp.Resize;
import pixelitor.filters.gui.ParametrizedFilterGUIPanel;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.history.History;
import pixelitor.io.Directories;
import pixelitor.io.OutputFormat;
import pixelitor.layers.Drawable;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static pixelitor.ChangeReason.TEST_WITH_HISTORY_AND_PREVIEW;

/**
 *
 */
public class FilterTests {
    /**
     * Utility class with static methods
     */
    private FilterTests() {
    }

    public static void saveTheResultOfEachFilter(Drawable dr) {
        boolean canceled = !SingleDirChooserPanel.selectOutputDir(true);
        if (canceled) {
            return;
        }
        File selectedDir = Directories.getLastSaveDir();
        OutputFormat outputFormat = OutputFormat.getLastUsed();

        ParametrizedFilterGUIPanel.setResetParams(false);
        ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor("Saving the Results of Each Filter");

        dr.startPreviewing();

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                if (selectedDir != null) {
                    Filter[] filters = FilterUtils.getFiltersShuffled(
                            filter -> (!(filter instanceof Fade)) // TODO Fade just hangs... (Threads?)
                                    && (!(filter instanceof RandomFilter))
                                    );
                    progressMonitor.setProgress(0);

                    for (int i = 0; i < filters.length; i++) {
                        Filter filter = filters[i];
                        String filterName = filter.getName();
                        System.out.println(String.format("FilterTests::doInBackground: filterName = '%s'", filterName));

                        progressMonitor.setProgress((int) ((float) i * 100 / filters.length));


                        progressMonitor.setNote("Running " + filter.getName());
                        if (progressMonitor.isCanceled()) {
                            break;
                        }

                        filter.randomizeSettings();
                        filter.startOn(dr, TEST_WITH_HISTORY_AND_PREVIEW);
                        Composition comp = dr.getComp();
                        String fileName = "test_" + Utils.toFileName(filter.getName()) + '.' + outputFormat.toString();
                        File f = new File(selectedDir, fileName);
                        outputFormat.saveComp(comp, f, false);

                        if (History.canUndo()) {
                            History.undo();
                        }

                    }
                    progressMonitor.close();
                }
                return null;
            }

            // Executed in EDT
            @Override
            protected void done() {
                try {
                    System.out.println("Done");
                    get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    Messages.showException(cause);
                } catch (InterruptedException e) {
                    // ignore
                } finally {
                    dr.stopPreviewing(); // reset to NORMAL
                    ParametrizedFilterGUIPanel.setResetParams(true);
                }
            }
        };
        worker.execute();
    }

    public static void runAllFiltersOn(Drawable dr) {
        ParametrizedFilterGUIPanel.setResetParams(false);
        try {
            ProgressMonitor progressMonitor = new ProgressMonitor(PixelitorWindow.getInstance(),
                    "Run All Filters on Current Layer",
                    "", 0, 100);

            progressMonitor.setProgress(0);

            // It is best to run this on the current EDT thread, using SwingWorker leads to strange things here

            Filter[] allFilters = FilterUtils.getFiltersShuffled(
                    filter -> !(filter instanceof RandomFilter));

            for (int i = 0, count = allFilters.length; i < count; i++) {
                progressMonitor.setProgress((int) ((float) i * 100 / count));
                Filter filter = allFilters[i];

                String msg = "Running " + filter.getName();

                progressMonitor.setNote(msg);
                if (progressMonitor.isCanceled()) {
                    break;
                }

                filter.randomizeSettings();
                filter.startOn(dr);
            }
            progressMonitor.close();
        } finally {
            ParametrizedFilterGUIPanel.setResetParams(true);
        }
    }

    public static void getCompositeImagePerformanceTest() {
        Composition comp = ImageComponents.getActiveCompOrNull();

        Runnable task = () -> {
            long startTime = System.nanoTime();
            int times = 100;
            for (int i = 0; i < times; i++) {
                comp.getCompositeImage();
            }

            long totalTime = (System.nanoTime() - startTime) / 1000000;
            String msg = String.format(
                    "Executing getCompositeImage() %d times took %d ms, average time = %d ms",
                    times, totalTime, totalTime / times);
            Messages.showInfo("Test Result", msg);
        };
        Utils.executeWithBusyCursor(task);
    }

    public static void randomResize() {
        ImageComponents.onActiveComp(comp -> {
            int targetWidth = 10 + RandomGUITest.rand.nextInt(1200);
            int targetHeight = 10 + RandomGUITest.rand.nextInt(800);
            new Resize(targetWidth, targetHeight, false).process(comp);
        });
    }

    public static void findSlowestFilter(Drawable dr) {
        assert SwingUtilities.isEventDispatchThread() : "not EDT thread";

        Filter[] filters = FilterUtils.getFiltersShuffled(
                f -> (!(f instanceof Fade || f instanceof Canny))
                        && (f instanceof FilterWithParametrizedGUI));

        Map<String, Double> results = new HashMap<>();

        dr.startPreviewing();

        // warmup
        for (Filter filter : filters) {
            System.out.println("Warmup for " + filter.getName());

            filter.startOn(dr, TEST_WITH_HISTORY_AND_PREVIEW);
        }

        // measuring
        for (Filter filter : filters) {
            long startTime = System.nanoTime();
            System.out.println("Testing " + filter.getName());

            filter.startOn(dr, TEST_WITH_HISTORY_AND_PREVIEW);

            double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            results.put(filter.getName(), estimatedSeconds);
        }
        dr.stopPreviewing();

        Arrays.sort(filters, (o1, o2) -> {
            double o1Time = results.get(o1.getName());
            double o2Time = results.get(o2.getName());
            return Double.compare(o1Time, o2Time);
        });
        for (Filter filter : filters) {
            String name = filter.getName();
            double time = results.get(name);
            System.out.println(String.format("%s: %.2f seconds", name, time));
        }
    }
}