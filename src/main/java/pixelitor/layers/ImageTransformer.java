/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

/**
 * An object that can apply an affine transformation to the output image
 * of a smart object's content before the image is processed by the smart filters.
 */
public class ImageTransformer implements ImageSource, Serializable {
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
        ImageTransformer copy = new ImageTransformer(newContent, new AffineTransform(transform), targetWidth, targetHeight);

        // can be shared because the new content is
        // either identical to the old one or a copy of it
        copy.cachedImage = cachedImage;

        return copy;
    }

    public void setContent(Composition content) {
        this.content = content;
        invalidateCache();
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

    public void chainTransform(AffineTransform newTransform, int targetWidth, int targetHeight) {
        transform.preConcatenate(newTransform);
        setTargetSize(targetWidth, targetHeight);
        invalidateCache();
    }

    public BufferedImage getCachedImage() {
        return cachedImage;
    }

    public void invalidateCache() {
        cachedImage = null;
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

        node.add(DebugNodes.createTransformNode("transform", transform));

        return node;
    }
}
