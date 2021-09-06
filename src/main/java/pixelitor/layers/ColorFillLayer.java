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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.colors.Colors;
import pixelitor.gui.PixelitorWindow;
import pixelitor.history.History;
import pixelitor.history.NewLayerEdit;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import static pixelitor.layers.LayerButtonLayout.thumbSize;

public class ColorFillLayer extends Layer {
    private Color color;

    private ColorFillLayer(Composition comp, String name, Color color) {
        super(comp, name);
        this.color = color;
    }

    public static void createNew() {
        Tools.forceFinish();
        var comp = OpenImages.getActiveComp();
        ColorFillLayer layer = new ColorFillLayer(comp, "color fill", null);
        var activeLayerBefore = comp.getActiveLayer();
        var oldViewMode = comp.getView().getMaskViewMode();
        // don't add it yet to history, only after the user presses OK (and not Cancel!)
        new Composition.LayerAdder(comp).add(layer);

        String title = "Add Color Fill Layer";
        Color defaultColor = Rnd.createRandomColor();
        layer.changeColor(defaultColor);
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), title,
            defaultColor, true, layer::changeColor)) {
            // dialog accepted, now it is safe to add it to the history
            History.add(new NewLayerEdit(title,
                comp, layer, activeLayerBefore, oldViewMode));
        } else {
            // dialog cancelled
            comp.deleteLayer(layer, false);
        }
    }

    @Override
    public void edit() {
        String title = "Edit Color Fill Layer";
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), title,
            color, true, this::changeColor)) {
            // TODO add to history
        }
    }

    private void changeColor(Color color) {
        this.color = color;
        comp.update();
        updateIconImage();
    }

    @Override
    public BufferedImage createIconThumbnail() {
        Canvas canvas = comp.getCanvas();
        Dimension thumbDim = ImageUtils.calcThumbDimensions(
            canvas.getWidth(), canvas.getHeight(), thumbSize);
        BufferedImage thumb = ImageUtils.createSysCompatibleImage(thumbDim.width, thumbDim.height);
        Graphics2D g2 = thumb.createGraphics();
        Colors.fillWith(color, g2, thumbDim.width, thumbDim.height);
        g2.dispose();
        return thumb;
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        JPopupMenu popup = super.createLayerIconPopupMenu();
        if (popup == null) {
            popup = new JPopupMenu();
        } else {
            popup.addSeparator();
        }

        Colors.setupCopyColorPopupMenu(popup, () -> color);
        Colors.setupPasteColorPopupMenu(popup, PixelitorWindow.get(), this::changeColor);

        return popup;
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        Color colorCopy = new Color(this.color.getRGB(), true);
        return new ColorFillLayer(comp, duplicateName, colorCopy);
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        Canvas canvas = comp.getCanvas();
        Colors.fillWith(color, g, canvas.getWidth(), canvas.getHeight());
    }

    @Override
    protected BufferedImage applyOnImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // do nothing
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCroppedPixels, boolean allowGrowing) {
        // do nothing
    }

    @Override
    public String getTypeStringLC() {
        return "color fill layer";
    }

    @Override
    public String getTypeStringUC() {
        return "Color Fill Layer";
    }
}
