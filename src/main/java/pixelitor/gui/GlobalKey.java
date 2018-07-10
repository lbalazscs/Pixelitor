/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;

/**
 * Represents a keyboard shortcut that is always working,
 * except when a dialog is active
 */
public class GlobalKey {
    // fields common to the two types
    private final String actionMapKey;
    private final Action action;

    // fields for char-based shortcuts
    private char activationChar;
    private boolean caseInsensitive;

    // field for KeyStroke-based shortcuts
    private KeyStroke keyStroke;

    /**
     * Constructor for char-based shortcuts
     */
    public GlobalKey(char activationChar, boolean caseInsensitive, String actionMapKey, Action action) {
        this.activationChar = activationChar;
        this.caseInsensitive = caseInsensitive;
        this.actionMapKey = actionMapKey;
        this.action = action;
    }

    /**
     * Constructor for KeyStroke-based shortcuts
     */
    public GlobalKey(KeyStroke keyStroke, String actionMapKey, Action action) {
        this.keyStroke = keyStroke;
        this.actionMapKey = actionMapKey;
        this.action = action;
    }

    public void registerOn(InputMap inputMap, ActionMap actionMap) {
        if (keyStroke != null) {
            inputMap.put(keyStroke, actionMapKey);
        } else {
            if (caseInsensitive) {
                char activationLC = Character.toLowerCase(activationChar);
                char activationUC = Character.toUpperCase(activationChar);

                inputMap.put(KeyStroke.getKeyStroke(activationLC), actionMapKey);
                inputMap.put(KeyStroke.getKeyStroke(activationUC), actionMapKey);
            } else {
                inputMap.put(KeyStroke.getKeyStroke(activationChar), actionMapKey);
            }
        }
        actionMap.put(actionMapKey, action);
    }
}
