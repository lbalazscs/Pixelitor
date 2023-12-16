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

package pixelitor.filters;

import pixelitor.filters.gui.TextParam;
import pixelitor.io.IO;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CommandLineFilter extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Command Line";

    private final TextParam textParam = new TextParam("Command",
        "", true);

    public CommandLineFilter() {
        super(true);

        // hack to prevent randomize from creating a random string
        textParam.setRandomCommands(List.of(""));

        setParams(textParam);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (textParam.isEmpty()) {
            return src;
        }

        List<String> commands = parseCommands(textParam.getValue());
        return IO.commandLineFilter(src, NAME, commands);
    }

    // Splits the input by whitespace, considering quoted parts
    private static List<String> parseCommands(String input) {
        List<String> result = new ArrayList<>();

        String[] parts = input.split("\\s(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        Pattern surroundingQuotes = Pattern.compile("^\"|\"$");
        for (String part : parts) {
            String cleanedPart = surroundingQuotes.matcher(part).replaceAll("");
            result.add(cleanedPart);
        }

        return result;
    }
}
