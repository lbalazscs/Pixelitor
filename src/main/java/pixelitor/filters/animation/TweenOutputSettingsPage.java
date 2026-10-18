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

package pixelitor.filters.animation;

import pixelitor.automate.WizardPage;
import pixelitor.gui.utils.ValidationResult;

import javax.swing.*;
import java.util.Objects;

/**
 * The final page in the {@link TweenWizard}, where the user configures
 * animation output settings such as frame rate, duration, and target file.
 */
public class TweenOutputSettingsPage implements WizardPage {
    // cached to remember output settings between wizard runs;
    // this may later be replaced by a general panel-caching mechanism
    private static TweenOutputSettingsPanel cachedPanel;

    private final TweenWizard wizard;
    private TweenOutputSettingsPanel outputSettingsPanel;

    public TweenOutputSettingsPage(TweenWizard wizard) {
        this.wizard = Objects.requireNonNull(wizard);
    }

    @Override
    public JComponent createPanel() {
        if (cachedPanel == null) {
            cachedPanel = new TweenOutputSettingsPanel();
        }
        outputSettingsPanel = cachedPanel;
        return outputSettingsPanel;
    }

    @Override
    public String getHelpText() {
        return "<b>Animation Output Settings</b>" +
            "<br>Choose how to save your animation:<ul>" +
            "<li>For file sequences: Select an existing folder" +
            "<li>For a single file: Choose a new or existing file in an existing folder";
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public boolean validatePage() {
        ValidationResult validity = outputSettingsPanel.validateSettings();
        if (!validity.isValid()) {
            validity.showErrorDialog(outputSettingsPanel);
            return false;
        }

        TweenAnimation animation = wizard.getAnimation();
        outputSettingsPanel.applyTo(animation);

        return animation.confirmOverwrite(outputSettingsPanel);
    }

    @Override
    public void onNext() {
        // the settings were already saved during validation
    }
}
