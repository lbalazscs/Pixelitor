/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Features;
import pixelitor.colors.ColorPickerHelper;
import pixelitor.filters.gui.ChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.*;
import pixelitor.gui.utils.SliderSpinner.LabelPosition;
import pixelitor.guides.GuideStrokeType;
import pixelitor.guides.GuideStyle;
import pixelitor.history.History;
import pixelitor.io.FileChoosers;
import pixelitor.utils.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagLayout;

import static java.lang.Integer.parseInt;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.utils.Texts.i18n;

/**
 * The GUI for the preferences dialog
 */
public class PreferencesPanel extends JTabbedPane {
    private static final Border PANEL_PADDING =
        BorderFactory.createEmptyBorder(5, 10, 5, 0);
    private static final String UNDO_LEVELS_LABEL = "Minimum Undo Levels";
    private static final String IMAGEMAGICK_FOLDER_LABEL = "ImageMagick 7 Folder";
    private static final String GMIC_FOLDER_LABEL = "G'MIC Folder";

    private JTextField undoLevelsTF;
    private JComboBox<Item> thumbSizeCB;
    private JComboBox<MouseZoomMethod> zoomMethodCB;
    private JComboBox<PanMethod> panMethodCB;
    private JCheckBox snapCB;
    private JTextField magickDirTF;
    private JTextField gmicDirTF;
    private JCheckBox nativeChoosersCB;
    private JCheckBox experimentalCB;

    // persists the last selected tab between dialog instances
    private static int lastSelectedTabIndex = 0;

    private PreferencesPanel() {
        super(LEFT);

        add("UI", createUIPanel());
        add("Mouse", createMousePanel());
        add("Guides", createGuidesPanel());
        add("Advanced", createAdvancedPanel());

        setupRememberingTabSelection();
    }

    private JPanel createUIPanel() {
        var uiPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(uiPanel);

        addLanguageChooser(gbh);
        addThemeChooser(gbh);
        addFontChoosers(gbh);
        addImageAreaChooser(gbh);
        addThumbSizeChooser(gbh);

        uiPanel.setBorder(PANEL_PADDING);
        return uiPanel;
    }

    private void addLanguageChooser(GridBagHelper gbh) {
        var languages = new EnumComboBoxModel<>(Language.class);
        languages.setSelectedItem(Language.getActive());

        @SuppressWarnings("unchecked")
        JComboBox<Language> langChooser = new JComboBox<>(languages);

        langChooser.setName("langChooser");
        gbh.addLabelAndControlNoStretch("Language: ", langChooser);
        langChooser.addActionListener(e -> {
            Language language = languages.getSelectedItem();
            if (language != Language.getActive()) {
                Language.setActive(language);
                EventQueue.invokeLater(() -> Dialogs.showInfoDialog(this,
                    "Needs Restart",
                    "Changing the display language will take effect after restarting Pixelitor."));
            }
        });
    }

    private void addThemeChooser(GridBagHelper gbh) {
        EnumComboBoxModel<Theme> themes = new EnumComboBoxModel<>(Theme.class);
        Theme activeTheme = Themes.getActive();
        themes.setSelectedItem(activeTheme);

        @SuppressWarnings("unchecked")
        JComboBox<Theme> themeChooser = new JComboBox<>(themes);

        themeChooser.setName("themeChooser");
        gbh.addLabelAndControlNoStretch("Theme: ", themeChooser);

/*        JLabel accentColorLabel = new JLabel("Accent Color (Flat Themes)");
        EnumComboBoxModel<AccentColor> accentColors = new EnumComboBoxModel<>(AccentColor.class);
        @SuppressWarnings("unchecked")
        JComboBox<Theme> accentColorChooser = new JComboBox<>(accentColors);

        Consumer<Boolean> accentColorEnabler = enable -> {
            accentColorLabel.setEnabled(enable);
            accentColorChooser.setEnabled(enable);
        };
        accentColorEnabler.accept(activeTheme.isFlat());

        gbh.addTwoControlsNoStretch(accentColorLabel, accentColorChooser);
*/
        themeChooser.addActionListener(e -> {
            Theme theme = themes.getSelectedItem();
            setCursor(Cursors.BUSY);

            EventQueue.invokeLater(() -> {
                Themes.apply(theme, true, false);
//                accentColorEnabler.accept(theme.isFlat());
                SwingUtilities.getWindowAncestor(this).pack();
                setCursor(Cursors.DEFAULT);
            });
        });

//        accentColorChooser.addActionListener(e -> Themes.changeAccentColor(accentColors.getSelectedItem()));
    }

