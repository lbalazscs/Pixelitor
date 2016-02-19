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

package pixelitor.automate;

import pixelitor.ChangeReason;
import pixelitor.filters.Filter;
import pixelitor.gui.PixelitorWindow;
import pixelitor.layers.ImageLayer;

import java.awt.Component;

import static pixelitor.automate.BatchFilterWizardPage.SELECT_FILTER_AND_DIRS;

/**
 * The batch filter wizard
 */
public class BatchFilterWizard extends Wizard {
    private final BatchFilterConfig config = new BatchFilterConfig();

    public BatchFilterWizard(ImageLayer layer) {
        super(SELECT_FILTER_AND_DIRS, "Batch Filter", "Start Processing", 490, 380, layer);
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
        Filter filter = config.getFilter();
        PixelitorWindow busyCursorParent = PixelitorWindow.getInstance();

        Automate.processEachFile(comp -> {
            ImageLayer layer = comp.getActiveMaskOrImageLayer();
            filter.executeFilterWithBusyCursor(layer, ChangeReason.BATCH_AUTOMATE, busyCursorParent);
        }, true, "Batch Filter Progress");
    }

    @Override
    protected void finalCleanup() {

    }

    public BatchFilterConfig getConfig() {
        return config;
    }
}
