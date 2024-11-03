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

    private static final int DEFAULT_TEXT_AREA_ROWS = 3;
    private static final int DEFAULT_TEXT_AREA_COLS = 20;
    private static final int DEFAULT_MAX_FONT_SIZE = 1000;

    private TextLayer textLayer; // null when used as a filter
    private FontInfo fontInfo;

    // stored here as long as the advanced dialog isn't created
    private double relLineHeight;
    private double scaleX;
    private double scaleY;
    private double shearX;
    private double shearY;

    private JTextArea textArea;
    private JComboBox<String> fontFamilyCB;
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

    private boolean suppressGuiUpdates = false;
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

        if (textArea.getText().equals(TextSettings.DEFAULT_TEXT)) {
            textArea.selectAll();
        }
    }

    private void init(TextSettings settings, Composition comp) {
        settings.setGuiUpdateCallback(this);
        createGUI(settings, comp);

        this.relLineHeight = settings.getRelLineHeight();
        this.scaleX = settings.getSx();
        this.scaleY = settings.getSy();
        this.shearX = settings.getShx();
        this.shearY = settings.getShy();
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
        createTextArea(settings);
        gbh.addLastControl(textArea);

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

    private void createTextArea(TextSettings settings) {
        textArea = new JTextArea(settings.getText(), DEFAULT_TEXT_AREA_ROWS, DEFAULT_TEXT_AREA_COLS);
        textArea.setName("textArea");

        textArea.getDocument().addDocumentListener(new DocumentListener() {
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

        int maxFontSize = DEFAULT_MAX_FONT_SIZE;
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
        fontFamilyCB = new JComboBox<>(availableFonts);

        // it's important to use Font.getName(), and not Font.getFontName(),
        // otherwise it might not be in the combo box
        String fontName = font.getName();
        fontFamilyCB.setSelectedItem(fontName);

        fontFamilyCB.addActionListener(this);
        gbh.addLastControl(fontFamilyCB);

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
                this, fontInfo, relLineHeight, scaleX, scaleY, shearX, shearY);
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
        String fontFamily = (String) fontFamilyCB.getSelectedItem();
        int size = fontSizeSlider.getCurrentValue();
        boolean bold = boldCB.isSelected();
        boolean italic = italicCB.isSelected();
        fontInfo.updateBasic(fontFamily, size, bold, italic);

        if (advancedSettingsDialog != null) {
            advancedSettingsPanel.saveStateTo(fontInfo);
        }

        return fontInfo.createFont();
    }

    private double getRelLineHeight() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getRelLineHeight();
        }
        return relLineHeight;
    }

    private double getScaleX() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getScaleX();
        }
        return scaleX;
    }

    private double getScaleY() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getScaleY();
        }
        return scaleY;
    }

    private double getShearX() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getShearX();
        }
        return shearX;
    }

    private double getShearY() {
        if (advancedSettingsDialog != null) {
            return advancedSettingsPanel.getShearY();
        }
        return shearY;
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
        if (suppressGuiUpdates) {
            return;
        }

        updateApp(createSettingsFromGui());
    }

    private TextSettings createSettingsFromGui() {
        AreaEffects effects = null;
        if (effectsPanel != null) {
            effects = effectsPanel.getEffects();
        }

        BoxAlignment alignment = getSelectedAlignment();

        return new TextSettings(
            textArea.getText(),
            getSelectedFont(),
            color.getColor(),
            effects,
            alignment.getHorizontal(),
            alignment.getVertical(),
            watermarkCB.isSelected(),
            rotationParam.getValueInRadians(),
            getRelLineHeight(),
            getScaleX(), getScaleY(),
            getShearX(), getShearY(), this);
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
            suppressGuiUpdates = true;
            updateGUI(settings);
        } finally {
            suppressGuiUpdates = false;
        }

        updateApp(settings);
    }

    private void updateGUI(TextSettings settings) {
        textArea.setText(settings.getText());
        color.setColor(settings.getColor(), false);
        rotationParam.setValue(settings.getRotation(), false);
        alignmentCB.setSelectedItem(settings.getAlignment());

        Font font = settings.getFont();
        fontSizeSlider.setValue(font.getSize());
        fontFamilyCB.setSelectedItem(font.getName());
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
