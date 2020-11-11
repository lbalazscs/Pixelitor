/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.painter.effects.*;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.ParamState;
import pixelitor.filters.gui.UserPreset;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A collection of 4 area effects, which can be enabled or disabled.
 * It also functions as the {@link ParamState} of {@link EffectsParam}
 */
public class AreaEffects implements Serializable, ParamState<AreaEffects> {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final AreaEffect[] EMPTY_ARRAY = new AreaEffect[0];

    // must not be renamed (serialized fields)
    private GlowPathEffect glowEffect;
    private InnerGlowPathEffect innerGlowEffect;
    private NeonBorderEffect neonBorderEffect;
    private ShadowPathEffect dropShadowEffect;

    public AreaEffects() {
    }

    public void setDropShadow(ShadowPathEffect dropShadow) {
        this.dropShadowEffect = dropShadow;
    }

    public void setGlow(GlowPathEffect glow) {
        this.glowEffect = glow;
    }

    public void setInnerGlow(InnerGlowPathEffect innerGlow) {
        this.innerGlowEffect = innerGlow;
    }

    public void setNeonBorder(NeonBorderEffect neonBorder) {
        this.neonBorderEffect = neonBorder;
    }

    public AreaEffect[] asArray() {
        List<AreaEffect> effects = new ArrayList<>(2);
        // draw the drop shadow first so that
        // it gets painted behind the other effects
        if (dropShadowEffect != null) {
            effects.add(dropShadowEffect);
        }
        if (glowEffect != null) {
            effects.add(glowEffect);
        }
        if (innerGlowEffect != null) {
            effects.add(innerGlowEffect);
        }
        if (neonBorderEffect != null) {
            effects.add(neonBorderEffect);
        }
        return effects.toArray(EMPTY_ARRAY);
    }

    public void drawOn(Graphics2D g2, Shape shape) {
        AreaEffect[] areaEffects = asArray();
        for (AreaEffect effect : areaEffects) {
            effect.apply(g2, shape, 0, 0);
        }
    }

    /**
     * Returns the extra thickness caused by the effect
     */
    public int getMaxEffectThickness() {
        // the inner glow is not considered here,
        // because it doesn't add extra thickness
        int max = 0;
        if (glowEffect != null) {
            int effectWidth = glowEffect.getEffectWidthInt();
            if (effectWidth > max) {
                max = effectWidth;
            }
        }
        if (neonBorderEffect != null) {
            int effectWidth = neonBorderEffect.getEffectWidthInt();
            if (effectWidth > max) {
                max = effectWidth;
            }
        }
        if (dropShadowEffect != null) {
            double safetyFactor = 2.0;
            int effectWidth = 3 + (int) (dropShadowEffect.getEffectWidth() * safetyFactor);

            Point2D offset = dropShadowEffect.getOffset();

            int xGap = effectWidth + (int) Math.abs(offset.getX() * safetyFactor);
            if (xGap > max) {
                max = xGap;
            }
            int yGap = effectWidth + (int) Math.abs(offset.getY() * safetyFactor);
            if (yGap > max) {
                max = yGap;
            }
        }
        return max;
    }

    public GlowPathEffect getGlow() {
        return glowEffect;
    }

    public ShadowPathEffect getDropShadow() {
        return dropShadowEffect;
    }

    public InnerGlowPathEffect getInnerGlow() {
        return innerGlowEffect;
    }

    public NeonBorderEffect getNeonBorder() {
        return neonBorderEffect;
    }

    public boolean isEmpty() {
        return glowEffect == null && innerGlowEffect == null
            && neonBorderEffect == null && dropShadowEffect == null;
    }

