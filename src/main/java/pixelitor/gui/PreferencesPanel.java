/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui;

import com.bric.swing.ColorSwatch;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.colors.ColorPickerDialog;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.gui.utils.*;
import pixelitor.guides.GuideStrokeType;
import pixelitor.guides.GuideStyle;
import pixelitor.history.History;
import pixelitor.layers.LayerButtonLayout;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Cursors;
import pixelitor.utils.Language;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;

import static java.lang.Integer.parseInt;
import static javax.swing.SwingConstants.LEFT;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.utils.Texts.i18n;

/**
 * The GUI for the preferences dialog
 */
public class PreferencesPanel extends JPanel {
    private static final Border EMPTY_BORDER =
        BorderFactory.createEmptyBorder(0, 10, 5, 0);
    private JTextField undoLevelsTF;
    private JComboBox<IntChoiceParam.Item> thumbSizeCB;
    private JComboBox<MouseZoomMethod> zoomMethodCB;
    private JComboBox<PanMethod> panMethodCB;
    private JTextField magickDirTF;

    // the panel is re-created every time, but the last selected tab
    // should be selected automatically the next time
    private static int lastSelectedTabIndex = 0;

    private PreferencesPanel() {
        var tabbedPane = new JTabbedPane(LEFT);

        tabbedPane.add("General", createGeneralPanel());
        tabbedPane.add("Mouse", createMousePanel());
        tabbedPane.add("Guides", createGuidesPanel());

        setupTabSelection(tabbedPane);

        add(tabbedPane);
    }

    private static void setupTabSelection(JTabbedPane tabbedPane) {
        if (lastSelectedTabIndex != 0) {
            tabbedPane.setSelectedIndex(lastSelectedTabIndex);
        }
        tabbedPane.addChangeListener(e ->
            lastSelectedTabIndex = tabbedPane.getSelectedIndex());
    }

    private JPanel createGeneralPanel() {
        var generalPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(generalPanel);

        addLanguageChooser(gbh);
//        addThemeChooser(gbh);
        addUIChooser(gbh);
        addUndoLevelsChooser(gbh);
        addThumbSizeChooser(gbh);
        addMagickDirField(gbh);

        generalPanel.setBorder(EMPTY_BORDER);
        return generalPanel;
    }

    private void addLanguageChooser(GridBagHelper gbh) {
        var languages = new EnumComboBoxModel<>(Language.class);
        languages.setSelectedItem(Language.getCurrent());

        @SuppressWarnings("unchecked")
        JComboBox<Language> langChooser = new JComboBox<>(languages);

        langChooser.setName("langChooser");
        gbh.addLabelAndControl("Language: ", langChooser);
        langChooser.addActionListener(e -> {
            Language language = languages.getSelectedItem();
            if (language != Language.getCurrent()) {
                Language.setCurrent(language);
                EventQueue.invokeLater(() -> Dialogs.showInfoDialog(this,
                    "Needs Restart",
                    "Changing the display language will take effect after restarting Pixelitor."));
            }
        });
    }

    private void addThemeChooser(GridBagHelper gbh) {
        EnumComboBoxModel<Theme> themes = new EnumComboBoxModel<>(Theme.class);
        themes.setSelectedItem(Themes.getCurrent());

        @SuppressWarnings("unchecked")
        JComboBox<Theme> themeChooser = new JComboBox<>(themes);

        themeChooser.setName("themeChooser");
        gbh.addLabelAndControl("Theme: ", themeChooser);
        themeChooser.addActionListener(e -> {
            Theme theme = themes.getSelectedItem();
            setCursor(Cursors.BUSY);
            EventQueue.invokeLater(() -> {
                Themes.install(theme, true, false);
                SwingUtilities.getWindowAncestor(this).pack();
                setCursor(Cursors.DEFAULT);
            });
        });
    }

    private static void addUIChooser(GridBagHelper gbh) {
        JComboBox<ImageArea.Mode> uiChooser = new JComboBox<>(ImageArea.Mode.values());
        uiChooser.setSelectedItem(ImageArea.getMode());
        uiChooser.setName("uiChooser");
        gbh.addLabelAndControl("Images In: ", uiChooser);
        uiChooser.addActionListener(e -> {
            ImageArea.Mode mode = (ImageArea.Mode) uiChooser.getSelectedItem();
            ImageArea.changeUI(mode);
        });
    }

    private void addUndoLevelsChooser(GridBagHelper gbh) {
        undoLevelsTF = new JTextField(3);
        undoLevelsTF.setName("undoLevelsTF");
        undoLevelsTF.setText(String.valueOf(History.getUndoLevels()));
        gbh.addLabelAndControl("Undo/Redo Levels: ",
            TextFieldValidator.createPositiveIntLayer("Undo/Redo Levels",
                undoLevelsTF, true));
    }

    private void addThumbSizeChooser(GridBagHelper gbh) {
        IntChoiceParam.Item[] thumbSizes = {
            new IntChoiceParam.Item("24x24 pixels", 24),
            new IntChoiceParam.Item("48x48 pixels", 48),
            new IntChoiceParam.Item("72x72 pixels", 72),
            new IntChoiceParam.Item("96x96 pixels", 96),
        };
        thumbSizeCB = new JComboBox<>(thumbSizes);
        thumbSizeCB.setName("thumbSizeCB");

        int currentSize = LayerButtonLayout.getThumbSize();
        thumbSizeCB.setSelectedIndex(currentSize / 24 - 1);

        gbh.addLabelAndControl("Layer/Mask Thumb Sizes: ", thumbSizeCB);
        thumbSizeCB.addActionListener(e -> updateThumbSize());
    }

