/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.event.InputEvent;

import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * A convenience class for keeping track of keyboard shortcuts
 */
public class Keys {
    // Ctrl on Win/Linux, Command on Mac
    private static final int CTRL
        = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    private static final int ALT = InputEvent.ALT_DOWN_MASK;
    private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;

    public static final KeyStroke PAGE_DOWN = getKeyStroke(VK_PAGE_DOWN, 0);
    public static final KeyStroke PAGE_UP = getKeyStroke(VK_PAGE_UP, 0);
    public static final KeyStroke CTRL_PAGE_DOWN = getKeyStroke(VK_PAGE_DOWN, CTRL);
    public static final KeyStroke CTRL_PAGE_UP = getKeyStroke(VK_PAGE_UP, CTRL);
    public static final KeyStroke CTRL_ALT_PAGE_DOWN = getKeyStroke(VK_PAGE_DOWN, CTRL | ALT);
    public static final KeyStroke CTRL_ALT_PAGE_UP = getKeyStroke(VK_PAGE_UP, CTRL | ALT);

    //    public static final KeyStroke CTRL_A = getKeyStroke('A', CTRL);
    public static final KeyStroke CTRL_B = getKeyStroke('B', CTRL);
    public static final KeyStroke CTRL_C = getKeyStroke('C', CTRL);
    public static final KeyStroke CTRL_D = getKeyStroke('D', CTRL);
    public static final KeyStroke CTRL_E = getKeyStroke('E', CTRL);
    public static final KeyStroke CTRL_F = getKeyStroke('F', CTRL);
    public static final KeyStroke CTRL_G = getKeyStroke('G', CTRL);
    public static final KeyStroke CTRL_H = getKeyStroke('H', CTRL);
    public static final KeyStroke CTRL_I = getKeyStroke('I', CTRL);
    public static final KeyStroke CTRL_J = getKeyStroke('J', CTRL);
    public static final KeyStroke CTRL_K = getKeyStroke('K', CTRL);
    public static final KeyStroke CTRL_L = getKeyStroke('L', CTRL);
    public static final KeyStroke CTRL_M = getKeyStroke('M', CTRL);
    public static final KeyStroke CTRL_N = getKeyStroke('N', CTRL);
    public static final KeyStroke CTRL_O = getKeyStroke('O', CTRL);
    public static final KeyStroke CTRL_P = getKeyStroke('P', CTRL);
    public static final KeyStroke CTRL_R = getKeyStroke('R', CTRL);
    public static final KeyStroke CTRL_S = getKeyStroke('S', CTRL);
    public static final KeyStroke CTRL_T = getKeyStroke('T', CTRL);
    public static final KeyStroke CTRL_U = getKeyStroke('U', CTRL);
    public static final KeyStroke CTRL_V = getKeyStroke('V', CTRL);
    public static final KeyStroke CTRL_W = getKeyStroke('W', CTRL);
    public static final KeyStroke CTRL_Z = getKeyStroke('Z', CTRL);

    public static final KeyStroke CTRL_1 = getKeyStroke('1', CTRL);
    public static final KeyStroke CTRL_2 = getKeyStroke('2', CTRL);
    public static final KeyStroke CTRL_3 = getKeyStroke('3', CTRL);
    public static final KeyStroke CTRL_4 = getKeyStroke('4', CTRL);

    public static final KeyStroke CTRL_ALT_D = getKeyStroke('D', CTRL | ALT);
    public static final KeyStroke CTRL_ALT_E = getKeyStroke('E', CTRL | ALT);
    public static final KeyStroke CTRL_ALT_F = getKeyStroke('F', CTRL | ALT);
    public static final KeyStroke CTRL_ALT_G = getKeyStroke('G', CTRL | ALT);
    public static final KeyStroke CTRL_ALT_I = getKeyStroke('I', CTRL | ALT);

