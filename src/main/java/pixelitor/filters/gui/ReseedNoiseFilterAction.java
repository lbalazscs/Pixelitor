/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Icons;

import java.awt.event.ActionListener;

/**
 * A {@link FilterAction} that deals with reseeding some randomness
 */
public class ReseedNoiseFilterAction extends FilterAction {
    // the first group of constructors is called if
    // Noise.reseed() does the reseeding...

    public ReseedNoiseFilterAction() {
        this("Reseed");
    }

    public ReseedNoiseFilterAction(String name) {
        this(name, "Reinitialize the randomness.");
    }

    public ReseedNoiseFilterAction(String name, String toolTip) {
        super(name, e -> Noise.reseed(),
                Icons.getTwoDicesIcon(), toolTip, "reseed");
        setIgnoreFinalAnimationSettingMode(false);
    }

    // ... and the second group of constructors is called if
    // the given ActionListener does the reseeding

    public ReseedNoiseFilterAction(ActionListener actionListener) {
        this("Reseed", actionListener);
    }

    public ReseedNoiseFilterAction(String name, ActionListener actionListener) {
        this(name, "Reinitialize the randomness", actionListener);
    }

    public ReseedNoiseFilterAction(String name, String toolTipText,
                                   ActionListener actionListener) {
        super(name, actionListener, Icons.getTwoDicesIcon(),
                toolTipText, "reseed");
        setIgnoreFinalAnimationSettingMode(false);
    }
}
