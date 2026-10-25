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

    private final Layout layout;
    private JPanel dialogContentPanel;

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
        var gui = switch (layout) {
            case DIALOG -> new DialogLauncherGUI(this::configureDialog, new ResetButton(this));
            case VERTICAL -> new VerGroupedParamGUI(this);
            case HORIZONTAL -> new HorGroupedParamGUI(this);
        };
        return installGUI(gui);
    }

    private void configureDialog(DialogBuilder builder) {
        if (dialogContentPanel == null) {
            dialogContentPanel = GUIUtils.createVerticalPanel(List.of(children));
        }
        builder
            .content(dialogContentPanel)
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
            child.saveStateTo(preset);
        }
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        for (FilterParam child : children) {
            child.loadStateFrom(preset);
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
        // call super to update this parameter's own state (and launcher button GUI)
        super.setEnabled(enabled, reason);

        // children inherit this composite's overall enabled state as PARENT_PARAM,
        // while ANIMATION_ENDING_STATE is forwarded so each child can check if it is animatable
        for (FilterParam child : children) {
            child.setEnabled(isEnabled(), EnabledReason.PARENT_PARAM);
            if (reason == EnabledReason.ANIMATION_ENDING_STATE) {
                child.setEnabled(enabled, reason); // each child applies its own animatable check
            }
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
            triggerFilter();
        } else {
            // this class updates the reset button state
            // by putting a decorator on the adjustment
            // listeners, so this needs to be called here manually
            updateResetButtonState();
        }
    }

    private void updateResetButtonState() {
        if (paramGUI != null) {
            paramGUI.updateGUI();
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
    public void guiDisposed() {
        super.guiDisposed();
        if (layout == Layout.DIALOG && dialogContentPanel == null) {
            // the dialog was never opened, the children GUIs
            // were never created, so no cleanup is necessary
            return;
        }

        for (FilterParam child : children) {
            child.guiDisposed();
        }
        dialogContentPanel = null;
    }

    @Override
    public String getValueAsString() {
        return Stream.of(children)
            .map(FilterParam::getValueAsString)
            .collect(Collectors.joining(", ", "[", "]"));
    }
}
