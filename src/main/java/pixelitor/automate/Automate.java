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

package pixelitor.automate;

import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.InternalImageFrame;
import pixelitor.PixelitorWindow;
import pixelitor.io.FileChoosers;
import pixelitor.io.FileExtensionUtils;
import pixelitor.io.OpenSaveManager;
import pixelitor.io.OutputFormat;
import pixelitor.utils.CompositionAction;
import pixelitor.utils.Dialogs;
import pixelitor.utils.Utils;
import pixelitor.utils.ValidatedDialog;

import javax.swing.*;
import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility class with static methods to support batch processing
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
     * Processes each file in the input directory with the given CompositionAction
     */
    public static void processEachFile(final CompositionAction action,
                                       final boolean closeImagesAfterProcessing,
                                       String progressMonitorTitle) {
        File lastOpenDir = FileChoosers.getLastOpenDir();
        if (lastOpenDir == null) {
            throw new IllegalStateException("lastOpenDir is null");
        }
        if (!lastOpenDir.exists()) {
            throw new IllegalStateException("Last open dir " + lastOpenDir.getAbsolutePath() + " does not exist");
        }

        final File lastSaveDir = FileChoosers.getLastSaveDir();
        if (lastSaveDir == null) {
            throw new IllegalStateException("lastSaveDir is null");
        }
        if (!lastSaveDir.exists()) {
            throw new IllegalStateException("Last save dir " + lastSaveDir.getAbsolutePath() + " does not exist");
        }

        final File[] children = FileExtensionUtils.getAllSupportedFilesInDir(lastOpenDir);
        if (children.length == 0) {
            Dialogs.showInfoDialog("No files", "There are no supported files in " + lastOpenDir.getAbsolutePath());
            return;
        }

        final ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor(progressMonitorTitle);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                overwriteAll = false;

                for (int i = 0, nrOfFiles = children.length; i < nrOfFiles; i++) {
                    if (progressMonitor.isCanceled()) {
                        break;
                    }

                    final File file = children[i];
                    progressMonitor.setProgress((int) ((float) i * 100 / nrOfFiles));
                    progressMonitor.setNote("Processing " + file.getName());

                    System.out.println("Processing " + file.getName());

                    Runnable edtTask = () -> processFile(file, action, lastSaveDir, closeImagesAfterProcessing);
                    try {
                        EventQueue.invokeAndWait(edtTask);
                    } catch (InterruptedException | InvocationTargetException e) {
                        Dialogs.showExceptionDialog(e);
                    }

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

    private static void processFile(File file,
                                    CompositionAction action,
                                    File lastSaveDir,
                                    boolean closeImagesAfterProcessing) {
        OpenSaveManager.openFile(file);
        Composition comp = ImageComponents.getActiveComp().get();

        InternalImageFrame frame = comp.getIC().getInternalFrame();
        frame.paintImmediately(frame.getBounds());

        action.process(comp);

        OutputFormat outputFormat = OutputFormat.getLastOutputFormat();

        String inputFileName = file.getName();
        String outFileName = FileExtensionUtils.replaceExtension(inputFileName, outputFormat.toString());

        File outputFile = new File(lastSaveDir, outFileName);

        if (outputFile.exists() && (!overwriteAll)) {
            String message = String.format("File %s already exists. Overwrite?", outputFile);
            JOptionPane pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE);

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
                    outputFormat.saveComposition(comp, outputFile);
                    break;
                case OVERWRITE_YES_ALL:
                    outputFormat.saveComposition(comp, outputFile);
                    overwriteAll = true;
                    break;
                case OVERWRITE_NO:
                    // do nothing
                    break;
                case OVERWRITE_CANCEL:
                    if (closeImagesAfterProcessing) {
                        OpenSaveManager.warnAndCloseImage(comp.getIC());
                    }
                    stopProcessing = true;
                    return;
            }
        } else { // the file does not exist or overwrite all was pressed previously
            frame.paintImmediately(frame.getBounds());
            outputFormat.saveComposition(comp, outputFile);
        }
        if (closeImagesAfterProcessing) {
            OpenSaveManager.warnAndCloseImage(comp.getIC());
        }
        stopProcessing = false;
    }

    /**
     * Lets the user select the input and output directory properties of the application.
     *
     * @param allowToBeTheSame
     * @param dialogTitle
     * @return true if a selection was made, false if the operation was cancelled
     */
    public static boolean selectInputAndOutputDir(boolean allowToBeTheSame, String dialogTitle) {
        OpenSaveDirsPanel p = new OpenSaveDirsPanel(allowToBeTheSame);
        ValidatedDialog chooser = new ValidatedDialog(p, PixelitorWindow.getInstance(), dialogTitle);
        chooser.setVisible(true);
        if (!chooser.isOkPressed()) {
            return false;
        }
        p.saveValues();

        return true;
    }
}