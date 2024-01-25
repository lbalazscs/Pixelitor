/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import java.awt.geom.Rectangle2D;
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
        ColorFillLayer layer = new ColorFillLayer(comp, createName(), null);
        var activeLayerBefore = comp.getActiveLayer();
        var oldViewMode = comp.getView().getMaskViewMode();
        // don't add it yet to history, only after the user presses OK (and not Cancel!)
        LayerHolder holder = comp.getHolderForNewLayers();
        holder.add(layer);

        String title = "Add Color Fill Layer";
        Color defaultColor = FgBgColors.getFGColor();
        layer.changeColor(defaultColor, false);
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), title,
            defaultColor, true, c -> layer.changeColor(c, false))) {
            // dialog accepted, now it is safe to add it to the history
            History.add(new NewLayerEdit(title,
                layer, activeLayerBefore, oldViewMode));
        } else {
            // dialog cancelled
            holder.deleteLayer(layer, false);
        }
    }

    private static String createName() {
        return "color fill " + (++count);
    }

    @Override
    public boolean edit() {
        String title = "Edit Color Fill Layer";
        Color oldColor = color;
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), title,
            color, true, c -> changeColor(c, false))) {
            // adds an edit to the history only after the dialog is accepted
            History.add(new ColorFillLayerChangeEdit(this, oldColor, color));
            return true;
        }
        return false;
    }

    public void changeColor(Color color, boolean addHistory) {
        Color oldColor = this.color;
        this.color = color;
        holder.update();
        updateIconImage();
        if (addHistory) {
            History.add(new ColorFillLayerChangeEdit(this, oldColor, color));
        }
    }

    @Override
    public BufferedImage createIconThumbnail() {
        Dimension thumbDim = comp.getCanvas().getThumbSize();
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

        popup.add(Colors.createCopyColorAction(() -> color));
        popup.add(Colors.createPasteColorAction(PixelitorWindow.get(), c -> changeColor(c, true)));

        return popup;
    }

    @Override
    protected ColorFillLayer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        Color colorCopy = new Color(this.color.getRGB(), true);
        String copyName = copyType.createLayerCopyName(name);
        return new ColorFillLayer(comp, copyName, colorCopy);
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
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        // do nothing
    }

    @Override
    public String getTypeString() {
        return "Color Fill Layer";
    }
}
