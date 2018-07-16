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

package pixelitor.automate;

import pixelitor.Composition;
import pixelitor.filters.comp.CompAction;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.PixelitorWindow;
import pixelitor.io.Directories;
import pixelitor.io.FileExtensionUtils;
import pixelitor.io.OpenSaveManager;
import pixelitor.io.OutputFormat;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * Utility class with static methods for batch processing
 */
public class Automate {
    private static final String OVERWRITE_YES = "Yes";
    private static final String OVERWRITE_YES_ALL = "Yes, All";
    private static final String OVERWRITE_NO = "No (Skip)";
    private static final String OVERWRITE_CANCEL = "Cancel Processing";

    private static volatile boolean overwriteAll = false;
    private static volatile boolean stopProcessing = false;

    private Automate() {
    }

    /**
     * Processes each file in the input directory
     * with the given {@link CompAction}
     */
    public static void processEachFile(CompAction action,
                                       String dialogTitle) {
        File lastOpenDir = requireNonNull(Directories.getLastOpenDir());
        if (!lastOpenDir.exists()) {
            throw new IllegalStateException("Last open dir " + lastOpenDir.getAbsolutePath() + " does not exist");
        }

        File lastSaveDir = requireNonNull(Directories.getLastSaveDir());
        if (!lastSaveDir.exists()) {
            throw new IllegalStateException("Last save dir " + lastSaveDir.getAbsolutePath() + " does not exist");
        }

        File[] inputFiles = FileExtensionUtils.listSupportedInputFilesIn(lastOpenDir);
        if (inputFiles.length == 0) {
            Messages.showInfo("No files", "There are no supported files in " + lastOpenDir.getAbsolutePath());
            return;
        }

        ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor(dialogTitle);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                overwriteAll = false;

                for (int i = 0, nrOfFiles = inputFiles.length; i < nrOfFiles; i++) {
                    if (progressMonitor.isCanceled()) {
                        break;
                    }

                    File file = inputFiles[i];
                    progressMonitor.setProgress((int) ((float) i * 100 / nrOfFiles));
                    progressMonitor.setNote("Processing " + file.getName());

                    System.out.println("Processing " + file.getName());

                    OpenSaveManager.openFileAsync(file)
                            .thenApplyAsync(
                                    comp -> Automate.process(comp, action),
                                    EventQueue::invokeLater)
                            .thenComposeAsync(
                                    comp -> saveAndClose(comp, lastSaveDir),
                                    EventQueue::invokeLater)
                            .exceptionally(Messages::showExceptionOnEDT)
                            .join();

                    if (stopProcessing) {
                        break;
                    }

                } // end of for loop
                progressMonitor.close();
                return null;
            } // end of doInBackground
        };
        worker.execute();
    }

    private static Composition process(Composition comp,
                                       CompAction action) {
        assert EventQueue.isDispatchThread();

        System.out.println("Automate::processFile: CALLED, comp = " + comp.getName());

        ImageComponent ic = comp.getIC();

        ic.paintImmediately(ic.getBounds());
        action.process(comp);
        ic.paintImmediately(ic.getBounds());

        return comp;
    }

    private static CompletableFuture<Void> saveAndClose(Composition comp, File lastSaveDir) {
        OutputFormat outputFormat = OutputFormat.getLastUsed();
        String inputFileName = comp.getFile().getName();
        String outFileName = FileExtensionUtils.replaceExt(inputFileName, outputFormat.toString());
        ImageComponent ic = comp.getIC();
        File outputFile = new File(lastSaveDir, outFileName);
        CompletableFuture<Void> retVal = null;

        // so that it doesn't ask to save again after we just saved it
        comp.setDirty(false);

        if (outputFile.exists() && (!overwriteAll)) {
            JOptionPane pane = new JOptionPane(String.format("File %s already exists. Overwrite?", outputFile),
                    JOptionPane.WARNING_MESSAGE);

            pane.setOptions(new String[]{OVERWRITE_YES, OVERWRITE_YES_ALL, OVERWRITE_NO, OVERWRITE_CANCEL});
            pane.setInitialValue(OVERWRITE_NO);

            JDialog dialog = pane.createDialog(PixelitorWindow.getInstance(), "Warning");
            dialog.setVisible(true);
            String value = (String) pane.getValue();
            String answer;

            if (value == null) { // cancelled
                answer = OVERWRITE_CANCEL;
            } else {
                answer = value;
            }

            switch (answer) {
                case OVERWRITE_YES:
                    retVal = comp.saveAsync(outputFile, outputFormat, false);
                    break;
                case OVERWRITE_YES_ALL:
                    retVal = comp.saveAsync(outputFile, outputFormat, false);
                    overwriteAll = true;
                    break;
                case OVERWRITE_NO:
                    // do nothing
                    break;
                case OVERWRITE_CANCEL:
                    OpenSaveManager.warnAndCloseImage(ic);
                    stopProcessing = true;
                    return CompletableFuture.completedFuture(null);
            }
        } else { // the file does not exist or overwrite all was pressed previously
            ic.paintImmediately(ic.getBounds());
            retVal = comp.saveAsync(outputFile, outputFormat, false);
        }
        OpenSaveManager.warnAndCloseImage(ic);
        stopProcessing = false;
        if (retVal != null) {
            return retVal;
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}