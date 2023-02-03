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

package pixelitor.filters.util;

import pixelitor.filters.*;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Rnd;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;

/**
 * A utility class with static methods for managing filters
 */
public class Filters {
    private static final List<FilterAction> allFilters = new ArrayList<>();

    // a performance optimization
    private static final FilterAction[] EMPTY_FA_ARRAY = new FilterAction[0];

    private static Filter lastFilter = null;

    private Filters() {
    }

    // it returns an array because JComboBox does not accept Lists as constructor arguments
    public static FilterAction[] getAllFiltersSorted() {
        FilterAction[] filters = allFilters.toArray(EMPTY_FA_ARRAY);
        Arrays.sort(filters, comparing(FilterAction::getName));
        return filters;
    }

    public static void forEachSmartFilter(Consumer<? super Filter> action) {
        // all filters can be serialized, but only those
        // with a no-arg constructor can be deserialized
        allFilters.stream()
            .map(FilterAction::getFilter)
            .filter(Filter::canBeSmart)
            .filter(filter -> !(filter instanceof RandomFilter))
            .sorted(comparing(Filter::getName))
            .forEach(action);
    }

    public static FilterAction[] getAnimationFilters() {
        return allFilters.stream()
            .filter(FilterAction::isAnimationFilter)
            .toArray(FilterAction[]::new);
    }

    public static FilterAction[] getAnimationFiltersSorted() {
        return allFilters.stream()
            .filter(FilterAction::isAnimationFilter)
            .sorted(comparing(FilterAction::getName))
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

    public static void setLastFilter(Filter lastFilter) {
        if (lastFilter instanceof Fade) {
            return;
        }
        Filters.lastFilter = lastFilter;
        RepeatLast.update(lastFilter);
    }

    public static boolean hasLastFilter() {
        return lastFilter != null;
    }

    public static boolean hasLastGUIFilter() {
        return lastFilter instanceof FilterWithGUI;
    }

    public static Optional<Filter> getLastFilter() {
        return Optional.ofNullable(lastFilter);
    }

    public static BufferedImage runRGBPixelOp(RGBPixelOp pixelOp,
                                              BufferedImage src,
                                              BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelArray(src);
        int[] destData = ImageUtils.getPixelArray(dest);

        for (int i = 0; i < srcData.length; i++) {
            int rgb = srcData[i];

            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

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
        System.out.printf("FilterUtils::createAllFilters: estimatedSeconds = '%.2f'%n", estimatedSeconds);
    }

    /**
     * Test whether all the filters have a no-argument constructor
     */
    public static void testFilterConstructors() {
        List<String> problems = new ArrayList<>();
        for (FilterAction action : allFilters) {
            Filter filter = action.getFilter();
            if (filter.canBeSmart()) {
                try {
                    filter.getClass().getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    problems.add(filter.getName());
                }
            }
        }
        problems.forEach(System.out::println);
    }

    public static FilterAction getFilterActionByName(String filterName) {
        for (FilterAction filter : allFilters) {
            if (filter.getName().equals(filterName)) {
                return filter;
            }
        }
        return null;
    }
}
