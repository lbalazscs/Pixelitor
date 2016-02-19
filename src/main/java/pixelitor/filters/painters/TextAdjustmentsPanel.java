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

package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import pixelitor.Composition;
import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ColorSelector;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.TextLayer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import static java.awt.Color.BLACK;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.USER_ONLY_OPACITY;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

/**
 * Customization panel for the text filters and text layers
 */
public class TextAdjustmentsPanel extends AdjustPanel implements ParamAdjustmentListener, ActionListener {
    private TextLayer textLayer;

    private JTextField textTF;
    private JComboBox<String> fontFamilyChooserCB;
    private SliderSpinner fontSizeSlider;

    private JCheckBox boldCB;
    private JCheckBox italicCB;

    private ColorParam color;

    private EffectsPanel effectsPanel;
    private JComboBox<VerticalAlignment> verticalAlignmentCombo;
    private JComboBox<HorizontalAlignment> horizontalAlignmentCombo;

    private JCheckBox watermarkCB;

    private static String lastText = "";

    private Map<TextAttribute, Object> map;
    private AdvancedTextSettingsDialog advancedSettingsDialog;

    // called for image layers
    public TextAdjustmentsPanel(TextFilter textFilter, ImageLayer layer) {
        super(textFilter, layer);
        createGUI(null);

        if(!textTF.getText().isEmpty()) {
            // a "last text" was set
            paramAdjusted();
        }
    }

    // called for text layers
    public TextAdjustmentsPanel(TextLayer textLayer) {
        super(null, null);
        this.textLayer = textLayer;
        createGUI(textLayer.getSettings());

        // make sure that the text layer has a settings object
        // even if the user presses OK without making any adjustments
        paramAdjusted();
    }

    private void createGUI(TextSettings settings) {
        Box verticalBox = Box.createVerticalBox();

        verticalBox.add(createTextPanel(settings));

        verticalBox.add(createFontPanel(settings));

        AreaEffects areaEffects = null;
        boolean hasWatermark = false;
        if (settings != null) {
            areaEffects = settings.getAreaEffects();
            hasWatermark = settings.isWatermark();
        }
        effectsPanel = new EffectsPanel(this, areaEffects);
        effectsPanel.setBorder(BorderFactory.createTitledBorder("Effects"));

        verticalBox.add(effectsPanel);

        watermarkCB = new JCheckBox("Use Text for Watermarking", hasWatermark);
        watermarkCB.addActionListener(this);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(watermarkCB);

        verticalBox.add(bottomPanel);

        add(verticalBox);
    }

    private JPanel createTextPanel(TextSettings settings) {
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(textPanel);

        gbh.addLabel("Text:", 0, 0);
        createTextTF(settings);
        gbh.addLastControl(textTF);

        gbh.addLabel("Color", 0, 1);
        Color defaultColor = settings == null ? BLACK : settings.getColor();
        color = new ColorParam("Color", defaultColor, USER_ONLY_OPACITY);
        ColorSelector colorSelector = new ColorSelector(color);
        gbh.addLastControl(colorSelector);
        color.setAdjustmentListener(this);

        verticalAlignmentCombo = new JComboBox(VerticalAlignment.values());
        horizontalAlignmentCombo = new JComboBox(HorizontalAlignment.values());
        if (settings != null) {
            verticalAlignmentCombo.setSelectedItem(settings.getVerticalAlignment());
            horizontalAlignmentCombo.setSelectedItem(settings.getHorizontalAlignment());
        }

        gbh.addLabel("Vertical Alignment", 0, 2);
        verticalAlignmentCombo.addActionListener(this);
        gbh.addControl(verticalAlignmentCombo);

        gbh.addLabel("Horizontal Alignment", 2, 2);
        horizontalAlignmentCombo.addActionListener(this);
        gbh.addControl(horizontalAlignmentCombo);

        return textPanel;
    }

