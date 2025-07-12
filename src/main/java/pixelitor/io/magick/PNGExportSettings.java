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

import pixelitor.colors.Colors;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.util.List;

class PNGExportSettings extends JPanel implements ExportSettings {
    public static final ExportSettings INSTANCE = new PNGExportSettings();

    enum Type {
        RGBA("RGBA (32 bit)"),
        RGB("RGB (24 bit, no transparency)"),
        INDEXED("Indexed, (8 bit, max 256 colors)");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final JComboBox<Type> typeCB;
    private final JCheckBox interlacedCB;
    private final RangeParam compression;

    private PNGExportSettings() {
        super(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(this);

        typeCB = new JComboBox<>(Type.values());
        gbh.addLabelAndControl("Type:", typeCB);

        interlacedCB = new JCheckBox(null, null, false);
        gbh.addLabelAndControl("Interlaced:", interlacedCB);

        compression = new RangeParam("Compression Level",
            0, 7, 9, true, SliderSpinner.LabelPosition.NONE);
        gbh.addLabelAndControl(compression);
    }

    @Override
    public void addMagickOptions(List<String> command) {
        Type type = getType();
        if (type == Type.RGB) {
            command.add("-background");
            command.add("#" + Colors.toHTMLHex(FgBgColors.getBGColor(), false));

            // remove: composite the image over the background color.
            command.add("-alpha");
            command.add("Remove");

            command.add("-alpha");
            command.add("Off");
        }
        if (type == Type.INDEXED) {
            command.add("-colors");
            command.add("255");
        }

        if (interlacedCB.isSelected()) {
            command.add("-interlace");
            command.add("PNG");
        }

        // the quality value sets the zlib compression level (quality / 10)
        // and filter-type (quality % 10)
        command.add("-quality");
        command.add(String.valueOf(compression.getValue() * 10));
    }

    @Override
    public String getFormatSpecifier() {
        return switch (getType()) {
            case RGBA -> "png32:";
            case RGB -> "png24:";
            case INDEXED -> "";
        };
    }

    private Type getType() {
        return (Type) typeCB.getSelectedItem();
    }

    @Override
    public String getFormatName() {
        return "PNG";
    }
}
