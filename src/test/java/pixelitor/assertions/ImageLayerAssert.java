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

package pixelitor.assertions;

import pixelitor.layers.ImageLayer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom AssertJ assertions for {@link ImageLayer} objects.
 */
public class ImageLayerAssert<S extends ImageLayerAssert<S, T>, T extends ImageLayer> extends ContentLayerAssert<S, ImageLayer> {
    public ImageLayerAssert(ImageLayer actual, Class<S> selfType) {
        super(actual, selfType);
    }

    public ImageLayerAssert stateIs(ImageLayer.State expected) {
        isNotNull();
        if (actual.getState() != expected) {
            throw new AssertionError("Expected " + expected +
                                     ", found " + actual.getState());
        }
        return this;
    }

    public ImageLayerAssert imageIs(BufferedImage image) {
        isNotNull();
        assertThat(actual.getImage()).isSameAs(image);
        return this;
    }

    public ImageLayerAssert previewImageIs(BufferedImage image) {
        isNotNull();
        assertThat(actual.getPreviewImage()).isSameAs(image);
        return this;
    }

    public ImageLayerAssert previewImageIsNot(BufferedImage image) {
        isNotNull();
        assertThat(actual.getPreviewImage()).isNotSameAs(image);
        return this;
    }

    public ImageLayerAssert imageSizeIs(int width, int height) {
        isNotNull();

        BufferedImage img = actual.getImage();
        Dimension foundSize = new Dimension(img.getWidth(), img.getHeight());
        Dimension expectedSize = new Dimension(width, height);
        assertThat(foundSize).isEqualTo(expectedSize);

        return this;
    }
}