    private void addFontChoosers(GridBagHelper gbh) {
        Font font = UIManager.getFont("defaultFont");
        if (font == null) {
            return;
        }

        int currentSize = font.getSize();
        int minSize = Math.min(10, currentSize);
        int maxSize = Math.max(30, currentSize);

        RangeParam fontSize = new RangeParam("Font Size", minSize, currentSize, maxSize, true, LabelPosition.NONE);
        gbh.addParam(fontSize);

        ChoiceParam<String> fontType = new ChoiceParam<>("Font Type", Utils.getAvailableFontNames(), font.getName());
        gbh.addParam(fontType);

        fontSize.setAdjustmentListener(() -> {
            Font currentFont = UIManager.getFont("defaultFont");
            Font newFont = currentFont.deriveFont(fontSize.getValueAsFloat());
            changeFont(newFont);
        });

        fontType.setAdjustmentListener(() ->
            changeFont(new Font(fontType.getSelected(), Font.PLAIN, fontSize.getValue())));
    }

    private void changeFont(Font newFont) {
        setCursor(Cursors.BUSY);

        UIManager.put("defaultFont", new FontUIResource(newFont));

        if (Themes.getActive().isNimbus()) {
            try {
                NimbusLookAndFeel laf = new NimbusLookAndFeel();
                UIManager.setLookAndFeel(laf);
                laf.getDefaults().put("defaultFont", new FontUIResource(newFont));
            } catch (UnsupportedLookAndFeelException e) {
                Messages.showException(e);
            }
        }

        EventQueue.invokeLater(() -> {
            Themes.refreshComponentUIs();
            SwingUtilities.getWindowAncestor(this).pack();
            setCursor(Cursors.DEFAULT);
        });
    }

    private static void addImageAreaChooser(GridBagHelper gbh) {
        JComboBox<ImageArea.Mode> uiChooser = new JComboBox<>(ImageArea.Mode.values());
        uiChooser.setSelectedItem(ImageArea.getMode());
        uiChooser.setName("uiChooser");
        gbh.addLabelAndControlNoStretch("Images In: ", uiChooser);
        uiChooser.addActionListener(e -> {
            ImageArea.Mode mode = (ImageArea.Mode) uiChooser.getSelectedItem();
            ImageArea.changeUI(mode);
        });
    }

    private void addThumbSizeChooser(GridBagHelper gbh) {
        thumbSizeCB = new JComboBox<>(new Item[]{
            new Item("24x24 pixels", 24),
            new Item("48x48 pixels", 48),
            new Item("72x72 pixels", 72),
            new Item("96x96 pixels", 96),
        });
        thumbSizeCB.setName("thumbSizeCB");

        int currentSize = Thumbnails.getMaxSize();
        for (int i = 0; i < thumbSizeCB.getItemCount(); i++) {
            if (thumbSizeCB.getItemAt(i).valueIs(currentSize)) {
                thumbSizeCB.setSelectedIndex(i);
                break;
            }
        }

        gbh.addLabelAndControlNoStretch("Layer/Mask Thumb Sizes: ", thumbSizeCB);
        thumbSizeCB.addActionListener(e -> updateThumbSize());
    }

    private JPanel createMousePanel() {
        var mousePanel = new JPanel(new BorderLayout());
        // put the contents to the north of a border layout,
        // so that it isn't stretched vertically
        var contents = new JPanel(new GridBagLayout());
        mousePanel.add(contents, BorderLayout.NORTH);

        var gbh = new GridBagHelper(contents);

        zoomMethodCB = new JComboBox<>(MouseZoomMethod.values());
        zoomMethodCB.setSelectedItem(MouseZoomMethod.ACTIVE);
        zoomMethodCB.setName("zoomMethod");
        gbh.addLabelAndControlNoStretch("Zoom with:", zoomMethodCB);

        panMethodCB = new JComboBox<>(PanMethod.values());
        panMethodCB.setSelectedItem(PanMethod.ACTIVE);
        panMethodCB.setName("panMethod");
        gbh.addLabelAndControlNoStretch("Pan with:", panMethodCB);

        snapCB = new JCheckBox("", AppPreferences.getFlag(AppPreferences.FLAG_PIXEL_SNAP));
        snapCB.setToolTipText("Snap vector tools to the pixel grid");
        gbh.addLabelAndControl("Snap Vector Tools to Pixels:", snapCB);

        mousePanel.setBorder(PANEL_PADDING);
        return mousePanel;
    }

    private static JPanel createGuidesPanel() {
        var guidesPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(guidesPanel);
        configureGuideStyle(AppPreferences.getGuideStyle(),
            "Guide", "guideStyleCB", gbh);
        configureGuideStyle(AppPreferences.getCropGuideStyle(),
            "Cropping Guide", "cropGuideStyleCB", gbh);
        guidesPanel.setBorder(PANEL_PADDING);
        return guidesPanel;
    }

