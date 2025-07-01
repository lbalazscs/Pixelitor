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

package pixelitor.filters.gmic;

import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.utils.Lazy;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GMICFilterCreator {
    private static final String NAME_REGEX = "([a-zA-Z0-9\\s-]+) = ";
    private static final Lazy<Pattern> INT_PATTERN = Lazy.of(() -> Pattern.compile(NAME_REGEX + "int\\((-?\\d+),(-?\\d+),(-?\\d+)\\)"));
    private static final Lazy<Pattern> FLOAT_PATTERN = Lazy.of(() -> Pattern.compile(NAME_REGEX + "float\\(([-+]?\\d*\\.?\\d+),([-+]?\\d*\\.?\\d+),([-+]?\\d*\\.?\\d+)\\)"));
    private static final Lazy<Pattern> COLOR_PATTERN = Lazy.of(() -> Pattern.compile(NAME_REGEX + "color\\((\\d+),(\\d+),(\\d+),(\\d+)\\)"));
    private static final Lazy<Pattern> CHOICE_PATTERN = Lazy.of(() -> Pattern.compile(NAME_REGEX + "choice\\((.+?)\\)"));
    private static final Lazy<Pattern> BOOL_PATTERN = Lazy.of(() -> Pattern.compile(NAME_REGEX + "bool\\((\\d)\\)"));
    private static final Lazy<Pattern> PERCENT_PATTERN = Lazy.of(() -> Pattern.compile(" \\(%\\)"));
    private static final Lazy<Pattern> WORD_SEPARATOR_PATTERN = Lazy.of(() -> Pattern.compile("[\\s-]+"));

    public static final String TITLE = "G'MIC Filter Creator";
    private static final String COULD_NOT_PARSE_MSG = "Could not parse the following:\n";
    private static final String RESEED_NONE = "None";
    private static final String RESEED_PARAMETER = "Parameter";
    private static final String RESEED_SRAND = "Srand";

    private final List<ParamInfo> paramInfos;
    private final String commandName;
    private final String filterName;
    private final StringBuilder sb;
    private final String className;
    private final String reseed;

    private GMICFilterCreator(String orig, String commandName, String filterName, String reseed) {
        this.paramInfos = orig.lines()
            .map(GMICFilterCreator::transformLine)
            .toList();
        this.commandName = commandName;
        this.filterName = filterName;
        this.className = filterName.replaceAll(" ", "");
        this.reseed = reseed;

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

        JComboBox<String> reseedCB = new JComboBox<>(new String[]{
            RESEED_NONE, RESEED_PARAMETER, RESEED_SRAND
        });

        String javaCode = createJavaCode(gmicDescrTA, commandNameTF, filterNameTF, reseedCB, p);
        JTextArea filterTA = new JTextArea(javaCode, 30, 80);

        JButton regenerateButton = new JButton("Regenerate");
        regenerateButton.addActionListener(e ->
            filterTA.setText(createJavaCode(gmicDescrTA, commandNameTF, filterNameTF, reseedCB, p)));

        gbh.addLabelAndControl("Filter Name:", filterNameTF);
        gbh.addLabelAndControl("G'MIC Command:", commandNameTF);
        gbh.addVerticallyStretchable("G'MIC UI:", addScrollBars(gmicDescrTA), 100);
        gbh.addLabelAndControlNoStretch("Reseed:", reseedCB);
        gbh.addLabelAndControlNoStretch("", regenerateButton);
        gbh.addVerticallyStretchable("Filter:", addScrollBars(filterTA), 200);

        GUIUtils.showCopyTextToClipboardDialog(p, filterTA::getText, TITLE);
    }

    private static String createJavaCode(JTextArea gmicTA, JTextField commandNameTF, JTextField filterNameTF, JComboBox<String> reseedCB, JPanel dialogParent) {
        String javaCode;
        try {
            javaCode = new GMICFilterCreator(
                gmicTA.getText().trim(),
                commandNameTF.getText().trim(),
                filterNameTF.getText().trim(),
                (String) reseedCB.getSelectedItem()).getOutput();
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.startsWith(COULD_NOT_PARSE_MSG)) {
                Messages.showError("Error", msg, dialogParent);
                return "";
            } else {
                throw e;
            }
        }
        return javaCode;
    }

    private static JScrollPane addScrollBars(JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return new JScrollPane(area);
    }

    public String getOutput() {
        return sb.toString();
    }

    private void createHeader() {
        addLine("package pixelitor.filters.gmic;", 0);
        addNewLine();
        createImports();
        addNewLine();

        String header = """
            public class %s extends GMICFilter {
                @Serial
                private static final long serialVersionUID = 1L;
            """.formatted(className);
        addLine(header, 0);

        addLine("public static final String NAME = \"" + filterName + "\";", 1);
        addNewLine();

        for (ParamInfo paramInfo : paramInfos) {
            addLine("private final " + paramInfo.declaration(), 1);
        }
    }

    private void createImports() {
        Set<String> imports = new TreeSet<>();
        for (ParamInfo paramInfo : paramInfos) {
            switch (paramInfo.type()) {
                case "color" -> {
                    imports.add("java.awt.Color");
                    imports.add("pixelitor.filters.gui.ColorParam");
                }
                case "int", "float" -> imports.add("pixelitor.filters.gui.RangeParam");
                case "bool" -> imports.add("pixelitor.filters.gui.BooleanParam");
                case "choice" -> {
                    imports.add("pixelitor.filters.gui.IntChoiceParam");
                    imports.add("pixelitor.filters.gui.IntChoiceParam.Item");
                }
                default -> throw new IllegalStateException("type = " + paramInfo.type());
            }
        }
        imports.add("java.io.Serial");
        imports.add("java.util.List");

        imports.forEach(s -> addLine("import " + s + ";", 0));
    }

    private void createConstructor() {
        addNewLine();
        addLine("public " + className + "() {", 1);
        addLine("initParams(", 2);
        for (int i = 0; i < paramInfos.size(); i++) {
            ParamInfo paramInfo = paramInfos.get(i);
            String line = paramInfo.name();
            if (i != paramInfos.size() - 1) {
                line += ",";
            }
            addLine(line, 3);
        }
        if (reseed.equals(RESEED_NONE)) {
            addLine(");", 2);
        } else {
            addLine(").withReseedGmicAction(this);", 2);
        }
        addLine("}", 1);
    }

    private void createGetArgs() {
        addNewLine();
        addLine("@Override", 1);
        addLine("public List<String> getArgs() {", 1);
        if (reseed.equals(RESEED_SRAND)) {
            addLine("return List.of(\"srand\", String.valueOf(seed), \"" + commandName + "\",", 2);
        } else {
            addLine("return List.of(\"" + commandName + "\",", 2);
        }
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
            addLine(line, 3);
        }
        addLine(");", 2);
        addLine("}", 1);
    }

    private void createFooter() {
        addLine("}", 0);
    }

    private static ParamInfo transformLine(String input) {
        // keep only the part after the colon
        String[] parts = input.split(" : ");
        if (parts.length != 2) {
            throw new RuntimeException(COULD_NOT_PARSE_MSG + input);
        }
        input = parts[1];

        // remove " (%)" from the string
        input = PERCENT_PATTERN.get().matcher(input).replaceAll("");

        ParamInfo output;
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
        } else {
            throw new RuntimeException(COULD_NOT_PARSE_MSG + input);
        }
        return output;
    }

    private static ParamInfo transformIntRange(String input) {
        Matcher matcher = INT_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            String paramTitle = toTitleCase(paramString);

            int defaultValue = Integer.parseInt(matcher.group(2));
            int minValue = Integer.parseInt(matcher.group(3));
            int maxValue = Integer.parseInt(matcher.group(4));

            return createIntDeclaration(paramName, paramTitle, minValue, defaultValue, maxValue);
        } else {
            throw new RuntimeException(COULD_NOT_PARSE_MSG + input);
        }
    }

    private static ParamInfo createIntDeclaration(String paramName, String paramTitle, int minValue, int defaultValue, int maxValue) {
        String declaration = String.format("RangeParam %s = new RangeParam(\"%s\", %d, %d, %d);",
            paramName,
            paramTitle,
            minValue,
            defaultValue,
            maxValue);
        return new ParamInfo(paramName, declaration, "int");
    }

    private static ParamInfo createFloatDeclaration(String defaultString, String minString, String maxString, String paramName, String paramTitle) {
        double defaultValue = Double.parseDouble(defaultString);
        double minValue = Double.parseDouble(minString);
        double maxValue = Double.parseDouble(maxString);

        String declaration = String.format("RangeParam %s = new RangeParam(\"%s\", %.0f, %.0f, %.0f);",
            paramName,
            paramTitle,
            minValue * 100,
            defaultValue * 100,
            maxValue * 100);
        return new ParamInfo(paramName, declaration, "float");
    }

    private static ParamInfo transformFloatRange(String input) {
        Matcher matcher = FLOAT_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            String paramTitle = toTitleCase(paramString);

            String defaultString = matcher.group(2);
            String minString = matcher.group(3);
            String maxString = matcher.group(4);

            if (isAllInt(defaultString, minString, maxString)) {
                int defaultValue = Integer.parseInt(defaultString);
                int minValue = Integer.parseInt(minString);
                int maxValue = Integer.parseInt(maxString);

                if (maxValue - minValue < 2) {
                    // handle cases like "Opacity = float(1,0,1)"
                    return createFloatDeclaration(defaultString, minString, maxString, paramName, paramTitle);
                } else {
                    return createIntDeclaration(paramName, paramTitle, minValue, defaultValue, maxValue);
                }
            } else { // not all ints
                return createFloatDeclaration(defaultString, minString, maxString, paramName, paramTitle);
            }
        } else {
            throw new RuntimeException(COULD_NOT_PARSE_MSG + input);
        }
    }

    private static boolean isAllInt(String s1, String s2, String s3) {
        try {
            Integer.parseInt(s1);
            Integer.parseInt(s2);
            Integer.parseInt(s3);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static ParamInfo transformColor(String input) {
        Matcher matcher = COLOR_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            String paramTitle = toTitleCase(paramString);

            int red = Integer.parseInt(matcher.group(2));
            int green = Integer.parseInt(matcher.group(3));
            int blue = Integer.parseInt(matcher.group(4));
            int alpha = Integer.parseInt(matcher.group(5));

            String declaration = String.format("ColorParam %s = new ColorParam(\"%s\", new Color(%d, %d, %d, %d));",
                paramName, paramTitle,
                red, green, blue, alpha);
            return new ParamInfo(paramName, declaration, "color");
        } else {
            throw new RuntimeException(COULD_NOT_PARSE_MSG + input);
        }
    }

    private static ParamInfo transformChoice(String input) {
        Matcher matcher = CHOICE_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            String paramTitle = toTitleCase(paramString);

            String choicesString = matcher.group(2);

            String[] choices = choicesString.split(",");
            StringBuilder output = new StringBuilder(String.format("IntChoiceParam %s = new IntChoiceParam(\"%s\", new Item[] {",
                paramName,
                paramTitle));

            boolean hasDefaultSpecified = !choices[0].contains("\"");
            int startIndex = hasDefaultSpecified ? 1 : 0;
            for (int i = startIndex; i < choices.length; i++) {
                String choice = choices[i].replaceAll("\"", "").trim();
                output.append(String.format("\n        new Item(\"%s\", %d),", choice, i - startIndex));
            }
            // Remove the trailing comma and close the array
            output.deleteCharAt(output.length() - 1);
            if (hasDefaultSpecified) {
                output.append("\n    }).withDefaultChoice(").append(choices[0]).append(");");
            } else {
                output.append("\n    });");
            }

            String declaration = output.toString();
            return new ParamInfo(paramName, declaration, "choice");
        } else {
            throw new RuntimeException(COULD_NOT_PARSE_MSG + input);
        }
    }

    private static ParamInfo transformBool(String input) {
        Matcher matcher = BOOL_PATTERN.get().matcher(input);

        if (matcher.matches()) {
            String paramString = matcher.group(1);
            String paramName = toCamelCase(paramString);
            String paramTitle = toTitleCase(paramString);

            int boolValue = Integer.parseInt(matcher.group(2));
            boolean boolResult = (boolValue == 1);

            String declaration = String.format(
                "BooleanParam %s = new BooleanParam(\"%s\", %s);",
                paramName,
                paramTitle,
                boolResult
            );
            return new ParamInfo(paramName, declaration, "bool");
        } else {
            throw new RuntimeException(COULD_NOT_PARSE_MSG + input);
        }
    }

    private static String toCamelCase(String input) {
        // it's important to treat hyphens as word separators
        // because they can't be in variable names
        String[] words = WORD_SEPARATOR_PATTERN.get().split(input);

        if (words.length > 0) {
            // Convert the first word to lowercase
            StringBuilder camelCase = new StringBuilder(words[0].toLowerCase(Locale.ENGLISH));

            // Convert the subsequent words, capitalizing the first letter of each
            for (int i = 1; i < words.length; i++) {
                camelCase.append(words[i].substring(0, 1).toUpperCase(Locale.ENGLISH))
                    .append(words[i].substring(1).toLowerCase(Locale.ENGLISH));
            }

            return camelCase.toString();
        } else {
            return "";
        }
    }

    private static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    private void addLine(String line, int indent) {
        sb.append("    ".repeat(indent)).append(line).append('\n');
    }

    private void addNewLine() {
        addLine("", 0);
    }

    private record ParamInfo(String name, String declaration, String type) {
    }
}
