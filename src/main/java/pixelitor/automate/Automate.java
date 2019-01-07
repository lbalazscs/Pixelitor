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

package pixelitor.automate;

import pixelitor.Composition;
import pixelitor.filters.comp.CompAction;
import pixelitor.gui.OpenComps;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.io.Dirs;
import pixelitor.io.FileUtils;
import pixelitor.io.OpenSave;
import pixelitor.io.OutputFormat;
import pixelitor.io.SaveSettings;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static javax.swing.JOptionPane.WARNING_MESSAGE;

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
        File openDir = Dirs.getLastOpen();
        File saveDir = Dirs.getLastSave();

        File[] inputFiles = FileUtils.listSupportedInputFilesIn(openDir);
        if (inputFiles.length == 0) {
            Messages.showInfo("No files", "There are no supported files in " + openDir.getAbsolutePath());
            return;
        }

        ProgressMonitor progressMonitor = GUIUtils.createPercentageProgressMonitor(
                dialogTitle);
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

                    processFile(file, action, saveDir);

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

    private static void processFile(File file, CompAction action, File saveDir) {
        OpenSave.openFileAsync(file)
                .thenApplyAsync(
                        comp -> Automate.process(comp, action),
                        EventQueue::invokeLater)
                .thenComposeAsync(
                        comp -> saveAndClose(comp, saveDir),
                        EventQueue::invokeLater)
                .exceptionally(Messages::showExceptionOnEDT)
                .join();
    }

    private static Composition process(Composition comp,
                                       CompAction action) {
        assert EventQueue.isDispatchThread() : "not EDT thread";

        System.out.println("Automate::processFile: CALLED, comp = " + comp.getName());

        View view = comp.getView();

        view.paintImmediately();
        action.process(comp);
        view.paintImmediately();

        return comp;
    }

    private static CompletableFuture<Void> saveAndClose(Composition comp, File lastSaveDir) {
        View view = comp.getView();
        OutputFormat outputFormat = OutputFormat.getLastUsed();
        File outputFile = calcOutputFile(comp, lastSaveDir, outputFormat);
        CompletableFuture<Void> retVal = null;

        // so that it doesn't ask to save again after we just saved it
        comp.setDirty(false);

        SaveSettings saveSettings = new SaveSettings(outputFormat, outputFile);
        if (outputFile.exists() && !overwriteAll) {
            String answer = showOverwriteWarningDialog(outputFile);

            switch (answer) {
                case OVERWRITE_YES:
                    retVal = comp.saveAsync(saveSettings, false);
                    break;
                case OVERWRITE_YES_ALL:
                    retVal = comp.saveAsync(saveSettings, false);
                    overwriteAll = true;
                    break;
                case OVERWRITE_NO:
                    // do nothing
                    break;
                case OVERWRITE_CANCEL:
                    OpenComps.warnAndClose(view);
                    stopProcessing = true;
                    return CompletableFuture.completedFuture(null);
            }
        } else { // the file does not exist or overwrite all was pressed previously
            view.paintImmediately();
            retVal = comp.saveAsync(saveSettings, false);
        }
        OpenComps.warnAndClose(view);
        stopProcessing = false;
        if (retVal != null) {
            return retVal;
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static File calcOutputFile(Composition comp, File lastSaveDir, OutputFormat outputFormat) {
        String inFileName = comp.getFile().getName();
        String outFileName = FileUtils.replaceExt(inFileName, outputFormat.toString());
        return new File(lastSaveDir, outFileName);
    }

    private static String showOverwriteWarningDialog(File outputFile) {
        JOptionPane pane = new JOptionPane(
                format("File %s already exists. Overwrite?", outputFile),
                WARNING_MESSAGE);

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
        return answer;
    }
}