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
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

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

    /**
     * Chains a new transformation, applying it to the output of the existing transform.
     *
     * @param newTransform The new transformation to apply. It is defined in the coordinate
     *                     space of the output of the current transform.
     * @param targetWidth  The final width of the image after the new transformation is applied.
     * @param targetHeight The final height of the image after the new transformation is applied.
     * @see AffineTransform#preConcatenate(AffineTransform)
     */
    public void chainTransform(AffineTransform newTransform, int targetWidth, int targetHeight) {
        // maintain the correct logical order by preconcatenating
        transform.preConcatenate(newTransform);

        setTargetSize(targetWidth, targetHeight);
        invalidateCache();
    }

    @Override
    public BufferedImage getImage() {
        if (cachedImage != null) {
            return cachedImage;
        }
        cachedImage = applyTransform(content.getCompositeImage(),
            transform, targetWidth, targetHeight);
        return cachedImage;
    }

    private static BufferedImage applyTransform(BufferedImage src, AffineTransform at, int targetWidth, int targetHeight) {
        assert targetWidth > 0 && targetHeight > 0 : "target = " + targetWidth + "x" + targetHeight;
        BufferedImage newImage = new BufferedImage(targetWidth, targetHeight, TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.setTransform(at);
        if (targetWidth > src.getWidth() || targetHeight > src.getHeight()) {
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        } else {
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        }
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return newImage;
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
