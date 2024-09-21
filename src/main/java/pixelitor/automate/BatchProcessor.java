/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.io.*;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.calledOutsideEDT;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * Handles the batch processing of compositions.
 */
public class BatchProcessor {
    private static final String OVERWRITE_YES = "Yes";
    private static final String OVERWRITE_YES_ALL = "Yes, All";
    private static final String OVERWRITE_NO = "No (Skip)";
    private static final String OVERWRITE_CANCEL = "Cancel Processing";

    private boolean overwriteAll = false;
    private boolean stopProcessing = false;

    private final CompAction action;
    private final String dialogTitle;
    private final File inputDir;
    private final File outputDir;

    public BatchProcessor(CompAction action, String dialogTitle) {
        this.action = action;
        this.dialogTitle = dialogTitle;

        inputDir = Dirs.getLastOpen();
        outputDir = Dirs.getLastSave();
    }

    /**
     * Processes each file in the input directory
     * using the given {@link CompAction}.
     */
    public void processFiles() {
        assert calledOnEDT() : threadInfo();

        List<File> filesToProcess = FileUtils.listSupportedInputFiles(inputDir);
        if (filesToProcess.isEmpty()) {
            String msg = "No supported files found in " + inputDir.getAbsolutePath();
            Messages.showInfo("No Files Found", msg);
            return;
        }

        stopProcessing = false;
        var progressMonitor = GUIUtils.createPercentageProgressMonitor(dialogTitle);
        var worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                return processFilesSequentially(filesToProcess, progressMonitor);
            } // end of doInBackground
        };
        worker.execute();
    }

    private Void processFilesSequentially(List<File> filesToProcess,
                                          ProgressMonitor monitor) {
        assert calledOutsideEDT() : "on EDT";

        overwriteAll = false;

        for (int i = 0, fileCount = filesToProcess.size(); i < fileCount; i++) {
            if (monitor.isCanceled() || stopProcessing) {
                break;
            }

            updateProgress(monitor, i, fileCount);
            processIndividualFile(filesToProcess.get(i));
        }

        monitor.close();
        return null;
    }

    private static void updateProgress(ProgressMonitor monitor, int currentIndex, int total) {
        monitor.setProgress((int) (currentIndex * 100.0 / total));
        monitor.setNote("Processing " + (currentIndex + 1) + " of " + total);
    }

    private void processIndividualFile(File file) {
        assert calledOutsideEDT() : "on EDT";

        IO.openFileAsync(file, false)
            .thenComposeAsync(action::process, onEDT)
            .thenComposeAsync(this::saveAndClose, onEDT)
            .exceptionally(Messages::showExceptionOnEDT)
            .join();
    }

    private CompletableFuture<Void> saveAndClose(Composition comp) {
        assert calledOnEDT() : threadInfo();

        var format = FileFormat.getLastSaved();
        File outputFile = createOutputPath(comp, format);

        // so that it doesn't ask to save again after we just saved it
        comp.setDirty(false);

        var saveSettings = new SaveSettings.Simple(format, outputFile);
        CompletableFuture<Void> saveFuture = null;

        View view = comp.getView();
        assert view != null : "no view for " + comp.getName();

        if (outputFile.exists() && !overwriteAll) {
            String userChoice = promptOverwriteConfirmation(outputFile);

            switch (userChoice) {
                case OVERWRITE_YES:
                    saveFuture = comp.saveAsync(saveSettings, false);
                    break;
                case OVERWRITE_YES_ALL:
                    saveFuture = comp.saveAsync(saveSettings, false);
                    overwriteAll = true;
                    break;
                case OVERWRITE_NO:
                    // do nothing
                    break;
                case OVERWRITE_CANCEL:
                    Views.warnAndClose(view);
                    stopProcessing = true;
                    return CompletableFuture.completedFuture(null);
                default:
                    throw new IllegalStateException("Unexpected value: " + userChoice);
            }
        } else { // the file doesn't exist or "overwrite all" was selected previously
            view.paintImmediately();
            saveFuture = comp.saveAsync(saveSettings, false);
        }
        Views.warnAndClose(view);
        stopProcessing = false;

        if (saveFuture != null) {
            return saveFuture;
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private File createOutputPath(Composition comp, FileFormat format) {
        String inFileName = comp.getFile().getName();
        String outFileName = FileUtils.replaceExtension(inFileName, format.toString());
        return new File(outputDir, outFileName);
    }

    private static String promptOverwriteConfirmation(File outputFile) {
        String msg = format("File %s already exists. Overwrite?", outputFile);
        var optionPane = new JOptionPane(msg, WARNING_MESSAGE);

        optionPane.setOptions(new String[]{
            OVERWRITE_YES, OVERWRITE_YES_ALL, OVERWRITE_NO, OVERWRITE_CANCEL});
        optionPane.setInitialValue(OVERWRITE_NO);

        JDialog dialog = optionPane.createDialog(PixelitorWindow.get(), "Warning");
        dialog.setVisible(true);

        String selectedValue = (String) optionPane.getValue();

        String answer;
        if (selectedValue == null) { // cancelled
            answer = OVERWRITE_CANCEL;
        } else {
            answer = selectedValue;
        }
        return answer;
    }
}