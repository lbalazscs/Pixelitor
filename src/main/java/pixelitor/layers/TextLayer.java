/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.compactions.Outsets;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.filters.gui.DialogMenuOwner;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TextSettingsPanel;
import pixelitor.filters.painters.TransformedTextPainter;
import pixelitor.gui.utils.BoxAlignment;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.*;
import pixelitor.io.TranslatedImage;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static pixelitor.gui.utils.Screens.Align.FRAME_RIGHT;
import static pixelitor.utils.ImageUtils.isWithinBounds;
import static pixelitor.utils.Keys.CTRL_T;

/**
 * A text layer that renders editable text.
 */
public class TextLayer extends ContentLayer implements DialogMenuOwner {
    @Serial
    private static final long serialVersionUID = 2L;
    public static final String TEXT_PRESETS_DIR_NAME = "text";
    private static final Pattern ALL_WHITESPACE = Pattern.compile("\\s+");

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
        var prevActiveLayer = comp.getActiveLayer();
        var prevViewMode = comp.getView().getMaskViewMode();
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
                textLayer.finalizeCreation(prevActiveLayer, prevViewMode))
            .cancelAction(() -> holder.deleteLayer(textLayer, false))
            .show();
        return textLayer;
    }

    public void finalizeCreation(Layer prevActiveLayer, MaskViewMode prevViewMode) {
        updateLayerName();

        // now it is safe to add it to the history
        History.add(new NewLayerEdit("Add Text Layer",
            this, prevActiveLayer, prevViewMode));
    }

    @Override
    public boolean edit() {
        TextSettings prevSettings = getSettings();
        var settingsPanel = new TextSettingsPanel(this);

        return new DialogBuilder()
            .title("Edit Text Layer")
            .menuBar(getMenuBar())
            .content(settingsPanel)
            .withScrollbars()
            .align(FRAME_RIGHT)
            .okAction(() -> commitSettings(prevSettings))
            .cancelAction(() -> resetPrevSettings(prevSettings))
            .show()
            .wasAccepted();
    }

    // the layer name is updated and a history edit is added
    // only after the user accepts the dialog
    public void commitSettings(TextSettings prevSettings) {
        if (settings == prevSettings) {
            // The settings object is replaced every time
            // the user changes something in the dialog.
            // If it is still the same, in means that
            // nothing was changed.
            return;
        }

        updateLayerName();
        History.add(new TextLayerChangeEdit(comp, this, prevSettings));
    }

    private void resetPrevSettings(TextSettings prevSettings) {
        applySettings(prevSettings);
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
            BufferedImage image = toImage(false, false);
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
                if (isWithinBounds(ix, iy, maskImage)) {
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
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
        painter.setColor(settings.getColor()); // TODO is this already set?
        painter.paint(g, comp.getCanvasWidth(), comp.getCanvasHeight(), comp);
    }

    @Override
    public BufferedImage render(Graphics2D g, BufferedImage currentComposite, boolean firstVisibleLayer) {
        if (settings == null) {
            // the layer was just created, nothing to paint yet
            return null;
        }

        // the text will be painted normally
        return super.render(g, currentComposite, firstVisibleLayer);
    }

    @Override
    public BufferedImage transformImage(BufferedImage src) {
        assert settings.hasWatermark(); // should be called only in this case
        return painter.watermarkImage(src, comp);
    }

    @Override
    public void moveWhileDragging(double imDx, double imDy) {
        super.moveWhileDragging(imDx, imDy);
        painter.setTranslation(getTx(), getTy());
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int prevTx, int prevTy) {
        return new ContentLayerMoveEdit(this, null, prevTx, prevTy);
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
            setName(nameFromText(settings.getText()), false);
        }
    }

    public static String nameFromText(String rawText) {
        String cleaned = ALL_WHITESPACE.matcher(rawText.trim()).replaceAll(" ");
        return Utils.shorten(cleaned, 30);
    }

    /**
     * This method ensures that the whole text is exported by ignoring
     * the canvas and only exporting an image corresponding to the text's bounds.
     */
    @Override
    public TranslatedImage getTranslatedImage() {
        Rectangle textBounds = painter.getBoundingBox();
        BufferedImage img = painter.renderArea(textBounds);
        return new TranslatedImage(img, textBounds.x, textBounds.y);
    }

    @Override
    public void enlargeCanvas(Outsets out) {
        BoxAlignment alignment = settings.getAlignment();
        if (alignment == BoxAlignment.PATH) {
            return;
        }

        int newTx = getTx() + switch (alignment.getHorizontal()) {
            case LEFT -> out.left;
            case CENTER -> (out.left - out.right) / 2;
            case RIGHT -> -out.right;
        };

        int newTy = getTy() + switch (alignment.getVertical()) {
            case TOP -> out.top;
            case CENTER -> (out.top - out.bottom) / 2;
            case BOTTOM -> -out.bottom;
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
        return painter.getTextShape();
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
        enlargeCanvas(new Outsets(
            -northMargin, -eastMargin, -southMargin, -westMargin));
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

        popup.add(new TaskAction("Selection from Text", this::createSelectionFromText));

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

    public void pathChanged(boolean deleted) {
        if (painter.isOnPath()) {
            painter.pathChanged();
            holder.invalidateImageCache();

            if (deleted) {
                settings.setAlignment(BoxAlignment.CENTER_CENTER);
                painter.setAlignment(BoxAlignment.CENTER_CENTER);
            }
        }
    }

    public void usePathEditing() {
        settings.setAlignment(BoxAlignment.PATH);
        painter.setAlignment(BoxAlignment.PATH);

        painter.pathChanged();
        holder.invalidateImageCache();
    }

    public boolean isOnPath() {
        return painter.isOnPath();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);
        node.addNullableDebuggable("text settings", settings);
        node.addNullableDebuggable("painter", painter);
        return node;
    }
}
