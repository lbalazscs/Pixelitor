/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.animation;

import javax.swing.*;
import java.awt.FlowLayout;

public class DurationPanel extends JPanel {
    JTextField nrFramesTF = new JTextField(3);

    public DurationPanel() {
        super(new FlowLayout());
        add(new JLabel("Number of frames:"));
        add(nrFramesTF);
    }


    public int getNumFrames() {
        int nrFrames = Integer.parseInt(nrFramesTF.getText());
        return nrFrames;
    }
}
