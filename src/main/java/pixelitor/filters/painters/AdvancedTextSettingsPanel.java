/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.Map;

import static java.awt.font.TextAttribute.KERNING;
import static java.awt.font.TextAttribute.KERNING_ON;
import static java.awt.font.TextAttribute.LIGATURES;
import static java.awt.font.TextAttribute.LIGATURES_ON;
import static java.awt.font.TextAttribute.STRIKETHROUGH;
import static java.awt.font.TextAttribute.STRIKETHROUGH_ON;
import static java.awt.font.TextAttribute.TRACKING;
import static java.awt.font.TextAttribute.UNDERLINE;
import static java.awt.font.TextAttribute.UNDERLINE_ON;

/**
 * GUI for the advanced font attribute settings
 */
public class AdvancedTextSettingsPanel extends JPanel {
    private JCheckBox underlineCB;
    private JCheckBox strikeThroughCB;
    private JCheckBox kerningCB;
    private JCheckBox ligaturesCB;
    private RangeParam trackingParam;
    private final GridBagHelper gbh;
    private final ActionListener actionListener;

    public AdvancedTextSettingsPanel(ActionListener actionListener,
                                     Map<TextAttribute, Object> map) {
        super(new GridBagLayout());
        this.actionListener = actionListener;
        gbh = new GridBagHelper(this);

        addCheckboxes(map);
        addTrackingGUI(map);
    }

    private void addCheckboxes(Map<TextAttribute, Object> map) {
        boolean strikethrough = false;
        boolean kerning = false;
        boolean underline = false;
        boolean ligatures = false;

        if (map != null) {
            strikethrough = STRIKETHROUGH_ON.equals(map.get(STRIKETHROUGH));
            kerning = KERNING_ON.equals(map.get(KERNING));
            underline = UNDERLINE_ON.equals(map.get(UNDERLINE));
            ligatures = LIGATURES_ON.equals(map.get(LIGATURES));
        }

        strikeThroughCB = addCheckBox("Strikethrough:", "strikeThroughCB", strikethrough);
        underlineCB = addCheckBox("Underline:", "underlineCB", underline);
        kerningCB = addCheckBox("Kerning:", "kerningCB", kerning);
        ligaturesCB = addCheckBox("Ligatures:", "ligaturesCB", ligatures);
    }

    private JCheckBox addCheckBox(String labelText, String name, boolean selected) {
        JCheckBox cb = new JCheckBox("", selected);
        cb.setName(name);
        cb.addActionListener(actionListener);
        gbh.addLabelWithControl(labelText, cb);
        return cb;
    }

    private void addTrackingGUI(Map<TextAttribute, Object> map) {
        int tracking = 0;
        if (map != null) {
            Float trackingSetting = (Float) map.get(TRACKING);
            if (trackingSetting != null) {
                tracking = (int) (100 * trackingSetting);
            }
        }

        trackingParam = new RangeParam("", -20, 0, 70);
        trackingParam.setValue(tracking);
        trackingParam.addChangeListener(e -> actionListener.actionPerformed(null));
        JComponent trackingGUI = trackingParam.createGUI();
        trackingGUI.setName("trackingGUI");
        gbh.addLabelWithControl("Tracking (Letter-spacing):", trackingGUI);
    }

    public void updateFontAttributesMap(Map<TextAttribute, Object> map) {
        Boolean strikeThroughSetting = Boolean.FALSE;
        if (strikeThroughCB.isSelected()) {
            strikeThroughSetting = STRIKETHROUGH_ON;
        }
        map.put(STRIKETHROUGH, strikeThroughSetting);

        Integer kerningSetting = 0;
        if (kerningCB.isSelected()) {
            kerningSetting = KERNING_ON;
        }
        map.put(KERNING, kerningSetting);

        Integer ligaturesSetting = 0;
        if (ligaturesCB.isSelected()) {
            ligaturesSetting = LIGATURES_ON;
        }
        map.put(LIGATURES, ligaturesSetting);

        Integer underlineSetting = -1;
        if (underlineCB.isSelected()) {
            underlineSetting = UNDERLINE_ON;
        }
        map.put(UNDERLINE, underlineSetting);

        Float tracking = trackingParam.getValueAsPercentage();
        map.put(TRACKING, tracking);
    }
}
