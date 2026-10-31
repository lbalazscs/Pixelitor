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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jdesktop.swingx.painter.effects.*;
import pixelitor.filters.painters.AreaEffects;

import java.awt.geom.Point2D;

/**
 * Fluent AssertJ assertions for {@link AreaEffects}.
 */
public class AreaEffectsAssert extends AbstractAssert<AreaEffectsAssert, AreaEffects> {
    private static final double EPSILON = 1e-6;

    public AreaEffectsAssert(AreaEffects actual) {
        super(actual, AreaEffectsAssert.class);
    }

    public AreaEffectsAssert hasNoEffects() {
        isNotNull();
        if (actual.hasEnabledEffects()) {
            failWithMessage("Expected no effects to be enabled, but effects were present: <%s>", actual);
        }
        return this;
    }

    public AreaEffectsAssert isEnabled() {
        isNotNull();
        if (!actual.hasEnabledEffects()) {
            failWithMessage("Expected AreaEffects to have enabled effects, but none were enabled");
        }
        return this;
    }

    public AreaEffectsAssert isDisabled() {
        return hasNoEffects();
    }

    public AreaEffectsAssert hasGlowWidth(double expectedWidth) {
        isNotNull();
        GlowPathEffect glow = actual.getGlow();
        if (glow == null) {
            failWithMessage("Expected glow effect to be present, but was null");
            return this;
        }
        if (Math.abs(glow.getEffectWidth() - expectedWidth) > EPSILON) {
            failWithMessage("Expected glow width <%s> but was <%s>", expectedWidth, glow.getEffectWidth());
        }
        return this;
    }

    public AreaEffectsAssert hasGlowOpacity(float expectedOpacity) {
        isNotNull();
        GlowPathEffect glow = actual.getGlow();
        if (glow == null) {
            failWithMessage("Expected glow effect to be present, but was null");
            return this;
        }
        if (Math.abs(glow.getOpacity() - expectedOpacity) > EPSILON) {
            failWithMessage("Expected glow opacity <%s> but was <%s>", expectedOpacity, glow.getOpacity());
        }
        return this;
    }

    public AreaEffectsAssert hasNoGlow() {
        isNotNull();
        if (actual.getGlow() != null) {
            failWithMessage("Expected glow effect to be null, but was <%s>", actual.getGlow());
        }
        return this;
    }

    public AreaEffectsAssert hasInnerGlowWidth(double expectedWidth) {
        isNotNull();
        InnerGlowPathEffect innerGlow = actual.getInnerGlow();
        if (innerGlow == null) {
            failWithMessage("Expected inner glow effect to be present, but was null");
            return this;
        }
        if (Math.abs(innerGlow.getEffectWidth() - expectedWidth) > EPSILON) {
            failWithMessage("Expected inner glow width <%s> but was <%s>", expectedWidth, innerGlow.getEffectWidth());
        }
        return this;
    }

    public AreaEffectsAssert hasNoInnerGlow() {
        isNotNull();
        if (actual.getInnerGlow() != null) {
            failWithMessage("Expected inner glow effect to be null, but was <%s>", actual.getInnerGlow());
        }
        return this;
    }

    public AreaEffectsAssert hasNeonBorderWidth(double expectedWidth) {
        isNotNull();
        NeonBorderEffect neonBorder = actual.getNeonBorder();
        if (neonBorder == null) {
            failWithMessage("Expected neon border effect to be present, but was null");
            return this;
        }
        if (Math.abs(neonBorder.getEffectWidth() - expectedWidth) > EPSILON) {
            failWithMessage("Expected neon border width <%s> but was <%s>", expectedWidth, neonBorder.getEffectWidth());
        }
        return this;
    }

    public AreaEffectsAssert hasNoNeonBorder() {
        isNotNull();
        if (actual.getNeonBorder() != null) {
            failWithMessage("Expected neon border effect to be null, but was <%s>", actual.getNeonBorder());
        }
        return this;
    }

    public AreaEffectsAssert hasDropShadowWidth(double expectedWidth) {
        isNotNull();
        ShadowPathEffect dropShadow = actual.getDropShadow();
        if (dropShadow == null) {
            failWithMessage("Expected drop shadow effect to be present, but was null");
            return this;
        }
        if (Math.abs(dropShadow.getEffectWidth() - expectedWidth) > EPSILON) {
            failWithMessage("Expected drop shadow width <%s> but was <%s>", expectedWidth, dropShadow.getEffectWidth());
        }
        return this;
    }

    public AreaEffectsAssert hasDropShadowOffset(Point2D expectedOffset) {
        isNotNull();
        ShadowPathEffect dropShadow = actual.getDropShadow();
        if (dropShadow == null) {
            failWithMessage("Expected drop shadow effect to be present, but was null");
            return this;
        }
        Point2D actualOffset = dropShadow.getOffset();
        if (actualOffset == null) {
            failWithMessage("Expected drop shadow offset <%s> but was null", expectedOffset);
            return this;
        }
        if (Math.abs(actualOffset.getX() - expectedOffset.getX()) > EPSILON
            || Math.abs(actualOffset.getY() - expectedOffset.getY()) > EPSILON) {
            failWithMessage("Expected drop shadow offset <%s> but was <%s>", expectedOffset, actualOffset);
        }
        return this;
    }

    public AreaEffectsAssert hasDropShadow(double expectedWidth, Point2D expectedOffset) {
        return hasDropShadowWidth(expectedWidth).hasDropShadowOffset(expectedOffset);
    }

    public AreaEffectsAssert hasDropShadow(double expectedWidth, double expectedOffsetX, double expectedOffsetY) {
        return hasDropShadow(expectedWidth, new Point2D.Double(expectedOffsetX, expectedOffsetY));
    }

    public AreaEffectsAssert hasNoDropShadow() {
        isNotNull();
        if (actual.getDropShadow() != null) {
            failWithMessage("Expected drop shadow effect to be null, but was <%s>", actual.getDropShadow());
        }
        return this;
    }

    public AreaEffectsAssert hasMaxPadding(double expectedPadding) {
        isNotNull();
        double actualPadding = actual.calcMaxEffectPadding();
        if (Math.abs(actualPadding - expectedPadding) > EPSILON) {
            failWithMessage("Expected max effect padding <%s> but was <%s>", expectedPadding, actualPadding);
        }
        return this;
    }

    public AreaEffectsAssert hasEnabledEffects(AreaEffect... expectedEffects) {
        isNotNull();
        AreaEffect[] actualEffects = actual.getEnabledEffects();
        Assertions.assertThat(actualEffects)
            .as("Enabled effects in rendering order")
            .containsExactly(expectedEffects);
        return this;
    }

    public AreaEffectsAssert hasNoEnabledEffects() {
        isNotNull();
        AreaEffect[] actualEffects = actual.getEnabledEffects();
        Assertions.assertThat(actualEffects)
            .as("Enabled effects")
            .isEmpty();
        return this;
    }
}
