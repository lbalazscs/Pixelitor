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

package pixelitor.filters.gui;

import com.jhlabs.math.Noise;
import pd.OpenSimplex2F;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gmic.GMICFilter;
import pixelitor.layers.Filterable;
import pixelitor.utils.CachedFloatRandom;
import pixelitor.utils.Icons;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.util.*;
import java.util.function.LongConsumer;

import static java.util.Locale.Category.FORMAT;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.ANIMATION_ENDING_STATE;

/**
 * A container/model for all the configurable elements of a {@link ParametrizedFilter}.
 * The GUI of the filter can be built automatically based on the contents of this class.
 */
public class ParamSet implements Debuggable {
    public static final String RANDOMIZE_BUTTON_TOOLTIP = "Randomize the settings for this filter.";

    private final List<FilterParam> params = new ArrayList<>();
    private final List<FilterButtonModel> actions = new ArrayList<>(3);
    private ParamAdjustmentListener adjustmentListener;
    private Runnable afterResetAction;
    private Preset[] builtinPresets;

    // reseed support
    private static final String SEED_KEY = "Seed";
    private long seed;
    private Random random;
    private OpenSimplex2F simplex;
    private boolean savesSeed;
    private LongConsumer seedChangedAction;

    public ParamSet() {
    }

    public void addParam(FilterParam newParam) {
        params.add(newParam);
    }

    public void addParams(FilterParam[] newParams) {
        Collections.addAll(params, newParams);
    }

    public void addParams(List<FilterParam> newParams) {
        params.addAll(newParams);
    }

    /**
     * Adds the given parameters before the existing ones
     */
    public void addParamsToFront(FilterParam[] newParams) {
        params.addAll(0, Arrays.asList(newParams));
    }

    public void insertParam(FilterParam newParam, int index) {
        params.add(index, newParam);
    }

    public void insertAction(FilterButtonModel newAction, int index) {
        actions.add(index, newAction);
    }

    public ParamSet withActions(FilterButtonModel... newActions) {
        actions.addAll(List.of(newActions));
        return this;
    }

    public ParamSet withActionsAtFront(FilterButtonModel... newActions) {
        for (FilterButtonModel action : newActions) {
            actions.addFirst(action);
        }
        return this;
    }

    public ParamSet withAction(FilterButtonModel newAction) {
        actions.add(newAction);
        return this;
    }

    /**
     * Adds actions that are common to all filters.
     */
    public void addCommonActions() {
        addRandomizeAction();
        addResetAllAction();
    }

    /**
     * Adds the "Randomize Settings" button model to the action list.
     */
    private void addRandomizeAction() {
        var randomizeAction = new FilterButtonModel("Randomize Settings",
            this::randomize,
            Icons.getRandomizeIcon(),
            RANDOMIZE_BUTTON_TOOLTIP,
            "randomize");
        actions.add(randomizeAction);
    }

    /**
     * Adds the "Reset All" button model to the action list.
     */
    private void addResetAllAction() {
        var resetAllAction = new FilterButtonModel("Reset All",
            this::reset,
            Icons.getResetIcon(),
            Resettable.RESET_ALL_TOOLTIP,
            "resetAll");
        actions.add(resetAllAction);
    }

    /**
     * Registers an action that will be executed after
     * the {@link #reset()} method has finished resetting all parameters.
     */
    public void setAfterResetAllAction(Runnable afterResetAction) {
        this.afterResetAction = afterResetAction;
    }

    /**
     * Resets all parameters to their default values without triggering a filter preview update.
     */
    public void reset() {
        for (FilterParam param : params) {
            param.reset(false);
        }
        if (afterResetAction != null) {
            afterResetAction.run();
        }
    }

    /**
     * Randomizes the values of all parameters without triggering a filter preview update.
     */
    public void randomize() {
        long before = Filter.executionCount;

        params.forEach(FilterParam::randomize);

        // check that the filter wasn't triggered
        long after = Filter.executionCount;
        assert before == after : "before = " + before + ", after = " + after;
    }

