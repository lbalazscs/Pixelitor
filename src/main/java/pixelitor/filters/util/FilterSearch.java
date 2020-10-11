/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.JXList;
import pixelitor.gui.utils.DialogBuilder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class FilterSearch extends JPanel {
    private static final int GAP = 8;
    private final JXList filtersList;

    private FilterSearch() {
        super(new BorderLayout(GAP, GAP));
        JTextField filterTF = new JTextField(30);
        filterTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (filtersList.getElementCount() >= 1) {
                        filtersList.getSelectionModel().setSelectionInterval(0, 0);
                        filtersList.requestFocus();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int numFilters = filtersList.getElementCount();
                    if (numFilters >= 1) {
                        filtersList.getSelectionModel().setSelectionInterval(numFilters - 1, numFilters - 1);
                        filtersList.requestFocus();
                    }
                }
            }
        });
        filterTF.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                FilterSearch.this.createFilter(filterTF.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                FilterSearch.this.createFilter(filterTF.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                FilterSearch.this.createFilter(filterTF.getText());
            }
        });

        add(filterTF, BorderLayout.NORTH);

        filtersList = new JXList(FilterUtils.getAllFiltersSorted());
        filtersList.setAutoCreateRowSorter(true);
        filtersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(new JScrollPane(filtersList), BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
    }

    private void createFilter(String text) {
        String filterText = text.trim().toLowerCase();

        filtersList.setRowFilter(new RowFilter<ListModel, Integer>() {
            @Override
            public boolean include(RowFilter.Entry<? extends ListModel
                , ? extends Integer> entry) {

                String entryValue = entry.getStringValue(0).toLowerCase();

                return filterText.isEmpty() || entryValue.contains(filterText);
            }
        });
    }

    private void startSelectedFilter() {
        FilterAction action = (FilterAction) filtersList.getSelectedValue();
        if (action == null) {
            if (filtersList.getElementCount() == 1) {
                // nothing is selected, but there is only one remaining filter
                action = (FilterAction) filtersList.getElementAt(0);
            }
        }
        if (action != null) {
            action.actionPerformed(null);
        }
    }

    public static void showInDialog() {
        FilterSearch panel = new FilterSearch();
        new DialogBuilder()
            .content(panel)
            .title("Filter Search")
            .okAction(panel::startSelectedFilter)
            .show();
    }
}
