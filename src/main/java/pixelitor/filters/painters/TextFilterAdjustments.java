/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.filters.painters;

import com.bric.util.JVM;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import org.jdesktop.swingx.painter.effects.AreaEffect;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ColorSelector;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.Hashtable;
import java.util.Map;

import static java.awt.Color.BLACK;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.USER_ONLY_OPACITY;
import static pixelitor.utils.SliderSpinner.TextPosition.NONE;

/**
 * Customization Panel for the Centered Text
 */
public class TextFilterAdjustments extends AdjustPanel implements ParamAdjustmentListener, ActionListener {
    private JTextField textTF;
    private JComboBox<String> fontFamilyChooserCB;
    private SliderSpinner fontSizeSlider;

    private JCheckBox boldCB;
    private JCheckBox italicCB;
    private JCheckBox underlineCB;
    private JCheckBox strikeThroughCB;
//    private JCheckBox kerningCB;

    private final ColorParam color = new ColorParam("Color", BLACK, USER_ONLY_OPACITY);

    private EffectsPanel effectsPanel;
    private JComboBox<VerticalAlignment> verticalAlignmentCombo;
    private JComboBox<HorizontalAlignment> horizontalAlignmentCombo;

    private final JCheckBox watermarkCB;

    private static String lastText = "";

    public TextFilterAdjustments(TextFilter textFilter) {
        super(textFilter);

        Box verticalBox = Box.createVerticalBox();

        verticalBox.add(createTextPanel());

        verticalBox.add(createFontPanel());

        if (!JVM.isLinux) { // TODO
            effectsPanel = new EffectsPanel(this);
            effectsPanel.setBorder(BorderFactory.createTitledBorder("Effects"));

            verticalBox.add(effectsPanel);
        }

        watermarkCB = new JCheckBox("Use Text for Watermarking");
        watermarkCB.addActionListener(this);

        verticalBox.add(watermarkCB);


        add(verticalBox);
    }

    private JPanel createTextPanel() {
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(textPanel);

        gbh.addLabel("Text:", 0, 0);
        createTextTF();
        gbh.addLastControl(textTF);

        gbh.addLabel("Color", 0, 1);
        ColorSelector colorSelector = new ColorSelector(color);
        gbh.addLastControl(colorSelector);
        color.setAdjustmentListener(this);

        gbh.addLabel("Vertical Alignment", 0, 2);
        verticalAlignmentCombo = new JComboBox(VerticalAlignment.values());
        verticalAlignmentCombo.addActionListener(this);
        gbh.addControl(verticalAlignmentCombo);

        gbh.addLabel("Horizontal Alignment", 2, 2);
        horizontalAlignmentCombo = new JComboBox(HorizontalAlignment.values());
        horizontalAlignmentCombo.addActionListener(this);
        gbh.addControl(horizontalAlignmentCombo);

        return textPanel;
    }

    private void createTextTF() {
        textTF = new JTextField(lastText, 20);
        textTF.setName("textTF");

        textTF.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                paramAdjusted();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                paramAdjusted();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                paramAdjusted();
            }
        });
    }

    private JPanel createFontPanel() {
        JPanel fontPanel = new JPanel();
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font"));
        fontPanel.setLayout(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(fontPanel);

        gbh.addLabel("Font Size:", 0, 0);
        RangeParam fontSizeParam = new RangeParam("", 1, 1000, 100);
        fontSizeSlider = new SliderSpinner(fontSizeParam, NONE, false);
        fontSizeSlider.setSliderName("fontSize");
        fontSizeParam.setAdjustmentListener(this);
        gbh.addLastControl(fontSizeSlider);

        gbh.addLabel("Font Type:", 0, 1);
        GraphicsEnvironment localGE = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = localGE.getAvailableFontFamilyNames();
        fontFamilyChooserCB = new JComboBox(availableFonts);
        fontFamilyChooserCB.addActionListener(this);
        gbh.addLastControl(fontFamilyChooserCB);

        gbh.addLabel("Bold:", 0, 2);
        boldCB = createAndAddEmphasisCheckBox("boldCB", gbh);

        gbh.addLabel("Italic:", 2, 2);
        italicCB = createAndAddEmphasisCheckBox("italicCB", gbh);

        gbh.addLabel("Underline:", 4, 2);
        underlineCB = createAndAddEmphasisCheckBox("underlineCB", gbh);

        gbh.addLabel("Strikethrough:", 6, 2);
        strikeThroughCB = createAndAddEmphasisCheckBox("strikeThroughCB", gbh);

//        gbHelper.addLabel("Kerning:", 8, 2);
//        kerningCB = createAndAddEmphasisCheckBox("kerningCB");

        return fontPanel;
    }

    private JCheckBox createAndAddEmphasisCheckBox(String name, GridBagHelper gbh) {
        JCheckBox cb = new JCheckBox();
        cb.setName(name);
        cb.addActionListener(this);
        gbh.addControl(cb);
        return cb;
    }


    private Font getSelectedFont() {
        String fontFamily = (String) fontFamilyChooserCB.getSelectedItem();
        int style = Font.PLAIN;
        if (boldCB.isSelected()) {
            style |= Font.BOLD;
        }
        if (italicCB.isSelected()) {
            style |= Font.ITALIC;
        }
        int size = fontSizeSlider.getCurrentValue();
        Font font = new Font(fontFamily, style, size);

        Map<TextAttribute, Object> map =
                new Hashtable<>();
        if (underlineCB.isSelected()) {
            map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        }
        if (strikeThroughCB.isSelected()) {
            map.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        }
//        if(kerningCB.isSelected()) {
//            map.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
//        }

        return font.deriveFont(map);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        paramAdjusted();
    }

    @Override
    public void paramAdjusted() {
        TextFilter textFilter = (TextFilter) op;
        String text = textTF.getText();
        lastText = text;
        textFilter.setText(text);
        textFilter.setFont(getSelectedFont());

        if (effectsPanel != null) {
            effectsPanel.updateEffectsFromGUI();
            AreaEffect[] areaEffects = effectsPanel.getEffectsAsArray();
            textFilter.setAreaEffects(areaEffects);
        }
        textFilter.setWatermark(watermarkCB.isSelected());

        textFilter.setVerticalAlignment((VerticalAlignment) verticalAlignmentCombo.getSelectedItem());
        textFilter.setHorizontalAlignment((HorizontalAlignment) horizontalAlignmentCombo.getSelectedItem());
        textFilter.setColor(color.getColor());

        super.executeFilterPreview();
    }

}
