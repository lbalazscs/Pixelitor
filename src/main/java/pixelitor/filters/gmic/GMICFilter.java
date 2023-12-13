/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gmic;

import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.io.IO;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Result;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class GMICFilter extends ParametrizedFilter {
    public static File GMIC_PATH;

    protected GMICFilter() {
        super(true);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        List<String> args = getArgs();
        System.out.println(String.join(" ", args));

        List<String> command = new ArrayList<>(10);
        command.add(GMIC_PATH.getAbsolutePath());
        command.add("-input");
        command.add("-.png");
        command.addAll(args);
        command.add("-output");
        command.add("-.png");

        Result<BufferedImage, String> result = IO.commandLineFilterImage(src, command);
        if (result.wasSuccess()) {
            return ImageUtils.toSysCompatibleImage(result.get());
        } else {
            Messages.showError("G'MIC Error", result.errorDetail());
            return src;
        }
    }

    public abstract List<String> getArgs();

    public static IntChoiceParam createValueAction() {
        return new IntChoiceParam("Value Action", new String[]{
            "None", "Cut", "Normalize"});
    }

    public static IntChoiceParam createChannelChoice() {
        return new IntChoiceParam("Channels", new String[]{
            "All", "RGBA [All]", "RGB [All]",
            "RGB [Red]", "RGB [Green]", "RGB [Blue]", "RGBA [Alpha]",
            "Linear RGB [All]", "Linear RGB [Red]", "Linear RGB [Green]", "Linear RGB [Blue]",
            "YCbCr [Luminance]", "YCbCr [Blue-Red Chrominances]", "YCbCr [Blue Chrominance]",
            "YCbCr [Red Chrominance]", "YCbCr [Green Chrominance]",
            "Lab [Lightness]", "Lab [ab-Chrominances]",
            "Lab [a-Chrominance]", "Lab [b-Chrominance]",
            "Lch [ch-Chrominances]", "Lch [c-Chrominance]", "Lch [h-Chrominance]",
            "HSV [Hue]", "HSV [Saturation]", "HSV [Value]",
            "HSI [Intensity]", "HSL [Lightness]",
            "CMYK [Cyan]", "CMYK [Magenta]", "CMYK [Yellow]", "CMYK [Key]",
            "YIQ [Luma]", "YIQ [Chromas]",
            "RYB [All]", "RYB [Red]", "RYB [Yellow]", "RYB [Blue]",
        });
    }
}
