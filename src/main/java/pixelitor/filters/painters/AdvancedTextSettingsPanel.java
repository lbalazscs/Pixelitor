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

package pixelitor.filters.painters;

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import static pixelitor.gui.utils.SliderSpinner.LabelPosition;

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

    private RangeParam scaleXParam;
    private RangeParam scaleYParam;
    private RangeParam shearXParam;
    private RangeParam shearYParam;

    private final GridBagHelper gbh;
    private final ActionListener actionListener;

    public AdvancedTextSettingsPanel(ActionListener actionListener,
                                     FontInfo fontInfo,
                                     double lineHeightRatio,
                                     double scaleX, double scaleY,
                                     double shearX, double shearY) {
        super(new GridBagLayout());
        this.actionListener = actionListener;
        gbh = new GridBagHelper(this);

        addCheckboxes(fontInfo);
        addTrackingGUI(fontInfo);
        addLineHeightGUI(lineHeightRatio);
        addTransformGUI(scaleX, scaleY, shearX, shearY);
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
            -20, 0, 70, true, LabelPosition.NONE_WITH_TICKS);
        trackingParam.setValue(font.getTracking());
        trackingParam.addChangeListener(e -> actionListener.actionPerformed(null));
        gbh.addParam(trackingParam, "trackingGUI");
    }

    private void addLineHeightGUI(double lineHeightRatio) {
        lineHeightParam = new RangeParam("Line Height (%)",
            0, 100 * lineHeightRatio, 200, true, LabelPosition.NONE_WITH_TICKS);
        lineHeightParam.addChangeListener(e -> actionListener.actionPerformed(null));
        gbh.addParam(lineHeightParam);
    }

    private void addTransformGUI(double scaleX, double scaleY,
                                 double shearX, double shearY) {
        // use a change listener so that the text appearance is
        // continuously updated while the slider is dragged
        ChangeListener changeListener = e -> actionListener.actionPerformed(null);

        scaleXParam = new RangeParam("Horizontal Scaling (%)",
            -300, 100 * scaleX, 300, true, LabelPosition.NONE_WITH_TICKS);
        scaleXParam.addChangeListener(changeListener);
        gbh.addParam(scaleXParam);

        scaleYParam = new RangeParam("Vertical Scaling (%)",
            -300, 100 * scaleY, 300, true, LabelPosition.NONE_WITH_TICKS);
        scaleYParam.addChangeListener(changeListener);
        gbh.addParam(scaleYParam);

        shearXParam = new RangeParam("Horizontal Shearing (%)",
            -100, 100 * shearX, 100, true, LabelPosition.NONE_WITH_TICKS);
        shearXParam.addChangeListener(changeListener);
        gbh.addParam(shearXParam);

        shearYParam = new RangeParam("Vertical Shearing (%)",
            -100, 100 * shearY, 100, true, LabelPosition.NONE_WITH_TICKS);
        shearYParam.addChangeListener(changeListener);
        gbh.addParam(shearYParam);
    }

    public void updateFontInfo(FontInfo fontInfo) {
        fontInfo.updateAdvanced(
            strikeThroughCB.isSelected(),
            kerningCB.isSelected(),
            ligaturesCB.isSelected(),
            underlineCB.isSelected(),
            trackingParam.getValue());
    }

    public void setUIValues(FontInfo font, double lineHeightRatio,
                            double scaleX, double scaleY,
                            double shearX, double shearY) {
        strikeThroughCB.setSelected(font.hasStrikeThrough());
        underlineCB.setSelected(font.hasUnderline());

        kerningCB.setSelected(font.hasKerning());
        ligaturesCB.setSelected(font.hasLigatures());
        trackingParam.setValue(font.getTracking());

        lineHeightParam.setValueNoTrigger(100 * lineHeightRatio);

        scaleXParam.setValueNoTrigger(100 * scaleX);
        scaleYParam.setValueNoTrigger(100 * scaleY);
        shearXParam.setValueNoTrigger(100 * shearX);
        shearYParam.setValueNoTrigger(100 * shearY);
    }

    public double getRelLineHeight() {
        return lineHeightParam.getPercentage();
    }

    public double getScaleX() {
        return scaleXParam.getPercentage();
    }

    public double getScaleY() {
        return scaleYParam.getPercentage();
    }

    public double getShearX() {
        return shearXParam.getPercentage();
    }

    public double getShearY() {
        return shearYParam.getPercentage();
    }
}
