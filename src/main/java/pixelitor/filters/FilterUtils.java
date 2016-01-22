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

package pixelitor.filters;

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
    private static final List<FilterAction> allFilters = new ArrayList<>();
    private static Filter lastExecutedFilter = null;

    /**
     * Utility class with static methods
     */
    private FilterUtils() {
    }

    // it returns an array because JComboBox does not accept Lists as constructor arguments
    public static FilterAction[] getAllFiltersSorted() {
        FilterAction[] filters = allFilters.toArray(new FilterAction[allFilters.size()]);
        Arrays.sort(filters, comparing(FilterAction::getName));
        return filters;
    }

    public static FilterAction[] getAnimationFiltersSorted() {
        List<FilterAction> animFilters = allFilters.stream()
                .filter(FilterAction::isAnimationFilter)
                .sorted(comparing(FilterAction::getListName))
                .collect(Collectors.toList());

        FilterAction[] asArray = animFilters.toArray(new FilterAction[animFilters.size()]);
        return asArray;
    }

    public static Filter getRandomFilter(Predicate<Filter> conditions) {
        FilterAction filterAction;
        do {
            // try a random filter until all conditions are true
            filterAction = allFilters.get((int) (Math.random() * allFilters.size()));
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

    public static void addFilter(FilterAction filter) {
        allFilters.add(filter);
    }

    public static void createAllFilters() {
        long startTime = System.nanoTime();

        allFilters.forEach(FilterAction::getFilter);

        double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(String.format("FilterUtils::createAllFilters: estimatedSeconds = '%.2f'", estimatedSeconds));
    }
}
