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

import pixelitor.Composition;
import pixelitor.CopyType;
import pixelitor.compactions.Flip;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.filters.gui.DialogMenuOwner;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TextSettingsPanel;
import pixelitor.filters.painters.TransformedTextPainter;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.PAction;
import pixelitor.history.*;
import pixelitor.io.TranslatedImage;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

import static pixelitor.gui.utils.Screens.Align.FRAME_RIGHT;
import static pixelitor.utils.Keys.CTRL_T;

/**
 * A text layer
 */
public class TextLayer extends ContentLayer implements DialogMenuOwner {
    @Serial
    private static final long serialVersionUID = 2L;
    public static final String TEXT_PRESETS_DIR_NAME = "text";

    private transient TransformedTextPainter painter;
    private TextSettings settings;

    public TextLayer(Composition comp) {
        this(comp, "", new TextSettings());
    }

    public TextLayer(Composition comp, String name, TextSettings settings) {
        super(comp, name);

        painter = new TransformedTextPainter();
        applySettings(settings);
    }

    public TextLayer(TextLayer other, String name) {
        super(other.comp, name);

        this.settings = other.settings.copy();

        // This copy constructor makes a copy of the painter instead of
        // just calling applySettings so that flip/rotate see fully
        // initialized internal data structures when they are applied.
        this.painter = other.painter.copy(settings);

        this.isAdjustment = other.isAdjustment;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        isAdjustment = settings.hasWatermark();

        painter = new TransformedTextPainter();
        settings.configurePainter(painter);
        painter.setTranslation(getTx(), getTy());
    }

    public static TextLayer createNew(Composition comp) {
        return createNew(comp, new TextSettings());
    }

    public static TextLayer createNew(Composition comp, TextSettings settings) {
        if (comp == null) {
            // It is possible to arrive here with no open images
            // because the T hotkey is always active, see issue #77
            return null;
        }

        Tools.forceFinish();

        var textLayer = new TextLayer(comp, "", settings);
        var activeLayerBefore = comp.getActiveLayer();
        var oldViewMode = comp.getView().getMaskViewMode();
        // don't add it yet to history, only after the user presses OK (and not Cancel!)
        LayerHolder holder = comp.getHolderForNewLayers();
        holder.add(textLayer);

        var settingsPanel = new TextSettingsPanel(textLayer);
        new DialogBuilder()
            .title("Create Text Layer")
            .menuBar(textLayer.getMenuBar())
            .content(settingsPanel)
            .withScrollbars()
            .align(FRAME_RIGHT)
            .okAction(() ->
                textLayer.finalizeCreation(activeLayerBefore, oldViewMode))
            .cancelAction(() -> holder.deleteLayer(textLayer, false))
            .show();
        return textLayer;
    }

    public void finalizeCreation(Layer activeLayerBefore, MaskViewMode oldViewMode) {
        updateLayerName();

        // now it is safe to add it to the history
        History.add(new NewLayerEdit("Add Text Layer",
            this, activeLayerBefore, oldViewMode));
    }

    @Override
    public boolean edit() {
        TextSettings oldSettings = getSettings();
        var settingsPanel = new TextSettingsPanel(this);

        return new DialogBuilder()
            .title("Edit Text Layer")
            .menuBar(getMenuBar())
            .content(settingsPanel)
            .withScrollbars()
            .align(FRAME_RIGHT)
            .okAction(() -> commitSettings(oldSettings))
            .cancelAction(() -> resetOldSettings(oldSettings))
            .show()
            .wasAccepted();
    }

    public void commitSettings(TextSettings oldSettings) {
        if (oldSettings == settings) {
            // The settings object is replaced every time
            // the user changes something in the dialog.
            // If it is still the same, in means that
            // nothing was changed.
            return;
        }

        updateLayerName();
        var edit = new TextLayerChangeEdit(comp, this, oldSettings);
        History.add(edit);
    }

    private void resetOldSettings(TextSettings oldSettings) {
        applySettings(oldSettings);
        holder.update();
    }

