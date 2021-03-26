/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.OpenImages;
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
import static pixelitor.utils.Threads.*;

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
    public static void processFiles(CompAction action, String dialogTitle) {
        assert calledOnEDT() : threadInfo();

        File openDir = Dirs.getLastOpen();
        File saveDir = Dirs.getLastSave();

        List<File> inputFiles = FileUtils.listSupportedInputFilesIn(openDir);
        if (inputFiles.isEmpty()) {
            String msg = "There are no supported files in " + openDir.getAbsolutePath();
            Messages.showInfo("No files", msg);
            return;
        }

        stopProcessing = false;
        var pm = GUIUtils.createPercentageProgressMonitor(dialogTitle);
        var worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                return processFilesOutsideTheEDT(inputFiles, action, saveDir, pm);
            } // end of doInBackground
        };
        worker.execute();
    }

    private static Void processFilesOutsideTheEDT(List<File> inputFiles,
                                                  CompAction action,
                                                  File saveDir,
                                                  ProgressMonitor monitor) {
        assert calledOutsideEDT() : "on EDT";

        overwriteAll = false;

        for (int i = 0, numFiles = inputFiles.size(); i < numFiles; i++) {
            if (monitor.isCanceled() || stopProcessing) {
                break;
            }

            File file = inputFiles.get(i);
            monitor.setProgress((int) ((float) i * 100 / numFiles));

            String msg = "Processing " + (i + 1) + " of " + numFiles;
            monitor.setNote(msg);
            System.out.println(msg);

            processFile(file, action, saveDir);
        }
        monitor.close();
        return null;
    }

    private static void processFile(File file, CompAction action, File saveDir) {
        assert calledOutsideEDT() : "on EDT";
        IO.openFileAsync(file, false)
            .thenComposeAsync(comp -> process(comp, action), onEDT)
            .thenComposeAsync(comp -> saveAndClose(comp, saveDir), onEDT)
            .exceptionally(Messages::showExceptionOnEDT)
            .join();
    }

    private static CompletableFuture<Composition> process(Composition comp,
                                                          CompAction action) {
        assert calledOnEDT() : threadInfo();
        assert comp.getView() != null : "no view for " + comp.getName();

        return action.process(comp);
    }

    private static CompletableFuture<Void> saveAndClose(Composition comp, File lastSaveDir) {
        assert calledOnEDT() : threadInfo();

        View view = comp.getView();
        assert view != null : "no view for " + comp.getName();

        var format = FileFormat.getLastOutput();
        File file = calcOutputFile(comp, lastSaveDir, format);

        // so that it doesn't ask to save again after we just saved it
        comp.setDirty(false);

        var saveSettings = new SaveSettings(format, file);
        CompletableFuture<Void> retVal = null;

        if (file.exists() && !overwriteAll) {
            String answer = showOverwriteWarningDialog(file);

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
                    OpenImages.warnAndClose(view);
                    stopProcessing = true;
                    return CompletableFuture.completedFuture(null);
                default:
                    throw new IllegalStateException("Unexpected value: " + answer);
            }
        } else { // the file does not exist or overwrite all was pressed previously
            view.paintImmediately();
            retVal = comp.saveAsync(saveSettings, false);
        }
        OpenImages.warnAndClose(view);
        stopProcessing = false;
        if (retVal != null) {
            return retVal;
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static File calcOutputFile(Composition comp, File lastSaveDir, FileFormat format) {
        String inFileName = comp.getFile().getName();
        String outFileName = FileUtils.replaceExt(inFileName, format.toString());
        return new File(lastSaveDir, outFileName);
    }

    private static String showOverwriteWarningDialog(File outputFile) {
        String msg = format("File %s already exists. Overwrite?", outputFile);
        var optionPane = new JOptionPane(msg, WARNING_MESSAGE);

        optionPane.setOptions(new String[]{
            OVERWRITE_YES, OVERWRITE_YES_ALL, OVERWRITE_NO, OVERWRITE_CANCEL});
        optionPane.setInitialValue(OVERWRITE_NO);

        JDialog dialog = optionPane.createDialog(PixelitorWindow.get(), "Warning");
        dialog.setVisible(true);
        String value = (String) optionPane.getValue();
        String answer;

        if (value == null) { // cancelled
            answer = OVERWRITE_CANCEL;
        } else {
            answer = value;
        }
        return answer;
    }
}