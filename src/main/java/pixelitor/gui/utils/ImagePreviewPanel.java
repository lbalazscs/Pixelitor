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

package pixelitor.gui.utils;

import pixelitor.io.FileUtils;
import pixelitor.io.TrackedIO;
import pixelitor.utils.JProgressBarTracker;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressPanel;
import pixelitor.utils.ProgressTracker;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
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

    private final ProgressPanel progressPanel;

    public ImagePreviewPanel(ProgressPanel progressPanel) {
        this.progressPanel = progressPanel;
        setPreferredSize(new Dimension(SIZE, SIZE));
        backgroundColor = getBackground();
        thumbsCache = new HashMap<>();

        this.progressPanel.setVisible(true);
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
        if (FileUtils.hasSupportedInputExt(file)) {
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
        try {
            int availableWidth = getWidth() - EMPTY_SPACE_AT_LEFT;
            int availableHeight = getHeight();

            ProgressTracker pt = new JProgressBarTracker(progressPanel);
            thumbInfo = TrackedIO.readSubsampledThumb(file, availableWidth, availableHeight, pt);
            thumbsCache.put(filePath, thumbInfo);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static File getFileFromFileChooserEvent(PropertyChangeEvent e) {
        File file;
        file = switch (e.getPropertyName()) {
            case JFileChooser.SELECTED_FILE_CHANGED_PROPERTY -> (File) e.getNewValue();
            default -> null;
        };
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

