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

package pixelitor.gui;

import com.bric.swing.ColorSwatch;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.colors.ColorPickerDialog;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.TextFieldValidator;
import pixelitor.gui.utils.Theme;
import pixelitor.gui.utils.Themes;
import pixelitor.guides.GuideStrokeType;
import pixelitor.guides.GuideStyle;
import pixelitor.history.History;
import pixelitor.layers.LayerButtonLayout;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Cursors;
import pixelitor.utils.Language;
import pixelitor.utils.Texts;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.EventQueue;
import java.awt.GridBagLayout;

import static java.lang.Integer.parseInt;
import static javax.swing.SwingConstants.LEFT;

/**
 * The GUI for the preferences dialog
 */
public class PreferencesPanel extends JPanel {
    private static final Border EMPTY_BORDER =
        BorderFactory.createEmptyBorder(0, 10, 5, 0);
    private JTextField undoLevelsTF;
    private JComboBox<IntChoiceParam.Value> thumbSizeCB;

    private PreferencesPanel() {
        JTabbedPane tabbedPane = new JTabbedPane(LEFT);
        JPanel generalPanel = createGeneralPanel();
        JPanel guidesPanel = createGuidesPanel();

        tabbedPane.add("General", generalPanel);
        tabbedPane.add("Guides", guidesPanel);
        add(tabbedPane);
    }

    private JPanel createGeneralPanel() {
        JPanel generalPanel = new JPanel(new GridBagLayout());

        var gbh = new GridBagHelper(generalPanel);

        addLanguageChooser(gbh);
        addThemeChooser(gbh);
        addUIChooser(gbh);
        addUndoLevelsChooser(gbh);
        addThumbSizeChooser(gbh);

        generalPanel.setBorder(EMPTY_BORDER);
        return generalPanel;
    }

    private void addLanguageChooser(GridBagHelper gbh) {
        var languages = new EnumComboBoxModel<Language>(Language.class);
        languages.setSelectedItem(Texts.getCurrentLanguage());

        @SuppressWarnings("unchecked")
        JComboBox<Language> langChooser = new JComboBox<>(languages);

        langChooser.setName("langChooser");
        gbh.addLabelAndControl("Language: ", langChooser);
        langChooser.addActionListener(e -> {
            Language language = languages.getSelectedItem();
            if (language != Texts.getCurrentLanguage()) {
                Texts.setCurrentLang(language);
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

    private void addUIChooser(GridBagHelper gbh) {
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
        IntChoiceParam.Value[] thumbSizes = {
                new IntChoiceParam.Value("24x24 pixels", 24),
                new IntChoiceParam.Value("48x48 pixels", 48),
                new IntChoiceParam.Value("72x72 pixels", 72),
                new IntChoiceParam.Value("96x96 pixels", 96),
        };
        thumbSizeCB = new JComboBox<>(thumbSizes);
        thumbSizeCB.setName("thumbSizeCB");

        int currentSize = LayerButtonLayout.getThumbSize();
        thumbSizeCB.setSelectedIndex(currentSize / 24 - 1);

        gbh.addLabelAndControl("Layer/Mask Thumb Sizes: ", thumbSizeCB);
        thumbSizeCB.addActionListener(e -> updateThumbSize());
    }

    private static JPanel createGuidesPanel() {
        JPanel guidesPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(guidesPanel);
        configureGuidesSettings(gbh);
        configureCropGuidesSettings(gbh);
        guidesPanel.setBorder(EMPTY_BORDER);
        return guidesPanel;
    }

    private static void configureGuidesSettings(GridBagHelper gbh) {
        GuideStyle guideStyle = AppPreferences.getGuideStyle();

        ColorSwatch guideColorSwatch = new ColorSwatch(guideStyle.getColorA(), 20);
        var guideStyleCB = new JComboBox<GuideStrokeType>(GuideStrokeType.values());
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

        ColorSwatch guideColorSwatch = new ColorSwatch(guideStyle.getColorA(), 20);
        var cropGuideStyleCB = new JComboBox<GuideStrokeType>(GuideStrokeType.values());
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

    private boolean validate(JDialog d) {
        // we don't want to continuously set the undo levels
        // as the user edits the text field, because low levels
        // erase the history, so we set it in the validator
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
            return true;
        } else {
            Dialogs.showErrorDialog(d, "Error",
                "<html>The <b>Undo/Redo Levels</b> must be a positive integer.");
            return false;
        }
    }

    private int getUndoLevels() {
        return parseInt(undoLevelsTF.getText().trim());
    }

    private void updateThumbSize() {
        int newSize = ((IntChoiceParam.Value) thumbSizeCB.getSelectedItem()).getValue();
        LayerButtonLayout.setThumbSize(newSize);
    }

    public static void showInDialog() {
        PreferencesPanel prefPanel = new PreferencesPanel();

        new DialogBuilder()
                .content(prefPanel)
                .noCancelButton()
                .title("Preferences")
                .okText("Close")
                .validator(prefPanel::validate)
                .validateWhenCanceled()
                .show();
    }
}
