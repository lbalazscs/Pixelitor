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

import org.jdesktop.swingx.painter.effects.*;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.ParamState;
import pixelitor.filters.gui.UserPreset;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A collection of effects that can be applied to a shape.
 * Each effect can be independently enabled or disabled.
 * This class also functions as the {@link ParamState} of {@link EffectsParam}
 */
public class AreaEffects implements ParamState<AreaEffects>, Debuggable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final AreaEffect[] EMPTY_EFFECTS_ARRAY = new AreaEffect[0];

    // These fields must not be renamed (serialized fields)
    private GlowPathEffect glowEffect;
    private InnerGlowPathEffect innerGlowEffect;
    private NeonBorderEffect neonBorderEffect;
    private ShadowPathEffect dropShadowEffect;

    public AreaEffects() {
        // creates an instance with all effects disabled
    }

    /**
     * Returns all enabled effects in the correct rendering order.
     */
    public AreaEffect[] getEnabledEffects() {
        List<AreaEffect> enabledEffects = new ArrayList<>(2);

        // drop shadow must be first to render behind other effects
        if (dropShadowEffect != null) {
            enabledEffects.add(dropShadowEffect);
        }
        if (glowEffect != null) {
            enabledEffects.add(glowEffect);
        }
        if (innerGlowEffect != null) {
            enabledEffects.add(innerGlowEffect);
        }
        if (neonBorderEffect != null) {
            enabledEffects.add(neonBorderEffect);
        }
        return enabledEffects.toArray(EMPTY_EFFECTS_ARRAY);
    }

    public void apply(Graphics2D g2, Shape shape) {
        AreaEffect[] areaEffects = getEnabledEffects();
        for (AreaEffect effect : areaEffects) {
            effect.apply(g2, shape, 0, 0);
        }
    }

    /**
     * Calculates the maximum additional space needed around
     * the shape to accommodate all effects.
     */
    public double calcMaxEffectPadding() {
        // inner glow is not considered here as it doesn't
        // extend beyond the shape's bounds
        double maxPadding = 0;
        
        if (glowEffect != null) {
            maxPadding = Math.max(maxPadding,
                glowEffect.getEffectWidth() / 2.0);
        }
        if (neonBorderEffect != null) {
            maxPadding = Math.max(maxPadding,
                neonBorderEffect.getEffectWidth() / 2.0);
        }
        if (dropShadowEffect != null) {
            double padding = dropShadowEffect.getEffectWidth() / 2.0;
            Point2D offset = dropShadowEffect.getOffset();

            maxPadding = Math.max(maxPadding,
                padding + Math.abs(offset.getX()));
            maxPadding = Math.max(maxPadding,
                padding + Math.abs(offset.getY()));
        }

        return Math.ceil(maxPadding);
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

    public boolean hasEnabledEffects() {
        return glowEffect != null || innerGlowEffect != null
            || neonBorderEffect != null || dropShadowEffect != null;
    }

    @Override
    public AreaEffects interpolate(AreaEffects endState, double progress) {
        float progressFloat = (float) progress;
        AreaEffects interpolatedEffects = new AreaEffects();

        if (dropShadowEffect != null) {
            var endEffect = endState.getDropShadow();
            float newOpacity = dropShadowEffect.interpolateOpacity(
                endEffect.getOpacity(), progressFloat);
            var newDropShadow = new ShadowPathEffect(newOpacity);
            Color newBrushColor = dropShadowEffect.interpolateBrushColor(
                endEffect.getBrushColor(), progress);
            newDropShadow.setBrushColor(newBrushColor);
            interpolatedEffects.setDropShadow(newDropShadow);
        }
        if (glowEffect != null) {
            var endEffect = endState.getGlow();
            float newOpacity = glowEffect.interpolateOpacity(
                endEffect.getOpacity(), progressFloat);
            var newGlowEffect = new GlowPathEffect(newOpacity);
            Color newBrushColor = glowEffect.interpolateBrushColor(
                endEffect.getBrushColor(), progress);
            glowEffect.setBrushColor(newBrushColor);

            interpolatedEffects.setGlow(newGlowEffect);
        }
        if (innerGlowEffect != null) {
            var endEffect = endState.getInnerGlow();
            float newOpacity = innerGlowEffect.interpolateOpacity(
                endEffect.getOpacity(), progressFloat);
            var newInnerGlow = new InnerGlowPathEffect(newOpacity);
            Color newBrushColor = innerGlowEffect.interpolateBrushColor(
                endEffect.getBrushColor(), progress);
            newInnerGlow.setBrushColor(newBrushColor);
            interpolatedEffects.setInnerGlow(newInnerGlow);
        }
        if (neonBorderEffect != null) {
            var endEffect = endState.getNeonBorder();
            Color newEdgeColor = neonBorderEffect.interpolateEdgeColor(
                endEffect.getEdgeColor(), progress);
            Color newCenterColor = neonBorderEffect.interpolateCenterColor(
                endEffect.getCenterColor(), progress);
            float newOpacity = neonBorderEffect.interpolateOpacity(
                endEffect.getOpacity(), progressFloat);
            double newWidth = neonBorderEffect.interpolateEffectWidth(
                endEffect.getEffectWidth(), progress);
            var newNeonBorder = new NeonBorderEffect(newEdgeColor, newCenterColor,
                newWidth, newOpacity);
            interpolatedEffects.setNeonBorder(newNeonBorder);
        }
        return interpolatedEffects;
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
        loadGlowStateFrom(preset);
        loadInnerGlowStateFrom(preset);
        loadNeonBorderStateFrom(preset);
        loadDropShadowStateFrom(preset);
    }

    private void loadGlowStateFrom(UserPreset preset) {
        if (preset.getBoolean("Glow.Enabled")) {
            glowEffect = new GlowPathEffect();
            glowEffect.loadStateFrom(preset, "Glow.", false);
        } else {
            glowEffect = null;
        }
    }

    private void loadInnerGlowStateFrom(UserPreset preset) {
        if (preset.getBoolean("InnerGlow.Enabled")) {
            innerGlowEffect = new InnerGlowPathEffect();
            innerGlowEffect.loadStateFrom(preset, "InnerGlow.", false);
        } else {
            innerGlowEffect = null;
        }
    }

    private void loadNeonBorderStateFrom(UserPreset preset) {
        if (preset.getBoolean("NeonBorder.Enabled")) {
            neonBorderEffect = new NeonBorderEffect();
            neonBorderEffect.loadStateFrom(preset, "NeonBorder.", false);
        } else {
            neonBorderEffect = null;
        }
    }

    private void loadDropShadowStateFrom(UserPreset preset) {
        if (preset.getBoolean("DropShadow.Enabled")) {
            dropShadowEffect = new ShadowPathEffect();
            dropShadowEffect.loadStateFrom(preset, "DropShadow.", true);
        } else {
            dropShadowEffect = null;
        }
    }

    public void saveStateTo(UserPreset preset) {
        saveEffectState(glowEffect, "Glow.", preset, false);
        saveEffectState(innerGlowEffect, "InnerGlow.", preset, false);
        saveEffectState(neonBorderEffect, "NeonBorder.", preset, false);
        saveEffectState(dropShadowEffect, "DropShadow.", preset, true);
    }

    private static void saveEffectState(AbstractAreaEffect effect, String keyPrefix,
                                        UserPreset preset, boolean includeOffset) {
        String enabledKey = keyPrefix + "Enabled";
        if (effect != null) {
            preset.putBoolean(enabledKey, true);
            effect.saveStateTo(preset, keyPrefix, includeOffset);
        } else {
            preset.putBoolean(enabledKey, false);
        }
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);
        node.addNullableProperty("glow", glowEffect);
        node.addNullableProperty("inner glow", innerGlowEffect);
        node.addNullableProperty("neon border", neonBorderEffect);
        node.addNullableProperty("drop shadow", dropShadowEffect);
        return node;
    }

    @Override
    public String toString() {
        return "AreaEffects{glow=" + (glowEffect == null ? "null" : "not null") +
            ", innerGlow=" + (innerGlowEffect == null ? "null" : "not null") +
            ", neonBorder=" + (neonBorderEffect == null ? "null" : "not null") +
            ", dropShadow=" + (dropShadowEffect == null ? "null" : "not null") +
            '}';
    }
}
