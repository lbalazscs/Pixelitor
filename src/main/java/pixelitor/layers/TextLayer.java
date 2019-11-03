/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TextSettingsPanel;
import pixelitor.filters.painters.TranslatedTextPainter;
import pixelitor.gui.OpenComps;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.history.NewLayerEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.TextLayerChangeEdit;
import pixelitor.history.TextLayerRasterizeEdit;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.CompletableFuture;

import static org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment.CENTER;
import static org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment.LEFT;
import static org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment.TOP;
import static pixelitor.utils.Keys.CTRL_T;

/**
 * A text layer
 */
public class TextLayer extends ContentLayer {
    private static final long serialVersionUID = 2L;
    private transient TranslatedTextPainter painter;
    private TextSettings settings;

    public TextLayer(Composition comp) {
        this(comp, "");
    }

    public TextLayer(Composition comp, String name) {
        super(comp, name, null);

        painter = new TranslatedTextPainter();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        isAdjustment = settings.isWatermark();

        painter = new TranslatedTextPainter();
        settings.configurePainter(painter);
        painter.setTranslation(getTX(), getTY());
    }

    public static void createNew(PixelitorWindow pw) {
        Composition comp = OpenComps.getActiveCompOrNull();
        if (comp == null) {
            // It is possible to arrive here with no open images
            // because the T hotkey is always active, see issue #77
            return;
        }
        TextLayer textLayer = new TextLayer(comp);

        Layer activeLayerBefore = comp.getActiveLayer();
        MaskViewMode oldViewMode = comp.getView().getMaskViewMode();

        // don't add it yet to history, only after the user chooses to press OK
        new LayerAdder(comp).add(textLayer);

        TextSettingsPanel p = new TextSettingsPanel(textLayer);
        new DialogBuilder()
                .content(p)
                .owner(pw)
                .title("Create Text Layer")
                .withScrollbars()
                .okAction(() -> {
                    textLayer.updateLayerName();

                    // now it is safe to add it to the history
                    NewLayerEdit newLayerEdit = new NewLayerEdit(
                            "New Text Layer", comp, textLayer,
                            activeLayerBefore, oldViewMode);
                    History.addEdit(newLayerEdit);
                })
                .cancelAction(() -> comp.deleteLayer(textLayer,
                        false, true))
                .show();
    }

    public void edit(PixelitorWindow pw) {
        if (RandomGUITest.isRunning()) {
            return; // avoid dialogs
        }

        TextSettings oldSettings = getSettings();
        TextSettingsPanel p = new TextSettingsPanel(this);

        new DialogBuilder()
                .content(p)
                .owner(pw)
                .title("Edit Text Layer")
                .withScrollbars()
                .okAction(() -> commitSettings(oldSettings))
                .cancelAction(() -> resetOldSettings(oldSettings))
                .show();
    }

    public void commitSettings(TextSettings oldSettings) {
        updateLayerName();
        TextLayerChangeEdit edit = new TextLayerChangeEdit(
                comp,
                this,
                oldSettings
        );
        History.addEdit(edit);
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
        d.painter.setTranslation(
                painter.getTX(),
                painter.getTY());

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
                    return 0x00ffffff | (maskAlpha << 24);
                }
            }
            return 0xffffffff;
        }
        return 0x00000000;
    }

    // TODO if a text layer has a mask, then this will apply the
    // mask to the layer, resulting in an image layer without a mask.
    // This probably should be considered a bug, and instead the mask
    // should be kept, and the rasterized pixels should not be affected
    // by the mask.
    public ImageLayer replaceWithRasterized() {
        BufferedImage rasterizedImage = createRasterizedImage();

        ImageLayer newImageLayer = new ImageLayer(comp, rasterizedImage, getName());

        TextLayerRasterizeEdit edit = new TextLayerRasterizeEdit(comp, this, newImageLayer);
        History.addEdit(edit);

        new LayerAdder(comp)
                .noRefresh()
                .add(newImageLayer);
        comp.deleteLayer(this, false, true);

        return newImageLayer;
    }

    public BufferedImage createRasterizedImage() {
        BufferedImage img = ImageUtils.createSysCompatibleImage(canvas.getImWidth(), canvas.getImHeight());
        Graphics2D g = img.createGraphics();
        applyLayer(g, img, true);
        g.dispose();
        return img;
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        painter.setFillPaint(settings.getColor());
        painter.paint(g, null, comp.getCanvasImWidth(), comp.getCanvasImHeight());
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
        assert settings.isWatermark(); // should be called only in this case
        return settings.watermarkImage(src, painter);
    }

    @Override
    public void moveWhileDragging(double x, double y) {
        super.moveWhileDragging(x, y);
        painter.setTranslation(getTX(), getTY());
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTX, int oldTY) {
        return new ContentLayerMoveEdit(this, null, oldTX, oldTY);
    }

    @Override
    public void setTranslation(int x, int y) {
        super.setTranslation(x, y);
        painter.setTranslation(x, y);
    }

    public void setSettings(TextSettings settings) {
        this.settings = settings;

        isAdjustment = settings.isWatermark();
        settings.configurePainter(painter);
    }

    public TextSettings getSettings() {
        return settings;
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
        int newTX = translationX;
        int newTY = translationY;

        if (horizontalAlignment == LEFT) {
            newTX += west;
        } else if (horizontalAlignment == CENTER) {
            newTX += (west - east) / 2;
        } else { // RIGHT
            newTX -= east;
        }

        if (verticalAlignment == TOP) {
            newTY += north;
        } else if (verticalAlignment == VerticalAlignment.CENTER) {
            newTY += (north - south) / 2;
        } else { // BOTTOM
            newTY -= south;
        }

        setTranslation(newTX, newTY);
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

    public Shape getTextShape() {
        return painter.getTextShape(comp.getCanvas());
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCroppedPixels, boolean allowGrowing) {
        // the text will not be cropped, but the translations have to be adjusted

        // as the cropping is the exact opposite of "enlarge canvas",
        // calculate the corresponding margins...
        int northMargin = (int) cropRect.getY();
        int westMargin = (int) cropRect.getX();
        int southMargin = (int) (canvas.getImHeight() - cropRect.getHeight() - cropRect.getY());
        int eastMargin = (int) (canvas.getImWidth() - cropRect.getWidth() - cropRect.getX());

        // ...and do a negative enlargement
        enlargeCanvas(-northMargin, -eastMargin, -southMargin, -westMargin);
    }

    @Override
    public BufferedImage getTmpLayerImage() {
        return createRasterizedImage();
    }

    @Override
    public JPopupMenu createLayerIconPopupMenu() {
        JPopupMenu popup = super.createLayerIconPopupMenu();
        if (popup == null) {
            popup = new JPopupMenu();
        }

        JMenuItem editMenuItem = new JMenuItem("Edit");
        editMenuItem.addActionListener(e -> edit(PixelitorWindow.getInstance()));
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
        PixelitorEdit selectionEdit = comp.changeSelectionFromShape(shape);
        if (selectionEdit != null) {
            History.addEdit(selectionEdit);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{text=" + (settings == null ? "null settings" : settings.getText())
                + ", super=" + super.toString() + '}';
    }
}
