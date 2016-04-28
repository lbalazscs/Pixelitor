/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.colors.palette.VariationsPanel;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.menus.MenuAction;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.colors.ColorUtils.showColorPickerDialog;

/**
 * A panel that contains the buttons for selecting the foreground and background colors
 */
public class FgBgColorSelector extends JLayeredPane {
    private final PixelitorWindow pw;
    private JButton fgButton;
    private JButton bgButton;

    private Color fgColor = BLACK;
    private Color bgColor = WHITE;
    private Color maskFgColor = BLACK;
    private Color maskBgColor = WHITE;

    private static final int BIG_BUTTON_SIZE = 30;
    private static final int SMALL_BUTTON_SIZE = 15;
    private static final int SMALL_BUTTON_VERTICAL_SPACE = 15;

    public Action randomizeColorsAction;
    private Action resetToDefaultAction;
    private Action swapColorsAction;

    private boolean layerMaskEditing = false;

    public FgBgColorSelector(PixelitorWindow pw) {
        this.pw = pw;
        setLayout(null);

        initFGButton();
        initBGButton();
        initResetDefaultsButton();
        initSwapColorsButton();
        initRandomizeButton();

        setupSize();

        setFgColor(AppPreferences.loadFgColor());
        setBgColor(AppPreferences.loadBgColor());

        setupKeyboardShortcuts();
    }

    private void initFGButton() {
        fgButton = initButton("Set Foreground Color", BIG_BUTTON_SIZE, 2);
        fgButton.addActionListener(e -> fgButtonPressed());
        fgButton.setLocation(0, SMALL_BUTTON_VERTICAL_SPACE);

        fgButton.setComponentPopupMenu(createPopupMenu(true));
    }

    private void initBGButton() {
        bgButton = initButton("Set Background Color", BIG_BUTTON_SIZE, 1);
        bgButton.addActionListener(e -> bgButtonPressed());
        bgButton.setLocation(BIG_BUTTON_SIZE / 2, SMALL_BUTTON_VERTICAL_SPACE + BIG_BUTTON_SIZE / 2);

        bgButton.setComponentPopupMenu(createPopupMenu(false));
    }

    private JPopupMenu createPopupMenu(boolean fg) {
        JPopupMenu menu = new JPopupMenu();

        String variationsTitle = fg
                ? "Foreground Color Variations..."
                : "Background Color Variations...";
        menu.add(new MenuAction(variationsTitle) {
            @Override
            public void onClick() {
                if (fg) {
                    VariationsPanel.showFGVariationsDialog(pw);
                } else {
                    VariationsPanel.showBGVariationsDialog(pw);
                }
            }
        });

        String mixTitle = fg
                ? "HSB Mix with Background Variations..."
                : "HSB Mix with Foreground Variations...";
        menu.add(new MenuAction(mixTitle) {
            @Override
            public void onClick() {
                VariationsPanel.showHSBMixDialog(pw, fg);
            }
        });

        String rgbMixTitle = fg
                ? "RGB Mix with Background Variations..."
                : "RGB Mix with Foreground Variations...";
        menu.add(new MenuAction(rgbMixTitle) {
            @Override
            public void onClick() {
                VariationsPanel.showRGBMixDialog(pw, fg);
            }
        });

        String historyTitle = fg
                ? "Foreground Color History..."
                : "Background Color History...";
        menu.add(new MenuAction(historyTitle) {
            @Override
            public void onClick() {
                if (fg) {
                    ColorHistory.FOREGROUND.showDialog(pw,
                            ColorSwatchClickHandler.STANDARD);
                } else {
                    ColorHistory.BACKGROUND.showDialog(pw,
                            ColorSwatchClickHandler.STANDARD);
                }
            }
        });

        menu.addSeparator();

        ColorUtils.setupCopyColorPopupMenu(menu, () -> fg ? getFgColor() : getBgColor());

        ColorUtils.setupPasteColorPopupMenu(menu, pw, color -> {
            if (fg) {
                setFgColor(color);
            } else {
                setBgColor(color);
            }
        });

        return menu;
    }

