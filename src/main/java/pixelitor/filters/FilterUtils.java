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

package pixelitor.filters;

import pixelitor.history.History;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * An utility class for managing filters
 */
public class FilterUtils {
    private static final List<Filter> allFilters = new ArrayList<>();
    private static Filter lastExecutedFilter = null;

    /**
     * Utility class with static methods
     */
    private FilterUtils() {
    }

    // it returns an array because JComboBox does not accept Lists as constructor arguments
    public static Filter[] getAllFiltersSorted() {
        Filter[] filters = allFilters.toArray(new Filter[allFilters.size()]);
        Arrays.sort(filters, comparing(Filter::getName));
        return filters;
    }

    public static FilterWithParametrizedGUI[] getAnimationFiltersSorted() {
        Predicate<FilterWithParametrizedGUI> canBeAnimated = fpg -> fpg.getParamSet().canBeAnimated();
        Predicate<FilterWithParametrizedGUI> notExcludedFromAnimation = fpg -> !fpg.excludeFromAnimation();
        Predicate<FilterWithParametrizedGUI> checkFadeInclusion = fpg -> {
            // include Fade only if there is something to fade
            if (fpg instanceof Fade) {
                return History.canFade();
            }
            return true;
        };

        List<FilterWithParametrizedGUI> animFilters = allFilters.stream()
                .filter(filter -> filter instanceof FilterWithParametrizedGUI)
                .map(filter -> (FilterWithParametrizedGUI) filter) // cast for further filtering
                .filter(canBeAnimated
                        .and(notExcludedFromAnimation)
                        .and(checkFadeInclusion))
                .sorted(comparing(Filter::getListName))
                .collect(Collectors.toList());

        FilterWithParametrizedGUI[] asArray = animFilters.toArray(new FilterWithParametrizedGUI[animFilters.size()]);
        return asArray;
    }

    public static Filter getRandomFilter(Predicate<Filter> conditions) {
        Filter filter;
        do {
            // try a random filter until all conditions are true
            filter = allFilters.get((int) (Math.random() * allFilters.size()));
        } while (!conditions.test(filter));

        return filter;
    }

    public static Filter[] getAllFiltersShuffled() {
        Filter[] filters = allFilters.toArray(new Filter[allFilters.size()]);
        Collections.shuffle(Arrays.asList(filters));
        return filters;
    }

    public static void setLastExecutedFilter(Filter lastExecutedFilter) {
        if (lastExecutedFilter instanceof Fade) {
            return;
        }
        FilterUtils.lastExecutedFilter = lastExecutedFilter;
    }

    public static Optional<Filter> getLastExecutedFilter() {
        return Optional.ofNullable(lastExecutedFilter);
    }

    public static BufferedImage runRGBPixelOp(RGBPixelOp pixelOp, BufferedImage src, BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        for (int i = 0; i < srcData.length; i++) {
            int rgb = srcData[i];

            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = (rgb) & 0xFF;

            destData[i] = pixelOp.changeRGB(a, r, g, b);
        }

        return dest;
    }

    public static void addFilter(Filter filter) {
        if (!(filter instanceof Brick)) {
            allFilters.add(filter);
        }
    }
}
