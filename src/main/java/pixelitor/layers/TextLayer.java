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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
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
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * A text layer
 */
public class TextLayer extends ShapeLayer implements ImageAdjustmentEffect {
    private static final long serialVersionUID = 2L;
    private final TranslatedTextPainter painter;
    private TextSettings settings;

    public TextLayer(Composition comp) {
        super(comp, "");

        painter = new TranslatedTextPainter();
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        setupDrawingComposite(g, firstVisibleLayer);

        painter.setFillPaint(settings.getColor());
        painter.paint(g, null, comp.getCanvasWidth(), comp.getCanvasHeight());
    }

    @Override
    public BufferedImage paintLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage imageSoFar) {
        if (settings == null) {
            // the layer was just created, nothing to paint yet
            return imageSoFar;
        } else if (settings.isWatermark()) {
            return adjustImageWithMasksAndBlending(imageSoFar, firstVisibleLayer);
        }

        // normal case: the text will be painted in paintLayerOnGraphics
        return super.paintLayer(g, firstVisibleLayer, imageSoFar);
    }

    @Override
    public BufferedImage adjustImage(BufferedImage src) {
        assert settings.isWatermark(); // should be called only in this case
        return settings.watermarkImage(src, painter);
    }

    public BufferedImage createRasterizedImage() {
        Canvas canvas = comp.getCanvas();
        BufferedImage img = ImageUtils.createCompatibleImage(canvas.getWidth(), canvas.getHeight());
        Graphics2D g = img.createGraphics();
        paintLayer(g, true, img);
        g.dispose();
        return img;
    }

    @Override
    public Shape getShape(Graphics2D g) {
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout textLayout = new TextLayout(settings.getText(), settings.getFont(), frc);
        return textLayout.getOutline(null);
    }

    @Override
    public Layer duplicate() {
        TextLayer d = new TextLayer(comp);

        d.translationX = translationX;
        d.translationY = translationY;
        d.painter.setTranslationX(painter.getTranslationX());
        d.painter.setTranslationY(painter.getTranslationY());

        d.setSettings(new TextSettings(settings));
        d.setName(getName() + " Copy", AddToHistory.NO);

        if (hasMask()) {
            d.addMaskBack(mask.duplicate(d));
        }

        return d;
    }

    @Override
    public void crop(Rectangle selectionBounds) {
        super.crop(selectionBounds);

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

    @Override
    public void moveWhileDragging(int x, int y) {
        super.moveWhileDragging(x, y);
        painter.setTranslationX(getTranslationX());
        painter.setTranslationY(getTranslationY());
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTranslationX, int oldTranslationY) {
        boolean moveMask = hasMask() && mask.isLinked();
        BufferedImage oldMask = null;
        if (moveMask) {
            boolean needsEnlarging = mask.checkImageDoesNotCoverCanvas();
            if (needsEnlarging) {
                oldMask = mask.getImage();
            }
        }
        return new ContentLayerMoveEdit(this, null, oldMask, oldTranslationX, oldTranslationY);
    }

    @Override
    public void setTranslationX(int x) {
        super.setTranslationX(x);
        painter.setTranslationX(x);
    }

    @Override
    public void setTranslationY(int y) {
        super.setTranslationY(y);
        painter.setTranslationY(y);
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

    public static void edit(final PixelitorWindow pw, final Composition comp, final TextLayer textLayer) {
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

}
