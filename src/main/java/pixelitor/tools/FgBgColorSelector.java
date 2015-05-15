/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import com.bric.swing.ColorPicker;
import pixelitor.GlobalKeyboardWatch;
import pixelitor.PixelitorWindow;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * A panel that contains the buttons for selecting the foreground and background colors
 */
public class FgBgColorSelector extends JLayeredPane {
    private JButton fgButton;
    private JButton bgButton;

    private Color fgColor = BLACK;
    private Color bgColor = WHITE;

    private static final int BIG_BUTTON_SIZE = 30;
    private static final int SMALL_BUTTON_SIZE = 15;
    private static final int SMALL_BUTTON_VERTICAL_SPACE = 15;

    public static final FgBgColorSelector INSTANCE = new FgBgColorSelector();

    private Action randomizeColorsAction;
    private Action resetToDefaultAction;
    private Action swapColorsAction;

    private final boolean layerMaskEditing = false;

    private FgBgColorSelector() {
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
    }

    private void initBGButton() {
        bgButton = initButton("Set Background Color", BIG_BUTTON_SIZE, 1);
        bgButton.addActionListener(e -> bgButtonPressed());
        bgButton.setLocation(BIG_BUTTON_SIZE / 2, SMALL_BUTTON_VERTICAL_SPACE + BIG_BUTTON_SIZE / 2);
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
                Color tmpFgColor = fgColor;

                setFgColor(bgColor);
                setBgColor(tmpFgColor);
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
                setFgColor(ImageUtils.getRandomColor(false));
                setBgColor(ImageUtils.getRandomColor(false));
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
        Color c = ColorPicker.showDialog(PixelitorWindow.getInstance(), "Set background color", bgColor, false);
        if (c != null) {
            setBgColor(c);
        }
    }

    private void fgButtonPressed() {
        Color c = ColorPicker.showDialog(PixelitorWindow.getInstance(), "Set foreground color", fgColor, false);
        if (c != null) {
            setFgColor(c);
        }
    }

    private Color getBgColor() {
        return getUsedColor(bgColor);
    }

    public static Color colorToGray(Color c) {
        int rgb = c.getRGB();
//        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        int average = r + g + b / 3;
        return new Color((0xFF << 24) | (average << 16) | (average << 8) | average);
    }

    private Color getFgColor() {
        return getUsedColor(fgColor);
    }

    private Color getUsedColor(Color c) {
        if (layerMaskEditing) {
            return colorToGray(c);
        }
        return c;
    }

    public static Color getFG() {
        return INSTANCE.getFgColor();
    }

    public static Color getBG() {
        return INSTANCE.getBgColor();
    }

    public static void setFG(Color c) {
        INSTANCE.setFgColor(c);
    }

    public static void setBG(Color c) {
        INSTANCE.setBgColor(c);
    }

    public static void setRandomColors() {
        INSTANCE.randomizeColorsAction.actionPerformed(null);
    }

    public void setFgColor(Color c) {
        Color old = fgColor;
        fgColor = c;

        Color usedColor = getUsedColor(fgColor);

        fgButton.setBackground(usedColor);
        if (old != null) {
            firePropertyChange("FG", old, fgColor);
        }
    }

    public void setBgColor(Color c) {
        Color old = bgColor;
        bgColor = c;

        Color usedColor = getUsedColor(bgColor);

        bgButton.setBackground(usedColor);
        if (old != null) {
            firePropertyChange("BG", old, bgColor);
        }
    }

    private void setupKeyboardShortcuts() {
        GlobalKeyboardWatch.addKeyboardShortCut('d', true, "reset", resetToDefaultAction);
        GlobalKeyboardWatch.addKeyboardShortCut('x', true, "switch", swapColorsAction);
        GlobalKeyboardWatch.addKeyboardShortCut('r', true, "randomize", randomizeColorsAction);
    }
}
