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

import org.jdesktop.swingx.painter.effects.AreaEffect;
import org.jdesktop.swingx.painter.effects.GlowPathEffect;
import org.jdesktop.swingx.painter.effects.InnerGlowPathEffect;
import org.jdesktop.swingx.painter.effects.NeonBorderEffect;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.ParamState;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A collection of 4 area effects, which can be enabled or disabled.
 * It also functions as the {@link ParamState} of {@link EffectsParam}
 */
public class AreaEffects implements Serializable, ParamState<AreaEffects> {
    private static final long serialVersionUID = 1L;
    private static final AreaEffect[] EMPTY_ARRAY = new AreaEffect[0];

    private GlowPathEffect glow;
    private InnerGlowPathEffect innerGlow;
    private NeonBorderEffect neonBorder;
    private ShadowPathEffect dropShadow;

    public AreaEffects() {
    }

    public void setDropShadow(ShadowPathEffect dropShadow) {
        this.dropShadow = dropShadow;
    }

    public void setGlow(GlowPathEffect glow) {
        this.glow = glow;
    }

    public void setInnerGlow(InnerGlowPathEffect innerGlow) {
        this.innerGlow = innerGlow;
    }

    public void setNeonBorder(NeonBorderEffect neonBorder) {
        this.neonBorder = neonBorder;
    }

    public AreaEffect[] asArray() {
        List<AreaEffect> effects = new ArrayList<>(2);
        // draw the drop shadow first so that
        // it gets painted behind the other effects
        if (dropShadow != null) {
            effects.add(dropShadow);
        }
        if (glow != null) {
            effects.add(glow);
        }
        if (innerGlow != null) {
            effects.add(innerGlow);
        }
        if (neonBorder != null) {
            effects.add(neonBorder);
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
        if (glow != null) {
            int effectWidth = glow.getEffectWidthInt();
            if (effectWidth > max) {
                max = effectWidth;
            }
        }
        if (neonBorder != null) {
            int effectWidth = neonBorder.getEffectWidthInt();
            if (effectWidth > max) {
                max = effectWidth;
            }
        }
        if (dropShadow != null) {
            double safetyFactor = 2.0;
            int effectWidth = 3 + (int) (dropShadow.getEffectWidth() * safetyFactor);

            Point2D offset = dropShadow.getOffset();

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
        return glow;
    }

    public ShadowPathEffect getDropShadow() {
        return dropShadow;
    }

    public InnerGlowPathEffect getInnerGlow() {
        return innerGlow;
    }

    public NeonBorderEffect getNeonBorder() {
        return neonBorder;
    }

    @Override
    public AreaEffects interpolate(AreaEffects endState, double progress) {
        float progressF = (float) progress;
        AreaEffects retVal = new AreaEffects();

        if (dropShadow != null) {
            var endEffect = endState.getDropShadow();
            float newOpacity = dropShadow.interpolateOpacity(
                    endEffect.getOpacity(), progressF);
            var newDropShadow = new ShadowPathEffect(newOpacity);
            Color newBrushColor = dropShadow.interpolateBrushColor(
                    endEffect.getBrushColor(), progressF);
            newDropShadow.setBrushColor(newBrushColor);
            retVal.setDropShadow(newDropShadow);
        }
        if (glow != null) {
            var endEffect = endState.getGlow();
            float newOpacity = glow.interpolateOpacity(
                    endEffect.getOpacity(), progressF);
            var newGlowEffect = new GlowPathEffect(newOpacity);
            Color newBrushColor = glow.interpolateBrushColor(
                    endEffect.getBrushColor(), progressF);
            glow.setBrushColor(newBrushColor);

            retVal.setGlow(newGlowEffect);
        }
        if (innerGlow != null) {
            var endEffect = endState.getInnerGlow();
            float newOpacity = innerGlow.interpolateOpacity(
                    endEffect.getOpacity(), progressF);
            var newInnerGlow = new InnerGlowPathEffect(newOpacity);
            Color newBrushColor = innerGlow.interpolateBrushColor(
                    endEffect.getBrushColor(), progressF);
            newInnerGlow.setBrushColor(newBrushColor);
            retVal.setInnerGlow(newInnerGlow);
        }
        if (neonBorder != null) {
            var endEffect = endState.getNeonBorder();
            Color newEdgeColor = neonBorder.interpolateEdgeColor(
                    endEffect.getEdgeColor(), progressF);
            Color newCenterColor = neonBorder.interpolateCenterColor(
                    endEffect.getCenterColor(), progressF);
            float newOpacity = neonBorder.interpolateOpacity(
                    endEffect.getOpacity(), progressF);
            double newWidth = neonBorder.interpolateEffectWidth(
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
        return Objects.equals(glow, that.glow) &&
                Objects.equals(innerGlow, that.innerGlow) &&
                Objects.equals(neonBorder, that.neonBorder) &&
                Objects.equals(dropShadow, that.dropShadow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(glow, innerGlow, neonBorder, dropShadow);
    }
}
