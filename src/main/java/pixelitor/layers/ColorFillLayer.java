/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.CopyOptions;
import pixelitor.colors.Colors;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.PixelitorWindow;
import pixelitor.history.ColorFillLayerChangeEdit;
import pixelitor.history.History;
import pixelitor.history.NewLayerEdit;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

/**
 * A layer that fills the entire canvas with a uniform color.
 */
public class ColorFillLayer extends Layer {
    @Serial
    private static final long serialVersionUID = -5181774134094137901L;

    private Color color;

    private static int count;

    public ColorFillLayer(Composition comp, String name, Color color) {
        super(comp, name);
        this.color = color;
    }

    public static void createNew(Composition comp) {
        Tools.forceFinish();
        Color defaultColor = FgBgColors.getFgColor();
        ColorFillLayer layer = new ColorFillLayer(comp, generateName(), defaultColor);
        var activeLayerBefore = comp.getActiveLayer();
        var prevViewMode = comp.getView().getMaskViewMode();

        // don't add it to the history yet; wait until the user accepts the dialog
        LayerHolder holder = comp.getHolderForNewLayers();
        holder.add(layer);

        String dialogTitle = "Add Color Fill Layer";
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), dialogTitle,
            defaultColor, true, c -> layer.updateColor(c, false))) {
            // the dialog was accepted, so it's now safe to add the layer to the history
            History.add(new NewLayerEdit(dialogTitle,
                layer, activeLayerBefore, prevViewMode));
        } else {
            // dialog canceled
            holder.deleteLayer(layer, false);
        }
    }

    private static String generateName() {
        return "color fill " + (++count);
    }

    @Override
    public boolean showEditUI() {
        String dialogTitle = "Edit Color Fill Layer";
        Color prevColor = color;
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), dialogTitle,
            color, true, c -> updateColor(c, false))) {
            // the dialog was accepted, so add the new color to the history
            History.add(new ColorFillLayerChangeEdit(this, prevColor, color));
            return true;
        }
        return false;
    }

    public void updateColor(Color newColor, boolean addToHistory) {
        if (newColor.equals(this.color)) {
            return;
        }

        Color prevColor = this.color;
        this.color = newColor;
        update();
        updateIconImage();
        if (addToHistory) {
            History.add(new ColorFillLayerChangeEdit(this, prevColor, newColor));
        }
    }

    @Override
    public BufferedImage createIconThumbnail() {
        Dimension thumbDim = comp.getCanvas().getThumbSize();
        BufferedImage thumb = ImageUtils.createSysCompatibleImage(thumbDim);
        Colors.fillWith(color, thumb);
        return thumb;
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        JPopupMenu popup = super.createLayerIconPopupMenu();
        popup.addSeparator();

        popup.add(Colors.createCopyColorAction(() -> color));
        popup.add(Colors.createPasteColorAction(PixelitorWindow.get(), c -> updateColor(c, true)));

        return popup;
    }

    @Override
    protected ColorFillLayer createTypeSpecificCopy(CopyOptions options, Composition newComp) {
        String copyName = options.createLayerCopyName(name);
        // java.awt.Color is immutable, so it can be safely shared
        return new ColorFillLayer(newComp, copyName, color);
    }

    @Override
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
        Canvas canvas = comp.getCanvas();
        Colors.fillWith(color, g, canvas.getWidth(), canvas.getHeight());
    }

    @Override
    protected BufferedImage transformImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // do nothing
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle cropRect, boolean deleteCropped, boolean allowGrowing) {
        // do nothing
    }

    @Override
    public String getTypeString() {
        return "Color Fill Layer";
    }
}
