/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.utils;

import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.OKCancelDialog;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a skeleton of source code for a filter
 * (used only for development)
 */
public class FilterCreator extends JPanel {
    private final JTextField nameTextField;
    private final JCheckBox guiCB;
    private final JCheckBox parametrizedGuiCB;
    private final JCheckBox copySrcCB;

    private final ParamPanel[] paramPanels = new ParamPanel[10];
    private final JCheckBox pixelLoopCB;
    private final JCheckBox proxyCB;
    @SuppressWarnings("FieldMayBeFinal")
    private JTextField proxyNameTF;
    private final JCheckBox edgeActionCB;
    private final JCheckBox interpolationCB;
    private final JCheckBox centerSelectorCB;
    private final JCheckBox colorCB;
    private final JCheckBox gradientCB;
    private final JCheckBox angleParamCB;

    private FilterCreator() {
        setLayout(new GridBagLayout());

        GridBagHelper gridBagHelper = new GridBagHelper(this);

        gridBagHelper.addLabel("Name:", 0, 0);
        nameTextField = new JTextField(20);
        gridBagHelper.addLastControl(nameTextField);

        gridBagHelper.addLabel("GUI:", 0, 1);
        guiCB = new JCheckBox();
        guiCB.setSelected(true);
        gridBagHelper.addControl(guiCB);

        gridBagHelper.addLabel("Parametrized GUI:", 2, 1);
        parametrizedGuiCB = new JCheckBox();
        parametrizedGuiCB.setSelected(true);
        parametrizedGuiCB.addChangeListener(e -> {
            if (parametrizedGuiCB.isSelected()) {
                guiCB.setSelected(true);
            }
        });
        gridBagHelper.addControl(parametrizedGuiCB);

        gridBagHelper.addLabel("Copy Src -> Dest:", 4, 1);
        copySrcCB = new JCheckBox();
        gridBagHelper.addControl(copySrcCB);

        gridBagHelper.addLabel("Angle Param:", 6, 1);
        angleParamCB = new JCheckBox();
        gridBagHelper.addControl(angleParamCB);

        gridBagHelper.addLabel("Pixel Loop:", 0, 2);
        pixelLoopCB = new JCheckBox();
        gridBagHelper.addControl(pixelLoopCB);

        gridBagHelper.addLabel("Proxy Filter:", 2, 2);
        proxyCB = new JCheckBox();
//        proxyCB.setSelected(true);
        proxyCB.addChangeListener(e -> proxyNameTF.setEnabled(proxyCB.isSelected()));
        gridBagHelper.addControl(proxyCB);

        gridBagHelper.addLabel("Proxy Name:", 4, 2);
        proxyNameTF = new JTextField(10);
        proxyNameTF.setEnabled(proxyCB.isSelected());
        gridBagHelper.addControl(proxyNameTF);

        gridBagHelper.addLabel("Center Selector:", 0, 3);
        centerSelectorCB = new JCheckBox();
        gridBagHelper.addControl(centerSelectorCB);

        gridBagHelper.addLabel("Edge Action:", 2, 3);
        edgeActionCB = new JCheckBox();
        gridBagHelper.addControl(edgeActionCB);

        gridBagHelper.addLabel("Interpolation:", 4, 3);
        interpolationCB = new JCheckBox();
        gridBagHelper.addControl(interpolationCB);

        gridBagHelper.addLabel("Color:", 6, 3);
        colorCB = new JCheckBox();
        gridBagHelper.addControl(colorCB);

        gridBagHelper.addLabel("Gradient:", 8, 3);
        gradientCB = new JCheckBox();
        gridBagHelper.addControl(gradientCB);

        for (int i = 0; i < paramPanels.length; i++) {
            gridBagHelper.addLabel("Param " + (i + 1) + ':', 0, i + 4);
            ParamPanel pp = new ParamPanel();
            paramPanels[i] = pp;
            gridBagHelper.addLastControl(pp);
        }
    }

    private ParameterInfo[] getParameterInfoArray() {
        List<ParameterInfo> piList = new ArrayList<>();
        for (ParamPanel panel : paramPanels) {
            ParameterInfo pi = panel.getParameterInfo();
            if (pi != null) {
                piList.add(pi);
            }
        }
        return piList.toArray(new ParameterInfo[piList.size()]);
    }

