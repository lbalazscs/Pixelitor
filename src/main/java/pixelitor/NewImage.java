/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor;

import pixelitor.colors.FillType;
import pixelitor.filters.Fill;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.gui.NewImagePanel;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.PAction;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static pixelitor.colors.FillType.TRANSPARENT;
import static pixelitor.utils.Texts.i18n;

/**
 * Static methods for creating new images
 */
public final class NewImage {
    private static final String NEW_IMAGE_STRING = i18n("new_image");
    private static int untitledCount = 1;

    public static final Dimension lastSize = AppPreferences.getNewImageSize();

    private NewImage() {
    }

    public static Composition addNewImage(FillType bg, int width, int height, String title) {
        var comp = createNewComposition(bg, width, height, title);
        Views.addAsNewComp(comp);
        return comp;
    }

    public static Composition createNewComposition(FillType bg, int width, int height, String title) {
        BufferedImage newImage = ImageUtils.createSysCompatibleImage(width, height);
        fillImage(newImage, bg);
        return Composition.fromImage(newImage, null, title);
    }

    private static void fillImage(BufferedImage img, FillType bg) {
        if (bg == TRANSPARENT) {
            return;
        }
        Color c = bg.getColor();
        Fill.fillImage(img, c);
    }

    private static void showInDialog() {
        var panel = new NewImagePanel();
        new DialogBuilder()
            .title(NEW_IMAGE_STRING)
            .menuBar(new DialogMenuBar(panel))
            .validatedContent(panel)
            .okAction(panel::okPressedInDialog)
            .show();
    }

    public static Action getAction() {
        return new PAction(NEW_IMAGE_STRING + "...") {
            @Override
            public void onClick() {
                showInDialog();
            }
        };
    }

    public static Dimension getLastSize() {
        return lastSize;
    }

    public static String generateTitle() {
        return "Untitled " + untitledCount++;
    }
}