    @Override
    protected TextLayer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        String duplicateName = copyType.createLayerCopyName(name);
        TextLayer d = new TextLayer(this, duplicateName);
        d.setTranslation(getTx(), getTy());
        return d;
    }

    public void checkFontIsInstalled() {
        settings.checkFontIsInstalled(this);
    }

    @Override
    public Rectangle getContentBounds(boolean includeTransparent) {
        if (includeTransparent) {
            // a quick and rough estimate of the text content,
            // it can include some transparency at the edges
            return painter.getBoundingBox();
        } else {
            // make an image-based calculation for an exact "content crop"
            BufferedImage image = asImage(false, false);
            Rectangle bounds = ImageUtils.getNonTransparentBounds(image);
            image.flush();
            return bounds;
        }
    }

    @Override
    public int getPixelAtPoint(Point p) {
        if (painter.getBoundingShape().contains(p)) {
            if (hasMask() && mask.isMaskEnabled()) {
                BufferedImage maskImage = mask.getImage();
                int ix = p.x - mask.getTx();
                int iy = p.y - mask.getTy();
                if (ix >= 0 && iy >= 0 && ix < maskImage.getWidth() && iy < maskImage.getHeight()) {
                    int maskPixel = maskImage.getRGB(ix, iy);
                    int maskAlpha = maskPixel & 0xFF;
                    return 0x00_FF_FF_FF | maskAlpha << 24;
                }
            }
            // There is no pixel-perfect hit detection for text layers, this value
            // signals that auto-select should select in the entire bounding box.
            return 0xFF_FF_FF_FF;
        } else {
            // signal that auto-select should ignore this layer
            return 0x00_00_00_00;
        }
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        painter.setFillPaint(settings.getColor());
        painter.paint(g, null, comp.getCanvasWidth(), comp.getCanvasHeight());
    }

    @Override
    public BufferedImage applyLayer(Graphics2D g, BufferedImage imageSoFar, boolean firstVisibleLayer) {
        if (settings == null) {
            // the layer was just created, nothing to paint yet
            return null;
        }

        // the text will be painted normally
        return super.applyLayer(g, imageSoFar, firstVisibleLayer);
    }

    @Override
    public BufferedImage applyOnImage(BufferedImage src) {
        assert settings.hasWatermark(); // should be called only in this case
        return settings.watermarkImage(src, painter);
    }

    @Override
    public void moveWhileDragging(double relImX, double relImY) {
        super.moveWhileDragging(relImX, relImY);
        painter.setTranslation(getTx(), getTy());
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTx, int oldTy) {
        return new ContentLayerMoveEdit(this, null, oldTx, oldTy);
    }

    @Override
    public void setTranslation(int x, int y) {
        super.setTranslation(x, y);
        painter.setTranslation(x, y);
    }

    public void applySettings(TextSettings settings) {
        this.settings = settings;

        isAdjustment = settings.hasWatermark();
        settings.configurePainter(painter);
    }

    public TextSettings getSettings() {
        return settings;
    }

    public void randomizeSettings() {
        settings.randomize();
        applySettings(settings); // to re-configure the painter
    }

    public void updateLayerName() {
        if (settings != null) {
            setName(settings.getText(), false);
        }
    }

    @Override
    public TranslatedImage getTranslatedImage() {
        // This method ensures that the whole text is exported by ignoring
        // the canvas and only exporting an image corresponding to the text's bounds

        // the translation is already taken into account
        // in the bounding box, but not the effect width
        Rectangle textBounds = painter.getBoundingBox();
        int effectsWidth = (int) settings.getEffects().getMaxEffectThickness();
        if (effectsWidth != 0) {
            textBounds = new Rectangle(textBounds); // the original mustn't be affected
            textBounds.grow(effectsWidth + 1, effectsWidth + 1);
        }

        BufferedImage img = painter.renderRectangle(textBounds);
        return new TranslatedImage(img, textBounds.x, textBounds.y);
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        int newTx = getTx() + switch (painter.getHorizontalAlignment()) {
            case LEFT -> west;
            case CENTER -> (west - east) / 2;
            case RIGHT -> -east;
        };

        int newTy = getTy() + switch (painter.getVerticalAlignment()) {
            case TOP -> north;
            case CENTER -> (north - south) / 2;
            case BOTTOM -> -south;
        };

        setTranslation(newTx, newTy);
    }

    @Override
    public void flip(Flip.Direction direction) {
        painter.flip(direction, comp.getCanvas());
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        painter.rotate(angle, comp.getCanvas());
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // TODO
        return CompletableFuture.completedFuture(null);
    }

    private Shape getTextShape() {
        return painter.getTextShape(comp.getCanvas());
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        // the text will not be cropped, but the translations have to be adjusted

        // as the cropping is the exact opposite of "enlarge canvas",
        // calculate the corresponding margins...
        int northMargin = (int) cropRect.getY();
        int westMargin = (int) cropRect.getX();
        int southMargin = (int) (comp.getCanvasHeight() - cropRect.getHeight() - cropRect.getY());
        int eastMargin = (int) (comp.getCanvasWidth() - cropRect.getWidth() - cropRect.getX());

        // ...and do a negative enlargement
        enlargeCanvas(-northMargin, -eastMargin, -southMargin, -westMargin);
    }

    @Override
    public boolean hasRasterThumbnail() {
        return false;
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        JPopupMenu popup = super.createLayerIconPopupMenu();
        if (popup == null) {
            popup = new JPopupMenu();
        }

        var editMenuItem = new JMenuItem("Edit");
        editMenuItem.addActionListener(e -> edit());
        editMenuItem.setAccelerator(CTRL_T);
        popup.add(editMenuItem);

        popup.add(new PAction("Selection from Text", this::createSelectionFromText));

        return popup;
    }

    public void createSelectionFromText() {
        Shape shape = getTextShape();
        PixelitorEdit selectionEdit = comp.changeSelection(shape);
        if (selectionEdit != null) {
            History.add(selectionEdit);
        }
    }

    @Override
    public void updateIconImage() {
        // do nothing
    }

    private JMenuBar getMenuBar() {
        return new DialogMenuBar(this);
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public String getPresetDirName() {
        return TEXT_PRESETS_DIR_NAME;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        settings.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        settings.loadUserPreset(preset);
    }

    @Override
    public String getTypeString() {
        return "Text Layer";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);
        node.addNullableDebuggable("text settings", settings);
        node.addNullableDebuggable("painter", painter);
        return node;
    }
}