    // Ctrl+Alt+L is lock screen in Linux!
    //    public static final KeyStroke CTRL_ALT_L = getKeyStroke('L', CTRL | ALT);
    public static final KeyStroke CTRL_ALT_O = getKeyStroke('O', CTRL | ALT);
    //    public static final KeyStroke CTRL_ALT_R = getKeyStroke('R', CTRL | ALT);
    public static final KeyStroke CTRL_ALT_S = getKeyStroke('S', CTRL | ALT);
    public static final KeyStroke CTRL_ALT_V = getKeyStroke('V', CTRL | ALT);
    public static final KeyStroke CTRL_ALT_W = getKeyStroke('W', CTRL | ALT);

    public static final KeyStroke CTRL_SHIFT_C = getKeyStroke('C', CTRL | SHIFT);
    public static final KeyStroke CTRL_SHIFT_E = getKeyStroke('E', CTRL | SHIFT);
    public static final KeyStroke CTRL_SHIFT_F = getKeyStroke('F', CTRL | SHIFT);
    public static final KeyStroke CTRL_SHIFT_G = getKeyStroke('G', CTRL | SHIFT);
    public static final KeyStroke CTRL_SHIFT_I = getKeyStroke('I', CTRL | SHIFT);
    //    public static final KeyStroke CTRL_SHIFT_L = getKeyStroke('L', CTRL | SHIFT);
//    public static final KeyStroke CTRL_SHIFT_R = getKeyStroke('R', CTRL | SHIFT);
    public static final KeyStroke CTRL_SHIFT_S = getKeyStroke('S', CTRL | SHIFT);
    public static final KeyStroke CTRL_SHIFT_V = getKeyStroke('V', CTRL | SHIFT);
    public static final KeyStroke CTRL_SHIFT_Z = getKeyStroke('Z', CTRL | SHIFT);

    public static final KeyStroke CTRL_SHIFT_ALT_E = getKeyStroke('E', CTRL | ALT | SHIFT);
//    public static final KeyStroke CTRL_SHIFT_ALT_L = getKeyStroke('L', CTRL | ALT | SHIFT);
//    public static final KeyStroke CTRL_SHIFT_ALT_R = getKeyStroke('R', CTRL | ALT | SHIFT);

    public static final KeyStroke CTRL_MINUS = getKeyStroke(VK_MINUS, CTRL);
    public static final KeyStroke CTRL_PLUS = getKeyStroke(VK_PLUS, CTRL);
    public static final KeyStroke FIT_SPACE_KEY = getKeyStroke(VK_0, CTRL | ALT);
    public static final KeyStroke ACTUAL_PIXELS_KEY = getKeyStroke(VK_0, CTRL);

    public static final KeyStroke DELETE = getKeyStroke(VK_DELETE, 0);
    public static final KeyStroke CTRL_BACKSPACE = getKeyStroke(VK_BACK_SPACE, CTRL);
    public static final KeyStroke ALT_BACKSPACE = getKeyStroke(VK_BACK_SPACE, ALT);

    public static final KeyStroke T = getKeyStroke('T');
    public static final KeyStroke F3 = getKeyStroke(VK_F3, 0);
    public static final KeyStroke F6 = getKeyStroke(VK_F6, 0);
    public static final KeyStroke F7 = getKeyStroke(VK_F7, 0);
    public static final KeyStroke F8 = getKeyStroke(VK_F8, 0);
    public static final KeyStroke F12 = getKeyStroke(VK_F12, 0);

    public static final KeyStroke CTRL_NUMPAD_PLUS = getKeyStroke(VK_ADD, CTRL);
    public static final KeyStroke CTRL_NUMPAD_MINUS = getKeyStroke(VK_SUBTRACT, CTRL);
    public static final KeyStroke CTRL_SHIFT_EQUALS = getKeyStroke(VK_EQUALS, CTRL | SHIFT);
    public static final KeyStroke CTRL_NUMPAD_0 = getKeyStroke(VK_NUMPAD0, CTRL);
    public static final KeyStroke CTRL_ALT_NUMPAD_0 = getKeyStroke(VK_NUMPAD0, CTRL | ALT);

    public static final KeyStroke ESC = getKeyStroke(VK_ESCAPE, 0);

    public static final KeyStroke CTRL_TAB = getKeyStroke("ctrl TAB");
    public static final KeyStroke CTRL_SHIFT_TAB = getKeyStroke("ctrl shift TAB");

    private Keys() {
        // do not instantiate
    }
}
