/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import static pixelitor.filters.animation.TweenWizardPage.SELECT_FILTER;

/**
 * The {@link Wizard} for tweening animations.
 */
public class TweenWizard extends Wizard {
    private final TweenAnimation animation = new TweenAnimation();

    public TweenWizard(Drawable dr) {
        super(SELECT_FILTER, "Export Tweening Animation",
            "Render", 500, 500, dr);
    }

    public TweenAnimation getAnimation() {
        return animation;
    }

    @Override
    protected void finalAction() {
        new RenderTweenFramesTask(animation, dr).execute();
    }

    @Override
    protected void finalCleanup() {
        ParametrizedFilter filter = animation.getFilter();
        if (filter != null) { // a filter was already selected
            filter.getParamSet().setFinalAnimationSettingMode(false);
        }
    }
}

