/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.TaskAction;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ResizeUnit;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static pixelitor.colors.FillType.TRANSPARENT;
import static pixelitor.colors.FillType.WHITE;
import static pixelitor.utils.Texts.i18n;

/**
 * Static methods for creating new images.
 */
public final class NewImage {
    private static final String NEW_IMAGE_STRING = i18n("new_image");
    private static int untitledCount = 1;

    // last used settings for the "New Image" dialog
    private static Dimension lastSize = AppPreferences.loadNewImageSize();

    // these last used settings are currently not restored after a restart
    private static ResizeUnit lastUnit = ResizeUnit.PIXELS;
    private static int lastDpi = Composition.DEFAULT_DPI;
    private static FillType lastFillType = WHITE;

    private NewImage() {
    }

    public static Composition addNewImage(FillType fillType, int width, int height, String title, int dpi) {
        var comp = createNewComposition(fillType, width, height, title, dpi);
        Views.addNew(comp);
        return comp;
    }

    public static Composition createNewComposition(FillType fillType, int width, int height, String name, int dpi) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width = " + width + ", height = " + height);
        }

        BufferedImage newImage = ImageUtils.createSysCompatibleImage(width, height);
        if (fillType != TRANSPARENT) {
            Fill.fillImage(newImage, fillType.getColor());
        }

        return Composition.fromImage(newImage, null, name, dpi);
    }

    private static void showInDialog() {
        var initialSettings = new NewImagePanel.Settings(
            lastSize.width,
            lastSize.height,
            lastDpi,
            lastUnit,
            lastFillType,
            generateTitle()
        );
        var panel = new NewImagePanel(initialSettings);

        Runnable okAction = () -> {
            NewImagePanel.Settings settings = panel.getSettings();

            addNewImage(settings.fillType(), settings.width(), settings.height(), settings.title(), settings.dpi());

            lastSize = new Dimension(settings.width(), settings.height());
            lastUnit = settings.unit();
            lastDpi = settings.dpi();
            lastFillType = settings.fillType();

            // increment it only after a sucessfully added image
            untitledCount++;
        };

        new DialogBuilder()
            .title(NEW_IMAGE_STRING)
            .menuBar(new DialogMenuBar(panel))
            .validatedContent(panel)
            .okAction(okAction)
            .show();
    }

    public static Dimension getLastSize() {
        return lastSize;
    }

    public static ResizeUnit getLastUnit() {
        return lastUnit;
    }

    public static int getLastDpi() {
        return lastDpi;
    }

    public static FillType getLastFillType() {
        return lastFillType;
    }

    public static Action getAction() {
        return new TaskAction(NEW_IMAGE_STRING + "...", NewImage::showInDialog);
    }

    private static String generateTitle() {
        return "Untitled " + untitledCount;
    }
}
