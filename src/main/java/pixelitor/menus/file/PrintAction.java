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

package pixelitor.menus.file;

import pixelitor.Composition;
import pixelitor.gui.utils.AbstractViewEnabledAction;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.DimensionHelper;
import pixelitor.menus.file.AlignmentSelector.Alignment;
import pixelitor.utils.Messages;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static pixelitor.menus.file.AlignmentSelector.HorizontalAlignment;
import static pixelitor.menus.file.AlignmentSelector.VerticalAlignment;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;

public class PrintAction extends AbstractViewEnabledAction implements Printable {
    // this is not the image's DPI or the printer's DPI,
    // it's just the resolution of the Java2D "user space"
    private static final float JAVA_2D_DPI = 72.0f;

    private static final float DEFAULT_PAGE_MARGIN_INCHES = 0.25f;
    private final ResourceBundle resourceBundle;

    public enum ScalingMode {ACTUAL_SIZE, FIT_TO_PAGE}
    private ScalingMode scalingMode = ScalingMode.ACTUAL_SIZE;

    private Alignment alignment = new Alignment(
        HorizontalAlignment.CENTER,
        VerticalAlignment.CENTER
    );
    private int imageDpi;

    private BufferedImage printedImage;
    private String compName;
    private PageFormat pageFormat;
    private AlignmentSelector alignmentSelector;

    public PrintAction(ResourceBundle resourceBundle) {
        super(resourceBundle.getString("print") + "...");
        this.resourceBundle = resourceBundle;
    }

    @Override
    protected void onClick(Composition comp) {
        // snapshot the current inputs, so they are not affected
        // if the composition is changed during the asynchronous printing
        printedImage = comp.getCompositeImage();
        compName = comp.getName();
        imageDpi = comp.getDpi();

        alignmentSelector = new AlignmentSelector();
        alignmentSelector.setAlignment(alignment);

        showPrintPreview(comp);
    }

    private void showPrintPreview(Composition comp) {
        PrinterJob job = PrinterJob.getPrinterJob();
        pageFormat = createDefaultPageFormat(job, printedImage);

        JPanel previewContainer = createPreviewContainer(job, comp);

        new DialogBuilder()
            .title(resourceBundle.getString("print_preview"))
            .content(previewContainer)
            .okText(resourceBundle.getString("print") + "...")
            .okAction(() -> previewAccepted(job))
            .show();
    }

    /**
     * Creates the main content panel for the print preview dialog.
     */
    private JPanel createPreviewContainer(PrinterJob job, Composition comp) {
        JPanel previewContainer = new JPanel(new BorderLayout());
        PrintPreviewPanel previewPanel = new PrintPreviewPanel(pageFormat, this);

        JPanel optionsPanel = createOptionsPanel(job, comp, previewPanel);

        previewContainer.add(optionsPanel, BorderLayout.NORTH);
        previewContainer.add(previewPanel, BorderLayout.CENTER);
        return previewContainer;
    }

