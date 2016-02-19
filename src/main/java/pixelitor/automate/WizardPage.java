/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.automate;

import pixelitor.layers.ImageLayer;

import javax.swing.*;

public interface WizardPage {
    String getHeaderText(Wizard wizard);

    WizardPage getNext();

    JComponent getPanel(Wizard wizard, ImageLayer layer);

    /**
     * Called if the wizard was cancelled while in this state
     */
    void onWizardCancelled(ImageLayer layer);

    /**
     * Called if next was pressed while in this state before moving to the next
     */
    void onMovingToTheNext(Wizard wizard, ImageLayer layer);
}
