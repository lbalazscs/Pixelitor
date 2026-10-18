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

package pixelitor.filters.animation;

import pixelitor.automate.Wizard;
import pixelitor.automate.WizardPage;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.layers.Drawable;

/**
 * A {@link Wizard} for configuring and rendering tweening animations.
 */
public class TweenWizard extends Wizard {
    private static final int INITIAL_DIALOG_WIDTH = 500;
    private static final int INITIAL_DIALOG_HEIGHT = 500;
    private static final String WIZARD_TITLE = "Export Tweening Animation";
    private static final String FINISH_BUTTON_TEXT = "Render";

    private final TweenAnimation animation = new TweenAnimation();

    public TweenWizard(Drawable dr) {
        super(WIZARD_TITLE, FINISH_BUTTON_TEXT,
            INITIAL_DIALOG_WIDTH, INITIAL_DIALOG_HEIGHT, dr);
    }

    @Override
    protected WizardPage createFirstPage() {
        return new TweenFilterSelectionPage(this);
    }

    @Override
    protected void onWizardComplete() {
        new RenderTweenFramesTask(animation, dr).execute();
    }

    @Override
    protected void onWizardClosed() {
        ParametrizedFilter filter = getFilter();
        if (filter != null) { // a filter was already selected on the first page
            filter.getParamSet().setAnimationEndStateMode(false);
        }
    }

    /**
     * Returns the {@link TweenAnimation} instance being configured by this wizard.
     */
    public TweenAnimation getAnimation() {
        return animation;
    }

    /**
     * Returns the {@link ParametrizedFilter} currently selected for this animation,
     * or {@code null} if no filter has been selected yet.
     */
    public ParametrizedFilter getFilter() {
        return animation.getFilter();
    }
}
