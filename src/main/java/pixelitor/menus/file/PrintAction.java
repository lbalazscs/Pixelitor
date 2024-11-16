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

package pixelitor.menus.file;

import pixelitor.Composition;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.utils.Messages;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.*;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;

public class PrintAction extends OpenViewEnabledAction.Checked implements Printable {
    private static final float DPI = 72.0f;
    private static final float PAGE_MARGIN_INCHES = 0.25f;
    private final ResourceBundle resourceBundle;

    private BufferedImage printedImage;
    private String compName;
    private PageFormat pageFormat;

    public PrintAction(ResourceBundle resourceBundle) {
        super(resourceBundle.getString("print") + "...");
        this.resourceBundle = resourceBundle;
    }

    @Override
    protected void onClick(Composition comp) {
        // Capture the current composition image and name.
        // This image will be printed even if the composition is
        // changed during the asynchronous printing.
        printedImage = comp.getCompositeImage();
        compName = comp.getName();

        showPrintPreview();
    }

    private void showPrintPreview() {
        PrinterJob job = PrinterJob.getPrinterJob();
        pageFormat = createDefaultPageFormat(job, printedImage);

        JPanel previewContainer = createPreviewContainer(job);

        new DialogBuilder()
            .title(resourceBundle.getString("print_preview"))
            .content(previewContainer)
            .okText(resourceBundle.getString("print") + "...")
            .okAction(() -> previewAccepted(job))
            .show();
    }

    private JPanel createPreviewContainer(PrinterJob job) {
        JPanel previewContainer = new JPanel(new BorderLayout());
        PrintPreviewPanel previewPanel = new PrintPreviewPanel(pageFormat, this);
        
        JButton setupPageButton = new JButton("Setup Page...");
        setupPageButton.addActionListener(e -> showPageSetupDialog(job, previewPanel));
        JPanel northPanel = new JPanel();
        northPanel.add(setupPageButton);

        previewContainer.add(northPanel, BorderLayout.NORTH);
        previewContainer.add(previewPanel, BorderLayout.CENTER);
        return previewContainer;
    }

    private void showPageSetupDialog(PrinterJob job, PrintPreviewPanel previewPanel) {
        PageFormat newPage = job.pageDialog(pageToAttributes(pageFormat));
        if (newPage != null) { // null if the dialog is canceled
            pageFormat = newPage;
            previewPanel.updatePage(pageFormat);
        }
    }

    private static PageFormat createDefaultPageFormat(PrinterJob job, BufferedImage img) {
        PageFormat format = job.defaultPage();
        format.setOrientation(img.getWidth() > img.getHeight() ?
            PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
        Paper paper = format.getPaper();

        float marginInDots = PAGE_MARGIN_INCHES * DPI;
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

        boolean diagogAccepted = job.printDialog(attributes);
        if (diagogAccepted) {
            startAsyncPrinting(job);
        }
    }

    private void startAsyncPrinting(PrinterJob job) {
        var progressHandler = Messages.startProgress(
            "Printing " + compName, -1);

        CompletableFuture.supplyAsync(() -> executePrintJob(job), onIOThread)
            .thenAcceptAsync(printingCompleted -> {
                progressHandler.stopProgress();
                if (!printingCompleted) {
                    Messages.showStatusMessage("Printing was cancelled.");
                }
            }, onEDT);
    }

    // Converts a PageFormat into a PrintRequestAttributeSet
    // in order to avoid the native page dialog.
    private static PrintRequestAttributeSet pageToAttributes(PageFormat page) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(DialogTypeSelection.COMMON);

        attributes.add(switch (page.getOrientation()) {
            case PageFormat.LANDSCAPE -> OrientationRequested.LANDSCAPE;
            case PageFormat.PORTRAIT -> OrientationRequested.PORTRAIT;
            case PageFormat.REVERSE_LANDSCAPE -> OrientationRequested.REVERSE_LANDSCAPE;
            default -> throw new IllegalStateException("Unexpected value: " + page.getOrientation());
        });

        Paper paper = page.getPaper();
        attributes.add(new MediaPrintableArea(
            (float) paper.getImageableX() / DPI,
            (float) paper.getImageableY() / DPI,
            (float) paper.getImageableWidth() / DPI,
            (float) paper.getImageableHeight() / DPI,
            MediaPrintableArea.INCH));

        attributes.add(MediaSize.findMedia(
            (float) paper.getWidth() / DPI,
            (float) paper.getHeight() / DPI,
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
            // user cancelled printing at the OS level
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

        // Scale to fit the image within the printable area
        // while maintaining the aspect ratio.
        double pageWidth = pf.getImageableWidth();
        double pageHeight = pf.getImageableHeight();
        double imageWidth = printedImage.getWidth();
        double imageHeight = printedImage.getHeight();

        double scaleX = pageWidth / imageWidth;
        double scaleY = pageHeight / imageHeight;
        double scale = Math.min(scaleX, scaleY);
        int scaledWidth = (int) (imageWidth * scale);
        int scaledHeight = (int) (imageHeight * scale);

        g.drawImage(printedImage, 0, 0, scaledWidth, scaledHeight, null);

        return PAGE_EXISTS;
    }
}
