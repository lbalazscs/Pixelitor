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

package pixelitor.filters.comp;

import pixelitor.Composition;
import pixelitor.history.History;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;

import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;

/**
 * Flips a ContentLayer horizontally or vertically
 */
public class Flip extends CompAction {
    private final Flip.Direction direction;

    private static final Flip horizontalFlip = new Flip(HORIZONTAL);
    private static final Flip verticalFlip = new Flip(VERTICAL);

    public static Flip createFlip(Direction dir) {
        if(dir == HORIZONTAL) {
            return horizontalFlip;
        }
        if(dir == VERTICAL) {
            return verticalFlip;
        }
        throw new IllegalStateException("should not get here");
    }

    private Flip(Direction dir) {
        super(dir.getName());
        direction = dir;
    }

    @Override
    public void transform(Composition comp) {
        int nrLayers = comp.getNrLayers();

        // Saved before the change, but the edit is
        // created after the change.
        // This way no image copy is necessary.
        BufferedImage backupImage = null;

        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ContentLayer) {
                if (layer instanceof ImageLayer) {
                    backupImage = ((ImageLayer) layer).getImage();
                }
                ContentLayer contentLayer = (ContentLayer) layer;
                contentLayer.flip(direction);
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                mask.flip(direction);
            }
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, "Flip", backupImage, null);
        History.addEdit(edit);

        comp.setDirty(true);
        comp.imageChanged(REPAINT);
    }

    /**
     * The direction of the flip
     */
    public enum Direction {
        HORIZONTAL {
            @Override
            public String getName() {
                return "Flip Horizontal";
            }
        }, VERTICAL {
            @Override
            public String getName() {
                return "Flip Vertical";
            }
        };

        public abstract String getName();
    }
}
