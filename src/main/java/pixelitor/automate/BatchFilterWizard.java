/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.compactions.CompAction;
import pixelitor.filters.Filter;
import pixelitor.layers.Drawable;

import java.util.concurrent.CompletableFuture;

import static pixelitor.FilterContext.BATCH_AUTOMATE;
import static pixelitor.automate.BatchFilterWizardPage.SELECT_FILTER_AND_DIRS;

/**
 * The batch filter wizard
 */
public class BatchFilterWizard extends Wizard {
    private Filter filter;

    public BatchFilterWizard(Drawable dr, String title) {
        super(SELECT_FILTER_AND_DIRS, title,
            "Start Processing", 490, 500, dr);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    protected void onWizardComplete() {
        var dialogTitle = "Batch Filter Progress";

        CompAction batchFilterAction = comp -> {
            comp.getActiveDrawable().startFilter(filter, BATCH_AUTOMATE);
            return CompletableFuture.completedFuture(comp);
        };
        new BatchProcessor(batchFilterAction, dialogTitle).processFiles();
    }

    @Override
    protected void performCleanup() {
        // nothing to do
    }
}
