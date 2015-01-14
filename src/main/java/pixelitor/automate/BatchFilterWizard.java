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

import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.PixelitorWindow;
import pixelitor.filters.Filter;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.CompositionAction;

import java.awt.Component;

/**
 * The batch filter wizard
 */
public class BatchFilterWizard extends Wizard {
    private BatchFilterConfig config = new BatchFilterConfig();

    public BatchFilterWizard() {
        super(BatchFilterWizardPage.SELECT_FILTER_AND_DIRS, "Batch Filter", "Start Processing", 490, 380);
    }

    @Override
    protected boolean mayMoveForwardIfNextPressed(WizardPage wizardPage, Component dialogParent) {
        return true;
    }

    @Override
    protected boolean mayProceedAfterMovingForward(WizardPage wizardPage, Component dialogParent) {
        return true;
    }

    @Override
    protected void executeFinalAction() {
        final Filter filter = config.getFilter();
        final PixelitorWindow busyCursorParent = PixelitorWindow.getInstance();

        Automate.processEachFile(new CompositionAction() {
            @Override
            public void process(Composition comp) {
                final ImageLayer layer = comp.getActiveImageLayer();
                comp.executeFilterWithBusyCursor(filter, ChangeReason.OP_WITHOUT_DIALOG, busyCursorParent);
            }
        }, true, "Batch Filter Progress");
    }

    @Override
    protected void finalCleanup() {

    }

    public BatchFilterConfig getConfig() {
        return config;
    }
}
