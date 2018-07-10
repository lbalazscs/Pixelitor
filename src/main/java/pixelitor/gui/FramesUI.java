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

import pixelitor.utils.Messages;

import javax.swing.*;
import java.beans.PropertyVetoException;
import java.util.List;

/**
 * An user interface where the edited images are in internal frames
 */
public class FramesUI extends JDesktopPane implements ImageAreaUI {
    private static final int CASCADE_HORIZONTAL_SHIFT = 15;
    private static final int CASCADE_VERTICAL_SHIFT = 25;

    public static int cascadeIndex = 0;

    public FramesUI() {
    }

    @Override
    public void activateIC(ImageComponent ic) {
        ImageFrame frame = (ImageFrame) ic.getImageWindow();
        assert frame != null;
        getDesktopManager().activateFrame(frame);
    }

    @Override
    public void addNewIC(ImageComponent ic) {
        int locX = CASCADE_HORIZONTAL_SHIFT * cascadeIndex;
        int locY = CASCADE_VERTICAL_SHIFT * cascadeIndex;

        int maxWidth = this.getWidth() - CASCADE_HORIZONTAL_SHIFT;
        locX %= maxWidth;

        int maxHeight = this.getHeight() - CASCADE_VERTICAL_SHIFT;
        locY %= maxHeight;

        ImageFrame frame = new ImageFrame(ic, locX, locY);
        ic.setImageWindow(frame);

        this.add(frame);
        try {
            frame.setSelected(true);
            this.getDesktopManager().activateFrame(frame);
            ImageComponents.newImageOpened(ic.getComp());
        } catch (PropertyVetoException e) {
            Messages.showException(e);
        }

        cascadeIndex++;
    }

    public void cascadeWindows() {
        List<ImageComponent> icList = ImageComponents.getICList();
        int locX = 0;
        int locY = 0;
        for (ImageComponent ic : icList) {
            ImageFrame frame = (ImageFrame) ic.getImageWindow();
            frame.setLocation(locX, locY);
            frame.setToNaturalSize();
            try {
                frame.setIcon(false);
                frame.setMaximum(false);
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }

            locX += CASCADE_HORIZONTAL_SHIFT;
            locY += CASCADE_VERTICAL_SHIFT;

            // wrap
            int maxWidth = this.getWidth() - CASCADE_HORIZONTAL_SHIFT;
            int maxHeight = this.getHeight() - CASCADE_VERTICAL_SHIFT;

            if (locX > maxWidth) {
                locX = 0;
            }
            if (locY > maxHeight) {
                locY = 0;
            }
        }
    }

    public void tileWindows() {
        List<ImageComponent> icList = ImageComponents.getICList();
        int numWindows = icList.size();

        int numRows = (int) Math.sqrt(numWindows);
        int numCols = numWindows / numRows;
        int extra = numWindows % numRows;

        int frameWidth = this.getWidth() / numCols;
        int frameHeight = this.getHeight() / numRows;
        int currRow = 0;
        int currCol = 0;

        for (ImageComponent ic : icList) {
            ImageFrame frame = (ImageFrame) ic.getImageWindow();
            try {
                frame.setIcon(false);
                frame.setMaximum(false);
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
            int frameX = currCol * frameWidth;
            int frameY = currRow * frameHeight;
            frame.reshape(frameX, frameY, frameWidth, frameHeight);
            currRow++;
            if (currRow == numRows) {
                currRow = 0;
                currCol++;
                if (currCol == numCols - extra) {
                    numRows++;
                    frameHeight = this.getHeight() / numRows;
                }
            }
        }
    }
}
