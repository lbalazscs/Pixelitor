/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.JXTipOfTheDay;
import org.jdesktop.swingx.tips.TipLoader;
import org.jdesktop.swingx.tips.TipOfTheDayModel;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * Loads the tips of the day from a properties file and shows them.
 */
public class TipsOfTheDay {
    private static final Preferences tipPrefs = AppPreferences.getMainPrefs();

    private static final String NEXT_TIP_INDEX_KEY = "next_tip_nr";

    private static int nextTipIndex = tipPrefs.getInt(NEXT_TIP_INDEX_KEY, 0);

    private TipsOfTheDay() {
    }

    public static void showTips(JFrame parent, boolean force) {
        try {
            TipOfTheDayModel model = loadTipsModel();
            int tipCount = model.getTipCount();

            // ensure that the tip index is within valid range
            nextTipIndex = Math.min(Math.max(nextTipIndex, 0), tipCount - 1);

            var dialogPreferredSize = new Dimension(480, 230);
            var tipOfTheDay = new JXTipOfTheDay(model) {
                @Override
                public Dimension getPreferredSize() {
                    return dialogPreferredSize;
                }
            };
            tipOfTheDay.setCurrentTip(nextTipIndex);

            // this blocks until the user closes the dialog
            tipOfTheDay.showDialog(parent, tipPrefs, force);

            // updates the next tip index based on the last shown tip
            int lastTipSeen = tipOfTheDay.getCurrentTip();
            nextTipIndex = lastTipSeen < tipCount - 1 ? lastTipSeen + 1 : 0;
        } catch (IOException ex) {
            Messages.showException(ex);
        }
    }

    /**
     * Loads the tips model based on the current locale.
     */
    // TODO this method is called at startup, and then called again
    //   whenever the dialog is shown
    private static TipOfTheDayModel loadTipsModel() throws IOException {
//        ResourceBundle bundle = ResourceBundle.getBundle("tips", Locale.getDefault());
        // Here we can't use a ResourceBundle, because the TipLoader
        // expects Properties, but we need the replicate the
        // ResourceBundle's file resolution logic for consistency.
        Locale locale = Locale.getDefault();
        String langCode = locale.getLanguage();
        String fileName = switch (langCode) {
            case "en" -> "/tips.properties";
            case "pt" -> "/tips_pt_BR.properties";
            default -> "/tips_" + langCode + ".properties";
        };

        try {
            return loadTipsModelFromFile(fileName);
        } catch (FileNotFoundException e) {
            // fallback to English tips in the case of a
            // supported language without a localized tips file
            return loadTipsModelFromFile("/tips.properties");
        }
    }

    /**
     * Loads the tips model from a specific properties file.
     */
    private static TipOfTheDayModel loadTipsModelFromFile(String fileName) throws IOException {
        var properties = new Properties();
        try (var inputStream = TipsOfTheDay.class.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new FileNotFoundException(fileName);
            }
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
        return TipLoader.load(properties);
    }

    public static void saveNextTipIndex() {
        tipPrefs.putInt(NEXT_TIP_INDEX_KEY, nextTipIndex);
    }
}