    /**
     * Programmatically triggers a filter execution by notifying the registered {@link ParamAdjustmentListener}.
     */
    public void runFilter() {
        if (adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    /**
     * Sets the listener that is notified when a parameter is adjusted.
     */
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;

        for (FilterParam param : params) {
            param.setAdjustmentListener(listener);
        }
        for (FilterButtonModel action : actions) {
            action.setAdjustmentListener(listener);
        }
    }

    /**
     * Adapts parameter ranges and choices to the current context, such as image size.
     */
    public void adaptToContext(Filterable layer, boolean applyNewDefault) {
        for (FilterParam param : params) {
            param.adaptToContext(layer, applyNewDefault);
        }
    }

    /**
     * A {@link ParamSet} can be animated if at least
     * one contained filter parameter can be.
     */
    public boolean isAnimatable() {
        for (FilterParam param : params) {
            if (param.isAnimatable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enables or disables UI controls based on whether the user
     * is configuring the end state of a tweening animation.
     */
    public void setAnimationEndStateMode(boolean endState) {
        for (FilterParam param : params) {
            param.setEnabled(!endState, ANIMATION_ENDING_STATE);
        }
        for (FilterButtonModel action : actions) {
            action.setEnabled(!endState, ANIMATION_ENDING_STATE);
        }
    }

    /**
     * Returns true if any parameter is a gradient parameter.
     */
    public boolean hasGradient() {
        for (FilterParam param : params) {
            if (param instanceof GradientParam) {
                return true;
            }
        }
        return false;
    }

    public List<FilterButtonModel> getActions() {
        return actions;
    }

    public List<FilterParam> getParams() {
        return params;
    }

    /**
     * Creates a snapshot of the current state of the parameters.
     */
    public FilterState copyState(boolean animOnly) {
        return new FilterState(this, animOnly);
    }

    /**
     * Loads parameter values from a {@link FilterState} without triggering the filter.
     */
    public void setState(FilterState newState, boolean forAnimation) {
        for (FilterParam param : params) {
            if (forAnimation && !param.isAnimatable()) {
                continue;
            }
            String name = param.getName();
            ParamState<?> newParamState = newState.get(name);

            if (newParamState != null) { // a preset doesn't have to contain all key-value pairs
                param.loadStateFrom(newParamState, !forAnimation);
            }
        }
    }

    /**
     * Loads parameter values from a {@link FilterState}
     * and executes the filter with the new parameters.
     */
    public void applyState(FilterState state, boolean reset) {
        if (reset) {
            reset();
        }
        setState(state, false);
        runFilter();
    }

    /**
     * Loads parameter values from a {@link UserPreset}
     * and executes the filter with the new parameters.
     */
    public void loadUserPreset(UserPreset preset) {
        long executionsBefore = Filter.executionCount;

        for (FilterParam param : params) {
            param.loadStateFrom(preset);
        }
        if (savesSeed) {
            seed = preset.getLong(SEED_KEY, seed);
            simplex = null; // make sure getLastSeedSimplex() is also reset
            if (seedChangedAction != null) {
                seedChangedAction.accept(seed);
            }
        }

        // check that the loading didn't trigger the filter
        assert Filter.executionCount == executionsBefore :
            "before = " + executionsBefore + ", after = " + Filter.executionCount;

        runFilter();
    }

    /**
     * Saves the current parameter values to a {@link UserPreset}.
     */
    public void saveStateTo(UserPreset preset) {
        Locale locale = Locale.getDefault(FORMAT);
        try {
            Locale.setDefault(FORMAT, Locale.US);
            for (FilterParam param : params) {
                param.saveStateTo(preset);
            }
            if (savesSeed) {
                preset.putLong(SEED_KEY, seed);
            }
        } finally {
            Locale.setDefault(FORMAT, locale);
        }
    }

    /**
     * Sets the value of a parameter by its name from a string representation.
     */
    public void set(String paramName, String value) {
        FilterParam modified = null;
        for (FilterParam param : params) {
            if (param.getName().equals(paramName)) {
                modified = param;
            }
        }
        if (modified == null) {
            throw new IllegalStateException("No param called " + paramName);
        }
        modified.loadStateFrom(value);
    }

    public void setBuiltinPresets(Preset... presets) {
        this.builtinPresets = presets;
    }

    public Preset[] getBuiltinPresets() {
        return builtinPresets;
    }

    public boolean hasBuiltinPresets() {
        return builtinPresets != null;
    }

    /**
     * Returns true if the filter is considered "complex", which means
     * it has multiple parameters or a single, complex parameter.
     * This affects whether user presets are enabled.
     */
    public boolean isComplex() {
        assert !params.isEmpty();
        if (params.size() > 1) {
            return true;
        }
        FilterParam singleParam = params.getFirst();
        return singleParam.isComplex();
    }

    // reseed support methods form here

    /**
     * Adds a "Reseed" action button.
     */
    public ParamSet withReseedAction() {
        return withAction(createReseedAction());
    }

    /**
     * Adds a "Reseed" action button for reseeding {@link Noise}.
     */
    public ParamSet withReseedNoiseAction() {
        return withAction(createReseedNoiseAction());
    }

    /**
     * Adds a "Reseed" action button for a GMIC filter.
     */
    public ParamSet withReseedGmicAction(GMICFilter filter) {
        return withAction(createReseedGmicAction(filter));
    }

    private void initReseedSupport(boolean createRandom) {
        seed = System.nanoTime();
        savesSeed = true;

        if (createRandom) {
            if (random != null) {
                throw new IllegalStateException(); // call only once
            }
            random = new Random(seed);
        }
    }

    /**
     * Returns the random number generator reseeded to the last value
     * in order to make sure that the filter runs with the same random
     * numbers as before.
     */
    public Random getLastSeedRandom() {
        random.setSeed(seed);
        return random;
    }

    public SplittableRandom getLastSeedSRandom() {
        return new SplittableRandom(seed);
    }

    /**
     * Similar to the method above, but for simplex noise
     */
    public OpenSimplex2F getLastSeedSimplex() {
        if (simplex == null) {
            simplex = new OpenSimplex2F(seed);
        }
        return simplex;
    }

    public long getLastSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public void reseed() {
        seed = System.nanoTime();
    }

    private void reseedSimplex() {
        seed = System.nanoTime();
        simplex = new OpenSimplex2F(seed);
    }

    private FilterButtonModel createReseedGmicAction(GMICFilter filter) {
        initReseedSupport(false);
        seedChangedAction = filter::setSeed;

        return FilterButtonModel.createReseed(() -> {
            reseed();
            filter.setSeed(seed);
        }, "Reseed", "Reinitialize the randomness");
    }

    public FilterButtonModel createReseedNoiseAction() {
        return createReseedNoiseAction("Reseed", "Reinitialize the randomness");
    }

    public FilterButtonModel createReseedNoiseAction(String text, String toolTip) {
        initReseedSupport(false);
        seedChangedAction = Noise::reseed;

        return FilterButtonModel.createReseed(() -> {
            reseed();
            Noise.reseed(seed);
        }, text, toolTip);
    }

    /**
     * Creates a "Reseed" button model for both {@link CachedFloatRandom} and {@link Noise}.
     */
    public FilterButtonModel createReseedCachedAndNoiseAction() {
        initReseedSupport(true);

        seedChangedAction = newSeed -> {
            Noise.reseed(newSeed);
            random.setSeed(newSeed);
            CachedFloatRandom.rebuildCache(random);
        };

        return FilterButtonModel.createReseed(() -> {
            reseed();
            seedChangedAction.accept(seed);
        });
    }

    /**
     * Creates a standard "Reseed" button model.
     */
    public FilterButtonModel createReseedAction() {
        initReseedSupport(true);
        return FilterButtonModel.createReseed(this::reseed);
    }

    /**
     * Creates a standard "Reseed" button model with custom text.
     */
    public FilterButtonModel createReseedAction(String name, String toolTipText) {
        initReseedSupport(true);
        return FilterButtonModel.createReseed(this::reseed, name, toolTipText);
    }

    /**
     * Creates a "Reseed" button model for simplex noise.
     */
    public FilterButtonModel createReseedSimplexAction() {
        initReseedSupport(true);
        return FilterButtonModel.createReseed(this::reseedSimplex);
    }

    /**
     * Creates a "Reseed" button model with a custom seed change handler.
     */
    public FilterButtonModel createReseedAction(LongConsumer seedChangedAction) {
        initReseedSupport(false);

        this.seedChangedAction = seedChangedAction;
        return FilterButtonModel.createReseed(() -> {
            reseed();
            seedChangedAction.accept(seed);
        });
    }

    // end of reseed support

    // used to determine whether two parametrized filters are equal
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParamSet that = (ParamSet) o;
        return Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public String toString() {
        return "ParamSet {" + params + "}";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        for (FilterParam param : params) {
            node.add(param.createDebugNode(param.getName()));
        }

        return node;
    }
}
