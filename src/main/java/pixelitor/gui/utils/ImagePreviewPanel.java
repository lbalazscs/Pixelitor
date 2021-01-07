/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.ProgressPanel;
import pixelitor.utils.ProgressTracker;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Image preview panel for the open file chooser
 */
public class ImagePreviewPanel extends JPanel implements PropertyChangeListener {
    private static final int SIZE = 200;
    public static final int EMPTY_SPACE_AT_LEFT = 5;

    private final Color backgroundColor;
    private final Map<String, SoftReference<ThumbInfo>> thumbsCache;
    private ThumbInfo thumbInfo;
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
        } else {
            if (FileUtils.hasSupportedInputExt(file)) {
                String filePath = file.getAbsolutePath();
                thumbInfo = getOrCreateThumb(file, filePath);
            } else {
                thumbInfo = null;
            }
        }
        repaint();
    }

    private ThumbInfo getOrCreateThumb(File file, String filePath) {
        if (thumbsCache.containsKey(filePath)) {
            SoftReference<ThumbInfo> thumbRef = thumbsCache.get(filePath);
            ThumbInfo cachedInfo = thumbRef.get();
            if (cachedInfo != null) {
                return cachedInfo;
            }
        }

        // Currently no thumb extraction is attempted for ora and pxc files.
        if (FileUtils.hasMultiLayerExtension(file)) {
            ThumbInfo fakeThumbInfo = ThumbInfo.failure(ThumbInfo.NO_PREVIEW);
            thumbsCache.put(filePath, new SoftReference<>(fakeThumbInfo));
            return fakeThumbInfo;
        }

        int availableWidth = getWidth() - EMPTY_SPACE_AT_LEFT;
        int availableHeight = getHeight();
        try {
            ProgressTracker pt = new JProgressBarTracker(progressPanel);
            ThumbInfo newThumbInfo = TrackedIO.readSubsampledThumb(file, availableWidth, availableHeight, pt);
            thumbsCache.put(filePath, new SoftReference<>(newThumbInfo));
            return newThumbInfo;
        } catch (Exception ex) {
            ThumbInfo fakeThumbInfo = ThumbInfo.failure(ThumbInfo.PREVIEW_ERROR);
            thumbsCache.put(filePath, new SoftReference<>(fakeThumbInfo));

            ex.printStackTrace();
            return fakeThumbInfo;
        }
    }

    private static File getFileFromFileChooserEvent(PropertyChangeEvent e) {
        return switch (e.getPropertyName()) {
            case JFileChooser.SELECTED_FILE_CHANGED_PROPERTY -> (File) e.getNewValue();
            default -> null;
        };
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(backgroundColor);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (thumbInfo != null) {
            thumbInfo.paint((Graphics2D) g, this);
        }
    }
}

