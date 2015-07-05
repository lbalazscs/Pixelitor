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
import pixelitor.history.CompoundEdit;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.PixelitorEdit;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.AlphaComposite.DstIn;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A layer with a content (text or image layer) that
 * can be moved/rotated.
 */
public abstract class ContentLayer extends Layer {
    private static final long serialVersionUID = 2L;

    // used only while dragging
    private transient int tmpTranslationX = 0;
    private transient int tmpTranslationY = 0;

    int translationX = 0;
    int translationY = 0;

    protected ContentLayer(Composition comp, String name, Layer parent) {
        super(comp, name, parent);
    }

    public int getTranslationX() {
        return translationX + tmpTranslationX;
    }

    public int getTranslationY() {
        return translationY + tmpTranslationY;
    }

    public void startMovement() {
        tmpTranslationX = 0;
        tmpTranslationY = 0;
        super.startMovement();
    }

    public void moveWhileDragging(int x, int y) {
        tmpTranslationX = x;
        tmpTranslationY = y;
        super.moveWhileDragging(x, y);
    }

    public PixelitorEdit endMovement() {

        int oldTranslationX = translationX;
        int oldTranslationY = translationY;

        // while dragging only the temporary values were updated
        // and now they can be committed to the final value
        translationX += tmpTranslationX;
        translationY += tmpTranslationY;
        tmpTranslationX = 0;
        tmpTranslationY = 0;

        PixelitorEdit linkedEdit = super.endMovement();

        ContentLayerMoveEdit ownEdit = createMovementEdit(oldTranslationX, oldTranslationY);
        if (linkedEdit == null) {
            return ownEdit;
        } else {
            return new CompoundEdit(comp, ContentLayerMoveEdit.NAME, ownEdit, linkedEdit);
        }
    }

    abstract ContentLayerMoveEdit createMovementEdit(int oldTranslationX, int oldTranslationY);

    /**
     * Programmatically set the translation.
     * There is no check for layer enlargement.
     * Also the linked layer is NOT translated.
     */
    public void setTranslation(int x, int y) {
        this.translationX = x;
        this.translationY = y;
    }

    public abstract void flip(Flip.Direction direction);

    public abstract void rotate(int angleDegree);

    public abstract void enlargeCanvas(int north, int east, int south, int west);

    @Override
    public BufferedImage paintLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage imageSoFar) {
        if (mask == null) {
            paintLayerOnGraphics(g, firstVisibleLayer);
        } else {
            BufferedImage maskedImage = getMaskedImage(firstVisibleLayer);

//            g.drawImage(maskedImage, getTranslationX(), getTranslationY(), null);
            g.drawImage(maskedImage, 0, 0, null);
        }

        // Content layers only use the Graphics2D
        // TODO not true, TextLayer overrides this with correct behavior,
        // but then why this method?
        return null;
    }

    /**
     * Returns an image that is canvas-sized, and the masks and the translations are taken into account
     * TODO Can be overridden if the masked image is cached
     */
    BufferedImage getMaskedImage(boolean firstVisibleLayer) {
//        Canvas canvas = comp.getCanvas();

        BufferedImage maskedImage = new BufferedImage(canvas.getWidth(), canvas.getHeight(), TYPE_INT_ARGB);
        Graphics2D mig = maskedImage.createGraphics();
        paintLayerOnGraphics(mig, firstVisibleLayer);
        mig.setComposite(DstIn);
        mig.drawImage(mask.getTransparencyImage(), mask.getTranslationX(), mask.getTranslationY(), null);
        mig.dispose();
        return maskedImage;
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
