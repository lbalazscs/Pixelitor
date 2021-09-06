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

package pixelitor.menus.file;

import pixelitor.Composition;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.OpenImageEnabledAction;
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
import java.util.concurrent.CompletableFuture;

import static pixelitor.OpenImages.getActiveComp;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;

public class PrintAction extends OpenImageEnabledAction implements Printable {
    private static final float DPI = 72.0f;

    private BufferedImage img;
    private String compName;
    private PageFormat page;

    public PrintAction() {
        super("Print...");
    }

    @Override
    public void onClick() {
        // The printed image will be the image at the start,
        // although it is editable during the asynchronous printing
        Composition comp = getActiveComp();
        img = comp.getCompositeImage();
        compName = comp.getName();

        showPreview();
    }

    private void showPreview() {
        PrinterJob job = PrinterJob.getPrinterJob();
        page = createDefaultPage(job, img);

        JPanel p = new JPanel(new BorderLayout());
        PrintPreviewPanel previewPanel = new PrintPreviewPanel(page, this);
        JButton setupPageButton = new JButton("Setup Page...");
        setupPageButton.addActionListener(e -> setupPageClicked(job, previewPanel));
        JPanel northPanel = new JPanel();
        northPanel.add(setupPageButton);
        p.add(northPanel, BorderLayout.NORTH);
        p.add(previewPanel, BorderLayout.CENTER);

        new DialogBuilder()
            .title("Print Preview")
            .content(p)
            .okText("Print...")
            .okAction(() -> previewAccepted(job))
            .show();
    }

    private void setupPageClicked(PrinterJob job, PrintPreviewPanel previewPanel) {
        PageFormat newPage = job.pageDialog(pageToAttributes(page));
        if (newPage != null) { // null if the dialog is canceled
            page = newPage;
            previewPanel.updatePage(page);
        }
    }

    private static PageFormat createDefaultPage(PrinterJob job, BufferedImage img) {
        PageFormat page = job.defaultPage();
        if (img.getWidth() > img.getHeight()) {
            page.setOrientation(PageFormat.LANDSCAPE);
        } else {
            page.setOrientation(PageFormat.PORTRAIT);
        }
        Paper paper = page.getPaper();

        float marginInch = 0.25f;
        float margin = marginInch * DPI;
        paper.setImageableArea(margin, margin,
            paper.getWidth() - 2 * margin, paper.getHeight() - 2 * margin);
        page.setPaper(paper);

        return page;
    }

    private void previewAccepted(PrinterJob job) {
        PrintRequestAttributeSet attributes = pageToAttributes(page);
        attributes.add(new PageRanges(1, 1));
        attributes.add(new JobName("Pixelitor " + compName, Locale.ENGLISH));

        boolean doPrint = job.printDialog(attributes);
        if (doPrint) {
            var progressHandler = Messages.startProgress(
                "Printing " + compName, -1);
            CompletableFuture.runAsync(() -> startPrintJob(job), onIOThread)
                .thenRunAsync(progressHandler::stopProgress, onEDT);
        }
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

    private void startPrintJob(PrinterJob job) {
        try {
            Book book = new Book();
            book.append(this, page);
            job.setPageable(book);
            job.print();
        } catch (PrinterException e) {
            Messages.showException(e);
        }
    }

    @Override
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
        if (page > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pf.getImageableX(), pf.getImageableY());

        double pageWidth = pf.getImageableWidth();
        double pageHeight = pf.getImageableHeight();
        double imageWidth = img.getWidth();
        double imageHeight = img.getHeight();
        double scaleX = pageWidth / imageWidth;
        double scaleY = pageHeight / imageHeight;
        double scale = Math.min(scaleX, scaleY);
        int actualWidth = (int) (imageWidth * scale);
        int actualHeight = (int) (imageHeight * scale);

        g.drawImage(img, 0, 0, actualWidth, actualHeight, null);

        return PAGE_EXISTS;
    }
}