    private void initResetDefaultsButton() {
        JButton defaultsButton = initButton("Reset Default Colors (D)", SMALL_BUTTON_SIZE, 1);
        defaultsButton.setLocation(0, 0);
        resetToDefaultAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFgColor(BLACK);
                setBgColor(WHITE);
            }
        };
        defaultsButton.addActionListener(resetToDefaultAction);
    }

    private void initSwapColorsButton() {
        JButton swapButton = initButton("Swap Colors (X)", SMALL_BUTTON_SIZE, 1);
        swapButton.setLocation(SMALL_BUTTON_SIZE, 0);
        swapColorsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (layerMaskEditing) {
                    Color tmpFgColor = maskFgColor;
                    setFgColor(maskBgColor);
                    setBgColor(tmpFgColor);
                } else {
                    Color tmpFgColor = fgColor;
                    setFgColor(bgColor);
                    setBgColor(tmpFgColor);
                }
            }
        };
        swapButton.addActionListener(swapColorsAction);
    }

    private void initRandomizeButton() {
        JButton randomizeButton = initButton("Randomize Colors (R)", SMALL_BUTTON_SIZE, 1);
        randomizeButton.setLocation(2 * SMALL_BUTTON_SIZE, 0);
        randomizeColorsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFgColor(ColorUtils.getRandomColor(false));
                setBgColor(ColorUtils.getRandomColor(false));
            }
        };
        randomizeButton.addActionListener(randomizeColorsAction);
    }

    private void setupSize() {
        int preferredHorizontalSize = (int) (BIG_BUTTON_SIZE * 1.5);
        int preferredVerticalSize = preferredHorizontalSize + SMALL_BUTTON_VERTICAL_SPACE;
        Dimension preferredDim = new Dimension(preferredHorizontalSize, preferredVerticalSize);
        setPreferredSize(preferredDim);
        setMinimumSize(preferredDim);
        setMaximumSize(preferredDim);
    }

    private JButton initButton(String toolTipText, int size, int addLayer) {
        JButton button = new JButton();
        button.setToolTipText(toolTipText);
        button.setSize(size, size);
        add(button, Integer.valueOf(addLayer));
        return button;
    }

    private void bgButtonPressed() {
        if (RandomGUITest.isRunning()) {
            return;
        }

        Color selectedColor = layerMaskEditing ? maskBgColor : bgColor;
        Color c = showColorPickerDialog(pw, "Set Background Color", selectedColor, false);

        if (c != null) { // OK was pressed
            setBgColor(c);
        }
    }

    private void fgButtonPressed() {
        if (RandomGUITest.isRunning()) {
            return;
        }

        Color selectedColor = layerMaskEditing ? maskFgColor : fgColor;
        Color c = showColorPickerDialog(pw, "Set Foreground Color", selectedColor, false);

        if (c != null) { // OK was pressed
            setFgColor(c);
        }
    }

    public Color getFgColor() {
        return layerMaskEditing ? maskFgColor : fgColor;
    }

    public Color getBgColor() {
        return layerMaskEditing ? maskBgColor : bgColor;
    }

    public void setFgColor(Color c) {
        Color newColor;
        if (layerMaskEditing) {
            maskFgColor = ColorUtils.colorToGray(c);
            newColor = maskFgColor;
        } else {
            fgColor = c;
            newColor = fgColor;
        }

        fgButton.setBackground(newColor);
        ColorHistory.FOREGROUND.add(newColor);
    }

    public void setBgColor(Color c) {
        Color newColor;
        if (layerMaskEditing) {
            maskBgColor = ColorUtils.colorToGray(c);
            newColor = maskBgColor;
        } else {
            bgColor = c;
            newColor = bgColor;
        }

        bgButton.setBackground(newColor);
        ColorHistory.BACKGROUND.add(newColor);
    }

    private void setupKeyboardShortcuts() {
        GlobalKeyboardWatch.addKeyboardShortCut('d', true, "reset", resetToDefaultAction);
        GlobalKeyboardWatch.addKeyboardShortCut('x', true, "switch", swapColorsAction);
        GlobalKeyboardWatch.addKeyboardShortCut('r', true, "randomize", randomizeColorsAction);
    }

    public void setLayerMaskEditing(boolean layerMaskEditing) {
        boolean oldValue = this.layerMaskEditing;
        this.layerMaskEditing = layerMaskEditing;

        if(oldValue != layerMaskEditing) {
            // force the redrawing of colors
            if (layerMaskEditing) {
                setFgColor(maskFgColor);
                setBgColor(maskBgColor);
            } else {
                setFgColor(fgColor);
                setBgColor(bgColor);
            }
        }
    }

    public void dumpState() {
        System.out.println("FgBgColorSelector::dumpState: layerMaskEditing = " + layerMaskEditing);
        System.out.println("FgBgColorSelector::dumpState: fgColor = " + fgColor + ", bgColor = " + bgColor);
        System.out.println("FgBgColorSelector::dumpState: maskFgColor = " + maskFgColor + ", maskBgColor = " + maskBgColor);
    }
}