    /**
     * Configures either regular guides or composition guides in the crop tool.
     */
    private static void configureGuideStyle(GuideStyle guideStyle, String labelPrefix, String comboName, GridBagHelper gbh) {
        var guideColorSwatch = new ColorSwatch(guideStyle.getColorA(), 20);
        var guideStyleCB = new JComboBox<>(GuideStrokeType.values());
        guideStyleCB.setName(comboName);
        guideStyleCB.setSelectedItem(guideStyle.getStrokeType());

        gbh.addLabelAndControlNoStretch(labelPrefix + " Color: ", guideColorSwatch);
        gbh.addLabelAndControlNoStretch(labelPrefix + " Style: ", guideStyleCB);

        new ColorPickerHelper(guideColorSwatch, e -> {
            guideStyle.setColorA(guideColorSwatch.getForeground());
            ImageArea.getUI().repaint();
        });

        guideStyleCB.addActionListener(e -> {
            guideStyle.setStrokeType((GuideStrokeType) guideStyleCB.getSelectedItem());
            ImageArea.getUI().repaint();
        });
    }

    private JPanel createAdvancedPanel() {
        var advancedPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(advancedPanel);

        addNativeChoosersCB(gbh);
        addUndoLevelsChooser(gbh);
        addMagickDirField(gbh);
        addGmicDirField(gbh);
        addExperimentalCB(gbh);

        advancedPanel.setBorder(PANEL_PADDING);
        return advancedPanel;
    }

    private void addNativeChoosersCB(GridBagHelper gbh) {
        nativeChoosersCB = new JCheckBox("", FileChoosers.useNativeDialogs());
        // no action listener, set only when OK is pressed
        gbh.addLabelAndControl("Use System File Choosers:", nativeChoosersCB);
    }

    private void addUndoLevelsChooser(GridBagHelper gbh) {
        undoLevelsTF = new JTextField(3);
        undoLevelsTF.setName("undoLevelsTF");
        undoLevelsTF.setText(String.valueOf(History.getUndoLevels()));
        gbh.addLabelAndControl(UNDO_LEVELS_LABEL + ": ",
            TextFieldValidator.createNonNegativeIntLayer(UNDO_LEVELS_LABEL,
                undoLevelsTF));
    }

    private void addMagickDirField(GridBagHelper gbh) {
        magickDirTF = new JTextField(AppPreferences.magickDirName);
        magickDirTF.setColumns(10);
        gbh.addLabelAndControl(IMAGEMAGICK_FOLDER_LABEL + ": ", magickDirTF);
    }

    private void addGmicDirField(GridBagHelper gbh) {
        gmicDirTF = new JTextField(AppPreferences.gmicDirName);
        gmicDirTF.setColumns(10);
        gbh.addLabelAndControl(GMIC_FOLDER_LABEL + ": ", gmicDirTF);
    }

    private void addExperimentalCB(GridBagHelper gbh) {
        experimentalCB = new JCheckBox("", Features.enableExperimental);
        gbh.addLabelAndControl("Enable Experimental Features:", experimentalCB);
    }

    // validates and applies settings that are not handled by immediate action listeners
    private boolean validateAndApply(JDialog d) {
        // validate all fields first
        ValidationResult result = TextFieldValidator.hasNonNegativeInt(undoLevelsTF, UNDO_LEVELS_LABEL)
            .checkOptionalDir(magickDirTF.getText(), IMAGEMAGICK_FOLDER_LABEL)
            .checkOptionalDir(gmicDirTF.getText(), GMIC_FOLDER_LABEL);

        // if there are errors, show them all at once and stop
        if (!result.isValid()) {
            result.showErrorDialog(d);
            return false; // keep dialog open
        }

        // apply undo levels (parsing is safe now because validation passed)
        // (we don't want to continuously set the undo levels
        // as the user edits the text field, because low levels
        // erase the history, therefore it is set here)
        int undoLevels = parseInt(undoLevelsTF.getText().trim());
        History.setUndoLevels(undoLevels);

        // apply remaining settings
        // (these can't be set interactively => set them here)
        AppPreferences.magickDirName = magickDirTF.getText().trim();
        AppPreferences.gmicDirName = gmicDirTF.getText().trim();
        MouseZoomMethod.changeTo((MouseZoomMethod) zoomMethodCB.getSelectedItem());
        PanMethod.changeTo((PanMethod) panMethodCB.getSelectedItem());
        View.snappingSettingChanged(snapCB.isSelected());
        FileChoosers.setUseNativeDialogs(nativeChoosersCB.isSelected());
        Features.enableExperimental(experimentalCB.isSelected());

        return true; // valid, dialog can be closed
    }

    private void updateThumbSize() {
        int newSize = ((Item) thumbSizeCB.getSelectedItem()).value();
        Thumbnails.updateThumbSize(newSize);
    }

    private void setupRememberingTabSelection() {
        if (lastSelectedTabIndex != 0) {
            setSelectedIndex(lastSelectedTabIndex);
        }
        addChangeListener(e ->
            lastSelectedTabIndex = getSelectedIndex());
    }

    public static void showInDialog() {
        var prefPanel = new PreferencesPanel();

        // there's no Cancel button, and changes are applied
        // even if the dialog is canceled with Esc/X
        new DialogBuilder()
            .content(prefPanel)
            .noCancelButton()
            .title(i18n("preferences"))
            .okText(CLOSE_DIALOG)
            .validator(prefPanel::validateAndApply)
            .validateOnCancel()
            .show();
    }
}