    // creates the panel for all options at the top
    private JPanel createOptionsPanel(PrinterJob job, Composition comp, PrintPreviewPanel previewPanel) {
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Print Options"));

        // create all components
        JComboBox<String> scalingCombo = new JComboBox<>(new String[]{
            "Actual Size (100%)", "Fit Page"
        });
        scalingCombo.setSelectedIndex(scalingMode == ScalingMode.ACTUAL_SIZE ? 0 : 1);

        JComboBox<Integer> dpiCombo = new JComboBox<>(DimensionHelper.DPI_VALUES);
        dpiCombo.setSelectedItem(imageDpi);

        JButton setupPageButton = new JButton("Setup Page...");

        // add the listeners
        scalingCombo.addActionListener(e -> {
            scalingMode = scalingCombo.getSelectedIndex() == 0 ? ScalingMode.ACTUAL_SIZE : ScalingMode.FIT_TO_PAGE;
            previewPanel.repaint();
            dpiCombo.setEnabled(scalingMode == ScalingMode.ACTUAL_SIZE);
        });

        alignmentSelector.addActionListener(e -> {
            alignment = alignmentSelector.getAlignment();
            previewPanel.repaint();
        });

        dpiCombo.addActionListener(e -> {
            Integer selectedDpi = (Integer) dpiCombo.getSelectedItem();
            if (selectedDpi != null) {
                imageDpi = selectedDpi;
                previewPanel.repaint();
                comp.setDpi(imageDpi);
            }
        });

        setupPageButton.addActionListener(e -> showPageSetupDialog(job, previewPanel));

        // lay them out
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        optionsPanel.add(setupPageButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        optionsPanel.add(new JLabel("Scaling:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;
        optionsPanel.add(scalingCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        optionsPanel.add(new JLabel("DPI:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;
        optionsPanel.add(dpiCombo, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridheight = 1; // reset to default
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        optionsPanel.add(new JLabel("Alignment:"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridheight = 3; // spanning 3 rows
        gbc.anchor = GridBagConstraints.CENTER;
        optionsPanel.add(alignmentSelector, gbc);

        return optionsPanel;
    }

    /**
     * Shows the page setup dialog and updates the preview if changes are made.
     */
    private void showPageSetupDialog(PrinterJob job, PrintPreviewPanel previewPanel) {
        PageFormat newPageFormat = job.pageDialog(pageToAttributes(pageFormat));
        if (newPageFormat != null) { // null if the dialog is canceled
            pageFormat = newPageFormat;
            previewPanel.updatePage(newPageFormat);
        }
    }

    /**
     * Creates a default page format with sensible margins and orientation.
     */
    private static PageFormat createDefaultPageFormat(PrinterJob job, BufferedImage img) {
        PageFormat format = job.defaultPage();
        format.setOrientation(img.getWidth() > img.getHeight() ?
            PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
        Paper paper = format.getPaper();

        float marginInDots = DEFAULT_PAGE_MARGIN_INCHES * JAVA_2D_DPI;
        paper.setImageableArea(
            marginInDots,
            marginInDots,
            paper.getWidth() - 2 * marginInDots,
            paper.getHeight() - 2 * marginInDots);
        format.setPaper(paper);

        return format;
    }

    private void previewAccepted(PrinterJob job) {
        PrintRequestAttributeSet attributes = pageToAttributes(pageFormat);
        attributes.add(new PageRanges(1, 1));
        attributes.add(new JobName("Pixelitor " + compName, Locale.ENGLISH));

        boolean dialogAccepted = job.printDialog(attributes);
        if (dialogAccepted) {
            startAsyncPrinting(job);
        }
    }

    /**
     * Executes the print job on a background thread and shows an indeterminate progress indicator in the status bar.
     */
    private void startAsyncPrinting(PrinterJob job) {
        var progressHandler = Messages.startProgress(
            "Printing " + compName, -1);

        CompletableFuture.supplyAsync(() -> executePrintJob(job), onIOThread)
            .thenAcceptAsync(printingCompleted -> {
                progressHandler.stopProgress();
                if (!printingCompleted) {
                    Messages.showStatusMessage("Printing was canceled.");
                }
            }, onEDT);
    }

    /**
     * Converts a PageFormat to a PrintRequestAttributeSet for configuring cross-platform dialogs.
     */
    private static PrintRequestAttributeSet pageToAttributes(PageFormat page) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        // use the cross-platform dialog for a consistent UI
        attributes.add(DialogTypeSelection.COMMON);

        attributes.add(switch (page.getOrientation()) {
            case PageFormat.LANDSCAPE -> OrientationRequested.LANDSCAPE;
            case PageFormat.PORTRAIT -> OrientationRequested.PORTRAIT;
            case PageFormat.REVERSE_LANDSCAPE -> OrientationRequested.REVERSE_LANDSCAPE;
            default -> throw new IllegalStateException("Unexpected value: " + page.getOrientation());
        });

        Paper paper = page.getPaper();
        attributes.add(new MediaPrintableArea(
            (float) paper.getImageableX() / JAVA_2D_DPI,
            (float) paper.getImageableY() / JAVA_2D_DPI,
            (float) paper.getImageableWidth() / JAVA_2D_DPI,
            (float) paper.getImageableHeight() / JAVA_2D_DPI,
            MediaPrintableArea.INCH));

        attributes.add(MediaSize.findMedia(
            (float) paper.getWidth() / JAVA_2D_DPI,
            (float) paper.getHeight() / JAVA_2D_DPI,
            Size2DSyntax.INCH));

        return attributes;
    }

    private boolean executePrintJob(PrinterJob job) {
        try {
            Book book = new Book();
            book.append(this, pageFormat);
            job.setPageable(book);
            job.print();
        } catch (PrinterAbortException e) {
            // user canceled printing at the OS level
            return false;
        } catch (PrinterException e) {
            Messages.showException(e);
            return false;
        }
        return true;
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pf.getImageableX(), pf.getImageableY());

        double pageWidth = pf.getImageableWidth();
        double pageHeight = pf.getImageableHeight();

        double imageWidth = printedImage.getWidth();
        double imageHeight = printedImage.getHeight();

        double scaledWidth;
        double scaledHeight;

        switch (scalingMode) {
            case ACTUAL_SIZE:
                // convert image pixels to points (1/72 inch) based on image DPI
                scaledWidth = imageWidth * (JAVA_2D_DPI / imageDpi);
                scaledHeight = imageHeight * (JAVA_2D_DPI / imageDpi);
                break;
            case FIT_TO_PAGE:
            default:
                // scale to fit the image within the printable area while maintaining its aspect ratio
                double scaleX = pageWidth / imageWidth;
                double scaleY = pageHeight / imageHeight;
                double scale = Math.min(scaleX, scaleY);
                scaledWidth = imageWidth * scale;
                scaledHeight = imageHeight * scale;
                break;
        }

        double x = switch (alignment.horizontal()) {
            case LEFT -> 0.0;
            case CENTER -> (pageWidth - scaledWidth) / 2.0;
            case RIGHT -> pageWidth - scaledWidth;
        };

        double y = switch (alignment.vertical()) {
            case TOP -> 0.0;
            case CENTER -> (pageHeight - scaledHeight) / 2.0;
            case BOTTOM -> pageHeight - scaledHeight;
        };

        g.drawImage(printedImage, (int) Math.round(x), (int) Math.round(y),
            (int) Math.round(scaledWidth), (int) Math.round(scaledHeight), null);

        return PAGE_EXISTS;
    }
}
