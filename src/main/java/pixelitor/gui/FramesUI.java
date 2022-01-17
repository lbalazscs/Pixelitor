/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
 * A user interface ({@link ImageAreaUI} implementation)
 * where the edited images are in internal frames
 */
public class FramesUI extends JDesktopPane implements ImageAreaUI {
    private static final int CASCADE_HORIZONTAL_SHIFT = 15;
    private static final int CASCADE_VERTICAL_SHIFT = 25;

    private static int cascadeIndex = 0;

    public FramesUI() {
    }

    @Override
    public void activateView(View view) {
        ImageFrame frame = (ImageFrame) view.getViewContainer();
        assert frame != null;
        activateFrame(frame);
    }

    @Override
    public void addNewView(View view) {
        int locX = CASCADE_HORIZONTAL_SHIFT * cascadeIndex;
        int locY = CASCADE_VERTICAL_SHIFT * cascadeIndex;

        int maxWidth = getWidth() - CASCADE_HORIZONTAL_SHIFT;
        locX %= maxWidth;

        int maxHeight = getHeight() - CASCADE_VERTICAL_SHIFT;
        locY %= maxHeight;

        ImageFrame frame = new ImageFrame(view, locX, locY);
        view.setViewContainer(frame);
        add(frame);
        activateFrame(frame);

        cascadeIndex++;
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
            Dialogs.showInfoDialog(this, "No open windows",
                "There are no open internal windows to cascade.");
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

            locX += CASCADE_HORIZONTAL_SHIFT;
            locY += CASCADE_VERTICAL_SHIFT;

            // wrap
            int maxWidth = getWidth() - CASCADE_HORIZONTAL_SHIFT;
            int maxHeight = getHeight() - CASCADE_VERTICAL_SHIFT;

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
            Dialogs.showInfoDialog(this, "No open windows",
                "There are no open internal windows to tile.");
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

    // ensure that the given frame is neither iconified nor maximized
    private static void ensureNormalDisplay(ImageFrame frame) {
        try {
            frame.setIcon(false);
            frame.setMaximum(false);
        } catch (PropertyVetoException e) {
            Messages.showException(e);
        }
    }

    public static void resetCascadeIndex() {
        cascadeIndex = 0;
    }
}
