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

package pixelitor.gui;

import pixelitor.Views;
import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.beans.PropertyVetoException;
import java.util.List;

/**
 * An {@link ImageAreaUI} implementation
 * where the edited images are in internal frames
 */
public final class FramesUI extends JDesktopPane implements ImageAreaUI {
    private static final int CASCADE_OFFSET_X = 15;
    private static final int CASCADE_OFFSET_Y = 25;

    private static int cascadeCount = 0;

    public FramesUI() {
    }

    @Override
    public void activateView(View view) {
        ImageFrame frame = (ImageFrame) view.getViewContainer();
        assert frame != null;
        activateFrame(frame);
    }

    @Override
    public void addView(View view) {
        // calculate the location of the next frame
        int locX = CASCADE_OFFSET_X * cascadeCount;
        int locY = CASCADE_OFFSET_Y * cascadeCount;
        int availableWidth = getWidth() - CASCADE_OFFSET_X;
        int availableHeight = getHeight() - CASCADE_OFFSET_Y;
        // wrap coordinates to keep frames within visible area
        locX %= availableWidth;
        locY %= availableHeight;

        ImageFrame frame = new ImageFrame(view, locX, locY);
        view.setViewContainer(frame);
        add(frame);
        activateFrame(frame);

        cascadeCount++;
    }

    private void activateFrame(ImageFrame frame) {
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            Messages.showException(e);
        }
        getDesktopManager().activateFrame(frame);
    }

    public void cascadeWindows() {
        if (Views.getNumViews() == 0) {
            showNoViewsDialog("cascade");
            return;
        }
        List<View> views = Views.getAll();
        int locX = 0;
        int locY = 0;

        for (View view : views) {
            ImageFrame frame = (ImageFrame) view.getViewContainer();
            frame.setLocation(locX, locY);
            frame.setToCanvasSize();
            ensureNormalDisplay(frame);

            locX += CASCADE_OFFSET_X;
            locY += CASCADE_OFFSET_Y;

            // wrap
            int maxWidth = getWidth() - CASCADE_OFFSET_X;
            int maxHeight = getHeight() - CASCADE_OFFSET_Y;

            if (locX > maxWidth) {
                locX = 0;
            }
            if (locY > maxHeight) {
                locY = 0;
            }
        }
    }

    public void tileWindows() {
        int numWindows = Views.getNumViews();
        if (numWindows == 0) {
            showNoViewsDialog("tile");
            return;
        }

        int numRows = (int) Math.sqrt(numWindows);
        int numCols = numWindows / numRows;
        int extra = numWindows % numRows;

        int frameWidth = getWidth() / numCols;
        int frameHeight = getHeight() / numRows;
        int currRow = 0;
        int currCol = 0;

        List<View> views = Views.getAll();
        for (View view : views) {
            ImageFrame frame = (ImageFrame) view.getViewContainer();
            ensureNormalDisplay(frame);
            int frameX = currCol * frameWidth;
            int frameY = currRow * frameHeight;
            frame.reshape(frameX, frameY, frameWidth, frameHeight);
            currRow++;
            if (currRow == numRows) {
                currRow = 0;
                currCol++;
                if (currCol == numCols - extra) {
                    numRows++;
                    frameHeight = getHeight() / numRows;
                }
            }
        }
    }

    private void showNoViewsDialog(String action) {
        Dialogs.showInfoDialog(this, "No Open Images",
            "There are no open images to " + action + ".");
    }

    // ensure that the given frame is neither iconified nor maximized
    private static void ensureNormalDisplay(ImageFrame frame) {
        try {
            frame.setIcon(false);
            frame.setMaximum(false);
        } catch (PropertyVetoException e) {
            Messages.showException(e);
        }
    }

    public static void resetCascadeCount() {
        cascadeCount = 0;
    }
}
