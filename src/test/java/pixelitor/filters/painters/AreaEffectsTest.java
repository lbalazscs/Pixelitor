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

package pixelitor.filters.painters;

import org.assertj.core.api.Assertions;
import org.jdesktop.swingx.painter.effects.GlowPathEffect;
import org.jdesktop.swingx.painter.effects.InnerGlowPathEffect;
import org.jdesktop.swingx.painter.effects.NeonBorderEffect;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("Area effects tests")
@TestMethodOrder(MethodOrderer.Random.class)
class AreaEffectsTest {
    @Nested
    @DisplayName("Interpolation")
    @TestMethodOrder(MethodOrderer.Random.class)
    class InterpolationTests {
        private final AreaEffects startEffects = createAllEffects(
            10.0, 8.0, 6.0, new Point2D.Double(3.0, 4.0), 12.0);
        private final AreaEffects endEffects = createAllEffects(
            20.0, 16.0, 18.0, new Point2D.Double(9.0, 12.0), 24.0);

        @Test
        @DisplayName("Progress 0.0 matches start widths and offset")
        void interpolateAtProgressZeroMatchesStartWidthAndOffset() {
            AreaEffects result = startEffects.interpolate(endEffects, 0.0);

            assertThat(result)
                .hasGlowWidth(10.0)
                .hasInnerGlowWidth(8.0)
                .hasDropShadow(6.0, 3.0, 4.0)
                .hasNeonBorderWidth(12.0);
        }

        @Test
        @DisplayName("Progress 1.0 matches end widths and offset")
        void interpolateAtProgressOneMatchesEndWidthAndOffset() {
            AreaEffects result = startEffects.interpolate(endEffects, 1.0);

            assertThat(result)
                .hasGlowWidth(20.0)
                .hasInnerGlowWidth(16.0)
                .hasDropShadow(18.0, 9.0, 12.0)
                .hasNeonBorderWidth(24.0);
        }

        @Test
        @DisplayName("Progress 0.5 interpolates widths and offset linearly")
        void interpolateAtHalfProgressInterpolatesWidthAndOffset() {
            AreaEffects result = startEffects.interpolate(endEffects, 0.5);

            assertThat(result)
                .hasGlowWidth(15.0)
                .hasInnerGlowWidth(12.0)
                .hasDropShadow(12.0, 6.0, 8.0)
                .hasNeonBorderWidth(18.0);
        }

        @Test
        @DisplayName("Interpolates with only a subset of effects enabled")
        void interpolateWithSubsetOfEffects() {
            AreaEffects start = effectsWithShadow(4.0, new Point2D.Double(2.0, 3.0));
            AreaEffects end = effectsWithShadow(10.0, new Point2D.Double(6.0, 7.0));

            AreaEffects result = start.interpolate(end, 0.5);

            assertThat(result)
                .hasDropShadow(7.0, 4.0, 5.0)
                .hasNoGlow()
                .hasNoInnerGlow()
                .hasNoNeonBorder();
        }

        @Test
        @DisplayName("Interpolating empty effects returns empty effects")
        void interpolateWithNoEffectsReturnsEmptyEffects() {
            AreaEffects emptyStart = new AreaEffects();
            AreaEffects emptyEnd = new AreaEffects();

            AreaEffects result = emptyStart.interpolate(emptyEnd, 0.5);

            assertThat(result).hasNoEffects();
        }

        @Test
        @DisplayName("Interpolates effect opacity linearly")
        void interpolateInterpolatesOpacity() {
            AreaEffects start = new AreaEffects();
            start.setGlow(new GlowPathEffect(0.2f));

            AreaEffects end = new AreaEffects();
            end.setGlow(new GlowPathEffect(0.8f));

            AreaEffects result = start.interpolate(end, 0.5);

            assertThat(result).hasGlowOpacity(0.5f);
        }

        @Test
        @DisplayName("Interpolating with null end state throws AssertionError")
        void interpolateWithNullEndStateThrowsAssertionError() {
            assertThatThrownBy(() -> startEffects.interpolate(null, 0.5))
                .isInstanceOf(AssertionError.class);
        }

        @ParameterizedTest(name = "mismatched {0}")
        @MethodSource("mismatchedEffects")
        @DisplayName("Interpolating mismatched enabled effects throws AssertionError")
        void interpolateMismatchedEffectsThrowsAssertionError(String effectName, Consumer<AreaEffects> effectSetter) {
            AreaEffects withEffect = new AreaEffects();
            effectSetter.accept(withEffect);
            AreaEffects withoutEffect = new AreaEffects();

            assertThatThrownBy(() -> withEffect.interpolate(withoutEffect, 0.5))
                .isInstanceOf(AssertionError.class);
            assertThatThrownBy(() -> withoutEffect.interpolate(withEffect, 0.5))
                .isInstanceOf(AssertionError.class);
        }

