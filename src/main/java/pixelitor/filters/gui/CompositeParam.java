/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.GUIText;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A composite {@link FilterParam} that groups child parameters.
 */
public class CompositeParam extends AbstractFilterParam {
    private final FilterParam[] children;
    private ResetButton resetButton;

    private final Layout layout;

    public CompositeParam(String name, FilterParam... children) {
        this(name, Layout.DIALOG, children);
    }

    private CompositeParam(String name, Layout layout, FilterParam... children) {
        super(name, RandomizeMode.ALLOW);
        this.layout = layout;
        this.children = children;
    }

    /**
     * Creates a {@link CompositeParam} that groups the given params
     * vertically using a common border.
     */
    public static CompositeParam bordered(String name, FilterParam... children) {
        for (FilterParam child : children) {
            child.setEmbedded(Layout.VERTICAL);
        }
        return new CompositeParam(name, Layout.VERTICAL, children);
    }

    /**
     * Creates a {@link CompositeParam} that groups the given params horizontally.
     */
    public static CompositeParam horizontal(String name, FilterParam... children) {
        for (FilterParam child : children) {
            child.setEmbedded(Layout.HORIZONTAL);
        }
        return new CompositeParam(name, Layout.HORIZONTAL, children);
    }

    @Override
    public JComponent createGUI() {
        paramGUI = switch (layout) {
            case DIALOG -> {
                resetButton = new ResetButton(this);
                yield new DialogLauncherGUI(this::configureDialog, resetButton);
            }
            case VERTICAL -> new VerGroupedParamGUI(this);
            case HORIZONTAL -> new HorGroupedParamGUI(this);
        };
        syncWithGui();
        return (JComponent) paramGUI;
    }

    private void configureDialog(DialogBuilder builder) {
        builder
            .content(GUIUtils.createVerticalPanel(List.of(children)))
            .title(getName())
            .withScrollbars()
            .okText(GUIText.CLOSE_DIALOG)
            .noCancelButton();
    }

    @Override
    protected void doRandomize() {
        for (FilterParam child : children) {
            child.randomize();
        }
        updateResetButtonState();
    }

    @Override
    public void adaptToContext(Filterable layer, boolean applyNewDefault) {
        for (FilterParam child : children) {
            child.adaptToContext(layer, applyNewDefault);
        }
    }

    @Override
    public CompositeParamState copyState() {
        return new CompositeParamState(children);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        CompositeParamState newStates = (CompositeParamState) state;
        Iterator<ParamState<?>> stateIterator = newStates.iterator();
        for (FilterParam child : children) {
            // this matching only works for animation
            if (child.isAnimatable()) {
                child.loadStateFrom(stateIterator.next(), updateGUI);
            }
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        for (FilterParam child : children) {
            preset.put(child.getPresetKey(), child.copyState().toPresetString());
        }
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        for (FilterParam child : children) {
            String savedString = preset.get(child.getPresetKey());
            if (savedString != null) { // presets don't have to include everything
                child.loadStateFrom(savedString);
            }
        }
        updateResetButtonState();
    }

    @Override
    public boolean isAnimatable() {
        for (FilterParam child : children) {
            if (child.isAnimatable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setEnabled(boolean enabled, EnabledReason reason) {
        // call super to set the enabled state of the launching button
        super.setEnabled(enabled, reason);

        for (FilterParam child : children) {
            // doesn't overwrite their internal FILTER_LOGIC state
            child.setEnabled(enabled, EnabledReason.PARENT_PARAM);
        }
    }

    @Override
    public boolean isAtDefault() {
        for (FilterParam child : children) {
            if (!child.isAtDefault()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset(boolean trigger) {
        for (FilterParam param : children) {
            param.reset(false);
        }
        if (trigger) {
            adjustmentListener.paramAdjusted();
        } else {
            // this class updates the reset button state
            // by putting a decorator on the adjustment
            // listeners, so this needs to be called here manually
            updateResetButtonState();
        }
    }

    private void updateResetButtonState() {
        if (resetButton != null) {
            resetButton.updateState();
        }
    }

    public FilterParam[] getChildren() {
        return children;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        ParamAdjustmentListener decoratedListener = () -> {
            updateResetButtonState();
            listener.paramAdjusted();
        };

        super.setAdjustmentListener(decoratedListener);

        for (FilterParam child : children) {
            child.setAdjustmentListener(decoratedListener);
        }
    }

    @Override
    public String getValueAsString() {
        return Stream.of(children)
            .map(FilterParam::getValueAsString)
            .collect(Collectors.joining(", ", "[", "]"));
    }
}
