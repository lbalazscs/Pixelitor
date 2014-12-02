/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.animation;

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.utils.GUIUtils;
import pixelitor.utils.OKCancelDialog;

import javax.swing.*;

/**
 * Wizard for keyframe-based animations
 */
public class KFWizard {
    private OKCancelDialog dialog = null;
    private KFWizardState wizardState = KFWizardState.SELECT_FILTER;
    private FilterWithParametrizedGUI filter;

    /**
     * Show the wizard in a dialog
     */
    public void show(JFrame dialogParent) {
        dialog = new OKCancelDialog(
                wizardState.getPanel(KFWizard.this),
                dialogParent,
                "Export Keyframe Animation",
                "Next", "Cancel") {

            @Override
            protected void dialogCanceled() {
                wizardState.onWizardCancelled(KFWizard.this);
                super.dialogCanceled();
                dispose();
            }

            @Override
            protected void dialogAccepted() {
                // "next" was pressed
                KFWizardState nextState = wizardState.getNext();
                if(nextState == null) {
                    wizardFinished();
                    dispose();
                } else {
                    JPanel panel = nextState.getPanel(KFWizard.this);
                    dialog.changeFormPanel(panel);
                    dialog.setHeaderMessage(nextState.getHelpMessage());
                    wizardState = nextState;
                }
            }
        };
        dialog.setHeaderMessage(wizardState.getHelpMessage());

        // it was packed already, but this is not correct because of the header message
        // and anyway we don't know the size of the filter dialogs in advance
        dialog.setSize(450, 300);

        GUIUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
    }

    private static void wizardFinished() {
        System.out.println("Wizard::wizardFinished: CALLED");
    }

    public void setFilter(FilterWithParametrizedGUI filter) {
        this.filter = filter;
    }

    public FilterWithParametrizedGUI getFilter() {
        return filter;
    }
}
