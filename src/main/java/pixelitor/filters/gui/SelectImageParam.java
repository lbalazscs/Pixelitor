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

package pixelitor.filters.gui;

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.layers.Drawable;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

import static pixelitor.filters.gui.SelectImageParam.NamedImage;

/**
 * A {@link FilterParam} that allows the user to select
 * an image from the list of currently opened images.
 */
public class SelectImageParam extends AbstractMultipleChoiceParam<NamedImage> {
    public SelectImageParam(String name) {
        super(name,
            openImageInfos(),
            new NamedImage(OpenImages.getActiveComp()),
            RandomizePolicy.IGNORE_RANDOMIZE);
    }

    public BufferedImage getImage() {
        return currentChoice.getImage();
    }

    @Override
    public void updateOptions(Drawable dr, boolean changeValue) {
        this.choices = openImageInfos();
        NamedImage activeImage = findActiveNamedImage(choices, dr);
        // if there is more than one open image,
        // try to offer one of the inactive ones as the default
        boolean defaultSet = false;
        if (choices.size() > 1) {
            for (NamedImage choice : choices) {
                if (activeImage.getComp() != choice.getComp()) {
                    defaultChoice = choice;
                    currentChoice = defaultChoice;
                    defaultSet = true;
                    break;
                }
            }
        }
        if (!defaultSet) {
            // settle for the active one
            defaultChoice = activeImage;
            currentChoice = defaultChoice;
        }
    }

    private static List<NamedImage> openImageInfos() {
        return OpenImages.getViews().stream()
            .map(View::getComp)
            .map(NamedImage::new)
            .toList();
    }

    private static NamedImage findActiveNamedImage(List<NamedImage> images, Drawable dr) {
        Composition comp = dr.getComp();
        for (NamedImage image : images) {
            if (image.getComp() == comp) {
                return image;
            }
        }
        throw new IllegalStateException("not found");
    }

    static class NamedImage {
        private final BufferedImage image;
        private final Composition comp;

        NamedImage(Composition comp) {
            // it's important to store this image before the filter starts,
            // because the current composite image is affected by the filter
            this.image = comp.getCompositeImage();
            this.comp = comp;
        }

        public BufferedImage getImage() {
            return image;
        }

        public Composition getComp() {
            return comp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NamedImage that = (NamedImage) o;
            return comp == that.comp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(comp);
        }

        @Override
        public String toString() {
            return comp.getName();
        }
    }
}
