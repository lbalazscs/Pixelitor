/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import com.twelvemonkeys.image.ImageUtil;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.IndexedModePanel;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Lazy;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

public enum ImageMode {
    RGB {
        @Override
        public BufferedImage convert(BufferedImage src) {
            if (src.getColorModel() instanceof IndexColorModel) {
                BufferedImage dst = ImageUtils.createSysCompatibleImage(
                    src.getWidth(), src.getHeight());
                Graphics2D g2 = dst.createGraphics();
                g2.drawImage(src, 0, 0, null);
                g2.dispose();
                src.flush();
                return dst;
            } else {
                return src;
            }
        }
    }, Indexed {
        @Override
        public BufferedImage convert(BufferedImage src) {
            if (src.getColorModel() instanceof IndexColorModel) {
                return src;
            } else {
                var panel = new IndexedModePanel();
                panel.showInDialog();

                int hints = ImageUtil.COLOR_SELECTION_FAST + ImageUtil.DITHER_DEFAULT;
                boolean transparency = panel.supportTransparency();
                hints += (transparency ?
                    ImageUtil.TRANSPARENCY_BITMASK :
                    ImageUtil.TRANSPARENCY_OPAQUE);
                Color matteColor = transparency ? null : FgBgColors.getBGColor();

                BufferedImage dst = ImageUtil.createIndexed(src,
                    256, matteColor, hints);
                src.flush();
                return dst;
            }
        }
    };

    private final Lazy<JMenuItem> menuItem = Lazy.of(this::createMenuItem);

    private JMenuItem createMenuItem() {
        Action action = new OpenViewEnabledAction(toString(),
            comp -> comp.changeMode(ImageMode.this));

        return new JRadioButtonMenuItem(action);
    }

    public JMenuItem getMenuItem() {
        return menuItem.get();
    }

    public abstract BufferedImage convert(BufferedImage src);
}
