/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.OpenImages;
import pixelitor.utils.Messages;

import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;

/**
 * A {@link ViewContainer} used in the {@link FramesUI}.
 */
public class ImageFrame extends JInternalFrame
    implements ViewContainer, InternalFrameListener {

    private static final int NIMBUS_HORIZONTAL_ADJUSTMENT = 18;
    private static final int NIMBUS_VERTICAL_ADJUSTMENT = 37;

    private final View view;
    private final JScrollPane scrollPane;

    public ImageFrame(View view, int locX, int locY) {
        super(view.createTitleWithZoom(),
            true, true, true, true);
        addInternalFrameListener(this);
        setFrameIcon(null);
        this.view = view;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        scrollPane = new JScrollPane(this.view);
        add(scrollPane);

        setLocation(locX, locY);
        setToCanvasSize();
        setVisible(true);
    }

    @Override
    public void internalFrameOpened(InternalFrameEvent e) {
    }

    @Override
    public void internalFrameActivated(InternalFrameEvent e) {
        // We can get here as the result of a user click or as part
        // of a programmatic activation, but it shouldn't matter as all
        // activation takes place in the following method
        OpenImages.viewActivated(view);
    }

    @Override
    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    @Override
    public void internalFrameClosing(InternalFrameEvent e) {
        OpenImages.warnAndClose(view);
    }

    @Override
    public void internalFrameClosed(InternalFrameEvent e) {
        OpenImages.imageClosed(view);
    }

    @Override
    public void internalFrameIconified(InternalFrameEvent e) {
        view.repaintNavigator(true);
    }

    @Override
    public void internalFrameDeiconified(InternalFrameEvent e) {
        view.repaintNavigator(true);
    }

    @Override
    public void close() {
        dispose();
    }

    @Override
    public void select() {
        try {
            setSelected(true);
        } catch (PropertyVetoException e) {
            Messages.showException(e);
        }
    }

    public void setToCanvasSize() {
        Canvas canvas = view.getCanvas();
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
            int newX = Math.max(bounds.x, 0);
            int newY = Math.max(bounds.y, 0);
            setBounds(newX, newY, bounds.width, bounds.height);
        }
    }

    @Override
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    @Override
    public void updateTitle(View view) {
        setTitle(view.createTitleWithZoom());
    }
}