    private void createTextTF(TextSettings settings) {
        String defaultText;
        if (settings == null) {
            if (filter == null) { // layer mode
                defaultText = ""; // no last text remembering when creating new text layers
            } else {
                defaultText = lastText;
            }
        } else {
            defaultText = settings.getText();
        }

        textTF = new JTextField(defaultText, 20);
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

    private JPanel createFontPanel(TextSettings settings) {
        JPanel fontPanel = new JPanel();
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font"));
        fontPanel.setLayout(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(fontPanel);

        gbh.addLabel("Font Size:", 0, 0);
        int defaultFontSize = settings == null ? 100 : settings.getFont().getSize();

        RangeParam fontSizeParam = new RangeParam("", 1, defaultFontSize, 1000);
        fontSizeSlider = new SliderSpinner(fontSizeParam, NONE, AddDefaultButton.NO);
        fontSizeSlider.setSliderName("fontSize");
        fontSizeParam.setAdjustmentListener(this);
        gbh.addLastControl(fontSizeSlider);

        gbh.addLabel("Font Type:", 0, 1);
        GraphicsEnvironment localGE = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = localGE.getAvailableFontFamilyNames();
        fontFamilyChooserCB = new JComboBox(availableFonts);
        if (settings != null) {
            // it is important to use Font.getName(), and not Font.getFontName(),
            // otherwise it might not be in the combo box
            String fontName = settings.getFont().getName();
            fontFamilyChooserCB.setSelectedItem(fontName);
        }
        fontFamilyChooserCB.addActionListener(this);
        gbh.addLastControl(fontFamilyChooserCB);

        boolean defaultBold = false;
        boolean defaultItalic = false;
        if (settings != null) {
            Font font = settings.getFont();
            defaultBold = font.isBold();
            defaultItalic = font.isItalic();
            if (font.hasLayoutAttributes()) {
                Map<TextAttribute, ?> attributes = font.getAttributes();
                this.map = (Map<TextAttribute, Object>) attributes;
            }
        }

        gbh.addLabel("Bold:", 0, 2);
        boldCB = createAndAddEmphasisCheckBox("boldCB", gbh, defaultBold);

        gbh.addLabel("   Italic:", 2, 2);
        italicCB = createAndAddEmphasisCheckBox("italicCB", gbh, defaultItalic);

        JButton showAdvancedSettingsButton = new JButton("Advanced...");
        showAdvancedSettingsButton.addActionListener(e -> {
            if (advancedSettingsDialog == null) {
                Dialog parentDialog = (Dialog) SwingUtilities.getWindowAncestor(this);
                advancedSettingsDialog = new AdvancedTextSettingsDialog(parentDialog, this, map);
            }
            advancedSettingsDialog.setVisible(true);
        });

        gbh.addLabel("      ", 4, 2);
        gbh.addControl(showAdvancedSettingsButton);

        return fontPanel;
    }

    private JCheckBox createAndAddEmphasisCheckBox(String name, GridBagHelper gbh, boolean selected) {
        JCheckBox cb = new JCheckBox("", selected);
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

        // It is important to create here a new Map, because
        // the old one stores old values in TextAttribute.SIZE
        // and other fields which would override the current ones.
        // TODO there has to be a simpler way, for example overwriting
        // however, it is not trivial, there is no single "style" TextAttribute
        Map<TextAttribute, Object> oldMap = map;
        map = new HashMap<>();

        if (advancedSettingsDialog != null) {
            advancedSettingsDialog.updateMap(map);
        } else if (oldMap != null) {
            // no dialog, copy manually the advanced settings
            TextAttribute[] advancedSettings = {
                    TextAttribute.STRIKETHROUGH,
                    TextAttribute.UNDERLINE,
                    TextAttribute.KERNING,
                    TextAttribute.LIGATURES,
                    TextAttribute.TRACKING
            };

            for (TextAttribute setting : advancedSettings) {
                map.put(setting, oldMap.get(setting));
            }
        }

        return font.deriveFont(map);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        paramAdjusted();
    }

    @Override
    public void paramAdjusted() {
        TextFilter textFilter = (TextFilter) filter;
        String text = textTF.getText();
        lastText = text;

        AreaEffects areaEffects = null;
        if (effectsPanel != null) {
            effectsPanel.updateEffectsFromGUI();
            areaEffects = effectsPanel.getEffects();
        }

        Font selectedFont = getSelectedFont();

        TextSettings settings = new TextSettings(
                text, selectedFont, color.getColor(), areaEffects,
                (HorizontalAlignment) horizontalAlignmentCombo.getSelectedItem(),
                (VerticalAlignment) verticalAlignmentCombo.getSelectedItem(),
                watermarkCB.isSelected());

        if (textFilter != null) { // filter mode
            textFilter.setSettings(settings);
            super.executeFilterPreview();
        } else {
            assert textLayer != null;
            textLayer.setSettings(settings);
            textLayer.getComp().imageChanged(Composition.ImageChangeActions.FULL);
        }
    }
}
