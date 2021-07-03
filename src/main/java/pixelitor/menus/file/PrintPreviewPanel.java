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

import pixelitor.gui.utils.Dialogs;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

public class PrintPreviewPanel extends JPanel {
    private static final int SIZE = 500;
    private static final int MARGIN = 20;
    private Rectangle2D paperRect;
    private final Dimension size = new Dimension(SIZE, SIZE);
    private PageFormat pageFormat;
    private final Printable printable;
    private double scaling;

    public PrintPreviewPanel(PageFormat pageFormat, Printable printable) {
        this.pageFormat = pageFormat;
        this.printable = printable;

        setPageFormat(pageFormat);
    }

    private void setPageFormat(PageFormat pageFormat) {
        this.pageFormat = pageFormat;

        // the paper is within the margin, centered
        double aspectRatio = pageFormat.getWidth() / pageFormat.getHeight();
        double maxRectSize = SIZE - 2 * MARGIN;
        if (aspectRatio > 1) { // fit to the width
            double rectWith = maxRectSize;
            double rectHeight = rectWith / aspectRatio;
            paperRect = new Rectangle2D.Double(
                MARGIN, (SIZE - rectHeight) / 2, rectWith, rectHeight);
            scaling = rectWith / pageFormat.getWidth();
        } else { // fit to the height
            double rectHeight = maxRectSize;
            double rectWidth = rectHeight * aspectRatio;
            paperRect = new Rectangle2D.Double(
                (SIZE - rectWidth) / 2, MARGIN, rectWidth, rectHeight);
            scaling = rectHeight / pageFormat.getHeight();
        }
    }

    public void updatePage(PageFormat pageFormat) {
        setPageFormat(pageFormat);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        AffineTransform origTransform = g2.getTransform();
        g2.setColor(Color.WHITE);
        g2.fill(paperRect);

        // Simulate the actual printing Graphics.
        // A difference is that this one doesn't implement PrinterGraphics.
        g2.setClip(paperRect);
        g2.translate(paperRect.getX(), paperRect.getY());
        g2.scale(scaling, scaling);
        try {
            printable.print(g2, pageFormat, 0);
        } catch (PrinterException e) {
            Dialogs.showExceptionDialog(e);
        }
        g2.setTransform(origTransform);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return size;
    }
}
