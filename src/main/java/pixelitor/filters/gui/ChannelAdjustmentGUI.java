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

import pixelitor.filters.Filter;
import pixelitor.filters.channels.ChannelAdjustmentsModel;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;

/**
 * Shared GUI shell for channel-based adjustments across color spaces.
 *
 * @param <M> the channel adjustments model type
 */
public abstract class ChannelAdjustmentGUI<M extends ChannelAdjustmentsModel<?>>
    extends FilterGUI implements ChannelAdjustmentsModel.View {
    protected final M model;
    protected final JComponent editorComponent;
    private final ChannelSelectorPanel channelSelectorPanel;
    private boolean initialized = false;

    /**
     * Creates a new channel adjustment GUI shell.
     *
     * @param filter          the filter being configured
     * @param layer           the filterable layer
     * @param model           the backing adjustment model
     * @param editorComponent the center editor component
     */
    protected ChannelAdjustmentGUI(Filter filter, Filterable layer, M model, JComponent editorComponent) {
        super(filter, layer);
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.editorComponent = Objects.requireNonNull(editorComponent, "editorComponent must not be null");

        setLayout(new BorderLayout());

        channelSelectorPanel = new ChannelSelectorPanel(channel -> {
            if (initialized && model.setActiveChannel(channel)) {
                refreshEditor();
            }
        });
        channelSelectorPanel.addResetButton(_ -> {
            if (initialized) {
                model.resetActiveAdjustment();
                refreshEditor();
                model.fireAdjustmentChanged();
            }
        });
        channelSelectorPanel.addColorSpaceChangedListener(colorSpace -> {
            if (initialized) {
                model.setColorSpace(colorSpace);
                refreshEditor();
                model.fireAdjustmentChanged();
            }
        });

        add(channelSelectorPanel, BorderLayout.NORTH);
        add(editorComponent, BorderLayout.CENTER);
        add(createSouthPanel(layer), BorderLayout.SOUTH);

        model.setAdjustmentListener(() -> startPreview(false));
        model.setGUI(this);

        initialized = true;
        updateGUI();
    }

    /**
     * Updates the editor component when the active channel or color space changes.
     */
    protected abstract void refreshEditor();

    @Override
    public void updateGUI() {
        channelSelectorPanel.setSelection(model.getColorSpace(), model.getActiveChannel());
        refreshEditor();
    }

    private JPanel createSouthPanel(Filterable layer) {
        var southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        var showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(_ -> layer.setShowOriginal(showOriginalCB.isSelected()));
        southPanel.add(showOriginalCB);

        southPanel.add(GUIUtils.createRandomizeSettingsButton(_ ->
            model.randomize()));

        southPanel.add(GUIUtils.createResetAllButton(_ ->
            model.resetAll()));

        return southPanel;
    }

    @Override
    public void dispose() {
        model.setGUI(null);
        model.setAdjustmentListener(null);
        super.dispose();
    }

    public M getModel() {
        return model;
    }

    public JComponent getEditorComponent() {
        return editorComponent;
    }
}
