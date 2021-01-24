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

package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import org.jdesktop.swingx.painter.TextPainter;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.UserPreset;
import pixelitor.layers.TextLayer;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;
import pixelitor.utils.VisibleForTesting;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;
import java.util.function.Consumer;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * Settings for the text filter and text layers.
 * Edited by the {@link TextSettingsPanel}.
 */
public class TextSettings implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_TEXT = "Pixelitor";

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
    public TextSettings(TextSettings other) {
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
        Composition comp = OpenImages.getActiveComp();
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

    public UserPreset createUserPreset(String presetName) {
        UserPreset preset = new UserPreset(presetName, TextLayer.TEXT_PRESETS_DIR_NAME);
        preset.put("text", text);
        preset.putColor("color", color);
        preset.putFloat("rotation", (float) rotation);
        preset.putInt("hor_align", horizontalAlignment.ordinal());
        preset.putInt("ver_align", verticalAlignment.ordinal());

        FontInfo fontInfo = new FontInfo(font);
        fontInfo.saveStateTo(preset);

        areaEffects.saveStateTo(preset);

        preset.putBoolean("watermark", watermark);

        return preset;
    }

    public void loadUserPreset(UserPreset preset) {
        text = preset.get("text");
        color = preset.getColor("color");
        rotation = preset.getFloat("rotation");
        horizontalAlignment = HorizontalAlignment.values()[preset.getInt("hor_align")];
        verticalAlignment = VerticalAlignment.values()[preset.getInt("ver_align")];

        FontInfo fontInfo = new FontInfo(preset);
        font = fontInfo.createFont();

        areaEffects.loadStateFrom(preset);
        watermark = preset.getBoolean("watermark");

        // should be always non-null while loading a preset,
        // because this happens only in the dialog
        guiUpdater.accept(this);
    }
}
