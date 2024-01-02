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
import static pixelitor.filters.gui.FilterSetting.EnabledReason.FINAL_ANIMATION_SETTING;

/**
 * All the information needed to automatically
 * build the user interface of a {@link ParametrizedFilter}
 */
public class ParamSet implements Debuggable {
    private final List<FilterParam> paramList = new ArrayList<>();
    private final List<FilterButtonModel> actionList = new ArrayList<>(3);
    private ParamAdjustmentListener adjustmentListener;
    private Runnable afterResetAction;
    private Preset[] builtinPresets;
    private boolean nonTrivialFilter;

    // reseed support
    private static final String SEED_KEY = "Seed";
    private long seed;
    private Random random;
    private OpenSimplex2F simplex;
    private boolean savesSeed;
    private LongConsumer seedChangedAction;

    public ParamSet() {
    }

    public void addParam(FilterParam param) {
        paramList.add(param);
    }

    public void addParams(FilterParam[] params) {
        Collections.addAll(paramList, params);
    }

    public void addParams(List<FilterParam> params) {
        paramList.addAll(params);
    }

    /**
     * Adds the given parameters before the existing ones
     */
    public void addParamsToFront(FilterParam[] params) {
        paramList.addAll(0, Arrays.asList(params));
    }

    public void insertParam(FilterParam param, int index) {
        paramList.add(index, param);
    }

    public void insertAction(FilterButtonModel action, int index) {
        actionList.add(index, action);
    }

    public ParamSet withActions(FilterButtonModel... actions) {
        actionList.addAll(List.of(actions));
        return this;
    }

    public ParamSet withActionsAtFront(FilterButtonModel... actions) {
        for (FilterButtonModel action : actions) {
            actionList.addFirst(action);
        }
        return this;
    }

    public ParamSet withAction(FilterButtonModel action) {
        actionList.add(action);
        return this;
    }

    public void addCommonActions(boolean nonTrivial) {
        nonTrivialFilter = nonTrivial;
        addRandomizeAction();
        addResetAllAction();
    }

    private void addRandomizeAction() {
        var randomizeAction = new FilterButtonModel("Randomize Settings",
            this::randomize,
            Icons.getDiceIcon(),
            "Randomize the settings for this filter.",
            "randomize");
        actionList.add(randomizeAction);
    }

    private void addResetAllAction() {
        var resetAllAction = new FilterButtonModel("Reset All",
            this::reset,
            Icons.getWestArrowIcon(),
            Resettable.RESET_ALL_TOOLTIP,
            "resetAll");
        actionList.add(resetAllAction);
    }

    /**
     * Allows registering an action that will run after "reset all"
     */
    public void setAfterResetAllAction(Runnable afterResetAction) {
        this.afterResetAction = afterResetAction;
    }

    /**
     * Resets all params without triggering the filter
     */
    public void reset() {
        for (FilterParam param : paramList) {
            param.reset(false);
        }
        if (afterResetAction != null) {
            afterResetAction.run();
        }
    }

    public void randomize() {
        long before = Filter.runCount;

        paramList.forEach(FilterParam::randomize);

        // the filter is not supposed to be triggered
        long after = Filter.runCount;
        assert before == after : "before = " + before + ", after = " + after;
    }

    public void runFilter() {
        if (adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;

        for (FilterParam param : paramList) {
            param.setAdjustmentListener(listener);
        }
        for (FilterButtonModel action : actionList) {
            action.setAdjustmentListener(listener);
        }
    }

    public void updateOptions(Filterable layer, boolean changeValue) {
        for (FilterParam param : paramList) {
            param.updateOptions(layer, changeValue);
        }
    }

    /**
     * A ParamSet can be animated if at least
     * one contained filter parameter can be
     */
    public boolean isAnimatable() {
        for (FilterParam param : paramList) {
            if (param.isAnimatable()) {
                return true;
            }
        }
        return false;
    }

    public void setFinalAnimationSettingMode(boolean b) {
        for (FilterParam param : paramList) {
            param.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
        for (FilterButtonModel action : actionList) {
            action.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
    }

    public boolean hasGradient() {
        for (FilterParam param : paramList) {
            if (param instanceof GradientParam) {
                return true;
            }
        }
        return false;
    }

    public List<FilterButtonModel> getActions() {
        return actionList;
    }

    public List<FilterParam> getParams() {
        return paramList;
    }

    public FilterState copyState(boolean animOnly) {
        return new FilterState(this, animOnly);
    }

    /**
     * Sets the state without triggering the filter.
     */
    public void setState(FilterState newState, boolean forAnimation) {
        for (FilterParam param : paramList) {
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

    public void applyState(FilterState preset, boolean reset) {
        if (reset) {
            reset();
        }
        setState(preset, false);
        runFilter();
    }

    public void loadUserPreset(UserPreset preset) {
        long runCountBefore = Filter.runCount;
        for (FilterParam param : paramList) {
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
        assert Filter.runCount == runCountBefore :
            "runCountBefore = " + runCountBefore + ", runCount = " + Filter.runCount;

        runFilter();
    }

    public void saveStateTo(UserPreset preset) {
        Locale locale = Locale.getDefault(FORMAT);
        try {
            Locale.setDefault(FORMAT, Locale.US);
            for (FilterParam param : paramList) {
                param.saveStateTo(preset);
            }
            if (savesSeed) {
                preset.putLong(SEED_KEY, seed);
            }
        } finally {
            Locale.setDefault(FORMAT, locale);
        }
    }

    public void set(String paramName, String value) {
        FilterParam modified = null;
        for (FilterParam param : paramList) {
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

    public boolean canHaveUserPresets() {
        return nonTrivialFilter;
    }

    // reseed support methods form here

    public ParamSet withReseedAction() {
        return withAction(createReseedAction());
    }

    public ParamSet withReseedNoiseAction() {
        return withAction(createReseedNoiseAction());
    }

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

    public FilterButtonModel createReseedCachedAndNoiseAction() {
        initReseedSupport(true);

        seedChangedAction = newSeed -> {
            Noise.reseed(newSeed);
            random.setSeed(newSeed);
            CachedFloatRandom.reBuildCache(random);
        };

        return FilterButtonModel.createReseed(() -> {
            reseed();
            seedChangedAction.accept(seed);
        });
    }

    public FilterButtonModel createReseedAction() {
        initReseedSupport(true);
        return FilterButtonModel.createReseed(this::reseed);
    }

    public FilterButtonModel createReseedAction(String name, String toolTipText) {
        initReseedSupport(true);
        return FilterButtonModel.createReseed(this::reseed, name, toolTipText);
    }

    public FilterButtonModel createReseedSimplexAction() {
        initReseedSupport(true);
        return FilterButtonModel.createReseed(this::reseedSimplex);
    }

    public FilterButtonModel createReseedAction(LongConsumer seedChangedAction) {
        initReseedSupport(false);

        this.seedChangedAction = seedChangedAction;
        return FilterButtonModel.createReseed(() -> {
            reseed();
            seedChangedAction.accept(seed);
        });
    }

    // end of reseed support

    @Override
    public String toString() {
        return "ParamSet {" + paramList + "}";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        for (FilterParam param : paramList) {
            node.add(param.createDebugNode(param.getName()));
        }

        return node;
    }
}
