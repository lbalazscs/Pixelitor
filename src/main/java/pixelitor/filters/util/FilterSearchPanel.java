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

package pixelitor.filters.util;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.prompt.BuddySupport;
import org.jdesktop.swingx.prompt.PromptSupport;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.layers.LayerGUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.Locale;

import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_UP;
import static org.jdesktop.swingx.prompt.PromptSupport.FocusBehavior.SHOW_PROMPT;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;
import static pixelitor.gui.utils.TFValidationLayerUI.wrapWithSimpleValidation;

public class FilterSearchPanel extends JPanel {
    private static final int PADDING = 4;

    private JTextField searchTF;
    private JXList filterList;
    private HighlightListCellRenderer highlighter;
    private boolean dialogCanceled;

    public FilterSearchPanel(FilterAction[] filters) {
        super(new BorderLayout(PADDING, PADDING));

        initSearchField();
        initFiltersList(filters);

        add(wrapWithSimpleValidation(searchTF, tf -> getMatchingFilterCount() > 0), BorderLayout.NORTH);
        add(new JScrollPane(filterList), BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
    }

    private void initSearchField() {
        searchTF = new JXTextField("Search");
        searchTF.setName("searchTF");

        // add placeholder behavior and left-aligned search icon
        PromptSupport.setFocusBehavior(SHOW_PROMPT, searchTF);

        Color searchIconColor = Themes.getActive().isDark() ? Themes.LIGHT_ICON_COLOR : LayerGUI.SELECTED_COLOR;
        VectorIcon searchIcon = new VectorIcon(searchIconColor, 20, 14, FilterSearchPanel::paintSearchIcon);
        JLabel searchBuddy = new JLabel(searchIcon);
        searchBuddy.setForeground(Color.GRAY);
        BuddySupport.addLeft(searchBuddy, searchTF);

        searchTF.requestFocusInWindow();

        // handle key events for navigation and search term updates
        searchTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                navigateFilterList(e);
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

    private void navigateFilterList(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == VK_DOWN || keyCode == VK_UP) {
            int numFilters = getMatchingFilterCount();
            if (numFilters > 0) {
                if (filterList.isSelectionEmpty()) {
                    if (keyCode == VK_DOWN) {
                        selectFilter(0);
                    } else if (keyCode == VK_UP) {
                        selectFilter(numFilters - 1);
                    }
                } else {
                    forwardEventTo(filterList, e);
                }
                filterList.requestFocusInWindow();
            }
        }
    }

    private void initFiltersList(FilterAction[] filters) {
        filterList = new JXList(filters);
        filterList.setAutoCreateRowSorter(true);
        filterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        highlighter = new HighlightListCellRenderer("");
        filterList.setCellRenderer(highlighter);

        filterList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == VK_UP) {
                    if (isFirstFilterSelected()) {
                        searchTF.requestFocusInWindow();
                    }
                } else if (e.getKeyCode() == VK_DOWN) {
                    if (isLastFilterSelected()) {
                        searchTF.requestFocusInWindow();
                    }
                } else if (e.getKeyCode() == VK_BACK_SPACE) {
                    forwardEventTo(searchTF, e);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (Character.isLetter(e.getKeyChar())) {
                    forwardEventTo(searchTF, e);
                }
            }
        });
    }

    private static void forwardEventTo(JComponent target, KeyEvent e) {
        e.setSource(target);
        target.dispatchEvent(e);
        target.requestFocusInWindow();
    }

    private int getMatchingFilterCount() {
        return filterList.getElementCount();
    }

    private void selectFilter(int index) {
        filterList.getSelectionModel().setSelectionInterval(index, index);
    }

    private boolean isFirstFilterSelected() {
        return filterList.getSelectionModel().isSelectedIndex(0);
    }

    private boolean isLastFilterSelected() {
        return filterList.getSelectionModel().isSelectedIndex(getMatchingFilterCount() - 1);
    }

    private void searchTermChanged() {
        String searchTextLC = searchTF.getText().trim().toLowerCase(Locale.getDefault());

        // dynamically filter the list based on the search text
        filterList.setRowFilter(new RowFilter<ListModel<FilterAction>, Integer>() {
            @Override
            public boolean include(Entry<? extends ListModel<FilterAction>, ? extends Integer> entry) {
                String filterNameLC = entry.getStringValue(0).toLowerCase(Locale.getDefault());
                // include filters that match the search text
                // or show all filters if the search text is empty
                return searchTextLC.isEmpty() || filterNameLC.contains(searchTextLC);
            }
        });

        // if there are matching filters and none are selected,
        // select the first matching filter by default
        if (getMatchingFilterCount() > 0 && filterList.isSelectionEmpty()) {
            selectFilter(0);
        }

        highlighter.updateSearchText(searchTextLC);

        // ensure the currently selected filter (if any) is visible in the list
        filterList.ensureIndexIsVisible(filterList.getSelectedIndex());
    }

    public FilterAction getSelectedFilter() {
        if (dialogCanceled) {
            return null;
        }
        FilterAction selected = (FilterAction) filterList.getSelectedValue();
        if (selected != null) {
            return selected;
        }
        if (getMatchingFilterCount() == 1) {
            // nothing is selected, but there is only one remaining filter
            return (FilterAction) filterList.getElementAt(0);
        }
        return null;
    }

    public boolean hasSelection() {
        return !filterList.isSelectionEmpty();
    }

    public void addSelectionListener(ListSelectionListener listener) {
        filterList.getSelectionModel().addListSelectionListener(listener);
    }

    public static FilterAction showInDialog(String title) {
        FilterSearchPanel panel = new FilterSearchPanel(Filters.getAllFilters());
        DialogBuilder builder = new DialogBuilder()
            .content(panel)
            .title(title)
            .cancelAction(() -> panel.dialogCanceled = true);

        JDialog dialog = builder.build();

        // this must be done after building, but before showing
        builder.getOkButton().setEnabled(false);
        panel.addSelectionListener(e ->
            builder.getOkButton().setEnabled(panel.hasSelection()));

        panel.filterList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (panel.hasSelection()) {
                        // Double-clicking on a selected filter will
                        // start it after the modal dialog is closed.
                        GUIUtils.closeDialog(dialog, true);
                    }
                }
            }
        });

        GUIUtils.showDialog(dialog, SCREEN_CENTER);

        // will get here only after the dialog is closed
        return panel.getSelectedFilter();
    }

    private static void paintSearchIcon(Graphics2D g) {
        // the shape is based on search.svg
        Path2D shape = new Path2D.Double();
        shape.moveTo(7.897525, 8.923619);
        shape.lineTo(12.009847, 12.81312);
        shape.curveTo(12.009847, 12.81312, 12.87233, 13.229692, 13.303571, 12.81312);
        shape.curveTo(13.734813, 12.396544, 13.303571, 11.563393, 13.303571, 11.563393);
        shape.lineTo(9.032443, 7.5904202);

        g.fill(shape);
        g.draw(shape);

        g.draw(new Ellipse2D.Double(1.0, 1.0, 8.5, 8.5));
    }
}
