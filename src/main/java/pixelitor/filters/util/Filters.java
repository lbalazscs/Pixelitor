/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.Fade;
import pixelitor.filters.Filter;
import pixelitor.filters.RepeatLast;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;

import java.awt.EventQueue;
import java.util.ArrayList;
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
    private static boolean finishedAdding = false;

    private Filters() {
        // there are only static utility functions in this class
    }

    // it returns an array because JComboBox does not accept Lists as constructor arguments
    public static FilterAction[] getAllFilters() {
        assert finishedAdding;
        return allFilters.toArray(EMPTY_FA_ARRAY);
    }

    public static void forEachSmartFilter(Consumer<? super Filter> action) {
        assert finishedAdding;
        // all filters can be serialized, but only those
        // with a no-arg constructor can be deserialized
        allFilters.stream()
            .map(FilterAction::getFilter)
            .filter(Filter::canBeSmart)
            .forEach(action);
    }

    public static FilterAction getRandomAnimationFilter() {
        assert finishedAdding;
        return Rnd.chooseFrom(getAnimationFilters());
    }

    public static FilterAction[] getAnimationFilters() {
        assert finishedAdding;
        return allFilters.stream()
            .filter(FilterAction::isAnimationFilter)
            .toArray(FilterAction[]::new);
    }

    public static Filter getRandomFilter(Predicate<Filter> conditions) {
        assert finishedAdding;

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

    public static void addFilter(FilterAction filter) {
        assert !finishedAdding;
        allFilters.add(filter);
    }

    public static void finishedAdding() {
        finishedAdding = true;
        allFilters.sort(comparing(FilterAction::getName));
    }

    public static void createAllFilters() {
        assert finishedAdding;

        long startTime = System.nanoTime();

        allFilters.forEach(FilterAction::getFilter);

        double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.printf("FilterUtils::createAllFilters: estimatedSeconds = '%.2f'%n", estimatedSeconds);
    }

    /**
     * Test whether all the filters have a no-argument constructor
     */
    public static void testFilterConstructors() {
        assert finishedAdding;

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
        assert finishedAdding;

        for (FilterAction filter : allFilters) {
            if (filter.getName().equals(filterName)) {
                return filter;
            }
        }
        return null;
    }

    public static void startFilter(String filterName) {
        FilterAction action = getFilterActionByName(filterName);
        if (action == null) {
            Messages.showError("Error", "<html>The filter <b>\"%s\"</b> was not found.".formatted(filterName));
            return;
        }
        EventQueue.invokeLater(() -> action.actionPerformed(null));
    }
}
