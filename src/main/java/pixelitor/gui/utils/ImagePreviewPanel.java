/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * Image preview panel for the open file chooser.
 */
public class ImagePreviewPanel extends JPanel implements PropertyChangeListener {
    private static final int PANEL_SIZE = 200;
    public static final int LEFT_MARGIN = 5;

    // supports removing key-value pairs corresponding
    // to garbage-collected thumbnails from the cache
    private static final ReferenceQueue<ThumbInfo> gcQueue = new ReferenceQueue<>();

    // a soft reference to a thumbnail that remembers its associated cache key
    private static class KeyedSoftReference extends SoftReference<ThumbInfo> {
        final String key;

        KeyedSoftReference(String key, ThumbInfo referent) {
            super(referent, gcQueue); // associate the thumb with the queue
            this.key = key;
        }
    }

    private static final int MAX_CACHE_SIZE = 200;
    private static final Map<String, KeyedSoftReference> thumbsCache =
        new LinkedHashMap<String, KeyedSoftReference>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, KeyedSoftReference> eldest) {
                return size() > MAX_CACHE_SIZE; // LRU cache
            }
        };

    // information about the currently shown thumbnail
    private ThumbInfo thumbInfo;

    private final ProgressPanel progressPanel;

    public ImagePreviewPanel(ProgressPanel progressPanel) {
        this.progressPanel = progressPanel;
        setPreferredSize(new Dimension(PANEL_SIZE, PANEL_SIZE));

        this.progressPanel.setVisible(true);
    }

    // the property change events from the JFileChooser
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
        if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(propertyName)) {
            File file = (File) e.getNewValue();
            if (file != null && FileUtils.hasSupportedInputExt(file)) {
                thumbInfo = getOrCreateThumb(file);
            } else {
                thumbInfo = null;
            }
            repaint();
        } else if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(propertyName)) {
            thumbInfo = null;
            repaint();
        }
    }

    private ThumbInfo getOrCreateThumb(File file) {
        removeDeadThumbs();

        String filePath = file.getAbsolutePath();
        KeyedSoftReference thumbRef = thumbsCache.get(filePath);
        if (thumbRef != null) {
            ThumbInfo cachedInfo = thumbRef.get();
            if (cachedInfo != null) {
                return cachedInfo;
            }
        }

        ThumbInfo readResult = readThumb(file);
        if (readResult.hasImage()) {
            // don't cache failures - perhaps the user
            // is retrying after fixing the problem
            thumbsCache.put(filePath, new KeyedSoftReference(filePath, readResult));
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
                    // old PXC file without an embedded thumbnail
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

        int panelWidth = getWidth();
        if (panelWidth <= 0) { // the panel is not laid out yet
            panelWidth = PANEL_SIZE;
        }
        int availableWidth = panelWidth - LEFT_MARGIN;

        int panelHeight = getHeight();
        if (panelHeight <= 0) {
            panelHeight = PANEL_SIZE;
        }
        int availableHeight = panelHeight;

        ProgressTracker pt = new JProgressBarTracker(progressPanel);
        try {
            return TrackedIO.readThumbnail(file, availableWidth, availableHeight, pt);
        } catch (IOException ex) {
            return ThumbInfo.failure(ThumbInfo.PREVIEW_ERROR);
        } finally {
            pt.finished();
        }
    }

    // called when Pixelitor itself overwrites a file
    public static void removeThumbFromCache(File file) {
        assert calledOnEDT() : callInfo(); // access the cache only on the EDT

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

    // removes garbage-collected thumbnails from the cache
    private static void removeDeadThumbs() {
        KeyedSoftReference ref;
        while ((ref = (KeyedSoftReference) gcQueue.poll()) != null) {
            // using remove(key, value) prevents a race condition
            // between the EDT and the GC where we might accidentally
            // remove a newly created live reference under the same key
            thumbsCache.remove(ref.key, ref);
        }
    }
}