    @Override
    public AreaEffects interpolate(AreaEffects endState, double progress) {
        float progressF = (float) progress;
        AreaEffects retVal = new AreaEffects();

        if (dropShadowEffect != null) {
            var endEffect = endState.getDropShadow();
            float newOpacity = dropShadowEffect.interpolateOpacity(
                endEffect.getOpacity(), progressF);
            var newDropShadow = new ShadowPathEffect(newOpacity);
            Color newBrushColor = dropShadowEffect.interpolateBrushColor(
                endEffect.getBrushColor(), progressF);
            newDropShadow.setBrushColor(newBrushColor);
            retVal.setDropShadow(newDropShadow);
        }
        if (glowEffect != null) {
            var endEffect = endState.getGlow();
            float newOpacity = glowEffect.interpolateOpacity(
                endEffect.getOpacity(), progressF);
            var newGlowEffect = new GlowPathEffect(newOpacity);
            Color newBrushColor = glowEffect.interpolateBrushColor(
                endEffect.getBrushColor(), progressF);
            glowEffect.setBrushColor(newBrushColor);

            retVal.setGlow(newGlowEffect);
        }
        if (innerGlowEffect != null) {
            var endEffect = endState.getInnerGlow();
            float newOpacity = innerGlowEffect.interpolateOpacity(
                endEffect.getOpacity(), progressF);
            var newInnerGlow = new InnerGlowPathEffect(newOpacity);
            Color newBrushColor = innerGlowEffect.interpolateBrushColor(
                endEffect.getBrushColor(), progressF);
            newInnerGlow.setBrushColor(newBrushColor);
            retVal.setInnerGlow(newInnerGlow);
        }
        if (neonBorderEffect != null) {
            var endEffect = endState.getNeonBorder();
            Color newEdgeColor = neonBorderEffect.interpolateEdgeColor(
                endEffect.getEdgeColor(), progressF);
            Color newCenterColor = neonBorderEffect.interpolateCenterColor(
                endEffect.getCenterColor(), progressF);
            float newOpacity = neonBorderEffect.interpolateOpacity(
                endEffect.getOpacity(), progressF);
            double newWidth = neonBorderEffect.interpolateEffectWidth(
                endEffect.getEffectWidth(), progress);
            var newNeonBorder = new NeonBorderEffect(newEdgeColor, newCenterColor,
                newWidth, newOpacity);
            retVal.setNeonBorder(newNeonBorder);
        }
        return retVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AreaEffects that = (AreaEffects) o;
        return Objects.equals(glowEffect, that.glowEffect) &&
            Objects.equals(innerGlowEffect, that.innerGlowEffect) &&
            Objects.equals(neonBorderEffect, that.neonBorderEffect) &&
            Objects.equals(dropShadowEffect, that.dropShadowEffect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(glowEffect, innerGlowEffect, neonBorderEffect, dropShadowEffect);
    }

    @Override
    public String toSaveString() {
        throw new UnsupportedOperationException();
    }

    public void loadStateFrom(UserPreset preset) {
        if (preset.get("Glow.Enabled").equals("yes")) {
            glowEffect = new GlowPathEffect();
            glowEffect.loadStateFrom(preset, "Glow.", false);
        }
        if (preset.get("InnerGlow.Enabled").equals("yes")) {
            innerGlowEffect = new InnerGlowPathEffect();
            innerGlowEffect.loadStateFrom(preset, "InnerGlow.", false);
        }
        if (preset.get("NeonBorder.Enabled").equals("yes")) {
            neonBorderEffect = new NeonBorderEffect();
            neonBorderEffect.loadStateFrom(preset, "NeonBorder.", false);
        }
        if (preset.get("DropShadow.Enabled").equals("yes")) {
            dropShadowEffect = new ShadowPathEffect();
            dropShadowEffect.loadStateFrom(preset, "DropShadow.", true);
        }
    }

    public void saveStateTo(UserPreset preset) {
        if (glowEffect != null) {
            preset.put("Glow.Enabled", "yes");
            glowEffect.saveStateTo(preset, "Glow.", false);
        } else {
            preset.put("Glow.Enabled", "no");
        }

        if (innerGlowEffect != null) {
            preset.put("InnerGlow.Enabled", "yes");
            innerGlowEffect.saveStateTo(preset, "InnerGlow.", false);
        } else {
            preset.put("InnerGlow.Enabled", "no");
        }

        if (neonBorderEffect != null) {
            preset.put("NeonBorder.Enabled", "yes");
            neonBorderEffect.saveStateTo(preset, "NeonBorder.", false);
        } else {
            preset.put("NeonBorder.Enabled", "no");
        }

        if (dropShadowEffect != null) {
            preset.put("DropShadow.Enabled", "yes");
            dropShadowEffect.saveStateTo(preset, "DropShadow.", true);
        } else {
            preset.put("DropShadow.Enabled", "no");
        }
    }

    @Override
    public String toString() {
        String sb = "AreaEffects{glow=" + (glowEffect == null ? "null" : "not null") +
            ", innerGlow=" + (innerGlowEffect == null ? "null" : "not null") +
            ", neonBorder=" + (neonBorderEffect == null ? "null" : "not null") +
            ", dropShadow=" + (dropShadowEffect == null ? "null" : "not null") +
            '}';
        return sb;
    }
}
