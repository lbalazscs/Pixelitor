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

package pixelitor.filters.util;

import org.jdesktop.swingx.renderer.DefaultListRenderer;

import javax.swing.*;
import java.awt.Component;
import java.util.Locale;

public class HighlightListCellRenderer extends DefaultListRenderer {
    private String searchText;

    public HighlightListCellRenderer(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // highlights the search matches in the selected filter
        if (isSelected && !searchText.isEmpty()) {
            String origText = value.toString();
            label.setText(highlightText(origText, searchText));
        }

        return label;
    }

    /**
     * Returns an HTML-formatted text with highlighted matches.
     */
    private static String highlightText(String text, String searchTextLC) {
        int index = text.toLowerCase(Locale.getDefault()).indexOf(searchTextLC);
        if (index == -1) {
            return text;
        }

        int searchTextLength = searchTextLC.length();
        return "<html>"
            + text.substring(0, index)
            + "<b><span style='background:#8A3958;'>"
            + text.substring(index, index + searchTextLength)
            + "</span></b>"
            + text.substring(index + searchTextLength);
    }

    public void updateSearchText(String searchText) {
        this.searchText = searchText;
    }
}
