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

import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Consumer;

/**
 * A UI panel with selectors for a color space and channel, plus a reset button.
 */
public class ChannelSelectorPanel extends JPanel {
    private final EnumParam<ColorSpace> colorSpaceParam;
    private final EnumParam<Channel> channelParam;

    /**
     * Creates a panel that notifies callbacks on channel change or reset.
     */
    public ChannelSelectorPanel(Consumer<Channel> channelChangedCallback, ActionListener resetListener) {
        super(new GridBagLayout());

        this.colorSpaceParam = ColorSpace.asParam();
        this.channelParam = Channel.asParam(colorSpaceParam.getSelected());

        GridBagHelper gbh = new GridBagHelper(this);

        // first row: color space
        gbh.addLabelAndControl(colorSpaceParam.getName() + ":", new JComboBox<>(colorSpaceParam));

        // second row: channel label + combo + reset
        JComboBox<Channel> channelCombo = new JComboBox<>(channelParam);
        gbh.addLabelAndControl(channelParam.getName() + ":", channelCombo);
        gbh.addNextControl(GUIUtils.createResetChannelButton(resetListener));

        wireComboBoxes(channelChangedCallback);
    }

    private void wireComboBoxes(Consumer<Channel> channelChangedCallback) {
        // when the color space changes, update the list of available channels
        colorSpaceParam.addOnChangeTask(() -> {
            List<Channel> newChoices = Channel.getChoices(colorSpaceParam.getSelected());
            // updating choices implicitly selects the first, triggering the channel change listener
            channelParam.setChoices(newChoices, false);
            revalidate();
        });

        // when the channel selection changes, notify the callback
        channelParam.addOnChangeTask(() -> {
            if (channelChangedCallback != null) {
                channelChangedCallback.accept(channelParam.getSelected());
            }
        });
    }

    /**
     * Adds a listener to be notified when the color space selection changes.
     */
    public void addColorSpaceChangedListener(Consumer<ColorSpace> listener) {
        colorSpaceParam.addOnChangeTask(() -> listener.accept(colorSpaceParam.getSelected()));
    }
}
