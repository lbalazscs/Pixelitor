/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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
import org.jdesktop.swingx.painter.AbstractLayoutPainter;
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
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.Hashtable;
import java.util.Map;

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

    private ColorParam colorParam = new ColorParam("Color", Color.BLACK, true, false);
    private ColorSelector colorSelector;

    private EffectsPanel effectsPanel;
    private JComboBox<AbstractLayoutPainter.VerticalAlignment> verticalAlignmentCombo;
    private JComboBox<AbstractLayoutPainter.HorizontalAlignment> horizontalAlignmentCombo;

    private final JCheckBox watermarkCB;

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

        GridBagHelper gridBagHelper = new GridBagHelper(textPanel);

        gridBagHelper.addLabel("Text:", 0, 0);
        textTF = new JTextField(20);

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
        gridBagHelper.addLastControl(textTF);

        gridBagHelper.addLabel("Color", 0, 1);
        colorSelector = new ColorSelector(colorParam);
        gridBagHelper.addLastControl(colorSelector);
        colorParam.setAdjustmentListener(this);

        gridBagHelper.addLabel("Vertical Alignment", 0, 2);
        verticalAlignmentCombo = new JComboBox(AbstractLayoutPainter.VerticalAlignment.values());
        verticalAlignmentCombo.addActionListener(this);
        gridBagHelper.addControl(verticalAlignmentCombo);

        gridBagHelper.addLabel("Horizontal Alignment", 2, 2);
        horizontalAlignmentCombo = new JComboBox(AbstractLayoutPainter.HorizontalAlignment.values());
        horizontalAlignmentCombo.addActionListener(this);
        gridBagHelper.addControl(horizontalAlignmentCombo);


        return textPanel;
    }

    private JPanel createFontPanel() {
        JPanel fontPanel = new JPanel();
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font"));
        fontPanel.setLayout(new GridBagLayout());

        GridBagHelper gridBagHelper = new GridBagHelper(fontPanel);

        gridBagHelper.addLabel("Font Size:", 0, 0);
        RangeParam fontSizeParam = new RangeParam("", 1, 1000, 100);
        fontSizeSlider = new SliderSpinner(fontSizeParam, false, SliderSpinner.TextPosition.NONE);
        fontSizeParam.setAdjustmentListener(this);
        gridBagHelper.addLastControl(fontSizeSlider);

        gridBagHelper.addLabel("Font Type:", 0, 1);
        GraphicsEnvironment localGE = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = localGE.getAvailableFontFamilyNames();
        fontFamilyChooserCB = new JComboBox(availableFonts);
        fontFamilyChooserCB.addActionListener(this);
        gridBagHelper.addLastControl(fontFamilyChooserCB);

        gridBagHelper.addLabel("Bold:", 0, 2);
        boldCB = new JCheckBox();
        boldCB.addActionListener(this);
        gridBagHelper.addControl(boldCB);

        gridBagHelper.addLabel("Italic:", 2, 2);
        italicCB = new JCheckBox();
        italicCB.addActionListener(this);
        gridBagHelper.addControl(italicCB);

        gridBagHelper.addLabel("Underline:", 4, 2);
        underlineCB = new JCheckBox();
        underlineCB.addActionListener(this);
        gridBagHelper.addControl(underlineCB);

        gridBagHelper.addLabel("Strikethrough:", 6, 2);
        strikeThroughCB = new JCheckBox();
        strikeThroughCB.addActionListener(this);
        gridBagHelper.addControl(strikeThroughCB);

//        gridBagHelper.addLabel("Kerning:", 8, 2);
//        kerningCB = new JCheckBox();
//        kerningCB.addActionListener(this);
//        gridBagHelper.addControl(kerningCB);

        return fontPanel;
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
        textFilter.setText(textTF.getText());
        textFilter.setFont(getSelectedFont());

        if (effectsPanel != null) {
            effectsPanel.updateEffectsFromGUI();
            AreaEffect[] areaEffects = effectsPanel.getEffectsAsArray();
            textFilter.setAreaEffects(areaEffects);
        }
        textFilter.setWatermark(watermarkCB.isSelected());

        textFilter.setVerticalAlignment((AbstractLayoutPainter.VerticalAlignment) verticalAlignmentCombo.getSelectedItem());
        textFilter.setHorizontalAlignment((AbstractLayoutPainter.HorizontalAlignment) horizontalAlignmentCombo.getSelectedItem());
        textFilter.setColor(colorParam.getColor());

        super.executeFilterPreview();
    }

}
