/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.AlignmentSelector;
import pixelitor.gui.utils.BoxAlignment;
import pixelitor.layers.TextLayer;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.Debuggable;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import static java.awt.Color.WHITE;
import static java.awt.font.TextAttribute.KERNING;
import static java.awt.font.TextAttribute.KERNING_ON;

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
    private static final String PRESET_KEY_ALIGN = "align";
    private static final String PRESET_KEY_MLP_ALIGN = "mlp_align";
    private static final String PRESET_KEY_WATERMARK = "watermark";
    private static final String PRESET_KEY_REL_LINE_HEIGHT = "rel_line_height";
    private static final String PRESET_KEY_SX = "sx";
    private static final String PRESET_KEY_SY = "sy";
    private static final String PRESET_KEY_SHX = "shx";
    private static final String PRESET_KEY_SHY = "shy";

    // legacy keys maintained for backward compatibility
    private static final String PRESET_KEY_HOR_ALIGN = "hor_align";
    private static final String PRESET_KEY_VER_ALIGN = "ver_align";

    private String text;
    private Font font;
    private AreaEffects areaEffects;
    private Color color;
    private VerticalAlignment verticalAlignment;
    private HorizontalAlignment horizontalAlignment;
    private boolean watermark;
    private int mlpAlignment;

    private double rotation;
    private double sx;
    private double sy;
    private double shx;
    private double shy;
    private double relLineHeight;

    // in old pxc files this flag will not be present
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
    private boolean transformFieldsInPxc = true;

    private transient Consumer<TextSettings> guiUpdateCallback;

    public TextSettings(String text, Font font, Color color,
                        AreaEffects effects,
                        HorizontalAlignment horizontalAlignment,
                        VerticalAlignment verticalAlignment,
                        int mlpAlignment,
                        boolean watermark, double rotation,
                        double relLineHeight,
                        double sx, double sy,
                        double shx, double shy,
                        Consumer<TextSettings> guiUpdateCallback) {
        assert effects != null;

        this.text = text;
        this.areaEffects = effects;
        this.color = color;
        this.font = font;
        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
        this.mlpAlignment = mlpAlignment;
        this.watermark = watermark;
        this.rotation = rotation;
        this.relLineHeight = relLineHeight;
        this.sx = sx;
        this.sy = sy;
        this.shx = shx;
        this.shy = shy;
        this.guiUpdateCallback = guiUpdateCallback;
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
        mlpAlignment = AlignmentSelector.LEFT;

        rotation = 0;
        relLineHeight = 1.0;
        sx = 1.0;
        sy = 1.0;
        shx = 0.0;
        shy = 0.0;
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
        mlpAlignment = other.mlpAlignment;
        watermark = other.watermark;
        rotation = other.rotation;
        relLineHeight = other.relLineHeight;
        sx = other.sx;
        sy = other.sy;
        shx = other.shx;
        shy = other.shy;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        guiUpdateCallback = null;
        // migrate old pxc files
        if (!transformFieldsInPxc) {
            relLineHeight = 1.0;
            sx = 1.0;
            sy = 1.0;
            shx = 0.0;
            shy = 0.0;
        }
        if (mlpAlignment == 0) {
            // field not found in old pxc files
            mlpAlignment = AlignmentSelector.LEFT;
        }
    }

    public TextSettings copy() {
        return new TextSettings(this);
    }

    public void configurePainter(TransformedTextPainter painter) {
        painter.setText(text);
        painter.setFont(font);
        painter.setEffects(areaEffects);
        painter.setAlignment(horizontalAlignment, verticalAlignment);
        painter.setMLPAlignment(mlpAlignment);
        painter.setRotation(rotation);
        painter.setAdvancedSettings(relLineHeight, sx, sy, shx, shy);
    }

    private static Font calcDefaultFont() {
        String[] fontNames = Utils.getAvailableFontNames();
        return new Font(fontNames[0], Font.PLAIN, calcDefaultFontSize())
            .deriveFont(Map.of(KERNING, KERNING_ON));
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

    public void randomize() {
        text = Rnd.createRandomString(10);
        font = Rnd.createRandomFont();
        areaEffects = Rnd.createRandomEffects();
        color = Rnd.createRandomColor();
        horizontalAlignment = Rnd.chooseFrom(HorizontalAlignment.values());
        verticalAlignment = Rnd.chooseFrom(VerticalAlignment.values());
        mlpAlignment = Rnd.chooseFrom(new int[]{
            AlignmentSelector.LEFT,
            AlignmentSelector.CENTER,
            AlignmentSelector.RIGHT
        });

        watermark = Rnd.nextBoolean();
        rotation = Rnd.nextDouble() * Math.PI * 2;
        relLineHeight = 0.5 + Rnd.nextDouble();
        sx = 0.5 + Rnd.nextDouble();
        sy = 0.5 + Rnd.nextDouble();
        shx = -0.5 + Rnd.nextDouble();
        shy = -0.5 + Rnd.nextDouble();
    }

    public void saveStateTo(UserPreset preset) {
        preset.put(PRESET_KEY_TEXT, Utils.encodeNewlines(text));
        preset.putColor(PRESET_KEY_COLOR, color);
        preset.putFloat(PRESET_KEY_ROTATION, (float) rotation);
        preset.putInt(PRESET_KEY_ALIGN, getAlignment().ordinal());
        preset.putInt(PRESET_KEY_MLP_ALIGN, mlpAlignment);

        new FontInfo(font).saveStateTo(preset);

        areaEffects.saveStateTo(preset);

        preset.putBoolean(PRESET_KEY_WATERMARK, watermark);
        preset.putDouble(PRESET_KEY_REL_LINE_HEIGHT, relLineHeight);
        preset.putDouble(PRESET_KEY_SX, sx);
        preset.putDouble(PRESET_KEY_SY, sy);
        preset.putDouble(PRESET_KEY_SHX, shx);
        preset.putDouble(PRESET_KEY_SHY, shy);
    }

    public void loadUserPreset(UserPreset preset) {
        text = Utils.decodeNewlines(preset.get(PRESET_KEY_TEXT));
        color = preset.getColor(PRESET_KEY_COLOR);
        rotation = preset.getFloat(PRESET_KEY_ROTATION);

        int alignIndex = preset.getInt(PRESET_KEY_ALIGN, -1);
        if (alignIndex == -1) {
            // old preset, can't have path alignment
            horizontalAlignment = HorizontalAlignment.values()[preset.getInt(PRESET_KEY_HOR_ALIGN)];
            verticalAlignment = VerticalAlignment.values()[preset.getInt(PRESET_KEY_VER_ALIGN)];
        } else {
            BoxAlignment alignment = BoxAlignment.values()[alignIndex];
            if (alignment == BoxAlignment.PATH && !Views.getActiveComp().hasActivePath()) {
                alignment = BoxAlignment.CENTER_CENTER;
            }
            horizontalAlignment = alignment.getHorizontal();
            verticalAlignment = alignment.getVertical();
        }
        mlpAlignment = preset.getInt(PRESET_KEY_MLP_ALIGN);

        font = new FontInfo(preset).createFont();

        areaEffects.loadStateFrom(preset);
        watermark = preset.getBoolean(PRESET_KEY_WATERMARK);
        relLineHeight = preset.getDouble(PRESET_KEY_REL_LINE_HEIGHT, 1.0);
        sx = preset.getDouble(PRESET_KEY_SX, 1.0);
        sy = preset.getDouble(PRESET_KEY_SY, 1.0);
        shx = preset.getDouble(PRESET_KEY_SHX, 0.0);
        shy = preset.getDouble(PRESET_KEY_SHY, 0.0);

        if (guiUpdateCallback != null) { // can be null in tests that don't create a dialog
            guiUpdateCallback.accept(this);
        }
    }

    public void checkFontIsInstalled(TextLayer textLayer) {
        if (AppMode.isUnitTesting()) {
            // the fonts are not found when testing in the cloud, but that's OK
            return;
        }
        String fontName = font.getName();

        int index = Arrays.binarySearch(Utils.getAvailableFontNames(), fontName);
        if (index < 0) {
            if (fontName.equals("Default")) {
                // for some reason the "all smart filters" test file has this
                return;
            }
            Messages.showError("Missing Font In " + textLayer.getComp().getName(),
                "<html>The font <b>" + fontName + "</b> was not found on this computer." +
                    "<br>It's used in the text layer <b>" + textLayer.getName() + "</b>.");
        }
    }

    public AreaEffects getEffects() {
        return areaEffects;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public BoxAlignment getAlignment() {
        return BoxAlignment.from(horizontalAlignment, verticalAlignment);
    }

    public void setAlignment(BoxAlignment newAlignment) {
        this.horizontalAlignment = newAlignment.getHorizontal();
        this.verticalAlignment = newAlignment.getVertical();
    }

    public int getMLPAlignment() {
        return mlpAlignment;
    }

    public void setMLPAlignment(int mlpAlignment) {
        this.mlpAlignment = mlpAlignment;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getRelLineHeight() {
        return relLineHeight;
    }

    public double getScaleX() {
        return sx;
    }

    public double getScaleY() {
        return sy;
    }

    public double getShearX() {
        return shx;
    }

    public double getShearY() {
        return shy;
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

    public boolean isOnPath() {
        return horizontalAlignment == null || verticalAlignment == null;
    }

    public void setGuiUpdateCallback(Consumer<TextSettings> guiUpdateCallback) {
        this.guiUpdateCallback = guiUpdateCallback;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addQuotedString("text", getText());
        node.add(DebugNodes.createFontNode("font", font));
        node.addColor("color", color);
        node.add(areaEffects.createDebugNode("effects"));
        node.addDouble("rotation", rotation);
        node.addBoolean("watermark", watermark);
        node.addInt("multiline/path alignment", mlpAlignment);

        return node;
    }
}
