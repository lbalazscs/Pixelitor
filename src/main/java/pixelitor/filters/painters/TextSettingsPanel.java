/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import pixelitor.filters.gui.*;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Drawable;
import pixelitor.layers.TextLayer;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.USER_ONLY_TRANSPARENCY;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;

/**
 * Customization panel for the text filter and for text layers
 */
public class TextSettingsPanel extends FilterGUI
    implements ParamAdjustmentListener, ActionListener, Consumer<TextSettings> {
    private TextLayer textLayer;
    private FontInfo fontInfo;

    private JTextField textTF;
    private JComboBox<String> fontFamilyChooserCB;
    private SliderSpinner fontSizeSlider;
    private AngleParam rotationParam;
    private JCheckBox boldCB;
    private JCheckBox italicCB;
    private ColorParam color;
    private EffectsPanel effectsPanel;
    private JComboBox<VerticalAlignment> vAlignmentCB;
    private JComboBox<HorizontalAlignment> hAlignmentCB;
    private JCheckBox watermarkCB;

    private JDialog advancedSettingsDialog;
    private AdvancedTextSettingsPanel advancedSettingsPanel;

    private boolean ignoreGUIChanges = false;

    /**
     * Used for the text filter on images
     */
    public TextSettingsPanel(TextFilter textFilter, Drawable dr) {
        super(textFilter, dr);
        TextSettings settings = textFilter.getSettings();
        init(settings);

        if (!textTF.getText().isEmpty()) {
            // a "last text" was set
            paramAdjusted();
        }
    }

    /**
     * Used for text layers
     */
    public TextSettingsPanel(TextLayer textLayer) {
        super(null, null);
        this.textLayer = textLayer;
        TextSettings settings = textLayer.getSettings();
        init(settings);

        if (textTF.getText().equals(TextSettings.DEFAULT_TEXT)) {
            textTF.selectAll();
        }
    }

    private void init(TextSettings settings) {
        settings.setGuiUpdater(this);
        createGUI(settings);
    }

    private void createGUI(TextSettings settings) {
        assert settings != null;
        setLayout(new VerticalLayout());

        add(createTextPanel(settings));
        add(createFontPanel(settings));

        createEffectsPanel(settings);
        add(effectsPanel);

        add(createBottomPanel(settings));
    }

    private JPanel createTextPanel(TextSettings settings) {
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());

        var gbh = new GridBagHelper(textPanel);

        gbh.addLabel("Text:", 0, 0);
        createTextTF(settings);
        gbh.addLastControl(textTF);

        gbh.addLabel("Color:", 0, 1);
        color = new ColorParam("Color", settings.getColor(), USER_ONLY_TRANSPARENCY);
        gbh.addControl(color.createGUI());
        color.setAdjustmentListener(this);

        gbh.addLabel("Rotation:", 2, 1);
        rotationParam = new AngleParam("", settings.getRotation());
        rotationParam.setAdjustmentListener(this);
        gbh.addControl(rotationParam.createGUI());

        hAlignmentCB = new JComboBox<>(HorizontalAlignment.values());
        hAlignmentCB.setName("hAlignmentCB");
        hAlignmentCB.setSelectedItem(settings.getHorizontalAlignment());
        hAlignmentCB.addActionListener(this);
        gbh.addLabel("Horizontal Alignment:", 0, 2);
        gbh.addControl(hAlignmentCB);

        vAlignmentCB = new JComboBox<>(VerticalAlignment.values());
        vAlignmentCB.setName("vAlignmentCB");
        vAlignmentCB.setSelectedItem(settings.getVerticalAlignment());
        vAlignmentCB.addActionListener(this);
        gbh.addLabel("Vertical Alignment:", 0, 3);
        gbh.addControl(vAlignmentCB);

        return textPanel;
    }

    private void createTextTF(TextSettings settings) {
        textTF = new JTextField(settings.getText(), 20);
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
        fontPanel.setBorder(createTitledBorder("Font"));
        fontPanel.setLayout(new GridBagLayout());

        var gbh = new GridBagHelper(fontPanel);

        int maxFontSize = 1000;
        Font font = settings.getFont();
        int defaultFontSize = font.getSize();
        if (maxFontSize < defaultFontSize) {
            // can get here if the canvas is downsized
            // after the text layer creation
            maxFontSize = defaultFontSize;
        }

        gbh.addLabel("Font Size:", 0, 0);

        RangeParam fontSizeParam = new RangeParam("", 1, defaultFontSize, maxFontSize);
        fontSizeSlider = SliderSpinner.from(fontSizeParam);
        fontSizeSlider.setName("fontSize");
        fontSizeParam.setAdjustmentListener(this);
        gbh.addLastControl(fontSizeSlider);

        gbh.addLabel("Font Type:", 0, 1);
        String[] availableFonts = Utils.getAvailableFontNames();
        fontFamilyChooserCB = new JComboBox<>(availableFonts);

        // it is important to use Font.getName(), and not Font.getFontName(),
        // otherwise it might not be in the combo box
        String fontName = font.getName();
        fontFamilyChooserCB.setSelectedItem(fontName);

        fontFamilyChooserCB.addActionListener(this);
        gbh.addLastControl(fontFamilyChooserCB);

        boolean defaultBold = font.isBold();
        boolean defaultItalic = font.isItalic();
        fontInfo = new FontInfo(font);

        gbh.addLabel("Bold:", 0, 2);
        boldCB = createCheckBox("boldCB", gbh, defaultBold);

        gbh.addLabel("   Italic:", 2, 2);
        italicCB = createCheckBox("italicCB", gbh, defaultItalic);

        JButton showAdvancedSettingsButton = new JButton("Advanced...");
        showAdvancedSettingsButton.addActionListener(e -> onAdvancedSettingsClick());

        gbh.addLabel("      ", 4, 2);
        gbh.addControl(showAdvancedSettingsButton);

        return fontPanel;
    }

    private void onAdvancedSettingsClick() {
        if (advancedSettingsDialog == null) {
            advancedSettingsPanel = new AdvancedTextSettingsPanel(
                this, fontInfo);
            JDialog owner = GUIUtils.getDialogAncestor(this);
            advancedSettingsDialog = new DialogBuilder()
                .owner(owner)
                .content(advancedSettingsPanel)
                .title("Advanced Text Settings")
                .noCancelButton()
                .okText(CLOSE_DIALOG)
                .build();
        }
        GUIUtils.showDialog(advancedSettingsDialog);
    }

    private JCheckBox createCheckBox(String name, GridBagHelper gbh, boolean selected) {
        JCheckBox cb = new JCheckBox("", selected);
        cb.setName(name);
        cb.addActionListener(this);
        gbh.addControl(cb);
        return cb;
    }

    private Font getSelectedFont() {
        String fontFamily = (String) fontFamilyChooserCB.getSelectedItem();
        int size = fontSizeSlider.getCurrentValue();
        boolean bold = boldCB.isSelected();
        boolean italic = italicCB.isSelected();
        fontInfo.updateBasic(fontFamily, size, bold, italic);

        if (advancedSettingsDialog != null) {
            advancedSettingsPanel.saveStateTo(fontInfo);
        }

        return fontInfo.createFont();
    }

    private void createEffectsPanel(TextSettings settings) {
        AreaEffects effects = settings.getEffects();
        effectsPanel = new EffectsPanel(effects);
        effectsPanel.setAdjustmentListener(this);
        effectsPanel.setBorder(createTitledBorder("Effects"));
    }

    private JPanel createBottomPanel(TextSettings settings) {
        watermarkCB = new JCheckBox("Watermarking", settings.hasWatermark());

        watermarkCB.addActionListener(this);

        JPanel p = new JPanel(new FlowLayout(LEFT, 5, 5));
        p.add(watermarkCB);
        return p;
    }

    // ensure that only one of them is selected and that only one update is triggered
    private void specialCBAction(JCheckBox other) {
        ignoreGUIChanges = true;
        if (other.isSelected()) {
            other.setSelected(false);
        }
        ignoreGUIChanges = false;
        paramAdjusted();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        paramAdjusted();
    }

    @Override
    public void paramAdjusted() {
        if (ignoreGUIChanges) {
            return;
        }

        String text = textTF.getText();

        AreaEffects effects = null;
        double textRotationAngle = rotationParam.getValueInRadians();
        if (effectsPanel != null) {
            effects = effectsPanel.getEffects();
        }

        Font selectedFont = getSelectedFont();

        var settings = new TextSettings(
            text, selectedFont, color.getColor(), effects,
            (HorizontalAlignment) hAlignmentCB.getSelectedItem(),
            (VerticalAlignment) vAlignmentCB.getSelectedItem(),
            watermarkCB.isSelected(), textRotationAngle, this);

        updateApp(settings);
    }

    private void updateApp(TextSettings settings) {
        if (isInFilterMode()) {
            ((TextFilter) filter).setSettings(settings);
            runFilterPreview();
        } else {
            assert textLayer != null;
            textLayer.applySettings(settings);
            textLayer.getComp().update();
        }
    }

    /**
     * If the settings change while being edited for external
     * reasons (preset), then the GUI is updated via this method.
     */
    @Override
    public void accept(TextSettings settings) {
        try {
            ignoreGUIChanges = true;
            updateGUI(settings);
        } finally {
            ignoreGUIChanges = false;
        }

        updateApp(settings);
    }

    private void updateGUI(TextSettings settings) {
        textTF.setText(settings.getText());
        color.setColor(settings.getColor(), false);
        rotationParam.setValue(settings.getRotation(), false);
        hAlignmentCB.setSelectedItem(settings.getHorizontalAlignment());
        vAlignmentCB.setSelectedItem(settings.getVerticalAlignment());

        Font font = settings.getFont();
        fontSizeSlider.setValue(font.getSize());
        fontFamilyChooserCB.setSelectedItem(font.getName());
        boldCB.setSelected(font.isBold());
        italicCB.setSelected(font.isItalic());

        // this stores the advanced settings
        fontInfo = new FontInfo(font);
        if (advancedSettingsPanel != null) {
            advancedSettingsPanel.updateFrom(fontInfo);
        }

        effectsPanel.setEffects(settings.getEffects());
        watermarkCB.setSelected(settings.hasWatermark());
    }

    private boolean isInFilterMode() {
        return filter != null;
    }
}
