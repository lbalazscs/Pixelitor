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
 * A utility class with static methods for managing filters.
 */
public class Filters {
    private static final List<FilterAction> allFilters = new ArrayList<>();

    // a performance optimization
    private static final FilterAction[] EMPTY_FA_ARRAY = new FilterAction[0];

    private static Filter lastFilter = null;
    private static boolean finishedRegistering = false;

    private Filters() {
        // there are only static utility functions in this class
    }

    // it returns an array because JComboBox does not accept Lists as constructor arguments
    public static FilterAction[] getAllFilters() {
        assert finishedRegistering;
        return allFilters.toArray(EMPTY_FA_ARRAY);
    }

    // used only for debugging
    public static void forEachSmartFilter(Consumer<? super Filter> action) {
        assert finishedRegistering;
        allFilters.stream()
            .map(FilterAction::getFilter)
            .filter(Filter::canBeSmart)
            .forEach(action);
    }

    // used only for testing
    public static FilterAction getRandomAnimationFilter() {
        assert finishedRegistering;
        return Rnd.chooseFrom(getAnimationFilters());
    }

    public static FilterAction[] getAnimationFilters() {
        assert finishedRegistering;
        return allFilters.stream()
            .filter(FilterAction::isAnimationFilter)
            .toArray(FilterAction[]::new);
    }

    public static Filter getRandomFilter(Predicate<Filter> conditions) {
        assert finishedRegistering;

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

    public static void register(FilterAction filter) {
        assert !finishedRegistering;
        allFilters.add(filter);
    }

    public static void finishedRegistering() {
        finishedRegistering = true;
        allFilters.sort(comparing(FilterAction::getName));
    }

    // only used for testing
    public static void createAllFilters() {
        assert finishedRegistering;

        long startTime = System.nanoTime();

        allFilters.forEach(FilterAction::getFilter);

        double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.printf("FilterUtils::createAllFilters: elapsedSeconds = '%.2f'%n", elapsedSeconds);
    }

    /**
     * Tests whether all filters that can be smart have a no-argument constructor.
     */
    public static void testFilterConstructors() {
        assert finishedRegistering;

        List<String> problems = new ArrayList<>();
        for (FilterAction action : allFilters) {
            Filter filter = action.getFilter();
            if (filter.canBeSmart()) {
                try {
                    filter.getClass().getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    problems.add(filter.getName());
                }
            }
        }
        problems.forEach(System.out::println);
    }

    public static FilterAction getFilterActionByName(String filterName) {
        assert finishedRegistering;

        for (FilterAction action : allFilters) {
            if (action.getName().equals(filterName)) {
                return action;
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
