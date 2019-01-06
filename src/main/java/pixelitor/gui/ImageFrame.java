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

package pixelitor.gui;

import pixelitor.Canvas;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;

/**
 * An {@link ImageWindow} used in the "internal frames" UI.
 */
public class ImageFrame extends JInternalFrame
        implements ImageWindow, InternalFrameListener {
    private static final int NIMBUS_HORIZONTAL_ADJUSTMENT = 18;
    private static final int NIMBUS_VERTICAL_ADJUSTMENT = 37;

    private final CompositionView cv;
    private final JScrollPane scrollPane;

    public ImageFrame(CompositionView cv, int locX, int locY) {
        super(cv.createTitleWithZoom(),
                true, true, true, true);
        addInternalFrameListener(this);
        setFrameIcon(null);
        this.cv = cv;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        scrollPane = new JScrollPane(this.cv);
        this.add(scrollPane);

        setLocation(locX, locY);
        setToNaturalSize();
        this.setVisible(true);
    }

    @Override
    public void internalFrameActivated(InternalFrameEvent e) {
        // We can get here as the result of a user click or as part
        // of a programmatic activation, but it shouldn't matter as all
        // activation takes place in the following method
        OpenComps.imageActivated(cv);
    }

    @Override
    public void internalFrameClosed(InternalFrameEvent e) {
        OpenComps.imageClosed(cv);
    }

    @Override
    public void internalFrameClosing(InternalFrameEvent e) {
        if (!RandomGUITest.isRunning()) {
            OpenComps.warnAndClose(cv);
        }
    }

    @Override
    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    @Override
    public void internalFrameDeiconified(InternalFrameEvent e) {
        cv.updateNavigator(true);
    }

    @Override
    public void internalFrameIconified(InternalFrameEvent e) {
        cv.updateNavigator(true);
    }

    @Override
    public void internalFrameOpened(InternalFrameEvent e) {
    }

    @Override
    public void select() {
        try {
            setSelected(true);
        } catch (PropertyVetoException e) {
            Messages.showException(e);
        }
    }

    public void setToNaturalSize() {
        Canvas canvas = cv.getCanvas();
        int zoomedWidth = canvas.getCoWidth();
        int zoomedHeight = canvas.getCoHeight();
        setSize(zoomedWidth, zoomedHeight);
    }

    @Override
    public void setSize(int width, int height) {
        Point loc = getLocation();
        int locX = loc.x;
        int locY = loc.y;

        Dimension desktopSize = ImageArea.getSize();
        int maxWidth = Math.max(0, desktopSize.width - 20 - locX);
        int maxHeight = Math.max(0, desktopSize.height - 40 - locY);

        if (width > maxWidth) {
            width = maxWidth;

            height += 15; // correction for the horizontal scrollbar
        }
        if (height > maxHeight) {
            height = maxHeight;

            width += 15; // correction for the vertical scrollbar
            if (width > maxWidth) { // check again
                width = maxWidth;
            }
        }

        super.setSize(width + NIMBUS_HORIZONTAL_ADJUSTMENT,
                height + NIMBUS_VERTICAL_ADJUSTMENT);
    }

    @Override
    public void ensurePositiveLocation() {
        Rectangle bounds = getBounds();
        if (bounds.x < 0 || bounds.y < 0) {
            int newX = bounds.x < 0 ? 0 : bounds.x;
            int newY = bounds.y < 0 ? 0 : bounds.y;
            setBounds(newX, newY, bounds.width, bounds.height);
        }
    }

    @Override
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    @Override
    public void updateTitle(CompositionView cv) {
        setTitle(cv.createTitleWithZoom());
    }
}
