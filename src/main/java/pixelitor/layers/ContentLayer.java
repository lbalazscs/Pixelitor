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
import pixelitor.filters.comp.Flip;
import pixelitor.history.History;
import pixelitor.history.TranslateEdit;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A layer with a content (text or image layer)
 */
public abstract class ContentLayer extends Layer {
    private static final long serialVersionUID = 2L;

    private transient int temporaryTranslationX = 0;
    private transient int temporaryTranslationY = 0;
    int translationX = 0;
    int translationY = 0;

    protected ContentLayer(Composition comp, String name) {
        super(comp, name);
    }

    /**
     * startTranslation(), endTranslation(), and moveLayerRelative(int, int) are used by the Move tool
     */
    public void moveLayerRelative(int x, int y) {
        temporaryTranslationX = x;
        temporaryTranslationY = y;
    }

    public int getTranslationX() {
        return translationX + temporaryTranslationX;
    }

    public int getTranslationY() {
        return translationY + temporaryTranslationY;
    }

    /**
     * startTranslation(), endTranslation(), and moveLayerRelative(int, int) are used by the Move tool
     */
    public void startTranslation() {
        temporaryTranslationX = 0;
        temporaryTranslationY = 0;
    }

    /**
     * startTranslation(), endTranslation(), and moveLayerRelative(int, int) are used by the Move tool
     */
    public void endTranslation() {
        int oldTranslationX = translationX;
        int oldTranslationY = translationY;

        translationX += temporaryTranslationX;
        translationY += temporaryTranslationY;
        temporaryTranslationX = 0;
        temporaryTranslationY = 0;

        TranslateEdit edit = createTranslateEdit(oldTranslationX, oldTranslationY);
        History.addEdit(edit);
    }

    abstract TranslateEdit createTranslateEdit(int oldTranslationX, int oldTranslationY);

    /**
     * setTranslationX and setTranslationY programmatically set the translation
     * There is no check for layer enlargement
     */
    public void setTranslationX(int translationX) {
        this.translationX = translationX;
    }

    /**
     * setTranslationX and setTranslationY programmatically set the translation
     * There is no check for layer enlargement
     */
    public void setTranslationY(int translationY) {
        this.translationY = translationY;
    }

    public abstract void flip(Flip.Direction direction);

    public abstract void rotate(int angleDegree);

    public abstract void enlargeCanvas(int north, int east, int south, int west);

    @Override
    public BufferedImage paintLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage imageSoFar) {
        if (layerMask == null) {
            paintLayerOnGraphics(g, firstVisibleLayer);
        } else {
            BufferedImage maskedImage = getMaskedImage(firstVisibleLayer);

//            g.drawImage(maskedImage, getTranslationX(), getTranslationY(), null);
            g.drawImage(maskedImage, 0, 0, null);
        }

        // Content layers only use the Graphics2D
        return null;
    }

    /**
     * Returns an image that is canvas-sized, and the masks and the translations are taken into account
     * Can be overridden if the masked image is cached
     */
    BufferedImage getMaskedImage(boolean firstVisibleLayer) {
//        Canvas canvas = comp.getCanvas();

        BufferedImage maskedImage = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D mig = maskedImage.createGraphics();
        paintLayerOnGraphics(mig, firstVisibleLayer);
        mig.setComposite(AlphaComposite.DstIn);
        mig.drawImage(layerMask.getTransparentImage(), 0, 0, null);
        mig.dispose();
        return maskedImage;
    }

    protected void setupDrawingComposite(Graphics2D g, boolean isFirstVisibleLayer) {
        if (isFirstVisibleLayer) {  // the first visible layer is always painted with normal mode
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        } else {
            Composite composite = blendingMode.getComposite(opacity);
            g.setComposite(composite);
        }
    }

    public abstract void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer);

    @Override
    public void mergeDownOn(ImageLayer bellowImageLayer) {

//        int aX = getTranslationX();
//        int aY = getTranslationY();
        BufferedImage bellowImage = bellowImageLayer.getImage();
//        int bX = bellowImageLayer.getTranslationX();
//        int bY = bellowImageLayer.getTranslationY();

        Graphics2D g = bellowImage.createGraphics();
//        int x = aX - bX;
//        int y = aY - bY;

        // TODO use x,y
        paintLayerOnGraphics(g, false);

        g.dispose();
    }

}
