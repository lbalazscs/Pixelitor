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
import pixelitor.CopyType;
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
 * A color fill layer that fills the entire canvas with a given color.
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
        ColorFillLayer layer = new ColorFillLayer(comp, generateName(), null);
        var activeLayerBefore = comp.getActiveLayer();
        var prevViewMode = comp.getView().getMaskViewMode();

        // don't add it yet to history, only after the user accepts the dialog
        LayerHolder holder = comp.getHolderForNewLayers();
        holder.add(layer);

        String dialogTitle = "Add Color Fill Layer";
        Color defaultColor = FgBgColors.getFGColor();
        layer.changeColor(defaultColor, false);
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), dialogTitle,
            defaultColor, true, c -> layer.changeColor(c, false))) {
            // dialog accepted, now it is safe to add it to the history
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
    public boolean edit() {
        String dialogTitle = "Edit Color Fill Layer";
        Color prevColor = color;
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), dialogTitle,
            color, true, c -> changeColor(c, false))) {
            // adds an edit to the history only after the dialog is accepted
            History.add(new ColorFillLayerChangeEdit(this, prevColor, color));
            return true;
        }
        return false;
    }

    public void changeColor(Color color, boolean addHistory) {
        Color prevColor = this.color;
        this.color = color;
        holder.update();
        updateIconImage();
        if (addHistory) {
            History.add(new ColorFillLayerChangeEdit(this, prevColor, color));
        }
    }

    @Override
    public BufferedImage createIconThumbnail() {
        Dimension thumbDim = comp.getCanvas().getThumbSize();
        BufferedImage thumb = ImageUtils.createSysCompatibleImage(thumbDim);
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

        popup.add(Colors.createCopyColorAction(() -> color));
        popup.add(Colors.createPasteColorAction(PixelitorWindow.get(), c -> changeColor(c, true)));

        return popup;
    }

    @Override
    protected ColorFillLayer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        String copyName = copyType.createLayerCopyName(name);
        // java.awt.Color is immutable => it can be shared
        return new ColorFillLayer(comp, copyName, color);
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
