/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.painters.TextAdjustmentsPanel;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TranslatedTextPainter;
import pixelitor.history.AddToHistory;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.history.NewLayerEdit;
import pixelitor.history.TextLayerChangeEdit;
import pixelitor.history.TextLayerRasterizeEdit;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.OKCancelDialog;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * A text layer
 */
public class TextLayer extends ContentLayer {
    private static final long serialVersionUID = 2L;
    private final TranslatedTextPainter painter;
    private TextSettings settings;

    public TextLayer(Composition comp) {
        super(comp, "", null);

        painter = new TranslatedTextPainter();
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        painter.setFillPaint(settings.getColor());
        painter.paint(g, null, comp.getCanvasWidth(), comp.getCanvasHeight());
    }

    @Override
    public BufferedImage applyLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage imageSoFar) {
        if (settings == null) {
            // the layer was just created, nothing to paint yet
            return imageSoFar;
        }

        // normal case: the text will be painted in paintLayerOnGraphics
        return super.applyLayer(g, firstVisibleLayer, imageSoFar);
    }

    @Override
    public BufferedImage adjustImage(BufferedImage src) {
        assert settings.isWatermark(); // should be called only in this case
        return settings.watermarkImage(src, painter);
    }

    public BufferedImage createRasterizedImage() {
        BufferedImage img = ImageUtils.createCompatibleImage(canvas.getWidth(), canvas.getHeight());
        Graphics2D g = img.createGraphics();
        applyLayer(g, true, img);
        g.dispose();
        return img;
    }

    @Override
    public Layer duplicate() {
        TextLayer d = new TextLayer(comp);

        d.translationX = translationX;
        d.translationY = translationY;
        d.painter.setTranslation(
                painter.getTranslationX(),
                painter.getTranslationY());

        d.setSettings(new TextSettings(settings));
        d.setName(getName() + " Copy", AddToHistory.NO);

        if (hasMask()) {
            d.addMaskBack(mask.duplicate(d));
        }

        return d;
    }

    @Override
    public void moveWhileDragging(int x, int y) {
        super.moveWhileDragging(x, y);
        painter.setTranslation(getTranslationX(), getTranslationY());
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTranslationX, int oldTranslationY) {
        return new ContentLayerMoveEdit(this, null, oldTranslationX, oldTranslationY);
    }

    @Override
    public void setTranslation(int x, int y) {
        super.setTranslation(x, y);
        painter.setTranslation(x, y);
    }

    public void setSettings(TextSettings settings) {
        this.settings = settings;

        settings.configurePainter(painter);
    }

    public TextSettings getSettings() {
        return settings;
    }

    private void updateLayerName() {
        if (settings != null) {
            setName(settings.getText(), AddToHistory.NO);
        }
    }

    ///// from here static utility methods

    public static void createNew(PixelitorWindow pw) {
        Optional<Composition> compOpt = ImageComponents.getActiveComp();
        if (!compOpt.isPresent()) {
            // TODO dialog?
            return;
        }
        Composition comp = compOpt.get();
        TextLayer textLayer = new TextLayer(comp);

        // don't add it yet to history, only after the user chooses to press OK
        comp.addLayer(textLayer, AddToHistory.NO, true, false);

        TextAdjustmentsPanel p = new TextAdjustmentsPanel(textLayer);
        OKCancelDialog d = new OKCancelDialog(p, pw, "Create Text Layer") {
            @Override
            protected void dialogAccepted() {
                close();
                textLayer.updateLayerName();

                // now it is safe to add it to the history
                NewLayerEdit newLayerEdit = new NewLayerEdit(comp, textLayer);
                History.addEdit(newLayerEdit);
            }

            @Override
            protected void dialogCanceled() {
                close();
                comp.removeLayer(textLayer, AddToHistory.NO);
            }
        };
        d.setVisible(true);
    }

    public static void editActive(PixelitorWindow pw) {
        Composition comp = ImageComponents.getActiveImageComponent().getComp();
        Layer layer = comp.getActiveLayer();
        TextLayer textLayer = (TextLayer) layer;
        edit(pw, comp, textLayer);
    }

    public static void edit(PixelitorWindow pw, Composition comp, TextLayer textLayer) {
        TextSettings oldSettings = textLayer.getSettings();
        TextAdjustmentsPanel p = new TextAdjustmentsPanel(textLayer);
        OKCancelDialog d = new OKCancelDialog(p, pw, "Edit Text Layer") {
            @Override
            protected void dialogAccepted() {
                close();

                textLayer.updateLayerName();
                TextLayerChangeEdit edit = new TextLayerChangeEdit(
                        textLayer.getComp(),
                        textLayer,
                        oldSettings
                );
                History.addEdit(edit);
            }

            @Override
            protected void dialogCanceled() {
                close();

                textLayer.setSettings(oldSettings);
                comp.imageChanged(Composition.ImageChangeActions.FULL);
            }
        };
        d.setVisible(true);
    }

    public static void replaceWithRasterized() {
        Composition comp = ImageComponents.getActiveImageComponent().getComp();
        Layer layer = comp.getActiveLayer();
        TextLayer textLayer = (TextLayer) layer;
        BufferedImage rasterizedImage = textLayer.createRasterizedImage();

        ImageLayer newLayer = new ImageLayer(comp, rasterizedImage, layer.getName(), null);
        comp.addLayer(newLayer, AddToHistory.NO, false, false);
        comp.removeLayer(textLayer, AddToHistory.NO);

        TextLayerRasterizeEdit edit = new TextLayerRasterizeEdit(comp, textLayer, newLayer);
        History.addEdit(edit);
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        // TODO
    }

    @Override
    public void flip(Flip.Direction direction) {
        // TODO
    }

    @Override
    public void rotate(int angleDegree) {
        // TODO
    }

    @Override
    public void resize(int targetWidth, int targetHeight, boolean progressiveBilinear) {
        // TODO
    }

    @Override
    public void crop(Rectangle selectionBounds) {
//        Rectangle textBounds = painter.getTextBounds();
//        int currentX = textBounds.x;
//        int currentY = textBounds.y;
//        int newX = currentX - selectionBounds.x;
//        int newY = currentY - selectionBounds.y;

        // TODO this still doesn't work probably because
        // the alignment has not been taken into account
//        setTranslationX(getTranslationX() - selectionBounds.x);
//        setTranslationY(getTranslationY() - selectionBounds.y);
    }
}
