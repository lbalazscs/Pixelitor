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

package pixelitor.filters.painters;

import org.jdesktop.swingx.VerticalLayout;
import pixelitor.Composition;
import pixelitor.filters.gui.*;
import pixelitor.gui.utils.*;
import pixelitor.layers.Filterable;
import pixelitor.layers.TextLayer;
import pixelitor.utils.Messages;
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
import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;

/**
 * Customization panel for the text filter and for text layers
 */
public class TextSettingsPanel extends FilterGUI
    implements ParamAdjustmentListener, ActionListener, Consumer<TextSettings> {
    private TextLayer textLayer;
    private FontInfo fontInfo;

    // stored here as long as the advanced dialog isn't created
    private double relLineHeight;
    private double sx;
    private double sy;
    private double shx;
    private double shy;

    private JTextArea textTF;
    private JComboBox<String> fontFamilyChooserCB;
    private SliderSpinner fontSizeSlider;
    private AngleParam rotationParam;
    private JCheckBox boldCB;
    private JCheckBox italicCB;
    private ColorParam color;
    private EffectsPanel effectsPanel;
    private JComboBox<BoxAlignment> alignmentCB;
    private JCheckBox watermarkCB;

    private JDialog advancedSettingsDialog;
    private AdvancedTextSettingsPanel advancedSettingsPanel;

    private boolean ignoreGUIChanges = false;
    private BoxAlignment lastAlignment;

    /**
     * Used for the text filter on images
     */
    public TextSettingsPanel(TextFilter textFilter, Filterable layer) {
        super(textFilter, layer);
        init(textFilter.getSettings(), layer.getComp());
    }

    /**
     * Used for text layers
     */
    public TextSettingsPanel(TextLayer textLayer) {
        super(null, null);
        this.textLayer = textLayer;
        TextSettings settings = textLayer.getSettings();
        init(settings, textLayer.getComp());

        if (textTF.getText().equals(TextSettings.DEFAULT_TEXT)) {
            textTF.selectAll();
        }
    }

    private void init(TextSettings settings, Composition comp) {
        settings.setGuiUpdater(this);
        createGUI(settings, comp);

        this.relLineHeight = settings.getRelLineHeight();
        this.sx = settings.getSx();
        this.sy = settings.getSy();
        this.shx = settings.getShx();
        this.shy = settings.getShy();
    }

    private void createGUI(TextSettings settings, Composition comp) {
        assert settings != null;
        setLayout(new VerticalLayout());

        add(createTextPanel(settings, comp));
        add(createFontPanel(settings));

        createEffectsPanel(settings);
        add(effectsPanel);

        add(createBottomPanel(settings));
    }

    private JPanel createTextPanel(TextSettings settings, Composition comp) {
        JPanel textPanel = new JPanel(new GridBagLayout());
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

        alignmentCB = new JComboBox<>(BoxAlignment.values());
        alignmentCB.setName("alignmentCB");
        BoxAlignment alignment = settings.getAlignment();
        lastAlignment = alignment;
        alignmentCB.setSelectedItem(alignment);

        alignmentCB.addActionListener(e -> {
            BoxAlignment selectedAlignment = getSelectedAlignment();
            if (selectedAlignment == BoxAlignment.PATH && !comp.hasActivePath()) {
                String msg = "<html>There's no path in <b>\"" + comp.getName() + "\"</b>.<br>You can have text along a path after creating a path with the Pen Tool.";
                Messages.showError("No Path", msg, this);
                alignmentCB.setSelectedItem(lastAlignment);
                return;
            }

            lastAlignment = selectedAlignment;
            actionPerformed(e);
        });
        gbh.addLabel("Alignment:", 0, 2);
        gbh.addControl(alignmentCB);

        return textPanel;
    }

    private void createTextTF(TextSettings settings) {
        textTF = new JTextArea(settings.getText(), 3, 20);
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
        JPanel fontPanel = new JPanel(new GridBagLayout());
        fontPanel.setBorder(createTitledBorder("Font"));

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

        // it's important to use Font.getName(), and not Font.getFontName(),
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
                this, fontInfo, relLineHeight, sx, sy, shx, shy);
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

        return fontInfo.createStyledFont();
    }

    private double getRelLineHeight() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getRelLineHeight();
        }
        return relLineHeight;
    }

    private double getSx() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getSx();
        }
        return sx;
    }

    private double getSy() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getSy();
        }
        return sy;
    }

    private double getShx() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getShx();
        }
        return shx;
    }

    private double getShy() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getShy();
        }
        return shy;
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

        BoxAlignment alignment = getSelectedAlignment();
        var settings = new TextSettings(
            text, selectedFont, color.getColor(), effects,
            alignment.getHorizontal(),
            alignment.getVertical(),
            watermarkCB.isSelected(), textRotationAngle,
            getRelLineHeight(), getSx(), getSy(), getShx(), getShy(), this);

        updateApp(settings);
    }

    private BoxAlignment getSelectedAlignment() {
        return (BoxAlignment) alignmentCB.getSelectedItem();
    }

    private void updateApp(TextSettings settings) {
        if (isFilter()) {
            ((TextFilter) filter).setSettings(settings);
            startPreview(false); // TODO currently always false
        } else {
            assert textLayer != null;
            textLayer.applySettings(settings);
            textLayer.update();
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
        alignmentCB.setSelectedItem(settings.getAlignment());

        Font font = settings.getFont();
        fontSizeSlider.setValue(font.getSize());
        fontFamilyChooserCB.setSelectedItem(font.getName());
        boldCB.setSelected(font.isBold());
        italicCB.setSelected(font.isItalic());

        // this stores the advanced settings
        fontInfo = new FontInfo(font);
        if (advancedSettingsPanel != null) {
            advancedSettingsPanel.updateFrom(fontInfo, settings.getRelLineHeight(),
                settings.getSx(), settings.getSy(), settings.getShx(), settings.getShy());
        }

        effectsPanel.setEffects(settings.getEffects());
        watermarkCB.setSelected(settings.hasWatermark());
    }

    private boolean isFilter() {
        return filter != null;
    }
}