        private static Stream<Arguments> mismatchedEffects() {
            return Stream.of(
                Arguments.of("glow", (Consumer<AreaEffects>) e -> e.setGlow(new GlowPathEffect())),
                Arguments.of("inner glow", (Consumer<AreaEffects>) e -> e.setInnerGlow(new InnerGlowPathEffect())),
                Arguments.of("drop shadow", (Consumer<AreaEffects>) e -> e.setDropShadow(new ShadowPathEffect())),
                Arguments.of("neon border", (Consumer<AreaEffects>) e -> e.setNeonBorder(new NeonBorderEffect()))
            );
        }
    }

    @Nested
    @DisplayName("Max Effect Padding Calculation")
    @TestMethodOrder(MethodOrderer.Random.class)
    class MaxEffectPaddingTests {

        @Test
        @DisplayName("Returns zero padding when no effects are enabled")
        void zeroWhenNoEffectsEnabled() {
            assertThat(new AreaEffects()).hasMaxPadding(0.0);
        }

        @Test
        @DisplayName("Inner glow is ignored because it renders inside the shape")
        void innerGlowDoesNotContributeToPadding() {
            AreaEffects effects = effectsWithInnerGlow(50.0);
            assertThat(effects).hasMaxPadding(0.0);
        }

        @Test
        @DisplayName("Glow padding is half of the effect width ceiled")
        void glowPaddingIsHalfEffectWidthCeiled() {
            // 11.0 / 2 = 5.5 -> ceil = 6.0
            AreaEffects effects = effectsWithGlow(11.0);
            assertThat(effects).hasMaxPadding(6.0);
        }

        @Test
        @DisplayName("Neon border padding is half of the effect width ceiled")
        void neonBorderPaddingIsHalfEffectWidthCeiled() {
            // 15.0 / 2 = 7.5 -> ceil = 8.0
            AreaEffects effects = effectsWithNeonBorder(15.0);
            assertThat(effects).hasMaxPadding(8.0);
        }

        @Test
        @DisplayName("Drop shadow padding includes half width plus positive offset")
        void dropShadowPaddingAccountsForWidthAndPositiveOffset() {
            // shadowPadding = 6.0 / 2 = 3.0; offset = (4.0, 2.0) -> max(3+4, 3+2) = 7.0
            AreaEffects effects = effectsWithShadow(6.0, new Point2D.Double(4.0, 2.0));
            assertThat(effects).hasMaxPadding(7.0);
        }

        @Test
        @DisplayName("Drop shadow padding uses absolute values for negative offset")
        void dropShadowPaddingAccountsForNegativeOffset() {
            // shadowPadding = 6.0 / 2 = 3.0; offset = (-5.0, -8.0) -> max(3+|-5|, 3+|-8|) = 11.0
            AreaEffects effects = effectsWithShadow(6.0, new Point2D.Double(-5.0, -8.0));
            assertThat(effects).hasMaxPadding(11.0);
        }

        @Test
        @DisplayName("Calculates maximum padding across multiple enabled effects")
        void maximumPaddingAcrossMultipleEnabledEffects() {
            // glow: 10.0 / 2 = 5.0
            // inner glow: 50.0 (ignored -> 0.0)
            // drop shadow: 4.0 / 2 + max(|3|, |2|) = 5.0
            // neon border: 16.0 / 2 = 8.0 (maximum)
            AreaEffects effects = createAllEffects(
                10.0, 50.0, 4.0, new Point2D.Double(3.0, 2.0), 16.0);
            assertThat(effects).hasMaxPadding(8.0);
        }
    }

    @Nested
    @DisplayName("Enabled Effects and Rendering Order")
    @TestMethodOrder(MethodOrderer.Random.class)
    class EnabledEffectsTests {

        @Test
        @DisplayName("Returns empty array when no effects are enabled")
        void emptyArrayWhenNoEffectsEnabled() {
            assertThat(new AreaEffects()).hasNoEnabledEffects();
        }

        @Test
        @DisplayName("Returns effects in rendering order with drop shadow first")
        void returnsEffectsInRenderingOrderWithDropShadowFirst() {
            AreaEffects effects = new AreaEffects();
            NeonBorderEffect neon = new NeonBorderEffect();
            GlowPathEffect glow = new GlowPathEffect();
            ShadowPathEffect shadow = new ShadowPathEffect();
            InnerGlowPathEffect innerGlow = new InnerGlowPathEffect();

            // Set effects in arbitrary order
            effects.setNeonBorder(neon);
            effects.setGlow(glow);
            effects.setDropShadow(shadow);
            effects.setInnerGlow(innerGlow);

            // Shadow must render first behind other effects
            assertThat(effects).hasEnabledEffects(shadow, glow, innerGlow, neon);
        }

