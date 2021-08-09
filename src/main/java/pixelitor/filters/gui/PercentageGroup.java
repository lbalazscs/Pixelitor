/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import java.util.function.ToIntFunction;

/**
 * A utility class which ensures that the sum of values in the {@link RangeParam}s added
 * to it is always 100, and therefore they can be interpreted as percentages.
 * See https://softwareengineering.stackexchange.com/questions/261017/algorithm-for-a-ui-showing-x-percentage-sliders-whose-linked-values-always-total
 */
public class PercentageGroup {
    private final RangeParam[] params;
    private boolean enabled = true;
    private boolean processing = false;

    public PercentageGroup(RangeParam... params) {
        this.params = params;
        for (RangeParam param : params) {
            param.addChangeListener(e -> paramStateChanged(param));
        }
    }

    private void paramStateChanged(RangeParam source) {
        if (!enabled) {
            return;
        }
        if (processing) {
            // avoid the scenario where the change
            // listeners are calling each other
            return;
        }
        processing = true;

        int sumOfAllValues = 0;
        for (RangeParam param : params) {
            sumOfAllValues += param.getValue();
        }
        int diff = sumOfAllValues - 100;
        if (diff == 0) {
            processing = false;
            return;
        }

        // Weighted move - the remaining sliders are moved by
        // an amount proportional to the space they have left.

        // The space left function depends on the direction of change.
        ToIntFunction<RangeParam> spaceLeftFunc;
        if (diff > 0) {
            // the other sliders have to decrease their value
            spaceLeftFunc = param -> param.getValue() - param.getMinimum();
        } else {
            // the other sliders have to increase their value
            spaceLeftFunc = param -> param.getMaximum() - param.getValue();
        }

        // first pass: calculate the sum of spaces left
        int sumOfSpacesLeft = 0;
        for (RangeParam param : params) {
            if (param != source) {
                int spaceLeft = spaceLeftFunc.applyAsInt(param);
                sumOfSpacesLeft += spaceLeft;
            }
        }

        // second pass: move the other sliders so that the difference is
        // distributed among them proportional to their space left
        for (RangeParam param : params) {
            if (param != source) {
                int spaceLeft = spaceLeftFunc.applyAsInt(param);
                double newValue = param.getValue() - diff * spaceLeft / (double) sumOfSpacesLeft;
                param.setValue(newValue, false);
            }
        }

        processing = false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
