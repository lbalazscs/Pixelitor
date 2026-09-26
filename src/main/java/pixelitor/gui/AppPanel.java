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

package pixelitor.gui;

import pixelitor.layers.LayersContainer;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.gui.ToolsPanel;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Defines the toggleable UI panels, and their target containers,
 * layout constraints, persistence keys, and component instances.
 */
public enum AppPanel {
    HISTOGRAMS(
        "histograms", false,
        TargetContainer.SIDE_PANEL, BorderLayout.NORTH,
        HistogramsPanel::new
    ),
    LAYERS(
        "layers", true,
        TargetContainer.SIDE_PANEL, BorderLayout.CENTER,
        LayersContainer::new
    ),
    TOOLS(
        "tools", true,
        TargetContainer.CONTENT_PANE, BorderLayout.WEST,
        ToolsPanel::new
    ),
    // Linked to TOOLS: has no independent preference key or toggle actions
    TOOL_SETTINGS(
        TargetContainer.CONTENT_PANE, BorderLayout.NORTH,
        ToolSettingsPanelContainer::new
    ),
    STATUS_BAR(
        "status_bar", true,
        TargetContainer.CONTENT_PANE, BorderLayout.SOUTH,
        StatusBar::new
    );

    public enum TargetContainer {
        CONTENT_PANE,
        SIDE_PANEL;

        Container resolveContainer(PixelitorWindow pw) {
            return switch (this) {
                case CONTENT_PANE -> pw.getContentPane();
                case SIDE_PANEL -> pw.getSidePanel();
            };
        }
    }

    private final String prefKey;
    private final boolean defaultVisible;
    private final TargetContainer target;
    private final Object layoutConstraint;
    private final Supplier<? extends JComponent> factory;
    private final String showActionKey;
    private final String hideActionKey;

    private static final Map<AppPanel, JComponent> registry = new EnumMap<>(AppPanel.class);
    public static final AppPanel[] PANELS = values();

    /**
     * Constructor for independently toggleable panels with persistent visibility.
     */
    AppPanel(String baseKey, boolean defaultVisible, TargetContainer target,
             Object layoutConstraint, Supplier<? extends JComponent> factory) {
        this(baseKey + "_shown", defaultVisible, target, layoutConstraint, factory,
            "show_" + baseKey, "hide_" + baseKey);
    }

    /**
     * Constructor for dependent panels (e.g. TOOL_SETTINGS) that are not
     * independently toggleable and do not store individual preferences.
     */
    AppPanel(TargetContainer target, Object layoutConstraint,
             Supplier<? extends JComponent> factory) {
        this(null, false, target, layoutConstraint, factory, null, null);
    }

    /**
     * Canonical private constructor.
     */
    AppPanel(String prefKey, boolean defaultVisible, TargetContainer target,
             Object layoutConstraint, Supplier<? extends JComponent> factory,
             String showActionKey, String hideActionKey) {
        this.prefKey = prefKey;
        this.defaultVisible = defaultVisible;
        this.target = target;
        this.layoutConstraint = layoutConstraint;
        this.factory = factory;
        this.showActionKey = showActionKey;
        this.hideActionKey = hideActionKey;
    }

    /**
     * Resolves and returns the panel's singleton instance from
     * the registry, instantiating it lazily if necessary.
     */
    @SuppressWarnings("unchecked")
    public <T extends JComponent> T getComponent() {
        return (T) registry.computeIfAbsent(this, k -> factory.get());
    }

    /**
     * Overrides an instance in the registry (useful for unit tests / mocking).
     */
    public void setComponent(JComponent component) {
        registry.put(this, component);
    }

    /**
     * Checks if the component is currently physically attached to a parent container.
     */
    public boolean isShown() {
        JComponent comp = registry.get(this);
        return comp != null && comp.getParent() != null;
    }

    public boolean hasPrefKey() {
        return prefKey != null;
    }

    public String getPrefKey() {
        return prefKey;
    }

    public boolean isDefaultVisible() {
        return defaultVisible;
    }

    public TargetContainer getTarget() {
        return target;
    }

    public Object getLayoutConstraint() {
        return layoutConstraint;
    }

    public boolean hasToggleAction() {
        return showActionKey != null;
    }

    public String getShowActionKey() {
        return showActionKey;
    }

    public String getHideActionKey() {
        return hideActionKey;
    }
}
