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

package pixelitor.gui.utils;

import pixelitor.io.*;
import pixelitor.utils.JProgressBarTracker;
import pixelitor.utils.ProgressPanel;
import pixelitor.utils.ProgressTracker;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Image preview panel for the open file chooser
 */
public class ImagePreviewPanel extends JPanel implements PropertyChangeListener {
    private static final int SIZE = 200;
    public static final int EMPTY_SPACE_AT_LEFT = 5;
    private static final Map<String, SoftReference<ThumbInfo>> thumbsCache = new HashMap<>();

    private ThumbInfo thumbInfo;
    private final ProgressPanel progressPanel;

    public ImagePreviewPanel(ProgressPanel progressPanel) {
        this.progressPanel = progressPanel;
        setPreferredSize(new Dimension(SIZE, SIZE));

        this.progressPanel.setVisible(true);
    }

    // the property change events form the JFileChooser
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(e.getPropertyName())) {
            File file = (File) e.getNewValue();
            if (file != null && FileUtils.hasSupportedInputExt(file)) {
                thumbInfo = getOrCreateThumb(file);
            } else {
                thumbInfo = null;
            }
        } else {
            thumbInfo = null;
        }

        repaint();
    }

    private ThumbInfo getOrCreateThumb(File file) {
        String filePath = file.getAbsolutePath();
        if (thumbsCache.containsKey(filePath)) {
            SoftReference<ThumbInfo> thumbRef = thumbsCache.get(filePath);
            ThumbInfo cachedInfo = thumbRef.get();
            if (cachedInfo != null) {
                return cachedInfo;
            }
        }

        ThumbInfo readResult = readThumb(file);
        if (readResult.isSuccess()) {
            // don't cache failures - perhaps the user
            // is retrying after fixing the problem
            thumbsCache.put(filePath, new SoftReference<>(readResult));
        }
        return readResult;
    }

    private ThumbInfo readThumb(File file) {
        if (!Files.isReadable(file.toPath())) {
            return ThumbInfo.failure(ThumbInfo.PREVIEW_ERROR);
        }

        String extension = FileUtils.getExtension(file.getName());

        if ("pxc".equalsIgnoreCase(extension)) {
            try {
                BufferedImage thumbnail = PXCFormat.readThumbnail(file);
                if (thumbnail == null) {
                    // old pxc file, without thumbnail
                    return ThumbInfo.failure(ThumbInfo.NO_PREVIEW);
                }
                return ThumbInfo.success(thumbnail);
            } catch (BadPxcFormatException e) {
                // not in pxc format
                return ThumbInfo.failure(ThumbInfo.PREVIEW_ERROR);
            }
        }

        if ("ora".equalsIgnoreCase(extension)) {
            try {
                BufferedImage thumbnail = OpenRaster.readThumbnail(file);
                if (thumbnail == null) {
                    // old ora file, without thumbnail
                    return ThumbInfo.failure(ThumbInfo.NO_PREVIEW);
                }
                return ThumbInfo.success(thumbnail);
            } catch (IOException e) {
                // not zip format
                return ThumbInfo.failure(ThumbInfo.PREVIEW_ERROR);
            }
        }

        int availableWidth = getWidth() - EMPTY_SPACE_AT_LEFT;
        int availableHeight = getHeight();
        try {
            ProgressTracker pt = new JProgressBarTracker(progressPanel);
            return TrackedIO.readThumbnail(file, availableWidth, availableHeight, pt);
        } catch (Exception ex) {
            return ThumbInfo.failure(ThumbInfo.PREVIEW_ERROR);
        }
    }

    public static void removeThumbFromCache(File file) {
        thumbsCache.remove(file.getAbsolutePath());
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (thumbInfo != null) {
            thumbInfo.paint((Graphics2D) g, this);
        }
    }
}

