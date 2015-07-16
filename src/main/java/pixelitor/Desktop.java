/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor;

import pixelitor.io.DropListener;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.beans.PropertyVetoException;
import java.util.List;

import static java.awt.Color.GRAY;

/**
 * The desktop area of the app
 */
public class Desktop {
    private static final int CASCADE_HORIZONTAL_SHIFT = 15;
    private static final int CASCADE_VERTICAL_SHIFT = 25;

    public static final Desktop INSTANCE = new Desktop();

    private final JDesktopPane desktopPane;

    private Desktop() {
        desktopPane = new JDesktopPane();
        GlobalKeyboardWatch.setAlwaysVisibleComponent(desktopPane);
        GlobalKeyboardWatch.registerBrushSizeActions();
        new DropTarget(desktopPane, new DropListener());

        desktopPane.setBackground(GRAY);
    }

    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    public void activateInternalImageFrame(InternalImageFrame frame) {
        assert frame != null;
        desktopPane.getDesktopManager().activateFrame(frame);
    }

    public void cascadeWindows() {
        List<ImageComponent> imageComponents = ImageComponents.getICList();
        int locationX = 0;
        int locationY = 0;
        for (ImageComponent ic : imageComponents) {
            InternalImageFrame internalFrame = ic.getInternalFrame();
            internalFrame.setLocation(locationX, locationY);
            internalFrame.setToNaturalSize(locationX, locationY);
            try {
                internalFrame.setIcon(false);
                internalFrame.setMaximum(false);
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }

            locationX += CASCADE_HORIZONTAL_SHIFT;
            locationY += CASCADE_VERTICAL_SHIFT;

            // wrap
            int maxWidth = desktopPane.getWidth() - CASCADE_HORIZONTAL_SHIFT;
            int maxHeight = desktopPane.getHeight() - CASCADE_VERTICAL_SHIFT;

            if (locationX > maxWidth) {
                locationX = 0;
            }
            if (locationY > maxHeight) {
                locationY = 0;
            }
        }
    }

    public void tileWindows() {
        List<ImageComponent> imageComponents = ImageComponents.getICList();
        int numComponents = imageComponents.size();

        int rows = (int) Math.sqrt(numComponents);
        int cols = numComponents / rows;
        int extra = numComponents % rows;

        int width = desktopPane.getWidth() / cols;
        int height = desktopPane.getHeight() / rows;
        int currentRow = 0;
        int currentColumn = 0;

        for (ImageComponent ic : imageComponents) {
            InternalImageFrame frame = ic.getInternalFrame();
            try {
                frame.setIcon(false);
                frame.setMaximum(false);
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
            frame.reshape(currentColumn * width, currentRow * height, width, height);
            currentRow++;
            if (currentRow == rows) {
                currentRow = 0;
                currentColumn++;
                if (currentColumn == cols - extra) {
                    rows++;
                    height = desktopPane.getHeight() / rows;
                }
            }
        }
    }

    public void addNewImageComponent(ImageComponent ic) {
        int nrOfOpenImages = ImageComponents.getNrOfOpenImages();

        // called deliberately after nrOfOpenImages is set
        ImageComponents.addIC(ic);

        int locationX = CASCADE_HORIZONTAL_SHIFT * nrOfOpenImages;
        int locationY = CASCADE_VERTICAL_SHIFT * nrOfOpenImages;

        int maxWidth = desktopPane.getWidth() - CASCADE_HORIZONTAL_SHIFT;
        locationX %= maxWidth;

        int maxHeight = desktopPane.getHeight() - CASCADE_VERTICAL_SHIFT;
        locationY %= maxHeight;

        InternalImageFrame internalFrame = new InternalImageFrame(ImageComponents.getActiveIC(), locationX, locationY);

        ImageComponents.getActiveIC().setInternalFrame(internalFrame);

        desktopPane.add(internalFrame);
        try {
            internalFrame.setSelected(true);
            desktopPane.getDesktopManager().activateFrame(internalFrame);
            ImageComponents.newImageOpened(ic.getComp());
        } catch (PropertyVetoException e) {
            Dialogs.showExceptionDialog(e);
        }
    }

    public Dimension getDesktopSize() {
        return desktopPane.getSize();
    }
}
