/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.layers.Drawable;

import static pixelitor.filters.animation.TweenWizardPage.FILTER_SELECTION;

/**
 * The {@link Wizard} used for tweening animations.
 */
public class TweenWizard extends Wizard {
    private static final int DEFAULT_DIALOG_WIDTH = 500;
    private static final int DEFAULT_DIALOG_HEIGHT = 500;
    private static final String WIZARD_TITLE = "Export Tweening Animation";
    private static final String FINISH_BUTTON_TEXT = "Render";

    private final TweenAnimation animation = new TweenAnimation();

    public TweenWizard(Drawable dr) {
        super(FILTER_SELECTION, WIZARD_TITLE, FINISH_BUTTON_TEXT,
            DEFAULT_DIALOG_WIDTH, DEFAULT_DIALOG_HEIGHT, dr);
    }

    @Override
    protected void onWizardComplete() {
        new RenderTweenFramesTask(animation, dr).execute();
    }

    @Override
    protected void performCleanup() {
        ParametrizedFilter filter = animation.getFilter();
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
}
