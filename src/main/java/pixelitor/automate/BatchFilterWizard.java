/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
 * A wizard that applies a selected filter to multiple images in batch mode.
 */
public class BatchFilterWizard extends Wizard {
    private Filter filter;

    public BatchFilterWizard(Drawable dr, String title) {
        super(SELECT_FILTER_AND_DIRS, title,
            "Start Processing", 600, 500, dr);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    protected void onWizardComplete() {
        var progressDialogTitle = "Batch Filter Progress";

        CompAction batchFilterAction = comp -> {
            // since we are processing newly opened image files, we 
            // assume that each has a single image layer (i.e. a Drawable)
            comp.getActiveDrawable().startFilter(filter, BATCH_AUTOMATE);
            return CompletableFuture.completedFuture(comp);
        };
        new BatchProcessor(batchFilterAction, progressDialogTitle).processFiles();
    }

    @Override
    protected void performCleanup() {
        // no cleanup needed
    }
}
