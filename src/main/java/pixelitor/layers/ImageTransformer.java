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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.Debuggable;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

public class ImageTransformer implements ImageSource, Serializable, Debuggable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Composition content;
    private final AffineTransform transform;
    private int targetWidth;
    private int targetHeight;

    private transient BufferedImage cachedImage;

    public ImageTransformer(Composition content, AffineTransform transform,
                            int targetWidth, int targetHeight) {
        this.content = content;
        this.transform = transform;
        setTargetSize(targetWidth, targetHeight);
        assert content != null;
        assert transform != null;
    }

    public ImageTransformer copy(Composition newContent) {
        return new ImageTransformer(newContent, new AffineTransform(transform), targetWidth, targetHeight);
    }

    public void setContent(Composition content) {
        this.content = content;
        cachedImage = null;
    }

    private void setTargetSize(int targetWidth, int targetHeight) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    @Override
    public BufferedImage getImage() {
        if (cachedImage != null) {
            return cachedImage;
        }
        cachedImage = ImageUtils.applyTransform(content.getCompositeImage(),
            transform, targetWidth, targetHeight);
        return cachedImage;
    }

    public void concatenate(AffineTransform newScaling, int targetWidth, int targetHeight) {
//        debug(transform, "transform before");
//        debug(newScaling, "newScaling");
        transform.concatenate(newScaling);
//        debug(transform, "transform after");
        setTargetSize(targetWidth, targetHeight);
        cachedImage = null;
    }

    private static void debug(AffineTransform at, String msg) {
        double scaleX = at.getScaleX();
        double scaleY = at.getScaleY();
        System.out.printf("%s:, scaleX = %.2f, scaleY = %.2f%n", msg, scaleX, scaleY);
    }

    public BufferedImage getCachedImage() {
        return cachedImage;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addInt("target width", targetWidth);
        node.addInt("target height", targetHeight);

        node.addBoolean("cached", cachedImage != null);
        if (cachedImage != null) {
            node.addString("cached image size", cachedImage.getWidth() + "x" + cachedImage.getHeight());
        }

        node.add(DebugNodes.createTransformNode(transform, "transform"));

        return node;
    }
}
