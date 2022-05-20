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

package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import org.jdesktop.swingx.painter.TextPainter;
import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.UserPreset;
import pixelitor.layers.TextLayer;
import pixelitor.utils.*;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Consumer;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * Settings for the text filter and text layers.
 * Edited by the {@link TextSettingsPanel}.
 */
public class TextSettings implements Serializable, Debuggable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_TEXT = "Pixelitor";
    private static final String PRESET_KEY_TEXT = "text";
    private static final String PRESET_KEY_COLOR = "color";
    private static final String PRESET_KEY_ROTATION = "rotation";
    private static final String PRESET_KEY_HOR_ALIGN = "hor_align";
    private static final String PRESET_KEY_VER_ALIGN = "ver_align";
    private static final String PRESET_KEY_WATERMARK = "watermark";

    private String text;
    private Font font;
    private AreaEffects areaEffects;
    private Color color;
    private VerticalAlignment verticalAlignment;
    private HorizontalAlignment horizontalAlignment;
    private boolean watermark;
    private double rotation;

    private transient Consumer<TextSettings> guiUpdater;

    public TextSettings(String text, Font font, Color color,
                        AreaEffects effects,
                        HorizontalAlignment horizontalAlignment,
                        VerticalAlignment verticalAlignment,
                        boolean watermark, double rotation,
                        Consumer<TextSettings> guiUpdater) {
        assert effects != null;

        this.areaEffects = effects;
        this.color = color;
        this.font = font;
        this.horizontalAlignment = horizontalAlignment;
        this.text = text;
        this.verticalAlignment = verticalAlignment;
        this.watermark = watermark;
        this.rotation = rotation;
        this.guiUpdater = guiUpdater;
    }

    /**
     * Default settings
     */
    public TextSettings() {
        areaEffects = new AreaEffects();
        color = WHITE;
        font = calcDefaultFont();
        horizontalAlignment = HorizontalAlignment.CENTER;
        text = DEFAULT_TEXT;
        verticalAlignment = VerticalAlignment.CENTER;
        watermark = false;
        rotation = 0;
    }

    /**
     * Copy constructor
     */
    private TextSettings(TextSettings other) {
        text = other.text;
        font = other.font;
        // even mutable objects can be shared, since they are re-created
        // after every editing
        areaEffects = other.areaEffects;
        color = other.color;
        verticalAlignment = other.verticalAlignment;
        horizontalAlignment = other.horizontalAlignment;
        watermark = other.watermark;
        rotation = other.rotation;
    }

    public AreaEffects getEffects() {
        return areaEffects;
    }

    public Color getColor() {
        return color;
    }

    @VisibleForTesting
    public void setColor(Color color) {
        this.color = color;
    }

    public Font getFont() {
        return font;
    }

    @VisibleForTesting
    public void setFont(Font font) {
        this.font = font;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    public boolean hasWatermark() {
        return watermark;
    }

    public void setWatermark(boolean watermark) {
        this.watermark = watermark;
    }

    public double getRotation() {
        return rotation;
    }

    public void randomize() {
        text = Rnd.createRandomString(10);
        font = Rnd.createRandomFont();
        areaEffects = Rnd.createRandomEffects();
        color = Rnd.createRandomColor();
        horizontalAlignment = Rnd.chooseFrom(HorizontalAlignment.values());
        verticalAlignment = Rnd.chooseFrom(VerticalAlignment.values());
        watermark = Rnd.nextBoolean();
        rotation = Rnd.nextDouble() * Math.PI * 2;
    }

    public void configurePainter(TransformedTextPainter painter) {
        painter.setAntialiasing(true);
        painter.setText(text);
        painter.setFont(font);
        if (areaEffects != null) {
            painter.setAreaEffects(areaEffects.asArray());
        }
        painter.setHorizontalAlignment(horizontalAlignment);
        painter.setVerticalAlignment(verticalAlignment);
        painter.setRotation(rotation);
    }

    public BufferedImage watermarkImage(BufferedImage src, TextPainter textPainter) {
        BufferedImage bumpImage = createBumpMapImage(
            textPainter, src.getWidth(), src.getHeight());
        return ImageUtils.bumpMap(src, bumpImage, "Watermarking");
    }

    // the bump map image has white text on a black background
    private BufferedImage createBumpMapImage(TextPainter textPainter,
                                             int width, int height) {
        BufferedImage bumpImage = new BufferedImage(width, height, TYPE_INT_RGB);
        Graphics2D g = bumpImage.createGraphics();
        Colors.fillWith(BLACK, g, width, height);
        textPainter.setFillPaint(WHITE);
        textPainter.paint(g, this, width, height);
        g.dispose();

        return bumpImage;
    }

    private static Font calcDefaultFont() {
        String[] fontNames = Utils.getAvailableFontNames();
        return new Font(fontNames[0], Font.PLAIN, calcDefaultFontSize());
    }

    private static int calcDefaultFontSize() {
        Composition comp = Views.getActiveComp();
        if (comp != null) {
            int canvasHeight = comp.getCanvasHeight();
            int size = (int) (canvasHeight * 0.2);
            if (size == 0) {
                size = 1;
            }
            return size;
        } else {
            return 100;
        }
    }

    public void setGuiUpdater(Consumer<TextSettings> guiUpdater) {
        this.guiUpdater = guiUpdater;
    }

    public TextSettings copy() {
        return new TextSettings(this);
    }

    public void saveStateTo(UserPreset preset) {
        preset.put(PRESET_KEY_TEXT, text);
        preset.putColor(PRESET_KEY_COLOR, color);
        preset.putFloat(PRESET_KEY_ROTATION, (float) rotation);
        preset.putInt(PRESET_KEY_HOR_ALIGN, horizontalAlignment.ordinal());
        preset.putInt(PRESET_KEY_VER_ALIGN, verticalAlignment.ordinal());

        FontInfo fontInfo = new FontInfo(font);
        fontInfo.saveStateTo(preset);

        areaEffects.saveStateTo(preset);

        preset.putBoolean(PRESET_KEY_WATERMARK, watermark);
    }

    public void loadUserPreset(UserPreset preset) {
        text = preset.get(PRESET_KEY_TEXT);
        color = preset.getColor(PRESET_KEY_COLOR);
        rotation = preset.getFloat(PRESET_KEY_ROTATION);
        horizontalAlignment = HorizontalAlignment.values()[preset.getInt(PRESET_KEY_HOR_ALIGN)];
        verticalAlignment = VerticalAlignment.values()[preset.getInt(PRESET_KEY_VER_ALIGN)];

        FontInfo fontInfo = new FontInfo(preset);
        font = fontInfo.createFont();

        areaEffects.loadStateFrom(preset);
        watermark = preset.getBoolean(PRESET_KEY_WATERMARK);

        if (guiUpdater != null) { // can be null in tests that don't create a dialog
            guiUpdater.accept(this);
        }
    }

    public void checkFontIsInstalled(TextLayer textLayer) {
        if (AppContext.isUnitTesting()) {
            // the fonts are not found when testing in the cloud, but that's OK
            return;
        }
        String fontName = font.getName();
        int index = Arrays.binarySearch(Utils.getAvailableFontNames(), fontName);
        if (index < 0) {
            EventQueue.invokeLater(() -> Messages.showError("Font Not Found",
                "<html>The font <b>" + fontName + "</b> was not found on this computer." +
                "<br>It's used in the text layer <b>" + textLayer.getName() + "</b>."));
        }
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addQuotedString("Text", getText());
        node.addBoolean("Watermark", watermark);

        return node;
    }
}