    private void addMagickDirField(GridBagHelper gbh) {
        magickDirTF = new JTextField(AppPreferences.magickDirName);
        // don't let the textfield grow too large
        magickDirTF.setPreferredSize(new Dimension(100, magickDirTF.getPreferredSize().height));
        gbh.addLabelAndControl("ImageMagick 7 Folder: ", magickDirTF);
    }

    private static JPanel createGuidesPanel() {
        var guidesPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(guidesPanel);
        configureGuidesSettings(gbh);
        configureCropGuidesSettings(gbh);
        guidesPanel.setBorder(EMPTY_BORDER);
        return guidesPanel;
    }

    private static void configureGuidesSettings(GridBagHelper gbh) {
        GuideStyle guideStyle = AppPreferences.getGuideStyle();

        var guideColorSwatch = new ColorSwatch(guideStyle.getColorA(), 20);
        var guideStyleCB = new JComboBox<>(GuideStrokeType.values());
        guideStyleCB.setName("guideStyleCB");
        guideStyleCB.setSelectedItem(guideStyle.getStrokeType());

        gbh.addLabelAndControl("Guide Color: ", guideColorSwatch);
        gbh.addLabelAndControl("Guide Style: ", guideStyleCB);

        new ColorPickerDialog(guideColorSwatch, e -> {
            guideStyle.setColorA(guideColorSwatch.getForeground());
            ImageArea.getUI().repaint();
        });

        guideStyleCB.addActionListener(e -> {
            guideStyle.setStrokeType((GuideStrokeType) guideStyleCB.getSelectedItem());
            ImageArea.getUI().repaint();
        });
    }

    private static void configureCropGuidesSettings(GridBagHelper gbh) {
        GuideStyle guideStyle = AppPreferences.getCropGuideStyle();

        var guideColorSwatch = new ColorSwatch(guideStyle.getColorA(), 20);
        var cropGuideStyleCB = new JComboBox<>(GuideStrokeType.values());
        cropGuideStyleCB.setName("cropGuideStyleCB");
        cropGuideStyleCB.setSelectedItem(guideStyle.getStrokeType());

        gbh.addLabelAndControl("Cropping Guide Color: ", guideColorSwatch);
        gbh.addLabelAndControl("Cropping Guide Style: ", cropGuideStyleCB);

        new ColorPickerDialog(guideColorSwatch, e -> {
            guideStyle.setColorA(guideColorSwatch.getForeground());
            ImageArea.getUI().repaint();
        });

        cropGuideStyleCB.addActionListener(e -> {
            guideStyle.setStrokeType((GuideStrokeType) cropGuideStyleCB.getSelectedItem());
            ImageArea.getUI().repaint();
        });
    }

    private JPanel createMousePanel() {
        var mousePanel = new JPanel(new BorderLayout());
        // put the contents to the north of a border layout,
        // so that it is not stretched vertically
        var contents = new JPanel(new GridBagLayout());
        mousePanel.add(contents, BorderLayout.NORTH);

        var gbh = new GridBagHelper(contents);

        zoomMethodCB = new JComboBox<>(MouseZoomMethod.values());
        zoomMethodCB.setSelectedItem(MouseZoomMethod.CURRENT);
        zoomMethodCB.setName("zoomMethod");
        gbh.addLabelAndControl("Zoom with:", zoomMethodCB);

        panMethodCB = new JComboBox<>(PanMethod.values());
        panMethodCB.setSelectedItem(PanMethod.CURRENT);
        panMethodCB.setName("panMethod");
        gbh.addLabelAndControl("Pan with:", panMethodCB);

        mousePanel.setBorder(EMPTY_BORDER);
        return mousePanel;
    }

    private boolean validate(JDialog d) {
        // we don't want to continuously set the undo levels
        // as the user edits the text field, because low levels
        // erase the history, therefore it is set here
        int undoLevels = 0;
        boolean couldParse = true;
        try {
            undoLevels = getUndoLevels();
            if (undoLevels < 0) {
                couldParse = false;
            }
        } catch (NumberFormatException ex) {
            couldParse = false;
        }

        if (couldParse) {
            History.setUndoLevels(undoLevels);
        } else {
            Dialogs.showErrorDialog(d, "Error",
                "<html>The <b>Undo/Redo Levels</b> must be a positive integer.");
            return false;
        }

        // these can't be set interactively => set it here
        MouseZoomMethod.changeTo((MouseZoomMethod) zoomMethodCB.getSelectedItem());
        PanMethod.changeTo((PanMethod) panMethodCB.getSelectedItem());
        AppPreferences.magickDirName = magickDirTF.getText().trim();

        return true;
    }

    private int getUndoLevels() {
        return parseInt(undoLevelsTF.getText().trim());
    }

    private void updateThumbSize() {
        int newSize = ((IntChoiceParam.Item) thumbSizeCB.getSelectedItem()).getValue();
        LayerButtonLayout.setThumbSize(newSize);
    }

    public static void showInDialog() {
        var prefPanel = new PreferencesPanel();

        new DialogBuilder()
            .content(prefPanel)
            .noCancelButton()
            .title(i18n("preferences"))
            .okText(CLOSE_DIALOG)
            .validator(prefPanel::validate)
            .validateWhenCanceled()
            .show();
    }
}
