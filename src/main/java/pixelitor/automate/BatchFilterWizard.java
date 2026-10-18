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

import static pixelitor.filters.FilterContext.BATCH_AUTOMATE;

/**
 * A wizard that applies a selected filter to multiple images in batch mode.
 */
public class BatchFilterWizard extends Wizard {
    private Filter filter;

    public BatchFilterWizard(Drawable dr, String title) {
        super(title,
            "Start Processing", 600, 500, dr);
    }

    @Override
    protected WizardPage createFirstPage() {
        return new BatchFilterSelectionPage(this);
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
            // since we are processing newly opened image files,
            // we assume that each has a single image layer
            // TODO ensure that pxc files can't be opened in this wizard
            Drawable compDr = comp.getActiveDrawable();
            if (compDr == null) {
                throw new IllegalStateException(
                    "The active layer is not drawable in " + comp.getName());
            }

            compDr.startFilter(filter, BATCH_AUTOMATE);
            return CompletableFuture.completedFuture(comp);
        };
        new BatchProcessor(batchFilterAction, progressDialogTitle).processFilesAsync();
    }
}
