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

package pixelitor.colors;

import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.colors.palette.PalettePanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.ColorIcon;
import pixelitor.gui.utils.TaskAction;
import pixelitor.gui.utils.Themes;
import pixelitor.tools.Tools;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.colors.Colors.selectColorWithDialog;

/**
 * A panel that contains the buttons for selecting
 * the foreground and background colors.
 */
public class FgBgColorSelector extends JLayeredPane {
    public static final String RANDOMIZE_BUTTON_NAME = "randomizeColorsButton";
    public static final String FG_BUTTON_NAME = "fgButton";
    public static final String BG_BUTTON_NAME = "bgButton";
    public static final String DEFAULTS_BUTTON_NAME = "resetColorsButton";
    public static final String SWAP_BUTTON_NAME = "swapColorsButton";

    private final PixelitorWindow pw;
    private JButton fgButton;
    private JButton bgButton;
    private final ColorIcon fgColorIcon;
    private final ColorIcon bgColorIcon;

    private Color fgColor = BLACK;
    private Color bgColor = WHITE;

    // in layer mask editing mode only grayscale colors are shown
    private boolean layerMaskEditing = false;

    // the grayscale colors used while in mask editing mode
    private Color maskFgColor = BLACK;
    private Color maskBgColor = WHITE;

    private static final int BIG_BUTTON_SIZE = 30;
    private static final int SMALL_BUTTON_SIZE = 15;
    private static final int SMALL_BUTTON_VERTICAL_SPACE = 15;
    private static final int ICON_PADDING = 2;
    private static final int ICON_SIZE = BIG_BUTTON_SIZE - ICON_PADDING * 2;

    private Action randomizeColorsAction;
    private Action resetToDefaultAction;
    private Action swapColorsAction;

    public FgBgColorSelector(PixelitorWindow pw) {
        this.pw = pw;
        setLayout(null);

        Color initialFg = AppPreferences.loadFgColor();
        fgColorIcon = new ColorIcon(initialFg, ICON_SIZE, ICON_SIZE);
        initFgButton();

        Color initialBg = AppPreferences.loadBgColor();
        bgColorIcon = new ColorIcon(initialBg, ICON_SIZE, ICON_SIZE);
        initBgButton();

        initResetButton();
        initSwapColorsButton();
        initRandomizeButton();

        configureSize();

        setFgColor(initialFg, false);
        setBgColor(initialBg, false);

        setupKeyboardShortcuts();
        Themes.addThemeChangeListener(theme -> themeChanged());
    }

    private void initFgButton() {
        fgButton = createColorButton(fgColorIcon);

        initButton(fgButton, "Set Foreground Color",
            BIG_BUTTON_SIZE, 2, FG_BUTTON_NAME, e -> showColorDialog(true));
        fgButton.setLocation(0, SMALL_BUTTON_VERTICAL_SPACE);
        fgButton.setComponentPopupMenu(createPopupMenu(true));
    }

    private void initBgButton() {
        bgButton = createColorButton(bgColorIcon);

        initButton(bgButton, "Set Background Color",
            BIG_BUTTON_SIZE, 1, BG_BUTTON_NAME, e -> showColorDialog(false));
        bgButton.setLocation(BIG_BUTTON_SIZE / 2,
            SMALL_BUTTON_VERTICAL_SPACE + BIG_BUTTON_SIZE / 2);
        bgButton.setComponentPopupMenu(createPopupMenu(false));
    }

    private static JButton createColorButton(Icon icon) {
        return Themes.getActive().isNimbus() ? new JButton() : new JButton(icon);
    }

    private JPopupMenu createPopupMenu(boolean fg) {
        JPopupMenu popup = new JPopupMenu();
        String activeName = fg ? "Foreground" : "Background";
        String mixName = fg ? "Background" : "Foreground";

        popup.add(new TaskAction(activeName + " Color Variations...", () ->
            PalettePanel.showVariationsDialog(pw, fg)));

        popup.add(new TaskAction("HSB Mix with " + mixName + "...", () ->
            PalettePanel.showHSBMixDialog(pw, fg)));

        popup.add(new TaskAction("RGB Mix with " + mixName + "...", () ->
            PalettePanel.showRGBMixDialog(pw, fg)));

        popup.add(new TaskAction("Color History...", () ->
            ColorHistory.INSTANCE.showDialog(pw, ColorSwatchClickHandler.STANDARD, false)));

        popup.addSeparator();

        popup.add(Colors.createCopyColorAction(() -> fg ? getFgColor() : getBgColor()));

        popup.add(Colors.createPasteColorAction(pw, color -> {
            if (fg) {
                setFgColor(color, true);
            } else {
                setBgColor(color, true);
            }
        }));

        return popup;
    }

    private void initResetButton() {
        resetToDefaultAction = new TaskAction(this::setDefaultColors);

        JButton resetButton = new JButton();
        initButton(resetButton, "Reset to Default Colors (D)",
            SMALL_BUTTON_SIZE, 1, DEFAULTS_BUTTON_NAME,
            resetToDefaultAction);
        resetButton.setLocation(0, 0);
    }

    public void setDefaultColors() {
        setFgColor(BLACK, false);
        setBgColor(WHITE, true);
    }

