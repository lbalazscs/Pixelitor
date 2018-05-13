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

package pixelitor.gui.utils;

import pixelitor.io.FileExtensionUtils;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Image preview panel for the open file chooser
 */
public class ImagePreviewPanel extends JPanel implements PropertyChangeListener {
    private static final int SIZE = 200;
    private static final int EMPTY_SPACE_AT_LEFT = 5;

    private final Color backgroundColor;
    private ThumbInfo thumbInfo;

    private static final int MSG_X = 20;
    private static final int MSG_Y = 10;

    private final Map<String, ThumbInfo> thumbsCache;

    public ImagePreviewPanel() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        backgroundColor = getBackground();
        thumbsCache = new HashMap<>();
    }

    // the property change events form the JFileChooser
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        File file = getFileFromFileChooserEvent(e);
        if (file == null) {
            thumbInfo = null;
            repaint();
            return;
        }

        String filePath = file.getAbsolutePath();
        if (FileExtensionUtils.hasSupportedInputExt(file)) {
            createThumbImage(file, filePath);
            repaint();
        }
    }

    private void createThumbImage(File file, String filePath) {
        if (thumbsCache.containsKey(filePath)) {
            thumbInfo = thumbsCache.get(filePath);
            return;
        }

        // TODO A problem is that ora and pxc files are reported as "Unrecognized"

        int availableWidth = getWidth() - EMPTY_SPACE_AT_LEFT;
        int availableHeight = getHeight();
        try {
            // TODO even the subsampled reading is slow for large
            // images - there could be a progress bar
            thumbInfo = ImageUtils.readSubsampledThumb(file, availableWidth, availableHeight);
        } catch (IOException ex) {
            Messages.showException(ex);
        }

        thumbsCache.put(filePath, thumbInfo);
    }

    private static File getFileFromFileChooserEvent(PropertyChangeEvent e) {
        File file;
        String propertyName = e.getPropertyName();
        switch (propertyName) {
            case JFileChooser.DIRECTORY_CHANGED_PROPERTY:
                file = null;
                break;
            case JFileChooser.SELECTED_FILE_CHANGED_PROPERTY:
                file = (File) e.getNewValue();
                break;
            default:
                file = null;
        }
        return file;
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(backgroundColor);
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        g.fillRect(0, 0, panelWidth, panelHeight);

        if (thumbInfo != null) {
            BufferedImage thumb = thumbInfo.getThumb();
            int x = (panelWidth - thumb.getWidth()) / 2 + EMPTY_SPACE_AT_LEFT;
            int y = (panelHeight - thumb.getHeight()) / 2;
            g.drawImage(thumb, x, y, this);

            int imgWidth = thumbInfo.getOrigWidth();
            int imgHeight = thumbInfo.getOrigHeight();
            String msg = "Size: " + imgWidth + " x " + imgHeight + " pixels";
            if (imgWidth == -1 || imgHeight == -1) {
                msg = "Unrecognized!";
            }

            g.setColor(BLACK);
            g.drawString(msg, MSG_X, MSG_Y);
            g.setColor(WHITE);
            g.drawString(msg, MSG_X - 1, MSG_Y - 1);
        }
    }
}

