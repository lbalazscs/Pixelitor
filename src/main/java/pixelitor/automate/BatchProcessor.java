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

package pixelitor.automate;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.compactions.CompAction;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.history.History;
import pixelitor.io.*;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static pixelitor.utils.Threads.*;

/**
 * Handles the batch processing of compositions.
 */
public class BatchProcessor {
    private static final String OVERWRITE_YES = "Yes";
    private static final String OVERWRITE_YES_ALL = "Yes, All";
    private static final String OVERWRITE_NO = "No (Skip)";
    private static final String OVERWRITE_CANCEL = "Cancel Processing";

    private volatile boolean overwriteAll = false;
    private volatile boolean stopProcessing = false;

    private final CompAction action;
    private final String progressDialogTitle;
    private final File inputDir;
    private final File outputDir;

    public BatchProcessor(CompAction action, String progressDialogTitle) {
        this.action = action;
        this.progressDialogTitle = progressDialogTitle;

        inputDir = RecentDirs.getLastOpen();
        outputDir = RecentDirs.getLastSave();
    }

    /**
     * Processes each file in the input directory. It starts the work
     * asynchronously in a background worker, and returns immediately.
     */
    public void processFilesAsync() {
        assert calledOnEDT() : callInfo();

        List<File> filesToProcess = FileUtils.listSupportedInputFiles(inputDir);
        if (filesToProcess.isEmpty()) {
            Messages.showInfo("No Files Found",
                "No supported files found in " + inputDir.getAbsolutePath());
            return;
        }

        stopProcessing = false;

        // batch edits shouldn't be added to the undo history
        History.setIgnoreEdits(true);

        var worker = new SwingWorker<Void, Integer>() {
            private final ProgressMonitor progressMonitor = GUIUtils.createPercentageProgressMonitor(progressDialogTitle);

            @Override
            public Void doInBackground() {
                overwriteAll = false;

                for (int i = 0, fileCount = filesToProcess.size(); i < fileCount; i++) {
                    if (progressMonitor.isCanceled() || stopProcessing) {
                        break;
                    }
                    publish(i);
                    processFile(filesToProcess.get(i));
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (isCancelled()) {
                    return;
                }
                Integer latestFileIndex = chunks.getLast();
                updateProgress(progressMonitor, latestFileIndex, filesToProcess.size());
            }

            @Override
            protected void done() {
                History.setIgnoreEdits(false);
                progressMonitor.close();
            }
        };
        worker.execute();
    }

    private static void updateProgress(ProgressMonitor monitor, int currentIndex, int fileCount) {
        monitor.setProgress((int) (currentIndex * 100.0 / fileCount));
        monitor.setNote("Processing " + (currentIndex + 1) + " of " + fileCount);
    }

    private void processFile(File file) {
        assert calledOutsideEDT() : "on EDT";

        FileIO.openFileAsync(file, false)
            .thenComposeAsync(action::process, onEDT)
            .thenComposeAsync(this::saveAndClose, onEDT)
            .exceptionally(Messages::showExceptionOnEDT)
            .join(); // ensures that files are handled one at a time
    }

    /**
     * A false in the return value indicates that no output file was created.
     */
    private CompletableFuture<Boolean> saveAndClose(Composition comp) {
        assert calledOnEDT() : callInfo();

        var format = FileFormat.getLastSaved();
        File outputFile = createOutputPath(comp, format);

        var saveSettings = new SaveSettings.Default(format, outputFile);
        CompletableFuture<Boolean> saveFuture = null;

        View view = comp.getView();
        assert view != null : "no view for " + comp.getName();

        if (outputFile.exists() && !overwriteAll) {
            String userChoice = promptOverwriteChoice(outputFile);

            switch (userChoice) {
                case OVERWRITE_YES:
                    saveFuture = comp.saveAsync(saveSettings, false);
                    break;
                case OVERWRITE_YES_ALL:
                    saveFuture = comp.saveAsync(saveSettings, false);
                    overwriteAll = true;
                    break;
                case OVERWRITE_CANCEL:
                    stopProcessing = true;
                    // fall through to the same handling as a skipped file
                case OVERWRITE_NO:
                    // the processed comp is a temporary result of the batch run,
                    // so discard it without the unsaved-changes warning
                    comp.setDirty(false);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + userChoice);
            }
        } else { // the output file doesn't exist or "overwrite all" was selected previously
            view.paintImmediately();
            saveFuture = comp.saveAsync(saveSettings, false);
        }

        if (saveFuture != null) {
            // close the view only after the async save has completed
            return saveFuture.whenCompleteAsync((_, _) -> {
                // this peeking callback runs on both success and failure:
                // if the save was successful, comp.dirty is naturally false => closes silently
                // if the save failed, comp.dirty is true (restored by saveAsync) => user gets prompted
                Views.warnAndClose(view);
            }, onEDT);
        } else {
            // the file was skipped or processing was canceled
            Views.warnAndClose(view);   // silent, since the comp is no longer dirty
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    private File createOutputPath(Composition comp, FileFormat format) {
        // works because all composition actions carry over the file reference from the original
        String inFileName = comp.getFile().getName();

        String outFileName = FileUtils.replaceExtension(inFileName, format.toString());
        return new File(outputDir, outFileName);
    }

    private static String promptOverwriteChoice(File outputFile) {
        String msg = String.format("File %s already exists. Overwrite?", outputFile);
        var optionPane = new JOptionPane(msg, WARNING_MESSAGE);

        optionPane.setOptions(new String[]{
            OVERWRITE_YES, OVERWRITE_YES_ALL, OVERWRITE_NO, OVERWRITE_CANCEL});
        optionPane.setInitialValue(OVERWRITE_NO);

        JDialog dialog = optionPane.createDialog(PixelitorWindow.get(), "Warning");
        dialog.setVisible(true);

        String selectedValue = (String) optionPane.getValue();

        String answer;
        if (selectedValue == null) { // canceled
            answer = OVERWRITE_CANCEL;
        } else {
            answer = selectedValue;
        }
        return answer;
    }
}
