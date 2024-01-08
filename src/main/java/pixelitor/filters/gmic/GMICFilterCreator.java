/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.utils.Lazy;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GMICFilterCreator {
    private static final Lazy<Pattern> INT_PATTERN = Lazy.of(() -> Pattern.compile("([a-zA-Z0-9\\s]+) = int\\((-?\\d+),(-?\\d+),(-?\\d+)\\)"));
    private static final Lazy<Pattern> FLOAT_PATTERN = Lazy.of(() -> Pattern.compile("([a-zA-Z0-9\\s]+) = float\\(([-+]?\\d*\\.?\\d+),([-+]?\\d*\\.?\\d+),([-+]?\\d*\\.?\\d+)\\)"));
    private static final Lazy<Pattern> COLOR_PATTERN = Lazy.of(() -> Pattern.compile("([a-zA-Z0-9\\s]+) = color\\((\\d+),(\\d+),(\\d+),(\\d+)\\)"));
    private static final Lazy<Pattern> CHOICE_PATTERN = Lazy.of(() -> Pattern.compile("([a-zA-Z0-9\\s]+) = choice\\((.+?)\\)"));
    private static final Lazy<Pattern> BOOL_PATTERN = Lazy.of(() -> Pattern.compile("([a-zA-Z0-9\\s]+) = bool\\((\\d)\\)"));
    public static final String TITLE = "G'MIC Filter Creator";

    private final List<ParamInfo> paramInfos;
    private final String commandName;
    private final String filterName;
    private final StringBuilder sb;
    private final String className;

    private GMICFilterCreator(String orig, String commandName, String filterName) {
        this.paramInfos = orig.lines()
            .map(s -> s.split(" : ")[1])
            .map(GMICFilterCreator::transformLine)
            .toList();
        this.commandName = commandName;
        this.filterName = filterName;
        this.className = filterName.replaceAll(" ", "");

        sb = new StringBuilder(1000);

        createHeader();
        createConstructor();
        createGetArgs();
        createFooter();
    }

    public static void showInDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(p);

        JTextField filterNameTF = new JTextField("Huffman Glitches");
        JTextField commandNameTF = new JTextField("fx_huffman_glitches");

        String orig = """
            #@gui : Noise Level = int(30,0,100)
            #@gui : Split Mode = choice("None","Horizontal Blocs","Vertical Blocs","Patches")
            #@gui : Bloc Size = int(25,0,100)
            #@gui : Patch Overlap = int(0,0,50)
            #@gui : Color Space = choice("RGB","CMYK","HCY","HSI","HSL","HSV","Jzazbz","Lab","Lch","OKLab","YCbCr","YIQ")
            #@gui : Quantization = int(0,0,64)
            #@gui : Random Seed = int(0,0,65536)
            """;
        JTextArea gmicDescrTA = new JTextArea(orig, 10, 80);

        String javaCode = createJavaCode(gmicDescrTA, commandNameTF, filterNameTF);
        JTextArea filterTA = new JTextArea(javaCode, 30, 80);

        JButton regenerateButton = new JButton("Regenerate");
        regenerateButton.addActionListener(e ->
            filterTA.setText(createJavaCode(gmicDescrTA, commandNameTF, filterNameTF)));

        gbh.addLabelAndControl("Filter Name", filterNameTF);
        gbh.addLabelAndControl("G'MIC Command", commandNameTF);
        gbh.addLabelAndControlVerticalStretch("G'MIC UI", addScrollBars(gmicDescrTA), 100);
        gbh.addLabelAndControlNoStretch("", regenerateButton);
        gbh.addLabelAndControlVerticalStretch("Filter", addScrollBars(filterTA), 200);

        GUIUtils.showCopyTextToClipboardDialog(p, filterTA::getText, TITLE);
    }

    private static String createJavaCode(JTextArea gmicTA, JTextField commandNameTF, JTextField filterNameTF) {
        String javaCode = new GMICFilterCreator(
            gmicTA.getText().trim(),
            commandNameTF.getText().trim(),
            filterNameTF.getText().trim()).getOutput();
        return javaCode;
    }

    private static JScrollPane addScrollBars(JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane pane = new JScrollPane(area);
        return pane;
    }

    public String getOutput() {
        return sb.toString();
    }

    private void createHeader() {
        String imports = """
            package pixelitor.filters.gmic;
                            
            import java.io.Serial;
            import java.util.List;
            import pixelitor.filters.gui.BooleanParam;
            import pixelitor.filters.gui.ColorParam;
            import pixelitor.filters.gui.IntChoiceParam;
            import pixelitor.filters.gui.IntChoiceParam.Item;
            import pixelitor.filters.gui.RangeParam;
            """;
        addString(imports, 0);

        String header = """
            public class %s extends GMICFilter {
                @Serial
                private static final long serialVersionUID = 1L;
            """.formatted(className);
        addString(header, 0);

        addString("public static final String NAME = \"" + filterName + "\";", 1);
        addNewLine();

        for (ParamInfo paramInfo : paramInfos) {
            addString("private final " + paramInfo.declaration(), 1);
        }
    }

    private void createConstructor() {
        addNewLine();
        addString("public " + className + "() {", 1);
        addString("setParams(", 2);
        for (int i = 0; i < paramInfos.size(); i++) {
            ParamInfo paramInfo = paramInfos.get(i);
            String line = paramInfo.name();
            if (i != paramInfos.size() - 1) {
                line += ",";
            }
            addString(line, 3);
        }
        addString(");", 2);
        addString("}", 1);
    }

    private void createGetArgs() {
        addNewLine();
        addString("@Override", 1);
        addString("public List<String> getArgs() {", 1);
        addString("return List.of(\"" + commandName + "\",", 2);
        for (int i = 0; i < paramInfos.size(); i++) {
            ParamInfo paramInfo = paramInfos.get(i);

            String method = switch (paramInfo.type()) {
                case "float" -> ".getPercentage()";
                case "bool" -> ".isCheckedStr()";
                case "color" -> ".getColorStr()";
                default -> ".getValue()";
            };
            String line = paramInfo.name() + method;
            if (i != paramInfos.size() - 1) {
                line += " + \",\" +";
            }
            addString(line, 3);
        }
        addString(");", 2);
        addString("}", 1);
    }

    private void createFooter() {
        addString("}", 0);
    }

    private static ParamInfo transformLine(String input) {
        ParamInfo output = null;
        if (input.contains(" = int(")) {
            output = transformIntRange(input);
        } else if (input.contains(" = float(")) {
            output = transformFloatRange(input);
        } else if (input.contains(" = color(")) {
            output = transformColor(input);
        } else if (input.contains(" = choice(")) {
            output = transformChoice(input);
        } else if (input.contains(" = bool(")) {
            output = transformBool(input);
        }
        return output;
    }

    private static ParamInfo transformIntRange(String input) {
        Matcher matcher = INT_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            int defaultValue = Integer.parseInt(matcher.group(2));
            int minValue = Integer.parseInt(matcher.group(3));
            int maxValue = Integer.parseInt(matcher.group(4));

            String declaration = String.format("RangeParam %s = new RangeParam(\"%s\", %d, %d, %d);",
                paramName,
                paramString,
                minValue,
                defaultValue,
                maxValue);
            return new ParamInfo(paramName, declaration, "int");
        } else {
            throw new RuntimeException("Invalid input format: " + input);
        }
    }

    private static ParamInfo transformFloatRange(String input) {
        Matcher matcher = FLOAT_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            double defaultValue = Double.parseDouble(matcher.group(2));
            double minValue = Double.parseDouble(matcher.group(3));
            double maxValue = Double.parseDouble(matcher.group(4));

            String declaration = String.format("RangeParam %s = new RangeParam(\"%s\", %.0f, %.0f, %.0f);",
                paramName,
                paramString,
                minValue * 100,
                defaultValue * 100,
                maxValue * 100);
            return new ParamInfo(paramName, declaration, "float");
        } else {
            throw new RuntimeException("Invalid input format: " + input);
        }
    }

    private static ParamInfo transformColor(String input) {
        Matcher matcher = COLOR_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            int red = Integer.parseInt(matcher.group(2));
            int green = Integer.parseInt(matcher.group(3));
            int blue = Integer.parseInt(matcher.group(4));
            int alpha = Integer.parseInt(matcher.group(5));

            String declaration = String.format("ColorParam %s = new ColorParam(\"%s\", new Color(%d, %d, %d, %d));",
                paramName,
                paramString,
                red,
                green,
                blue,
                alpha);
            return new ParamInfo(paramName, declaration, "color");
        } else {
            throw new RuntimeException("Invalid input format: " + input);
        }
    }

    private static ParamInfo transformChoice(String input) {
        Matcher matcher = CHOICE_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            String choicesString = matcher.group(2);

            String[] choices = choicesString.split(",");
            StringBuilder output = new StringBuilder(String.format("IntChoiceParam %s = new IntChoiceParam(\"%s\", new Item[] {",
                paramName,
                paramString));

            for (int i = 0; i < choices.length; i++) {
                String choice = choices[i].replaceAll("\"", "").trim();
                output.append(String.format("\n        new Item(\"%s\", %d),", choice, i));
            }

            // Remove the trailing comma and close the array
            output.deleteCharAt(output.length() - 1);
            output.append("\n    });");

            String declaration = output.toString();
            return new ParamInfo(paramName, declaration, "choice");
        } else {
            throw new RuntimeException("Invalid input format: " + input);
        }
    }

    private static ParamInfo transformBool(String input) {
        Matcher matcher = BOOL_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            int boolValue = Integer.parseInt(matcher.group(2));
            boolean boolResult = (boolValue == 1);

            String declaration = String.format(
                "BooleanParam %s = new BooleanParam(\"%s\", %s);",
                paramName,
                paramString,
                boolResult
            );
            return new ParamInfo(paramName, declaration, "bool");
        } else {
            throw new RuntimeException("Invalid input format: " + input);
        }
    }

    private static String toCamelCase(String input) {
        String[] words = input.split("\\s+");

        if (words.length > 0) {
            // Convert the first word to lowercase
            StringBuilder camelCase = new StringBuilder(words[0].toLowerCase());

            // Convert the subsequent words, capitalizing the first letter of each
            for (int i = 1; i < words.length; i++) {
                camelCase.append(words[i].substring(0, 1).toUpperCase())
                    .append(words[i].substring(1).toLowerCase());
            }

            return camelCase.toString();
        } else {
            return "";
        }
    }

    private void addString(String line, int indent) {
        sb.append("    ".repeat(indent)).append(line).append('\n');
    }

    private void addNewLine() {
        addString("", 0);
    }

    private record ParamInfo(String name, String declaration, String type) {
    }
}
