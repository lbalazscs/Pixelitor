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
    private final GridBagHelper gbh;

    // true while the selectors are being synchronized with an external model
    // via setSelection(); callbacks and listeners are suppressed during that time
    private boolean updatingProgrammatically = false;

    public ChannelSelectorPanel(Consumer<Channel> channelChangeListener) {
        super(new GridBagLayout());

        this.colorSpaceParam = ColorSpace.asParam();
        this.channelParam = Channel.asParam(colorSpaceParam.getSelected());

        gbh = new GridBagHelper(this);

        // first row: color space
        gbh.addLabelAndControl(colorSpaceParam.getName() + ":", new JComboBox<>(colorSpaceParam));

        // second row: channel label + combo
        JComboBox<Channel> channelCombo = new JComboBox<>(channelParam);
        gbh.addLabelAndControl(channelParam.getName() + ":", channelCombo);

        wireComboBoxes(channelChangeListener);
    }

    public void addResetButton(ActionListener resetListener) {
        gbh.addNextControl(GUIUtils.createResetChannelButton(resetListener));
    }

    private void wireComboBoxes(Consumer<Channel> channelChangeListener) {
        // when the color space changes, update the list of available channels
        colorSpaceParam.addSelectionListener(() -> {
            if (updatingProgrammatically) {
                return;
            }
            List<Channel> newChoices = colorSpaceParam.getSelected().getChannels();
            // updating choices implicitly selects the first, triggering the channel change listener
            channelParam.setChoices(newChoices, false);
            revalidate();
        });

        // when the channel selection changes, notify the listener
        channelParam.addSelectionListener(() -> {
            if (updatingProgrammatically) {
                return;
            }
            if (channelChangeListener != null) {
                channelChangeListener.accept(channelParam.getSelected());
            }
        });
    }

    /**
     * Adds a listener to be notified when the user changes the color space
     * selection. It is not notified by {@link #setSelection}.
     */
    public void addColorSpaceChangedListener(Consumer<ColorSpace> listener) {
        colorSpaceParam.addSelectionListener(() -> {
            if (updatingProgrammatically) {
                return;
            }
            listener.accept(colorSpaceParam.getSelected());
        });
    }

    public Channel getSelectedChannel() {
        return channelParam.getSelected();
    }

    /**
     * Synchronizes both selectors with the given state (typically the
     * state of the data model) without notifying any callbacks or listeners,
     * so the caller must update anything that depends on the selected channel.
     * The channel must belong to the given color space.
     */
    public void setSelection(ColorSpace space, Channel channel) {
        assert channel.getColorSpace() == space
            : "channel " + channel + " doesn't belong to " + space;

        updatingProgrammatically = true;
        try {
            colorSpaceParam.setSelectedItem(space, false);
            channelParam.setChoices(space.getChannels(), false);
            channelParam.setSelectedItem(channel, false);
        } finally {
            updatingProgrammatically = false;
        }
        // the guarded color-space listener didn't do it
        revalidate();
    }
}
