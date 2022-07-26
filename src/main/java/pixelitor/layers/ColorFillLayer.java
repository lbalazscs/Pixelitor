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

package pixelitor.layers;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
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

import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.layers.LayerGUILayout.thumbSize;

public class ColorFillLayer extends Layer {
    @Serial
    private static final long serialVersionUID = -5181774134094137901L;

    private Color color;

    public ColorFillLayer(Composition comp, String name, Color color) {
        super(comp, name);
        this.color = color;
    }

    public static void createNew() {
        Tools.forceFinish();
        var comp = Views.getActiveComp();
        ColorFillLayer layer = new ColorFillLayer(comp, "color fill", null);
        var activeLayerBefore = comp.getActiveLayer();
        var oldViewMode = comp.getView().getMaskViewMode();
        // don't add it yet to history, only after the user presses OK (and not Cancel!)
        new Composition.LayerAdder(comp).atPosition(ABOVE_ACTIVE).add(layer);

        String title = "Add Color Fill Layer";
        Color defaultColor = FgBgColors.getFGColor();
        layer.changeColor(defaultColor, false);
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), title,
            defaultColor, true, c -> layer.changeColor(c, false))) {
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
        Color oldColor = color;
        if (Colors.selectColorWithDialog(PixelitorWindow.get(), title,
            color, true, c -> changeColor(c, false))) {
            // adds an edit to the history only after the dialog is accepted
            History.add(new ColorFillLayerChangeEdit(this, oldColor, color));
        }
    }

    public void changeColor(Color color, boolean addHistory) {
        Color oldColor = this.color;
        this.color = color;
        comp.update();
        updateIconImage();
        if (addHistory) {
            History.add(new ColorFillLayerChangeEdit(this, oldColor, color));
        }
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
        Colors.setupPasteColorPopupMenu(popup, PixelitorWindow.get(), c -> changeColor(c, true));

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
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        // do nothing
    }

    @Override
    public String getTypeString() {
        return "Color Fill Layer";
    }
}