    public static void showInDialog(Frame owner) {
        FilterCreator filterCreator = new FilterCreator();
        OKCancelDialog d = new OKCancelDialog(filterCreator, owner, "Filter Creator", "Show Source", "Close") {
            @Override
            protected void dialogAccepted() {
//                close();
                String s = filterCreator.createFilterSource();
                JTextArea ta = new JTextArea(s);
                JScrollPane sp = new JScrollPane(ta);
                sp.setPreferredSize(new Dimension(sp.getPreferredSize().width + 50, 500));
                GUIUtils.showClipboardTextDialog(sp, "Source", s);
            }
        };
        d.setVisible(true);
    }

    public static class ParamPanel extends JPanel {
        private final JTextField nameTextField;
        private final JTextField minTextField;
        private final JTextField maxTextField;
        private final JTextField defaultTextField;

        private ParamPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            add(new JLabel("Name:"));
            nameTextField = new JTextField(20);
            add(nameTextField);
            add(new JLabel("Min:"));
            minTextField = new JTextField("0", 5);
            add(minTextField);
            add(new JLabel("Max:"));
            maxTextField = new JTextField("100", 5);
            add(maxTextField);
            add(new JLabel("Default:"));
            defaultTextField = new JTextField("10", 5);
            add(defaultTextField);
        }

