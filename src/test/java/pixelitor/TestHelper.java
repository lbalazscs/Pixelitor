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

import pixelitor.filters.Invert;
import pixelitor.filters.painters.TextSettings;
import pixelitor.layers.AdjustmentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMaskAddType;
import pixelitor.layers.TextLayer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class TestHelper {
    public static final int sizeX = 20;
    public static final int sizeY = 10;

    public static ImageLayer createImageLayer(String layerName, Composition comp) {
        BufferedImage image = createImage();
        ImageLayer layer = new ImageLayer(comp, image, layerName, null);
//        comp.addLayerNoGUI(layer);

        return layer;
    }

    public static TextLayer createTextLayer(Composition comp, String name) {
        TextLayer textLayer = new TextLayer(comp, name);
        textLayer.setSettings(TextSettings.createRandomSettings(new Random()));
        return textLayer;
    }

    public static Composition createEmptyComposition() {
        ImageDisplayStub imageDisplayStub = new ImageDisplayStub();

        Composition comp = Composition.empty(sizeX, sizeY);
        comp.setImageComponent(imageDisplayStub);
        comp.setName("Test");

        imageDisplayStub.setComp(comp);

        return comp;
    }

    public static Composition create2LayerComposition(boolean addMasks) {
        Composition c = createEmptyComposition();

        ImageLayer layer1 = createImageLayer("layer 1", c);
        ImageLayer layer2 = createImageLayer("layer 2", c);

        c.addLayerNoGUI(layer1);
        c.addLayerNoGUI(layer2);

        if (addMasks) {
            layer1.addMask(LayerMaskAddType.REVEAL_ALL);
            layer2.addMask(LayerMaskAddType.REVEAL_ALL);
        }

        assert layer2 == c.getActiveLayer();
        assert layer1 == c.getLayer(0);
        assert layer2 == c.getLayer(1);

        c.setDirty(false);

        return c;
    }

    public static BufferedImage createImage() {
        return new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_ARGB);
    }

    public static Graphics2D createGraphics() {
        return createImage().createGraphics();
    }

    public static Layer classToLayer(Class layerClass, Composition comp) {
        Layer layer;
        if (layerClass.equals(ImageLayer.class)) {
            layer = new ImageLayer(comp, "layer 1");
        } else if (layerClass.equals(TextLayer.class)) {
            layer = createTextLayer(comp, "layer 1");
        } else if (layerClass.equals(AdjustmentLayer.class)) {
            layer = new AdjustmentLayer(comp, "layer 1", new Invert());
        } else {
            throw new IllegalStateException();
        }
        return layer;
    }
}
