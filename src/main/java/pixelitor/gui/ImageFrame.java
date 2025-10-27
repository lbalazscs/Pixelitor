/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Views;
import pixelitor.gui.utils.Themes;
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

    // extra space reserved for potential scrollbars
    private static final int SCROLLBAR_SAFETY_HOR = 20;
    private static final int SCROLLBAR_SAFETY_VER = 10;

    private final int frameDecorationHeight;

    private final View view;
    private final JScrollPane scrollPane;

    public ImageFrame(View view, int locX, int locY) {
        super(view.createTitleWithZoom(),
            true, true, true, true);
        frameDecorationHeight = Themes.getActive().getFrameDecorationHeight();

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
        // this event is fired both by user clicks and programmatic activation,
        // but it shouldn't matter as the following method handles both cases
        Views.viewActivated(view);
    }

    @Override
    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    @Override
    public void internalFrameClosing(InternalFrameEvent e) {
        Views.warnAndClose(view);
    }

    @Override
    public void internalFrameClosed(InternalFrameEvent e) {
        Views.viewClosed(view);
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
        setSize(view.getCanvas().getCoSize());
    }

    @Override
    public void setSize(int contentWidth, int contentHeight) {
        // the required size for the frame to hold the content
        int requiredWidth = contentWidth + SCROLLBAR_SAFETY_HOR;
        int requiredHeight = contentHeight + frameDecorationHeight + SCROLLBAR_SAFETY_VER;

        Point loc = getLocation();
        Dimension desktopSize = ImageArea.getSize();

        // the maximum allowed size at the current location
        int maxWidth = Math.max(0, desktopSize.width - loc.x);
        int maxHeight = Math.max(0, desktopSize.height - loc.y);

        // constrain the required size to the available space
        int finalWidth = Math.min(requiredWidth, maxWidth);
        int finalHeight = Math.min(requiredHeight, maxHeight);

        super.setSize(finalWidth, finalHeight);
    }

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