        private ParameterInfo getParameterInfo() {
            String name = nameTextField.getText().trim();
            if (!name.isEmpty()) {
                int min = Integer.parseInt(minTextField.getText());
                int max = Integer.parseInt(maxTextField.getText());
                int defaultValue = Integer.parseInt(defaultTextField.getText());
                return new ParameterInfo(name, min, max, defaultValue);
            } else {
                return null;
            }
        }
    }

    private String createFilterSource() {
        boolean parametrizedGui = parametrizedGuiCB.isSelected();
        boolean gui = guiCB.isSelected();
        boolean copySrc = copySrcCB.isSelected();
        String name = nameTextField.getText();
        boolean pixelLoop = pixelLoopCB.isSelected();
        boolean proxy = proxyCB.isSelected();
        String proxyName = proxyNameTF.getText();

        boolean angleParam = angleParamCB.isSelected();
        boolean center = centerSelectorCB.isSelected();
        boolean edge = edgeActionCB.isSelected();
        boolean color = colorCB.isSelected();
        boolean gradient = gradientCB.isSelected();
        boolean interpolation = interpolationCB.isSelected();

        ParameterInfo[] params = getParameterInfoArray();

        return getCode(new FilterDescription(parametrizedGui, gui, copySrc, name, pixelLoop, proxy, proxyName, angleParam, center, edge, color, gradient, interpolation, params));
    }

    private static String getCode(FilterDescription desc) {


        String retVal = addImports(desc);

        retVal += addSuperClass(desc);

        retVal += String.format("    public static final String NAME = \"%s\";\n\n", desc.getName());

        if (desc.isGui() && desc.isParametrizedGui()) {
            retVal += addParamsDeclaration(desc);
        }

        if (desc.isProxy()) {
            retVal += "\n    private " + desc.getProxyName() + " filter;\n";
        }

        retVal += addConstructor(desc);
        retVal += addTransform(desc);
        retVal += addGetAdjustPanel(desc);

        return retVal;
    }

    private static String addGetAdjustPanel(FilterDescription desc) {
        String retVal = "";
        if (desc.isGui() && (!desc.isParametrizedGui())) {
            retVal += "\n    @Override\n";
            retVal += "    public AdjustPanel createAdjustPanel() {\n";
            retVal += "        return new " + desc.getClassName() + "Adjustments(this);\n";
            retVal += "    }\n";
        }

        retVal += '}';
        return retVal;
    }

    private static String addTransform(FilterDescription desc) {
        String retVal = "";
        retVal += "\n    @Override\n";
        retVal += "    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {\n";

        if (desc.hasPixelLoop()) {
            retVal += "        int[] srcData = ImageUtils.getPixelsAsArray(src);\n";
            retVal += "        int[] destData = ImageUtils.getPixelsAsArray(dest);\n";
        }

        if (desc.isProxy()) {
            retVal += "       if(filter == null) {\n";
            retVal += "           filter = new " + desc.getProxyName() + "(NAME);\n";
            retVal += "       }\n";

            retVal += '\n';

            if (desc.hasCenter()) {
                retVal += "        filter.setCenterX(center.getRelativeX());\n";
                retVal += "        filter.setCenterY(center.getRelativeY());\n";
            }
            if (desc.hasEdgeAction()) {
                retVal += "        filter.setEdgeAction(edgeAction.getValue());\n";
            }
            if (desc.hasInterpolation()) {
                retVal += "        filter.setInterpolation(interpolation.getValue());\n";
            }

            retVal += '\n';

            if (desc.hasAngleParam()) {
                retVal += "        filter.setAngle(angle.getValueInRadians());\n";
            }

            retVal += "        dest = filter.filter(src, dest);\n";
        }

        retVal += "        return dest;\n";
        retVal += "    }\n";
        return retVal;
    }

    private static String addSuperClass(FilterDescription desc) {
        String retVal = "";
        String superClassName1;
        if (desc.isGui()) {
            if (desc.isParametrizedGui()) {
                superClassName1 = "FilterWithParametrizedGUI";
            } else {
                superClassName1 = "FilterWithGUI";
            }
        } else {
            superClassName1 = "Filter";
        }
        String superClassName = superClassName1;

        retVal += "public class " + desc.getClassName();
        retVal += " extends " + superClassName;
        retVal += " {\n";
        return retVal;
    }

    private static String addImports(FilterDescription desc) {
        String retVal = "";
        if (desc.hasPixelLoop()) {
            retVal += "import pixelitor.utils.ImageUtils;\n";
        }
        retVal += "import pixelitor.filters.gui.ParamSet;\n";
        retVal += "import pixelitor.filters.gui.RangeParam;\n";
        if (desc.isParametrizedGui()) {
            retVal += "import pixelitor.filters.FilterWithParametrizedGUI;\n";
        }

        retVal += "\nimport java.awt.image.BufferedImage;\n";
        retVal += "\n/**\n";

        if (desc.isProxy()) {
            retVal += " * " + desc.getName() + " filter based on " + desc.getProxyName() + '\n';
        } else {
            retVal += " * " + desc.getName() + " filter\n";
        }

        retVal += " */\n";
        return retVal;
    }

    private static String addConstructor(FilterDescription desc) {
        String retVal = "";
        retVal += "\n    public " + desc.getClassName() + "() {\n";

        if (desc.isParametrizedGui()) {
            retVal += "        super(ShowOriginal.YES);\n";
        }

        if (desc.copySrc()) {
            retVal += "        copySrcToDstBeforeRunning = true;\n";
        }

        if (desc.isGui() && desc.isParametrizedGui()) {
            retVal += addParamSetToConstructor(desc);
        }
        retVal += "    }\n";
        return retVal;
    }

    private static String addParamSetToConstructor(FilterDescription desc) {
        String retVal = "";
        if (desc.getParams().length == 1) {
            retVal += "        setParamSet(new ParamSet(" + desc.getParams()[0].getVariableName() + "));\n";
        } else {
            retVal += "        setParamSet(new ParamSet(\n";
            for (int i = 0; i < desc.getParams().length; i++) {
                ParameterInfo param = desc.getParams()[i];
                String paramName = param.getVariableName();
                retVal += "            " + paramName;
                if (i < desc.getParams().length - 1 || desc.hasEdgeAction() || desc.hasInterpolation()) {
                    retVal += ',';
                }
                retVal += '\n';
            }
            if (desc.hasEdgeAction()) {
                retVal += "            edgeAction,\n";
            }
            if (desc.hasInterpolation()) {
                retVal += "            interpolation\n";
            }

            retVal += "        ));\n";
        }
        return retVal;
    }

    private static String addParamsDeclaration(FilterDescription desc) {
        String retVal = "";
        for (ParameterInfo param : desc.getParams()) {

            String paramLine = String.format(
                    "    private final RangeParam %s = new RangeParam(\"%s\", %d, %d, %d);",
                    param.getVariableName(), param.getName(),
                    param.getMin(), param.getDefaultValue(), param.getMax());

            retVal += paramLine;
            retVal += '\n';
        }

        if (desc.hasCenter()) {
            retVal += "    private final ImagePositionParam center = new ImagePositionParam(\"Center\");\n";
        }
        if (desc.hasAngleParam()) {
            retVal += "    private final AngleParam angle = new AngleParam(\"Angle\", 0);\n";
        }
        if (desc.hasEdgeAction()) {
            retVal += "    private final IntChoiceParam edgeAction =  IntChoiceParam.getEdgeActionChoices();\n";
        }
        if (desc.hasInterpolation()) {
            retVal += "    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();\n";
        }
        if (desc.hasColor()) {
            retVal += "    private final ColorParam color = new ColorParam(\"Color:\", Color.WHITE, false, false);\n";
        }
        if (desc.hasGradient()) {
            retVal += "    private final float[] defaultThumbPositions = new float[]{0f, 1f};\n";
            retVal += "    private final Color[] defaultValues = new Color[]{Color.BLACK, Color.WHITE};\n";
            retVal += "    private final GradientParam gradient = new GradientParam(\"Colors:\", defaultThumbPositions, defaultValues);\n";
        }
        return retVal;
    }

    private static class FilterDescription {
        private final boolean parametrizedGui;
        private final boolean gui;
        private final boolean copySrc;
        private final String name;
        private final boolean pixelLoop;
        private final boolean proxy;
        private final String proxyName;
        private final boolean angleParam;
        private final boolean center;
        private final boolean edge;
        private final boolean color;
        private final boolean gradient;
        private final boolean interpolation;
        private final ParameterInfo[] params;

        private FilterDescription(boolean parametrizedGui, boolean gui, boolean copySrc, String name, boolean pixelLoop, boolean proxy, String proxyName, boolean angleParam, boolean center, boolean edge, boolean color, boolean gradient, boolean interpolation, ParameterInfo[] params) {
            this.parametrizedGui = parametrizedGui;
            this.gui = gui;
            this.copySrc = copySrc;
            this.name = name;
            this.pixelLoop = pixelLoop;
            this.proxy = proxy;
            this.proxyName = proxyName;
            this.angleParam = angleParam;
            this.center = center;
            this.edge = edge;
            this.color = color;
            this.gradient = gradient;
            this.interpolation = interpolation;
            this.params = params;
        }

        private boolean isParametrizedGui() {
            return parametrizedGui;
        }

        private boolean isGui() {
            return gui;
        }

        private boolean copySrc() {
            return copySrc;
        }

        public String getName() {
            return name;
        }

        private boolean hasPixelLoop() {
            return pixelLoop;
        }

        private boolean isProxy() {
            return proxy;
        }

        private String getProxyName() {
            return proxyName;
        }

        private boolean hasAngleParam() {
            return angleParam;
        }

        private boolean hasCenter() {
            return center;
        }

        private boolean hasEdgeAction() {
            return edge;
        }

        private boolean hasColor() {
            return color;
        }

        private boolean hasGradient() {
            return gradient;
        }

        private boolean hasInterpolation() {
            return interpolation;
        }

        private ParameterInfo[] getParams() {
            return params;
        }

        private String getClassName() {
            return getName().replaceAll(" ", "");
        }
    }

    private static class ParameterInfo {
        final String name;
        final String variableName;
        final int min;
        final int max;
        final int defaultValue;

        public ParameterInfo(String name, int min, int max, int defaultValue) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;

            String tmp = name.replaceAll(" ", "");
            this.variableName = tmp.substring(0, 1).toLowerCase() + tmp.substring(1);
        }

        private String getName() {
            return name;
        }

        private String getVariableName() {
            return variableName;
        }

        private int getMin() {
            return min;
        }

        private int getMax() {
            return max;
        }

        private int getDefaultValue() {
            return defaultValue;
        }
    }
}
