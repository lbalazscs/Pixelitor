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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.Gradient;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class GradientFillLayer extends Layer {
    private Gradient gradient;

    private GradientFillLayer(Composition comp, String name) {
        super(comp, name);
    }

    public static void createNew() {
        var comp = OpenImages.getActiveComp();
        var layer = new GradientFillLayer(comp, "gradient fill");
        new Composition.LayerAdder(comp)
            .withHistory("Add Gradient Fill Layer")
            .add(layer);
        Tools.startAndSelect(Tools.GRADIENT);
    }

    @Override
    public void edit() {
        Tools.startAndSelect(Tools.GRADIENT);
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        return null;
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        if (gradient != null) {
            // TODO first visible layer
            gradient.drawOnGraphics(g, comp, comp.getCanvasWidth(), comp.getCanvasHeight(), false);
        }
    }

    @Override
    protected BufferedImage applyOnImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage createIconThumbnail() {
        if (gradient != null) {
            return gradient.createIconThumbnail(comp.getCanvas());
        }
        return null;
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        return null;
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCroppedPixels, boolean allowGrowing) {

    }

    public Gradient getGradient() {
        return gradient;
    }

    public void setGradient(Gradient gradient) {
        assert gradient != null;
        this.gradient = gradient;
        comp.update();
        updateIconImage();
    }

    @Override
    public String getTypeStringLC() {
        return "gradient fill layer";
    }

    @Override
    public String getTypeStringUC() {
        return "Gradient Fill Layer";
    }
}
