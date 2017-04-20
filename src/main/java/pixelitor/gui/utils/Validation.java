/*
 * Copyright 2017 Laszlo Balazs-Csiki
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

package pixelitor.gui.utils;

import java.awt.Component;

public class Validation {
    private final boolean valid;
    private final String errorMsg;
    private static final Validation okInstance = new Validation(true, null);

    private Validation(boolean valid, String errorMsg) {
        this.valid = valid;
        this.errorMsg = errorMsg;

        if (!valid) {
            if (errorMsg == null) {
                throw new IllegalStateException("missing error message");
            }
        }
    }

    public static Validation ok() {
        return okInstance;
    }

    public static Validation error(String msg) {
        return new Validation(false, msg);
    }

    public Validation and(Validation other) {
        if (valid) {
            assert this == okInstance;
            if (other.isOK()) {
                return okInstance;
            } else {
                return other;
            }
        } else {
            if (other.isOK()) {
                return this;
            } else {
                return error(errorMsg + "\n" + other.errorMsg);
            }
        }
    }

    public Validation andTrue(boolean condition, String msg) {
        return andFalse(!condition, msg);
    }

    public Validation andFalse(boolean condition, String msg) {
        if (valid) {
            assert this == okInstance;
            if (condition) {
                return error(msg);
            } else {
                return okInstance;
            }
        } else {
            if (condition) {
                return error(errorMsg + "\n" + msg);
            } else {
                return this;
            }
        }
    }

    public boolean isOK() {
        return valid;
    }

    public void showErrorDialog(Component dialogParent) {
        Dialogs.showErrorDialog(dialogParent, "Error", errorMsg);
    }
}
