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

package pixelitor.io.magick;

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Locale;

import static pixelitor.gui.utils.SliderSpinner.LabelPosition;

class WebPExportSettings extends JPanel implements ExportSettings {
    public static final ExportSettings INSTANCE = new WebPExportSettings();

    private static final int HINT_DEFAULT = 0;
    private static final int HINT_PHOTO = 1;
    private static final int HINT_PICTURE = 2;
    private static final int HINT_GRAPH = 3;

    private final RangeParam quality = new RangeParam("Quality (Size)",
        1, 50, 100, false, LabelPosition.NONE);
    private final BooleanParam lossless = new BooleanParam("Lossless");
    private final IntChoiceParam imageHint = new IntChoiceParam(
        "Image Hint", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Default", HINT_DEFAULT),
        new IntChoiceParam.Item("Photo", HINT_PHOTO),
        new IntChoiceParam.Item("Picture", HINT_PICTURE),
        new IntChoiceParam.Item("Graph", HINT_GRAPH)
    });

    private WebPExportSettings() {
        super(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(this);

        var controls = List.of(quality, lossless, imageHint);
        for (FilterParam control : controls) {
            gbh.addLabelAndControl(control);
        }
    }

    @Override
    public void addMagickOptions(List<String> command) {
        command.add("-quality");
        command.add(String.valueOf(quality.getValue()));

        command.add("-define");
        command.add("webp:lossless=" + lossless.isChecked());
        command.add("webp:image-hint=" + imageHint.getSelectedItem().toString().toLowerCase(Locale.ENGLISH));
    }

    @Override
    public String getFormatName() {
        return "WebP";
    }
}
