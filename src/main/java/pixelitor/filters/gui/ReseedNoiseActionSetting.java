/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.utils.IconUtils;

import java.awt.event.ActionListener;

/**
 * An ActionParam that deals with reseeding some randomness
 */
public class ReseedNoiseActionSetting extends ActionSetting {
    // the first group of constructors is called if
    // Noise.reseed() does the reseed...

    public ReseedNoiseActionSetting() {
        this("Reseed");
    }

    public ReseedNoiseActionSetting(String name) {
        this(name, "<html>Reinitialize the randomness.<br>Has effect only if some randomness is enabled in the settings.");
    }

    public ReseedNoiseActionSetting(String name, String toolTip) {
        super(name, e -> Noise.reseed(), IconUtils.getDice2Icon(), toolTip);
        setIgnoreFinalAnimationSettingMode(false);
    }

        // ... and the second group of constructors is called if
    // the given ActionListener does the reseed

    public ReseedNoiseActionSetting(ActionListener actionListener) {
        this("Reseed", actionListener);
    }

    public ReseedNoiseActionSetting(String name, ActionListener actionListener) {
        this(name, "Reinitialize the randomness", actionListener);
    }

    public ReseedNoiseActionSetting(String name, String toolTipText, ActionListener actionListener) {
        super(name, actionListener, IconUtils.getDice2Icon(), toolTipText);
        setIgnoreFinalAnimationSettingMode(false);
    }
}
