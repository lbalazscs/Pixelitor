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

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

/**
 * GUI for the advanced text settings.
 */
public class AdvancedTextSettingsPanel extends JPanel {
    private JCheckBox underlineCB;
    private JCheckBox strikeThroughCB;
    private JCheckBox kerningCB;
    private JCheckBox ligaturesCB;
    private RangeParam trackingParam;
    private RangeParam lineHeightParam;
    private final GridBagHelper gbh;
    private final ActionListener actionListener;

    public AdvancedTextSettingsPanel(ActionListener actionListener,
                                     FontInfo fontInfo, double relLineHeight) {
        super(new GridBagLayout());
        this.actionListener = actionListener;
        gbh = new GridBagHelper(this);

        addCheckboxes(fontInfo);
        addTrackingGUI(fontInfo);
        addLineHeightGUI(relLineHeight);
    }

    private void addCheckboxes(FontInfo font) {
        strikeThroughCB = addCheckBox("Strikethrough:",
            "strikeThroughCB", font.hasStrikeThrough());
        underlineCB = addCheckBox("Underline:",
            "underlineCB", font.hasUnderline());
        kerningCB = addCheckBox("Kerning:",
            "kerningCB", font.hasKerning());
        ligaturesCB = addCheckBox("Ligatures:",
            "ligaturesCB", font.hasLigatures());
    }

    private JCheckBox addCheckBox(String labelText, String name, boolean selected) {
        var cb = new JCheckBox("", selected);
        cb.setName(name);
        cb.addActionListener(actionListener);
        gbh.addLabelAndControl(labelText, cb);
        return cb;
    }

    private void addTrackingGUI(FontInfo font) {
        trackingParam = new RangeParam("Tracking (Letter-spacing)",
            -20, 0, 70, true, SliderSpinner.TextPosition.NONE);
        trackingParam.setValue(font.getTracking());
        trackingParam.addChangeListener(e -> actionListener.actionPerformed(null));
        gbh.addParam(trackingParam, "trackingGUI");
    }

    private void addLineHeightGUI(double relLineHeight) {
        lineHeightParam = new RangeParam("Line Height (%)",
            50, 100 * relLineHeight, 200, true, SliderSpinner.TextPosition.NONE);
        lineHeightParam.addChangeListener(e -> actionListener.actionPerformed(null));
        gbh.addParam(lineHeightParam);
    }

    public void saveStateTo(FontInfo fontInfo) {
        boolean strikeThrough = strikeThroughCB.isSelected();
        boolean kerning = kerningCB.isSelected();
        boolean ligatures = ligaturesCB.isSelected();
        boolean underline = underlineCB.isSelected();
        int tracking = trackingParam.getValue();

        fontInfo.updateAdvanced(strikeThrough, kerning, ligatures, underline, tracking);
    }

    public void updateFrom(FontInfo font, double relLineHeight) {
        strikeThroughCB.setSelected(font.hasStrikeThrough());
        kerningCB.setSelected(font.hasKerning());
        ligaturesCB.setSelected(font.hasLigatures());
        underlineCB.setSelected(font.hasUnderline());
        trackingParam.setValue(font.getTracking());
        lineHeightParam.setValueNoTrigger(100 * relLineHeight);
    }

    public double getRelLineHeight() {
        return lineHeightParam.getPercentage();
    }
}