    private void initSwapColorsButton() {
        swapColorsAction = new TaskAction(this::swapColors);

        JButton swapButton = new JButton();
        initButton(swapButton, "Swap Colors (X)",
            SMALL_BUTTON_SIZE, 1, SWAP_BUTTON_NAME, swapColorsAction);
        swapButton.setLocation(SMALL_BUTTON_SIZE, 0);
    }

    private void swapColors() {
        Color newFgColor = getBgColor();
        Color newBgColor = getFgColor();

        // no history and notify the listeners only once
        setFgColor(newFgColor, false, false);
        setBgColor(newBgColor, true, false);
    }

    private void initRandomizeButton() {
        randomizeColorsAction = new TaskAction(this::randomizeColors);

        JButton randomizeButton = new JButton();
        initButton(randomizeButton, "Randomize Colors (R)",
            SMALL_BUTTON_SIZE, 1, RANDOMIZE_BUTTON_NAME, randomizeColorsAction);
        randomizeButton.setLocation(2 * SMALL_BUTTON_SIZE, 0);
    }

    private void configureSize() {
        int preferredWidth = (int) (BIG_BUTTON_SIZE * 1.5);
        int preferredHeight = preferredWidth + SMALL_BUTTON_VERTICAL_SPACE;
        Dimension size = new Dimension(preferredWidth, preferredHeight);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    private void initButton(JButton button, String toolTip,
                            int size, int layer,
                            String name, ActionListener action) {
        button.setSize(size, size);
        button.addActionListener(action);
        button.setToolTipText(toolTip);
        button.setName(name);
        add(button, Integer.valueOf(layer));
    }

    private void showColorDialog(boolean fg) {
        Color currentColor = fg ? getFgColor() : getBgColor();
        String title = fg ? GUIText.FG_COLOR : GUIText.BG_COLOR;
        Consumer<Color> onColorChange = color -> setColor(color, fg, true, true);

        selectColorWithDialog(pw, title,
            currentColor, false, onColorChange);
    }

    /**
     * Returns the currently visible foreground color.
     */
    public Color getFgColor() {
        return layerMaskEditing ? maskFgColor : fgColor;
    }

    /**
     * Returns the currently visible background color.
     */
    public Color getBgColor() {
        return layerMaskEditing ? maskBgColor : bgColor;
    }

    /**
     * Returns the actual foreground color, ignoring mask editing mode.
     */
    public Color getActualFgColor() {
        return fgColor;
    }

    /**
     * Returns the actual background color, ignoring mask editing mode.
     */
    public Color getActualBgColor() {
        return bgColor;
    }

    public void setFgColor(Color color, boolean notifyListeners) {
        setFgColor(color, notifyListeners, true);
    }

    private void setFgColor(Color color, boolean notifyListeners, boolean addHistory) {
        setColor(color, true, notifyListeners, addHistory);
    }

    public void setBgColor(Color color, boolean notifyListeners) {
        setBgColor(color, notifyListeners, true);
    }

    private void setBgColor(Color color, boolean notifyListeners, boolean addHistory) {
        setColor(color, false, notifyListeners, addHistory);
    }

    private void setColor(Color color, boolean fg,
                          boolean notifyListeners, boolean addHistory) {
        Color displayColor;
        if (layerMaskEditing) {
            displayColor = Colors.toGray(color);
            if (fg) {
                maskFgColor = displayColor;
            } else {
                maskBgColor = displayColor;
            }
        } else {
            displayColor = color;
            if (fg) {
                fgColor = displayColor;
            } else {
                bgColor = displayColor;
            }
        }

        if (fg) {
            updateFgButtonColor(displayColor);
        } else {
            updateBgButtonColor(displayColor);
        }

        if (addHistory) {
            ColorHistory.remember(displayColor);
        }

        if (notifyListeners) {
            notifyListeners();
        }
    }

    private static void notifyListeners() {
        Tools.fgBgColorsChanged();
    }

    private void updateFgButtonColor(Color newColor) {
        fgColorIcon.setColor(newColor);
        fgButton.setBackground(newColor);
    }

    private void updateBgButtonColor(Color newColor) {
        bgColorIcon.setColor(newColor);
        bgButton.setBackground(newColor);
    }

    private void setupKeyboardShortcuts() {
        GlobalEvents.registerHotkey('D', resetToDefaultAction);
        GlobalEvents.registerHotkey('X', swapColorsAction);
        GlobalEvents.registerHotkey('R', randomizeColorsAction);
    }

    /**
     * Notifies this component that layer-mask editing mode has changed.
     */
    public void maskEditingChanged(boolean maskEditing) {
        if (this.layerMaskEditing == maskEditing) {
            return;
        }
        this.layerMaskEditing = maskEditing;

        // update the button colors to reflect the new mode and notify listeners
        updateFgButtonColor(getFgColor());
        updateBgButtonColor(getBgColor());
        notifyListeners();
    }

    public void randomizeColors() {
        setFgColor(Rnd.createRandomColor(), false);
        setBgColor(Rnd.createRandomColor(), true);
    }

    /**
     * Re-initializes components when the application theme has changed.
     */
    public void themeChanged() {
        remove(fgButton);
        remove(bgButton);
        initFgButton();
        initBgButton();
        updateFgButtonColor(getFgColor());
        updateBgButtonColor(getBgColor());
    }
}
