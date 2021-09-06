/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.fixture.*;
import pixelitor.utils.Rnd;

import javax.swing.*;

/**
 * Static utility methods related to AssertJ-Swing
 */
public class AJSUtils {
    private AJSUtils() {
        // should not be instantiated
    }

    static JButtonFixture findButtonByText(ComponentContainerFixture container, String text) {
        var matcher = JButtonMatcher.withText(text).andShowing();
        return container.button(matcher);
    }

    static JMenuItemFixture findPopupMenuFixtureByText(JPopupMenuFixture popupMenu, String text) {
        return popupMenu.menuItem(
            new GenericTypeMatcher<>(JMenuItem.class) {
                @Override
                protected boolean isMatching(JMenuItem menuItem) {
                    if (!menuItem.isShowing()) {
                        return false; // not interested in menuItems that are not currently displayed
                    }
                    String menuItemText = menuItem.getText();
                    if (menuItemText == null) {
                        menuItemText = "";
                    }
                    return menuItemText.equals(text);
                }

                @Override
                public String toString() {
                    return "[Popup menu item Matcher, text = " + text + "]";
                }
            });
    }

    static JButtonFixture findButtonByActionName(ComponentContainerFixture container, String actionName) {
        return container.button(
            new GenericTypeMatcher<>(JButton.class) {
                @Override
                protected boolean isMatching(JButton button) {
                    if (!button.isShowing()) {
                        return false; // not interested in buttons that are not currently displayed
                    }
                    Action action = button.getAction();
                    if (action == null) {
                        return false;
                    }
                    String buttonActionName = (String) action.getValue(Action.NAME);
                    return actionName.equals(buttonActionName);
                }

                @Override
                public String toString() {
                    return "[Button Action Name Matcher, action name = " + actionName + "]";
                }
            });
    }

    static JButtonFixture findButtonByToolTip(ComponentContainerFixture container, String toolTip) {
        return container.button(
            new GenericTypeMatcher<>(JButton.class) {
                @Override
                protected boolean isMatching(JButton button) {
                    if (!button.isShowing()) {
                        return false; // not interested in buttons that are not currently displayed
                    }
                    String buttonToolTip = button.getToolTipText();
                    if (buttonToolTip == null) {
                        buttonToolTip = "";
                    }
                    return buttonToolTip.equals(toolTip);
                }

                @Override
                public String toString() {
                    return "[Button Tooltip Matcher, tooltip = " + toolTip + "]";
                }
            });
    }

    static void slideRandomly(JSliderFixture slider) {
        JSlider target = slider.target();
        int min = target.getMinimum();
        int max = target.getMaximum();
        slider.slideTo(Rnd.intInRange(min, max));
    }

    static void chooseRandomly(JComboBoxFixture combo) {
        String newValue = Rnd.chooseFrom(combo.contents());
        combo.selectItem(newValue);
    }

    static void checkRandomly(JCheckBoxFixture checkBox) {
        if (Rnd.nextBoolean()) {
            checkBox.uncheck();
        } else {
            checkBox.check();
        }
    }

    static void pushRandomly(double p, JButtonFixture button) {
        Rnd.withProbability(p, button::click);
    }
}
