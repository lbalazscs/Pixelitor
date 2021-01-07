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

package pixelitor.colors;

import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.colors.palette.PalettePanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.ColorIcon;
import pixelitor.gui.utils.Themes;
import pixelitor.menus.MenuAction;
import pixelitor.tools.Tools;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.colors.Colors.selectColorWithDialog;

/**
 * A panel that contains the buttons for selecting
 * the foreground and background colors
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

    private Action randomizeColorsAction;
    private Action resetToDefaultAction;
    private Action swapColorsAction;

    public FgBgColorSelector(PixelitorWindow pw) {
        this.pw = pw;
        setLayout(null);

        int iconSize = BIG_BUTTON_SIZE - 4;
        Color fg = AppPreferences.loadFgColor();
        fgColorIcon = new ColorIcon(fg, iconSize, iconSize);
        initFGButton();

        Color bg = AppPreferences.loadBgColor();
        bgColorIcon = new ColorIcon(bg, iconSize, iconSize);
        initBGButton();

        initResetDefaultsButton();
        initSwapColorsButton();
        initRandomizeButton();

        setupSize();

        setFgColor(fg, false);
        setBgColor(bg, false);

        setupKeyboardShortcuts();
    }

    private void initFGButton() {
        if (Themes.getCurrent().isNimbus()) {
            fgButton = new JButton();
        } else {
            fgButton = new JButton(fgColorIcon);
        }
        initButton(fgButton, "Set Foreground Color",
            BIG_BUTTON_SIZE, 2, FG_BUTTON_NAME, e -> fgButtonPressed());
        fgButton.setLocation(0, SMALL_BUTTON_VERTICAL_SPACE);

        fgButton.setComponentPopupMenu(createPopupMenu(true));
    }

    private void initBGButton() {
        if (Themes.getCurrent().isNimbus()) {
            bgButton = new JButton();
        } else {
            bgButton = new JButton(bgColorIcon);
        }
        initButton(bgButton, "Set Background Color",
            BIG_BUTTON_SIZE, 1, BG_BUTTON_NAME, e -> bgButtonPressed());
        bgButton.setLocation(BIG_BUTTON_SIZE / 2,
            SMALL_BUTTON_VERTICAL_SPACE + BIG_BUTTON_SIZE / 2);

        bgButton.setComponentPopupMenu(createPopupMenu(false));
    }

    private JPopupMenu createPopupMenu(boolean fg) {
        JPopupMenu popup = new JPopupMenu();
        String selectorName = fg ? "Foreground" : "Background";
        String otherName = fg ? "Background" : "Foreground";

        popup.add(new MenuAction(selectorName + " Color Variations...") {
            @Override
            public void onClick() {
                if (fg) {
                    PalettePanel.showFGVariationsDialog(pw);
                } else {
                    PalettePanel.showBGVariationsDialog(pw);
                }
            }
        });

        popup.add(new MenuAction("HSB Mix with " + otherName + "...") {
            @Override
            public void onClick() {
                PalettePanel.showHSBMixDialog(pw, fg);
            }
        });

        popup.add(new MenuAction("RGB Mix with " + otherName + "...") {
            @Override
            public void onClick() {
                PalettePanel.showRGBMixDialog(pw, fg);
            }
        });

        ColorHistory history = fg ? ColorHistory.FOREGROUND : ColorHistory.BACKGROUND;
        popup.add(new MenuAction(selectorName + " Color History...") {
            @Override
            public void onClick() {
                history.showDialog(pw, ColorSwatchClickHandler.STANDARD);
            }
        });

        popup.addSeparator();

        Colors.setupCopyColorPopupMenu(popup,
            () -> fg ? getFgColor() : getBgColor());

        Colors.setupPasteColorPopupMenu(popup, pw, color -> {
            if (fg) {
                setFgColor(color, true);
            } else {
                setBgColor(color, true);
            }
        });

        return popup;
    }

    private void initResetDefaultsButton() {
        resetToDefaultAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setDefaultColors();
            }
        };
        JButton defaultsButton = new JButton();
        initButton(defaultsButton, "Reset Default Colors (D)",
            SMALL_BUTTON_SIZE, 1, DEFAULTS_BUTTON_NAME,
            resetToDefaultAction);
        defaultsButton.setLocation(0, 0);
    }

    public void setDefaultColors() {
        setFgColor(BLACK, false);
        setBgColor(WHITE, true);
    }

    private void initSwapColorsButton() {
        swapColorsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                swapColors();
            }
        };
        JButton swapButton = new JButton();
        initButton(swapButton, "Swap Colors (X)",
            SMALL_BUTTON_SIZE, 1, SWAP_BUTTON_NAME, swapColorsAction);
        swapButton.setLocation(SMALL_BUTTON_SIZE, 0);
    }

    private void swapColors() {
        if (layerMaskEditing) {
            Color tmpFgColor = maskFgColor;
            setFgColor(maskBgColor, false);
            setBgColor(tmpFgColor, true);
        } else {
            Color tmpFgColor = fgColor;
            setFgColor(bgColor, false);
            setBgColor(tmpFgColor, true);
        }
    }

    private void initRandomizeButton() {
        randomizeColorsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFgColor(Rnd.createRandomColor(), false);
                setBgColor(Rnd.createRandomColor(), true);
            }
        };

        JButton randomizeButton = new JButton();
        initButton(randomizeButton, "Randomize Colors (R)",
            SMALL_BUTTON_SIZE, 1, RANDOMIZE_BUTTON_NAME, randomizeColorsAction);
        randomizeButton.setLocation(2 * SMALL_BUTTON_SIZE, 0);
    }

    private void setupSize() {
        int preferredWidth = (int) (BIG_BUTTON_SIZE * 1.5);
        int preferredHeight = preferredWidth + SMALL_BUTTON_VERTICAL_SPACE;
        var dim = new Dimension(preferredWidth, preferredHeight);
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
    }

    private void initButton(JButton button, String toolTip,
                            int size, int layer,
                            String name, ActionListener action) {
        button.setSize(size, size);
        button.addActionListener(action);
//        button.setContentAreaFilled(false);
//        button.setOpaque(true);
        button.setBorderPainted(true);
//        button.setDefaultCapable(false);

        button.setToolTipText(toolTip);
        button.setName(name);
        add(button, Integer.valueOf(layer));
    }

    private void fgButtonPressed() {
        Color selectedColor = layerMaskEditing ? maskFgColor : fgColor;
        selectColorWithDialog(pw, GUIText.FG_COLOR,
            selectedColor, false,
            color -> setFgColor(color, true));
    }

    private void bgButtonPressed() {
        Color selectedColor = layerMaskEditing ? maskBgColor : bgColor;
        selectColorWithDialog(pw, GUIText.BG_COLOR,
            selectedColor, false,
            color -> setBgColor(color, true));
    }

    /**
     * Return the user-visible foreground color
     */
    public Color getFgColor() {
        return layerMaskEditing ? maskFgColor : fgColor;
    }

    /**
     * Return the user-visible background color
     */
    public Color getBgColor() {
        return layerMaskEditing ? maskBgColor : bgColor;
    }

    /**
     * Return the actual foreground color, even in mask editing mode
     */
    public Color getRealFgColor() {
        return fgColor;
    }

    /**
     * Return the actual background color, even in mask editing mode
     */
    public Color getRealBgColor() {
        return bgColor;
    }

    public void setFgColor(Color c, boolean notifyListeners) {
        Color newColor;
        if (layerMaskEditing) {
            maskFgColor = Colors.toGray(c);
            newColor = maskFgColor;
        } else {
            fgColor = c;
            newColor = fgColor;
        }

        setFgButtonColor(newColor);
        ColorHistory.FOREGROUND.add(newColor);
        if (notifyListeners) {
            Tools.fgBgColorsChanged();
        }
    }

    private void setFgButtonColor(Color newColor) {
        fgColorIcon.setColor(newColor);
        fgButton.setBackground(newColor);
    }

    public void setBgColor(Color c, boolean notifyListeners) {
        Color newColor;
        if (layerMaskEditing) {
            maskBgColor = Colors.toGray(c);
            newColor = maskBgColor;
        } else {
            bgColor = c;
            newColor = bgColor;
        }

        setBgButtonColor(newColor);
        ColorHistory.BACKGROUND.add(newColor);
        if (notifyListeners) {
            Tools.fgBgColorsChanged();
        }
    }

    private void setBgButtonColor(Color newColor) {
        bgColorIcon.setColor(newColor);
        bgButton.setBackground(newColor);
    }

    private void setupKeyboardShortcuts() {
        GlobalEvents.addHotKey('D', resetToDefaultAction);
        GlobalEvents.addHotKey('X', swapColorsAction);
        GlobalEvents.addHotKey('R', randomizeColorsAction);
    }

    public void setLayerMaskEditing(boolean layerMaskEditing) {
        boolean oldValue = this.layerMaskEditing;
        this.layerMaskEditing = layerMaskEditing;

        if (oldValue != layerMaskEditing) {
            // force the redrawing of colors
            if (layerMaskEditing) {
                setFgColor(maskFgColor, false);
                setBgColor(maskBgColor, false);
            } else {
                setFgColor(fgColor, false);
                setBgColor(bgColor, false);
            }
        }
    }

    public void randomize() {
        randomizeColorsAction.actionPerformed(null);
    }

    public void themeChanged() {
        remove(fgButton);
        remove(bgButton);
        initFGButton();
        initBGButton();
        setFgButtonColor(fgColor);
        setBgButtonColor(bgColor);
    }
}
