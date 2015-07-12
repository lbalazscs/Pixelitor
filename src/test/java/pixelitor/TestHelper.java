/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor;

import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class TestHelper {
    public static final int sizeX = 20;
    public static final int sizeY = 10;

    public static ImageLayer createTestImageLayer(String layerName, Composition comp) {
        BufferedImage image = createTestImage();
        ImageLayer layer = new ImageLayer(comp, image, layerName, null);
        comp.addLayerNoGUI(layer);

        return layer;
    }

    public static Composition createEmptyTestComposition() {
        ImageDisplayStub imageDisplayStub = new ImageDisplayStub();

        Composition comp = Composition.empty(sizeX, sizeY);
        comp.setImageComponent(imageDisplayStub);

        imageDisplayStub.setComp(comp);

        return comp;
    }

    public static Composition create2LayerTestComposition() {
        Composition c = createEmptyTestComposition();

        ImageLayer layer1 = createTestImageLayer("layer 1", c);
        ImageLayer layer2 = createTestImageLayer("layer 2", c);

        c.setActiveLayer(layer1, AddToHistory.NO);

        assert layer1 == c.getActiveLayer();
        assert layer1 == c.getLayer(0);
        assert layer2 == c.getLayer(1);

        return c;
    }

    public static BufferedImage createTestImage() {
        return new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_ARGB);
    }

    public static Graphics2D createGraphics() {
        return createTestImage().createGraphics();
    }

}