        @Test
        @DisplayName("Returns only the enabled subset in the correct order")
        void returnsOnlyEnabledSubsetInCorrectOrder() {
            AreaEffects effects = new AreaEffects();
            ShadowPathEffect shadow = new ShadowPathEffect();
            InnerGlowPathEffect innerGlow = new InnerGlowPathEffect();

            effects.setInnerGlow(innerGlow);
            effects.setDropShadow(shadow);

            assertThat(effects).hasEnabledEffects(shadow, innerGlow);
        }

        @Test
        @DisplayName("hasEnabledEffects reflects presence of any enabled effect")
        void hasEnabledEffectsReflectsPresenceOfAnyEffect() {
            AreaEffects effects = new AreaEffects();
            assertThat(effects).isDisabled();

            effects.setGlow(new GlowPathEffect());
            assertThat(effects).isEnabled();

            effects.setGlow(null);
            assertThat(effects).isDisabled();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Contract")
    @TestMethodOrder(MethodOrderer.Random.class)
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Empty instances are equal and have matching hashCodes")
        void emptyInstancesAreEqualAndShareHashCode() {
            AreaEffects first = new AreaEffects();
            AreaEffects second = new AreaEffects();

            assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
        }

        @Test
        @DisplayName("Instances with identical effects are equal and have matching hashCodes")
        void instancesWithIdenticalEffectsAreEqualAndShareHashCode() {
            AreaEffects first = createAllEffects(
                10.0, 8.0, 6.0, new Point2D.Double(3.0, 4.0), 12.0);
            AreaEffects second = createAllEffects(
                10.0, 8.0, 6.0, new Point2D.Double(3.0, 4.0), 12.0);

            assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
        }

        @Test
        @DisplayName("Equals is reflexive")
        void equalsIsReflexive() {
            AreaEffects effects = createAllEffects(
                10.0, 8.0, 6.0, new Point2D.Double(3.0, 4.0), 12.0);

            assertThat(effects).isEqualTo(effects);
        }

        @Test
        @DisplayName("Not equal when effects differ or when compared to other types")
        void notEqualWhenEffectsDifferOrOtherType() {
            AreaEffects first = effectsWithGlow(10.0);
            AreaEffects second = effectsWithGlow(20.0);
            AreaEffects third = new AreaEffects();

            assertThat(first)
                .isNotEqualTo(second)
                .isNotEqualTo(third);

            Assertions.assertThat(first.equals(null)).isFalse();
            Assertions.assertThat(first.equals("not an AreaEffects instance")).isFalse();
        }

        @Test
        @DisplayName("toPresetString throws UnsupportedOperationException")
        void toPresetStringThrowsUnsupportedOperationException() {
            AreaEffects effects = new AreaEffects();
            assertThatThrownBy(effects::toPresetString)
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // --- Helper Factory Methods ---

    private static AreaEffects createAllEffects(
        double glowWidth, double innerGlowWidth,
        double shadowWidth, Point2D shadowOffset, double neonWidth) {
        AreaEffects effects = new AreaEffects();

        GlowPathEffect glow = new GlowPathEffect();
        glow.setEffectWidth(glowWidth);
        effects.setGlow(glow);

        InnerGlowPathEffect innerGlow = new InnerGlowPathEffect();
        innerGlow.setEffectWidth(innerGlowWidth);
        effects.setInnerGlow(innerGlow);

        ShadowPathEffect shadow = new ShadowPathEffect();
        shadow.setEffectWidth(shadowWidth);
        shadow.setOffset(shadowOffset);
        effects.setDropShadow(shadow);

        NeonBorderEffect neon = new NeonBorderEffect(Color.GREEN, Color.WHITE, neonWidth, 1.0f);
        effects.setNeonBorder(neon);

        return effects;
    }

    private static AreaEffects effectsWithGlow(double width) {
        AreaEffects effects = new AreaEffects();
        GlowPathEffect glow = new GlowPathEffect();
        glow.setEffectWidth(width);
        effects.setGlow(glow);
        return effects;
    }

    private static AreaEffects effectsWithInnerGlow(double width) {
        AreaEffects effects = new AreaEffects();
        InnerGlowPathEffect innerGlow = new InnerGlowPathEffect();
        innerGlow.setEffectWidth(width);
        effects.setInnerGlow(innerGlow);
        return effects;
    }

    private static AreaEffects effectsWithShadow(double width, Point2D offset) {
        AreaEffects effects = new AreaEffects();
        ShadowPathEffect shadow = new ShadowPathEffect();
        shadow.setEffectWidth(width);
        shadow.setOffset(offset);
        effects.setDropShadow(shadow);
        return effects;
    }

    private static AreaEffects effectsWithNeonBorder(double width) {
        AreaEffects effects = new AreaEffects();
        NeonBorderEffect neon = new NeonBorderEffect(Color.GREEN, Color.WHITE, width, 1.0f);
        effects.setNeonBorder(neon);
        return effects;
    }
}
