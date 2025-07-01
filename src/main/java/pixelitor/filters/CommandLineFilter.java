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

package pixelitor.filters;

import pixelitor.filters.gui.CommandLineGUI;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.TextParam;
import pixelitor.io.FileIO;
import pixelitor.layers.Filterable;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CommandLineFilter extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Command Line";

    // Pattern to split on whitespace, while preserving quoted text as single arguments
    private static final Pattern WHITESPACE_OUTSIDE_QUOTES_PATTERN =
        Pattern.compile("\\s(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    // Pattern to match and remove surrounding quotes from strings
    private static final Pattern SURROUNDING_QUOTES_PATTERN =
        Pattern.compile("^\"|\"$");

    private final TextParam textParam = new TextParam("Command",
        "", true);

    public CommandLineFilter() {
        super(true);

        // hack to prevent randomize from creating a random string
        textParam.setRandomCommands(List.of(""));

        initParams(textParam);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (textParam.isEmpty()) {
            return src;
        }

        List<String> commands = parseCommands(textParam.getValue());
        return FileIO.applyCommandLineFilter(src, commands);
    }

    private static List<String> parseCommands(String input) {
        List<String> result = new ArrayList<>();

        // splits the input by whitespace, while ignoring whitespace within quotes
        String[] tokens = WHITESPACE_OUTSIDE_QUOTES_PATTERN.split(input);

        for (String token : tokens) {
            // removes surrounding quotes if present
            String cleanedToken = SURROUNDING_QUOTES_PATTERN.matcher(token).replaceAll("");
            if (!cleanedToken.isBlank()) {
                result.add(cleanedToken);
            }
        }

        return result;
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        return new CommandLineGUI(this, layer, true, reset);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
