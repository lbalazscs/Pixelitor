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
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.prompt.BuddySupport;
import org.jdesktop.swingx.prompt.PromptSupport;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TFValidationLayerUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.*;
import static org.jdesktop.swingx.prompt.PromptSupport.FocusBehavior.SHOW_PROMPT;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;

public class FilterSearchPanel extends JPanel {
    private static final int GAP = 4;

    private JXTextField searchTF;
    private JXList filtersList;
    private HighlightListCellRenderer highlighter;

    public FilterSearchPanel(FilterAction[] filters) {
        super(new BorderLayout(GAP, GAP));

        createSearchTextField();
        createFiltersList(filters);

        var searchLayerUI = new TFValidationLayerUI(tf -> numMatchingFilters() > 0);
        add(new JLayer<>(searchTF, searchLayerUI), BorderLayout.NORTH);
        add(new JScrollPane(filtersList), BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
    }

    private void createSearchTextField() {
        searchTF = new JXTextField("Search");
        searchTF.setName("searchTF");
        PromptSupport.setFocusBehavior(SHOW_PROMPT, searchTF);
        JLabel searchIcon = new JLabel(new String(Character.toChars(0x1F50D)) + " ");
        searchIcon.setForeground(Color.GRAY);
        BuddySupport.addLeft(searchIcon, searchTF);

        searchTF.requestFocus();

        searchTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == VK_DOWN || keyCode == VK_UP) {
                    int numFilters = numMatchingFilters();
                    if (numFilters > 0) {
                        if (filtersList.isSelectionEmpty()) {
                            if (keyCode == VK_DOWN) {
                                selectFilter(0);
                            } else if (keyCode == VK_UP) {
                                selectFilter(numFilters - 1);
                            }
                        } else {
                            filtersList.dispatchEvent(e);
                        }
                        filtersList.requestFocus();
                    }
                }
            }
        });
        searchTF.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchTermChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchTermChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchTermChanged();
            }
        });
    }

    private void createFiltersList(FilterAction[] filters) {
        filtersList = new JXList(filters);
        filtersList.setAutoCreateRowSorter(true);
        filtersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        highlighter = new HighlightListCellRenderer("");
        filtersList.setCellRenderer(highlighter);

        filtersList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == VK_BACK_SPACE) {
                    // a backspace surely was meant for the search field
                    searchTF.dispatchEvent(e);
                    searchTF.requestFocus();
                } else if (e.getKeyCode() == VK_UP) {
                    if (firstFilterIsSelected()) {
                        searchTF.requestFocus();
                    }
                } else if (e.getKeyCode() == VK_DOWN) {
                    if (lastFilterIsSelected()) {
                        searchTF.requestFocus();
                    }
                }
            }
        });
    }

    private int numMatchingFilters() {
        return filtersList.getElementCount();
    }

    private void selectFilter(int index) {
        filtersList.getSelectionModel().setSelectionInterval(index, index);
    }

    private boolean firstFilterIsSelected() {
        return filtersList.getSelectionModel().isSelectedIndex(0);
    }

    private boolean lastFilterIsSelected() {
        return filtersList.getSelectionModel().isSelectedIndex(numMatchingFilters() - 1);
    }

    private void searchTermChanged() {
        String filterText = searchTF.getText().trim().toLowerCase();

        filtersList.setRowFilter(new RowFilter<ListModel, Integer>() {
            @Override
            public boolean include(Entry<? extends ListModel, ? extends Integer> entry) {
                String filterName = entry.getStringValue(0).toLowerCase();
                return filterText.isEmpty() || filterName.contains(filterText);
            }
        });

        if (numMatchingFilters() > 0 && filtersList.isSelectionEmpty()) {
            selectFilter(0);
        }
        highlighter.setFilterText(filterText);
        filtersList.ensureIndexIsVisible(filtersList.getSelectedIndex());
    }

    private void startSelectedFilter() {
        FilterAction action = getSelectedFilter();
        if (action == null) {
            if (numMatchingFilters() == 1) {
                // nothing is selected, but there is only one remaining filter
                action = (FilterAction) filtersList.getElementAt(0);
            }
        }
        if (action != null) {
            action.actionPerformed(null);
        }
    }

    public FilterAction getSelectedFilter() {
        return (FilterAction) filtersList.getSelectedValue();
    }

    public boolean hasSelection() {
        return !filtersList.isSelectionEmpty();
    }

    public void addSelectionListener(ListSelectionListener listener) {
        filtersList.getSelectionModel().addListSelectionListener(listener);
    }

    public static void showInDialog() {
        FilterSearchPanel panel = new FilterSearchPanel(FilterUtils.getAllFiltersSorted());
        DialogBuilder builder = new DialogBuilder()
            .content(panel)
            .title("Filter Search")
            .okAction(panel::startSelectedFilter);

        JDialog dialog = builder.build();

        // this must be done after building, but before showing
        panel.addSelectionListener(e ->
            builder.getOkButton().setEnabled(panel.hasSelection()));

        GUIUtils.showDialog(dialog, SCREEN_CENTER);
    }
}
