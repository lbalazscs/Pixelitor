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
 * Represents a keyboard shortcut that can be added
 * to an InputMap/ActionMap pair
 */
public class MappedKey {
    // fields common to the two types
    private String actionMapKey;
    private Action action;

    // fields for char-based shortcuts
    private char activationChar;
    private boolean caseInsensitive;

    // field for KeyStroke-based shortcuts
    private KeyStroke keyStroke;

    private MappedKey() {
        // private because the factory methods should be used
    }

    public static MappedKey fromKeyStroke(KeyStroke keyStroke,
                                          String actionMapKey,
                                          Action action) {
        MappedKey key = new MappedKey();

        key.keyStroke = keyStroke;
        key.actionMapKey = actionMapKey;
        key.action = action;

        return key;
    }

    public static MappedKey fromChar(char activationChar,
                                     boolean caseInsensitive,
                                     String actionMapKey,
                                     Action action) {
        MappedKey key = new MappedKey();

        key.activationChar = activationChar;
        key.caseInsensitive = caseInsensitive;
        key.actionMapKey = actionMapKey;
        key.action = action;

        return key;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{ actionMapKey='").append(actionMapKey).append('\'');
        sb.append(", activationChar=").append(activationChar);
        sb.append(", caseInsensitive=").append(caseInsensitive);
        sb.append(", keyStroke=").append(keyStroke);
        sb.append('}');
        return sb.toString();
    }
}
