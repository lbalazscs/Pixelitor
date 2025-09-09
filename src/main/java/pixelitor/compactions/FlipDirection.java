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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.utils.AbstractViewEnabledAction;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.Messages;
import pixelitor.utils.Texts;

import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * The direction of a {@link Flip}.
 */
public enum FlipDirection {
    HORIZONTAL("flip_horizontal") {
        @Override
        public String getStatusBarMessage() {
            return "Image flipped horizontally";
        }

        @Override
        public AffineTransform createTransform(int width, int height) {
            var at = new AffineTransform();
            at.translate(width, 0); // keeps the content visible
            at.scale(-1, 1);
            return at;
        }
    }, VERTICAL("flip_vertical") {
        @Override
        public String getStatusBarMessage() {
            return "Image flipped vertically";
        }

        @Override
        public AffineTransform createTransform(int width, int height) {
            var at = new AffineTransform();
            at.translate(0, height); // keeps the content visible
            at.scale(1, -1);
            return at;
        }
    };

    private final String displayName;

    FlipDirection(String uiKey) {
        this.displayName = Texts.i18n(uiKey);
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract String getStatusBarMessage();

    public abstract AffineTransform createTransform(int width, int height);

    /**
     * Returns the transformation in image space, relative to the canvas.
     * Needed for transforming the selection.
     */
    public AffineTransform createCanvasTransform(Canvas canvas) {
        return createTransform(canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Returns the transformation for the image (image space, relative to the image).
     */
    public AffineTransform createImageTransform(BufferedImage image) {
        return createTransform(image.getWidth(), image.getHeight());
    }

    public Action createLayerTransformAction() {
        return new AbstractViewEnabledAction(getDisplayName()) {
            @Override
            protected void onClick(Composition comp) {
                if (comp.getActiveLayer() instanceof ImageLayer layer) {
                    layer.flip(FlipDirection.this, true);
                } else {
                    Messages.showNotImageLayerError(comp.getActiveLayer());
                }
            }
        };
    }
}
