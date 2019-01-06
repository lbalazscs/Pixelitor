/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.ImageUtils;
import pixelitor.utils.Rnd;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Comparator.comparing;

/**
 * A utility class with static methods for managing filters
 */
public class FilterUtils {
    private static final List<FilterAction> allFilters = new ArrayList<>();

    // a performance optimization
    private static final FilterAction[] EMPTY_FA_ARRAY = new FilterAction[0];

    private static Filter lastFilter = null;

    private FilterUtils() {
    }

    // it returns an array because JComboBox does not accept Lists as constructor arguments
    public static FilterAction[] getAllFiltersSorted() {
        FilterAction[] filters = allFilters.toArray(EMPTY_FA_ARRAY);
        Arrays.sort(filters, comparing(FilterAction::getName));
        return filters;
    }

    public static FilterAction[] getAnimationFilters() {
        return allFilters.stream()
                .filter(FilterAction::isAnimationFilter)
                .toArray(FilterAction[]::new);
    }

    public static FilterAction[] getAnimationFiltersSorted() {
        return allFilters.stream()
                .filter(FilterAction::isAnimationFilter)
                .sorted(comparing(FilterAction::getListName))
                .toArray(FilterAction[]::new);
    }

    public static Filter getRandomFilter(Predicate<Filter> conditions) {
        // tries to avoid the instantiation of filters
        FilterAction filterAction;
        do {
            // try a random filter until all conditions are true
            filterAction = Rnd.chooseFrom(allFilters);
        } while (!conditions.test(filterAction.getFilter()));

        return filterAction.getFilter();
    }

    public static Filter[] getFiltersShuffled(Predicate<Filter> predicate) {
        // used only in test code, no problem if all filters are instantiated
        Filter[] filters = allFilters.stream()
                .map(FilterAction::getFilter)
                .filter(predicate).toArray(Filter[]::new);

        Collections.shuffle(Arrays.asList(filters));
        return filters;
    }

    public static void setLastFilter(Filter lastFilter) {
        if (lastFilter instanceof Fade) {
            return;
        }
        FilterUtils.lastFilter = lastFilter;
        RepeatLast.INSTANCE.setActionName("Repeat " + lastFilter.getName());
    }

    public static Optional<Filter> getLastFilter() {
        return Optional.ofNullable(lastFilter);
    }

    public static BufferedImage runRGBPixelOp(RGBPixelOp pixelOp,
                                              BufferedImage src,
                                              BufferedImage dest) {
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

    public static void addFilter(FilterAction filter) {
        allFilters.add(filter);
    }

    public static void createAllFilters() {
        long startTime = System.nanoTime();

        allFilters.forEach(FilterAction::getFilter);

        double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(format("FilterUtils::createAllFilters: estimatedSeconds = '%.2f'", estimatedSeconds));
    }
}
