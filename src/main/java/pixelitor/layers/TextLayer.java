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

import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import pixelitor.Composition;
import pixelitor.Composition.LayerAdder;
import pixelitor.OpenImages;
import pixelitor.compactions.Flip;
import pixelitor.compactions.Rotate;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.filters.gui.DialogMenuOwner;
import pixelitor.filters.gui.FilterState;
import pixelitor.filters.gui.UserPreset;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TextSettingsPanel;
import pixelitor.filters.painters.TransformedTextPainter;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.history.*;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

import static org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment.CENTER;
import static org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment.LEFT;
import static org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment.TOP;
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
        this(comp, "");
    }

    public TextLayer(Composition comp, String name) {
        super(comp, name, null);

        painter = new TransformedTextPainter();
        setSettings(new TextSettings());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        isAdjustment = settings.hasWatermark();

        painter = new TransformedTextPainter();
        settings.configurePainter(painter);
        painter.setTranslation(getTx(), getTy());
    }

    public static void createNew() {
        var comp = OpenImages.getActiveComp();
        if (comp == null) {
            // It is possible to arrive here with no open images
            // because the T hotkey is always active, see issue #77
            return;
        }

        var textLayer = new TextLayer(comp);
        var activeLayerBefore = comp.getActiveLayer();
        var oldViewMode = comp.getView().getMaskViewMode();
        // don't add it yet to history, only after the user chooses to press OK
        new LayerAdder(comp).add(textLayer);

        var settingsPanel = new TextSettingsPanel(textLayer);
        new DialogBuilder()
            .title("Create Text Layer")
            .menuBar(textLayer.getMenuBar())
            .content(settingsPanel)
            .withScrollbars()
            .okAction(() ->
                textLayer.finalizeCreation(comp, activeLayerBefore, oldViewMode))
            .cancelAction(() -> comp.deleteLayer(textLayer, false))
            .show();
    }

    private void finalizeCreation(Composition comp, Layer activeLayerBefore, MaskViewMode oldViewMode) {
        updateLayerName();

        // now it is safe to add it to the history
        var newLayerEdit = new NewLayerEdit(
            "Add Text Layer", comp, this,
            activeLayerBefore, oldViewMode);
        History.add(newLayerEdit);
    }

    public void edit(PixelitorWindow pw) {
        TextSettings oldSettings = getSettings();
        var settingsPanel = new TextSettingsPanel(this);

        new DialogBuilder()
            .title("Edit Text Layer")
            .menuBar(getMenuBar())
            .content(settingsPanel)
            .owner(pw)
            .withScrollbars()
            .okAction(() -> commitSettings(oldSettings))
            .cancelAction(() -> resetOldSettings(oldSettings))
            .show();
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
        setSettings(oldSettings);
        comp.imageChanged();
    }

    @Override
    public TextLayer duplicate(boolean compCopy) {
        String duplicateName = compCopy ? name : Utils.createCopyName(name);
        TextLayer d = new TextLayer(comp, duplicateName);

        d.translationX = translationX;
        d.translationY = translationY;
        d.painter.setTranslation(painter.getTx(), painter.getTy());
        d.setSettings(new TextSettings(settings));
        duplicateMask(d, compCopy);

        return d;
    }

    @Override
    public Rectangle getEffectiveBoundingBox() {
        return painter.getBoundingBox();
    }

    @Override
    public Rectangle getSnappingBoundingBox() {
        return painter.getBoundingBox();
    }

    @Override
    public Rectangle getContentBounds() {
        return painter.getBoundingBox();
    }

    @Override
    public int getMouseHitPixelAtPoint(Point p) {
        // we treat surrounding rect as area for mouse hit detection
        // for small font sizes or thin font styles it can be helpful
        // for larger font sizes it could be more appropriate to use pixel perfect test
        if (painter.getBoundingShape().contains(p)) {
            if (hasMask() && getMask().isMaskEnabled()) {
                BufferedImage maskImage = getMask().getImage();
                int ix = p.x - getMask().translationX;
                int iy = p.y - getMask().translationY;
                if (ix >= 0 && iy >= 0 && ix < maskImage.getWidth() && iy < maskImage.getHeight()) {
                    int maskPixel = maskImage.getRGB(ix, iy);
                    int maskAlpha = maskPixel & 0xff;
                    return 0x00ffffff | maskAlpha << 24;
                }
            }
            return 0xffffffff;
        }
        return 0x00000000;
    }

    public ImageLayer replaceWithRasterized() {
        var rasterizedImage = createRasterizedImage(false);
        var newImageLayer = new ImageLayer(comp, rasterizedImage, getName());
        newImageLayer.setBlendingMode(getBlendingMode(), false);
        newImageLayer.setOpacity(getOpacity(), false);
        History.add(new TextLayerRasterizeEdit(comp, this, newImageLayer));
        comp.replaceLayer(this, newImageLayer);
        Messages.showInStatusBar(String.format(
            "The layer <b>\"%s\"</b> was rasterized.", getName()));
        return newImageLayer;
    }

    /**
     * Returns a canvas-sized image corresponding to the contents of this layer.
     */
    public BufferedImage createRasterizedImage(boolean applyMask) {
        BufferedImage img = comp.getCanvas().createTmpImage();
        Graphics2D g = img.createGraphics();
        if (applyMask) {
            // the layer's blending mode will be ignored
            // because firstVisibleLayer is set to true
            applyLayer(g, img, true);
        } else {
            paintLayerOnGraphics(g, true);
        }
        g.dispose();
        return img;
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
            return imageSoFar;
        }

        // the text will be painted normally
        return super.applyLayer(g, imageSoFar, firstVisibleLayer);
    }

    @Override
    public BufferedImage actOnImageFromLayerBellow(BufferedImage src) {
        assert settings.hasWatermark(); // should be called only in this case
        return settings.watermarkImage(src, painter);
    }

    @Override
    public void moveWhileDragging(double x, double y) {
        super.moveWhileDragging(x, y);
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

    public void setSettings(TextSettings settings) {
        this.settings = settings;

        isAdjustment = settings.hasWatermark();
        settings.configurePainter(painter);
    }

    public TextSettings getSettings() {
        return settings;
    }

    public void randomizeSettings() {
        settings.randomize();
        setSettings(settings); // to re-configure the painter
    }

    public void updateLayerName() {
        if (settings != null) {
            setName(settings.getText(), false);
        }
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        VerticalAlignment verticalAlignment = painter.getVerticalAlignment();
        HorizontalAlignment horizontalAlignment = painter.getHorizontalAlignment();
        int newTx = translationX;
        int newTy = translationY;

        if (horizontalAlignment == LEFT) {
            newTx += west;
        } else if (horizontalAlignment == CENTER) {
            newTx += (west - east) / 2;
        } else { // RIGHT
            newTx -= east;
        }

        if (verticalAlignment == TOP) {
            newTy += north;
        } else if (verticalAlignment == VerticalAlignment.CENTER) {
            newTy += (north - south) / 2;
        } else { // BOTTOM
            newTy -= south;
        }

        setTranslation(newTx, newTy);
    }

    @Override
    public void flip(Flip.Direction direction) {
        // TODO
    }

    @Override
    public void rotate(Rotate.SpecialAngle angle) {
        // TODO
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
    public void crop(Rectangle2D cropRect, boolean deleteCroppedPixels, boolean allowGrowing) {
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
    public BufferedImage getRepresentingImage() {
        return createRasterizedImage(true);
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        JPopupMenu popup = super.createLayerIconPopupMenu();
        if (popup == null) {
            popup = new JPopupMenu();
        }

        var editMenuItem = new JMenuItem("Edit");
        editMenuItem.addActionListener(e -> edit(PixelitorWindow.get()));
        editMenuItem.setAccelerator(CTRL_T);
        popup.add(editMenuItem);

        popup.add(new AbstractAction("Rasterize") {
            @Override
            public void actionPerformed(ActionEvent e) {
                replaceWithRasterized();
            }
        });

        popup.add(new AbstractAction("Selection from Text") {
            @Override
            public void actionPerformed(ActionEvent e) {
                createSelectionFromText();
            }
        });

        return popup;
    }

    public void createSelectionFromText() {
        Shape shape = getTextShape();
        PixelitorEdit selectionEdit = comp.changeSelection(shape);
        if (selectionEdit != null) {
            History.add(selectionEdit);
        }
    }

    private JMenuBar getMenuBar() {
        return new DialogMenuBar(this, true);
    }

    @Override
    public boolean hasBuiltinPresets() {
        return false;
    }

    @Override
    public FilterState[] getBuiltinPresets() {
        throw new UnsupportedOperationException();
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
    public UserPreset createUserPreset(String presetName) {
        return settings.createUserPreset(presetName);
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        settings.loadStateFrom(preset);
    }

    @Override
    public boolean hasHelp() {
        return false;
    }

    @Override
    public String getHelpURL() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "{text=" + (settings == null ? "null settings" : settings.getText())
            + ", super=" + super.toString() + '}';
    }
}
