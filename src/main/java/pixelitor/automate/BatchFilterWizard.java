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

package pixelitor.automate;

import java.awt.Component;

/**
 * The batch filter wizard
 */
public class BatchFilterWizard extends Wizard {

    public BatchFilterWizard() {
        super(BatchFilterWizardPage.SELECT_FILTER, "Batch Filter");
    }

    @Override
    protected boolean mayMoveForwardIfNextPressed(WizardPage wizardPage, Component dialogParent) {
        return false;
    }

    @Override
    protected boolean mayProceedAfterMovingForward(WizardPage wizardPage, Component dialogParent) {
        return false;
    }

    @Override
    protected void executeFinalAction() {

    }

    @Override
    protected void finalCleanup() {

    }
}
