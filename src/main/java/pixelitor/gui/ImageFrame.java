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

package pixelitor.gui;

import pixelitor.Canvas;
import pixelitor.io.OpenSaveManager;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * A JInternalFrame for displaying the compositions
 */
public class ImageFrame extends JInternalFrame implements InternalFrameListener {
    private static final int NIMBUS_HORIZONTAL_ADJUSTMENT = 18;
    private static final int NIMBUS_VERTICAL_ADJUSTMENT = 37;

    private final ImageComponent ic;
    private final JScrollPane scrollPane;

    public ImageFrame(ImageComponent ic, int locX, int locY) {
        super(ic.createFrameTitle(), true, true, true, true);
        addInternalFrameListener(this);
        setFrameIcon(null);
        this.ic = ic;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        scrollPane = new JScrollPane(this.ic);
        this.add(scrollPane);

        Dimension ps = ic.getPreferredSize();
        setSize(locX, locY, (int) ps.getWidth(), (int) ps.getHeight());
        setLocation(locX, locY);
        this.setVisible(true);
    }

    @Override
    public void internalFrameActivated(InternalFrameEvent e) {
        ImageComponents.activeImageHasChanged(ic);
        ic.onActivation();
    }

    @Override
    public void internalFrameClosed(InternalFrameEvent e) {
        ImageComponents.imageClosed(ic);
    }

    @Override
    public void internalFrameClosing(InternalFrameEvent e) {
        if (!RandomGUITest.isRunning()) {
            OpenSaveManager.warnAndCloseImage(ic);
        }
    }

    @Override
    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    @Override
    public void internalFrameDeiconified(InternalFrameEvent e) {
        ic.updateNavigator(true);
    }

    @Override
    public void internalFrameIconified(InternalFrameEvent e) {
        ic.updateNavigator(true);
    }

    @Override
    public void internalFrameOpened(InternalFrameEvent e) {
    }

    public void setToNaturalSize(int locX, int locY) {
        Canvas canvas = ic.getCanvas();
        int zoomedWidth = canvas.getZoomedWidth();
        int zoomedHeight = canvas.getZoomedHeight();
        setSize(locX, locY, zoomedWidth, zoomedHeight);
    }

    public void setSize(int locX, int locY, int width, int height) {
        // if this is a simple resize, then locX and locY are -1
        if (locX == -1) {
            locX = getLocation().x;
        }
        if (locY == -1) {
            locY = getLocation().y;
        }

        Dimension desktopSize = Desktop.INSTANCE.getDesktopSize();
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

        setSize(width + NIMBUS_HORIZONTAL_ADJUSTMENT, height + NIMBUS_VERTICAL_ADJUSTMENT);
    }

    public void makeSureItIsVisible() {
        Rectangle bounds = getBounds();
        if (bounds.x < 0 || bounds.y < 0) {
            int newX = bounds.x < 0 ? 0 : bounds.x;
            int newY = bounds.y < 0 ? 0 : bounds.y;
            setBounds(newX, newY, bounds.width, bounds.height);
        }
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}

