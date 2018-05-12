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
import pixelitor.utils.Messages;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
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
    private int newImgWidth;
    private int newImgHeight;

    private ImageInfo imageInfo;

    private static final int MSG_X = 20;
    private static final int MSG_Y = 10;

    private final Map<String, ImageInfo> thumbsCache;

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
            imageInfo = null;
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
            imageInfo = thumbsCache.get(filePath);
            return;
        }
        // we read the whole image and downscale it to a thumb
        // which is annoyingly slow for large images
        // TODO the image format might contain an embedded thumbnail

        // TODO or subsampling while reading!!
        // https://stackoverflow.com/questions/3294388/make-a-bufferedimage-use-less-ram

        // TODO or there might be already a thumb on the file system
        // in some special directory

        // TODO or there could be a progress bar like
        // https://stackoverflow.com/questions/24815494/using-jprogressbar-while-converting-image-to-byte-array
        // https://stackoverflow.com/questions/24835638/issues-with-swingworker-and-jprogressbar
        // http://www.java2s.com/Code/JavaAPI/javax.imageio.event/implementsIIOReadProgressListener.htm

        // TODO another problem is that ora and pxc files are reported as "Unrecognized"
        Image bigImage = null;
        try {
            bigImage = ImageIO.read(file);
        } catch (IOException ex) {
            Messages.showException(ex);
        }

        imageInfo = scaleImage(bigImage);
        thumbsCache.put(filePath, imageInfo);
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

    private ImageInfo scaleImage(Image img) {
        if (img == null) {
            return null;
        }

        int imgWidth = img.getWidth(null);
        int imgHeight = img.getHeight(null);

        int availableWidth = getWidth() - EMPTY_SPACE_AT_LEFT;
        int availableHeight = getHeight();

        double heightScale = availableHeight / (double) imgHeight;
        double widthScale = availableWidth / (double) imgWidth;
        double scale = Math.min(heightScale, widthScale);

        newImgWidth = (int) (scale * (double) imgWidth);
        newImgHeight = (int) (scale * (double) imgHeight);

        // perhaps the imgscalr library would be faster
        Image thumb = img.getScaledInstance(
                newImgWidth, newImgHeight, Image.SCALE_FAST);
        return new ImageInfo(thumb, imgWidth, imgHeight);
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(backgroundColor);
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        g.fillRect(0, 0, panelWidth, panelHeight);

        if (imageInfo != null) {
            int x = (panelWidth - newImgWidth) / 2 + EMPTY_SPACE_AT_LEFT;
            int y = (panelHeight - newImgHeight) / 2;
            g.drawImage(imageInfo.thumb, x, y, this);

//            boolean doubleDrawMsg = y < MSG_STRING_Y - 10;

            int imgWidth = imageInfo.origWidth;
            int imgHeight = imageInfo.origHeight;
            String msg = "Size: " + imgWidth + " x " + imgHeight + " pixels";
            if (imgWidth == -1 || imgHeight == -1) {
                msg = "Unrecognized!";
//                doubleDrawMsg = false;
            }

            g.setColor(BLACK);
            g.drawString(msg, MSG_X, MSG_Y);
//            if (doubleDrawMsg) {
                g.setColor(WHITE);
            g.drawString(msg, MSG_X - 1, MSG_Y - 1);
//            }
        }
    }

    private static class ImageInfo {
        public Image thumb;

        // these sizes refer to the original, not to the thumb!
        public int origWidth;
        public int origHeight;

        public ImageInfo(Image thumb, int origWidth, int origHeight) {
            this.thumb = thumb;
            this.origWidth = origWidth;
            this.origHeight = origHeight;
        }
    }
}

