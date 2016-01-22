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
package pixelitor.gui.utils;

import java.awt.Frame;

/**
 * An OKCancelDialog that uses a ValidatedForm as the form panel
 */
public class ValidatedDialog extends OKCancelDialog {
    // for some reason it only works if this is declared static
    // otherwise isOkPressed returns false even if it was previously set to true
    // (must be some thread stuff related to modal dialogs and dispose)
    private static boolean okPressed = false;

    public ValidatedDialog(ValidatedForm formPanel, Frame owner, String title) {
        super(formPanel, owner, title);
    }

    public ValidatedDialog(ValidatedForm formPanel, Frame owner, String title, String okText, String cancelText) {
        super(formPanel, owner, title, okText, cancelText);
    }

    @Override
    protected void dialogAccepted() {
        ValidatedForm validatedForm = (ValidatedForm) formPanel;
        if (validatedForm.isDataValid()) {
            setOkPressed(true);
            close();
        } else {
            String message = validatedForm.getErrorMessage();
            Dialogs.showErrorDialog(this, "Error", message);
        }
    }

    @Override
    protected void dialogCanceled() {
        setOkPressed(false);
        close();
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    private synchronized void setOkPressed(boolean okPressed) {
        ValidatedDialog.okPressed = okPressed;
    }
}
