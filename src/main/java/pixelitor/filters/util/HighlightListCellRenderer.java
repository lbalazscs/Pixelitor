/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.util;

import org.jdesktop.swingx.renderer.DefaultListRenderer;

import javax.swing.*;
import java.awt.Component;
import java.util.Locale;

public class HighlightListCellRenderer extends DefaultListRenderer {
    private String filterText;

    public HighlightListCellRenderer(String filterText) {
        this.filterText = filterText;
    }

    private static String highlight(String text, String filter) {
        int index = text.toLowerCase(Locale.getDefault()).indexOf(filter);
        if (index == -1) {
            return text;
        }

        int filterLength = filter.length();
        return "<html>"
            + text.substring(0, index)
            + "<b><span style='background:#8A3958;'>"
            + text.substring(index, index + filterLength)
            + "</span></b>"
            + text.substring(index + filterLength);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (isSelected && !filterText.isEmpty()) {
            String fullText = value.toString();
            c.setText(highlight(fullText, filterText));
        }

        return c;
    }

    public void setFilterText(String filterText) {
        this.filterText = filterText;
    }
}
