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

package pixelitor.utils;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_ADD;
import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_EQUALS;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_F12;
import static java.awt.event.KeyEvent.VK_F6;
import static java.awt.event.KeyEvent.VK_F7;
import static java.awt.event.KeyEvent.VK_MINUS;
import static java.awt.event.KeyEvent.VK_NUMPAD0;
import static java.awt.event.KeyEvent.VK_PLUS;
import static java.awt.event.KeyEvent.VK_SUBTRACT;
import static java.awt.event.KeyEvent.VK_TAB;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * A convenience class for keeping track of keyboard shortcuts
 */
public class Keys {
    // Ctrl on Win/Linux, Command on Mac
    private static final int MENU_CTRL_MASK
            = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    public static final KeyStroke CTRL_MINUS = getKeyStroke(VK_MINUS, MENU_CTRL_MASK);
    public static final KeyStroke CTRL_PLUS = getKeyStroke(VK_PLUS, MENU_CTRL_MASK);
    public static final KeyStroke FIT_SPACE_KEY = getKeyStroke(KeyEvent.VK_0, MENU_CTRL_MASK + ALT_DOWN_MASK);
    public static final KeyStroke ACTUAL_PIXELS_KEY = getKeyStroke(KeyEvent.VK_0, MENU_CTRL_MASK);

    public static final KeyStroke CTRL_ALT_R = getKeyStroke('R', MENU_CTRL_MASK + ALT_DOWN_MASK);
    public static final KeyStroke CTRL_ALT_L = getKeyStroke('L', MENU_CTRL_MASK + ALT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_ALT_R = getKeyStroke('R', MENU_CTRL_MASK + ALT_DOWN_MASK + SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_ALT_L = getKeyStroke('L', MENU_CTRL_MASK + ALT_DOWN_MASK + SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_R = getKeyStroke('R', MENU_CTRL_MASK + SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_L = getKeyStroke('L', MENU_CTRL_MASK + SHIFT_DOWN_MASK);

    public static final KeyStroke CTRL_SHIFT_ALT_E = getKeyStroke('E', MENU_CTRL_MASK + ALT_DOWN_MASK + SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_I = getKeyStroke('I', MENU_CTRL_MASK + SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_BACKSPACE = getKeyStroke(VK_BACK_SPACE, MENU_CTRL_MASK);
    public static final KeyStroke CTRL_ALT_I = getKeyStroke('I', MENU_CTRL_MASK | ALT_DOWN_MASK);
    public static final KeyStroke CTRL_ALT_V = getKeyStroke('V', MENU_CTRL_MASK | ALT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_V = getKeyStroke('V', MENU_CTRL_MASK | SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_C = getKeyStroke('C', MENU_CTRL_MASK | SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_F = getKeyStroke('F', MENU_CTRL_MASK | SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_Z = getKeyStroke('Z', SHIFT_DOWN_MASK | MENU_CTRL_MASK);
    public static final KeyStroke CTRL_ALT_W = getKeyStroke('W', MENU_CTRL_MASK | ALT_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_S = getKeyStroke('S', MENU_CTRL_MASK | SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_W = getKeyStroke('W', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_Z = getKeyStroke('Z', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_V = getKeyStroke('V', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_U = getKeyStroke('U', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_T = getKeyStroke('T', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_S = getKeyStroke('S', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_R = getKeyStroke('R', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_O = getKeyStroke('O', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_N = getKeyStroke('N', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_M = getKeyStroke('M', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_L = getKeyStroke('L', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_K = getKeyStroke('K', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_J = getKeyStroke('J', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_I = getKeyStroke('I', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_H = getKeyStroke('H', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_F = getKeyStroke('F', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_E = getKeyStroke('E', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_D = getKeyStroke('D', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_C = getKeyStroke('C', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_B = getKeyStroke('B', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_4 = getKeyStroke('4', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_3 = getKeyStroke('3', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_2 = getKeyStroke('2', MENU_CTRL_MASK);
    public static final KeyStroke CTRL_1 = getKeyStroke('1', MENU_CTRL_MASK);
    public static final KeyStroke ALT_BACKSPACE = getKeyStroke(VK_BACK_SPACE, ALT_DOWN_MASK);
    public static final KeyStroke T = getKeyStroke('T');
    public static final KeyStroke F6 = getKeyStroke(VK_F6, 0);
    public static final KeyStroke F7 = getKeyStroke(VK_F7, 0);
    public static final KeyStroke F12 = getKeyStroke(VK_F12, 0);
    public static final KeyStroke TAB = getKeyStroke(VK_TAB, 0);

    public static final KeyStroke CTRL_NUMPAD_PLUS = getKeyStroke(VK_ADD, CTRL_DOWN_MASK);
    public static final KeyStroke CTRL_NUMPAD_MINUS = getKeyStroke(VK_SUBTRACT, CTRL_DOWN_MASK);
    public static final KeyStroke CTRL_SHIFT_EQUALS = getKeyStroke(VK_EQUALS, CTRL_DOWN_MASK + SHIFT_DOWN_MASK);
    public static final KeyStroke CTRL_NUMPAD_0 = getKeyStroke(VK_NUMPAD0, CTRL_DOWN_MASK);
    public static final KeyStroke CTRL_ALT_NUMPAD_0 = getKeyStroke(VK_NUMPAD0, CTRL_DOWN_MASK + ALT_DOWN_MASK);

    public static final KeyStroke ESC = getKeyStroke(VK_ESCAPE, 0);

    public static final KeyStroke CTRL_TAB = getKeyStroke("ctrl TAB");
    public static final KeyStroke CTRL_SHIFT_TAB = getKeyStroke("ctrl shift TAB");

    private Keys() {
        // do not instantiate
    }
}
