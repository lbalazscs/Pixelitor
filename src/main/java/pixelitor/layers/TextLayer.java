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
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.Outsets;
import pixelitor.compactions.QuadrantAngle;
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
import pixelitor.io.ORAImageInfo;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
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

        // makes a copy of the painter instead of just calling
        // applySettings so that flip/rotate see fully initialized
        // internal data structures when they are applied
        // TODO still needed? what about readObject, which calls applySettings?
        this.painter = other.painter.copy(settings);

        this.isAdjustment = other.isAdjustment;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        painter = new TransformedTextPainter();
        painter.setTranslation(getTx(), getTy());
        applySettings(settings);
    }

    /**
     * Creates a new text layer with default settings and shows an editing dialog.
     */
    public static TextLayer createNew(Composition comp) {
        return createNew(comp, new TextSettings());
    }

    /**
     * Creates a new text layer with the given settings and shows an editing dialog.
     */
    public static TextLayer createNew(Composition comp, TextSettings settings) {
        if (comp == null) {
            // it's possible to arrive here with no open images
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

    /**
     * Finalizes the creation of this text layer after the settings dialog is accepted.
     */
    public void finalizeCreation(Layer prevActiveLayer, MaskViewMode prevViewMode) {
        updateLayerName();

        // now it is safe to add it to the history
        History.add(new NewLayerEdit("Add Text Layer",
            this, prevActiveLayer, prevViewMode));
    }

    /**
     * Shows a dialog to edit the settings of this text layer.
     */
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

    /**
     * Commits the current settings after the edit dialog is accepted, updating history.
     */
    public void commitSettings(TextSettings prevSettings) {
        if (settings == prevSettings) {
            // The settings object is replaced every time
            // the user changes something in the dialog.
            // If it is still the same, it means that
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

    /**
     * Checks if the font used by this layer is installed on the system.
     */
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
            Rectangle bounds = ImageUtils.calcOpaqueBounds(image);
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
            // TODO actually there should be an image cache in the painter
            return 0xFF_FF_FF_FF;
        } else {
            // signal that auto-select should ignore this layer
            return 0x00_00_00_00;
        }
    }

    @Override
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
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
        return painter.createWatermark(src, comp);
    }

    @Override
    public void prepareMovement() {
        super.prepareMovement();
        if (painter.isOnPath()) {
            comp.getActivePath().getSubPath(0).saveImTransformRefPoints();
        }
    }

    @Override
    public void moveWhileDragging(double imDx, double imDy) {
        super.moveWhileDragging(imDx, imDy);
        if (painter.isOnPath()) {
            AffineTransform at = AffineTransform.getTranslateInstance(imDx, imDy);
            comp.getActivePath().getSubPath(0).imTransform(at);
        } else {
            painter.setTranslation(getTx(), getTy());
        }
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

    /**
     * Applies the given text settings to this layer and reconfigures the painter.
     */
    public void applySettings(TextSettings settings) {
        this.settings = settings;

        // Dynamically changes the rendering behavior of a text layer
        // based on whether the user has configured it as a watermark.
        // A watermarking text layer doesn't just draw text; it adjusts
        // the underlying image to embed the text as a watermark.
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

    /**
     * Updates the layer's name based on its current text content.
     */
    public void updateLayerName() {
        if (settings != null) {
            setName(nameFromText(settings.getText()), false);
        }
    }

    /**
     * Generates a sanitized and shortened layer name from a given text string.
     */
    public static String nameFromText(String rawText) {
        String cleaned = ALL_WHITESPACE.matcher(rawText.trim()).replaceAll(" ");
        return Utils.shorten(cleaned, 30);
    }

    /**
     * Ensures that the whole text (and only the text) is exported by ignoring
     * the canvas and exporting an image corresponding to the text's bounds.
     */
    @Override
    public ORAImageInfo getORAImageInfo() {
        Rectangle textBounds = painter.getBoundingBox();
        BufferedImage img = painter.renderArea(textBounds);
        return new ORAImageInfo(img, textBounds.x, textBounds.y);
    }

    @Override
    public void enlargeCanvas(Outsets out) {
        BoxAlignment alignment = settings.getAlignment();
        if (alignment == BoxAlignment.PATH) {
            // the path will handle the canvas enlargement
            // and the text is glued to it => nothing to do
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
    public void flip(FlipDirection direction) {
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
    public boolean hasRasterIcon() {
        return false;
    }

    @Override
    public void updateIconImage() {
        // do nothing, as text layers have a static and vector-based image
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

    /**
     * Creates a selection in the composition based on the shape of the text.
     */
    public void createSelectionFromText() {
        Shape shape = getTextShape();
        PixelitorEdit selectionEdit = comp.changeSelection(shape);
        if (selectionEdit != null) {
            History.add(selectionEdit);
        }
    }

    private JMenuBar getMenuBar() {
        return new DialogMenuBar(this);
    }

    @Override
    public boolean supportsUserPresets() {
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

    /**
     * Handles changes to the path this text layer might be attached to.
     */
    public void pathChanged(boolean deleted) {
        if (painter.isOnPath()) {
            painter.pathChanged();
            holder.invalidateImageCache();

            if (deleted) {
                settings.setAlignment(BoxAlignment.CENTER_CENTER);
                painter.setBoxAlignment(BoxAlignment.CENTER_CENTER);
            }
        }
    }

    /**
     * Switches this text layer to use path-based alignment.
     */
    public void usePathEditing() {
        settings.setAlignment(BoxAlignment.PATH);
        painter.setBoxAlignment(BoxAlignment.PATH);

        painter.pathChanged();
        holder.invalidateImageCache();
    }

    /**
     * Checks if this text layer is currently aligned to a path.
     */
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
