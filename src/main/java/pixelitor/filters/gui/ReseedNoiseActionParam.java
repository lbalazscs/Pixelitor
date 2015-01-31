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

import java.awt.event.ActionListener;

/**
 * An ActionParam that deals with reseeding some randomness
 */
public class ReseedNoiseActionParam extends ActionParam {
    // the first group of constructors is called if
    // Noise.reseed() does the reseed...

    public ReseedNoiseActionParam() {
        this("Reseed");
    }

    public ReseedNoiseActionParam(String name) {
        super(name, e -> Noise.reseed(), "<html>Reinitialize the randomness.<br>Has effect only if some randomness is enabled in the settings.");
        setIgnoreFinalAnimationSettingMode(false);
    }

    // ... and the second group of constructors is called if
    // the given ActionListener does the reseed

    public ReseedNoiseActionParam(ActionListener actionListener) {
        this("Reseed", actionListener);
    }

    public ReseedNoiseActionParam(String name, ActionListener actionListener) {
        this(name, "Reinitialize the randomness", actionListener);
    }

    public ReseedNoiseActionParam(String name, String toolTipText, ActionListener actionListener) {
        super(name, actionListener, toolTipText);
        setIgnoreFinalAnimationSettingMode(false);
    }
}
