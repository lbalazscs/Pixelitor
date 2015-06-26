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
import pixelitor.history.History;
import pixelitor.history.NewLayerEdit;
import pixelitor.history.TextLayerChangeEdit;
import pixelitor.history.TextLayerRasterizeEdit;
import pixelitor.history.TranslateEdit;
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
public class TextLayer extends ShapeLayer {
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
            return settings.watermarkImage(imageSoFar, painter);
        }

        return super.paintLayer(g, firstVisibleLayer, imageSoFar);
    }

    public BufferedImage createRasterizedImage() {
        Canvas canvas = comp.getCanvas();
        BufferedImage img = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
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
        TextLayer duplicate = new TextLayer(comp);

        duplicate.translationX = translationX;
        duplicate.translationY = translationY;
        duplicate.painter.setTranslationX(painter.getTranslationX());
        duplicate.painter.setTranslationY(painter.getTranslationY());

        duplicate.setSettings(new TextSettings(settings));
        duplicate.setName(getName() + " Copy", AddToHistory.NO);
        return duplicate;
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

    @Override
    public void moveWhileDragging(int x, int y) {
        super.moveWhileDragging(x, y);
        painter.setTranslationX(getTranslationX());
        painter.setTranslationY(getTranslationY());
    }

    @Override
    TranslateEdit createTranslateEdit(int oldTranslationX, int oldTranslationY) {
        return new TranslateEdit(this, null, oldTranslationX, oldTranslationY);
    }

    @Override
    public void setTranslationX(int translationX) {
        super.setTranslationX(translationX);
        painter.setTranslationX(translationX);
    }

    @Override
    public void setTranslationY(int translationY) {
        super.setTranslationY(translationY);
        painter.setTranslationY(translationY);
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
        TextSettings oldSettings = textLayer.getSettings();
        TextAdjustmentsPanel p = new TextAdjustmentsPanel(textLayer);
        OKCancelDialog d = new OKCancelDialog(p, pw, "Edit Text Layer") {
            @Override
            protected void dialogAccepted() {
                close();

                textLayer.updateLayerName();
                TextLayerChangeEdit edit = new TextLayerChangeEdit(
                        textLayer.getComposition(),
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

        ImageLayer newLayer = new ImageLayer(comp, rasterizedImage, layer.getName());
        comp.addLayer(newLayer, AddToHistory.NO, false, false);
        comp.removeLayer(textLayer, AddToHistory.NO);

        TextLayerRasterizeEdit edit = new TextLayerRasterizeEdit(comp, textLayer, newLayer);
        History.addEdit(edit);
    }
}
