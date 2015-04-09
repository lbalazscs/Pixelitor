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

package pixelitor;

import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.OKCancelDialog;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static pixelitor.utils.SliderSpinner.TextPosition.BORDER;

/**
 *
 */
public class EnlargeCanvas {
    /**
     * Utility class with static methods
     */
    private EnlargeCanvas() {
    }

    private static void showInDialog() {
        EnlargeCanvasPanel panel = new EnlargeCanvasPanel();
        OKCancelDialog d = new OKCancelDialog(panel, "Enlarge Canvas") {
            @Override
            protected void dialogAccepted() {
                ImageComponents.getActiveComp().get().enlargeCanvas(
                        panel.getNorth(),
                        panel.getEast(),
                        panel.getSouth(),
                        panel.getWest()
                );
                close();
            }
        };
        d.setVisible(true);
    }

    public static Action getAction() {
        return new AbstractAction("Enlarge Canvas...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInDialog();
            }
        };
    }

    static class EnlargeCanvasPanel extends JPanel {
        final RangeParam northRange = new RangeParam("North", 0, 500, 0);
        final RangeParam eastRange = new RangeParam("East", 0, 500, 0);
        final RangeParam southRange = new RangeParam("South", 0, 500, 0);
        final RangeParam westRange = new RangeParam("West", 0, 500, 0);

        private EnlargeCanvasPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(new SliderSpinner(northRange, BORDER, false));
            add(new SliderSpinner(eastRange, BORDER, false));
            add(new SliderSpinner(southRange, BORDER, false));
            add(new SliderSpinner(westRange, BORDER, false));
        }

        public int getNorth() {
            return northRange.getValue();
        }

        public int getSouth() {
            return southRange.getValue();
        }

        public int getWest() {
            return westRange.getValue();
        }

        public int getEast() {
            return eastRange.getValue();
        }
    }
}
