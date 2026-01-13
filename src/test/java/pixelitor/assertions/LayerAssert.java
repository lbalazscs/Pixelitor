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
import pixelitor.layers.BlendingMode;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Custom AssertJ assertions for {@link Layer} objects.
 */
public class LayerAssert<S extends LayerAssert<S, T>, T extends Layer> extends AbstractAssert<S, T> {
    public LayerAssert(T actual, Class<S> selfType) {
        super(actual, selfType);
    }

    public S nameIs(String expected) {
        isNotNull();
        assertThat(actual.getName()).isEqualTo(expected);
        return myself;
    }

    public S uiNameIs(String expected) {
        isNotNull();
        hasUI();

        assertThat(actual.getUI().getLayerName()).isEqualTo(expected);
        return myself;
    }

    public S opacityIs(float expected) {
        isNotNull();
        assertThat(actual.getOpacity()).isCloseTo(expected, within(0.001f));
        return myself;
    }

    public S isOpaque() {
        return opacityIs(1.0f);
    }

    public S blendingModeIs(BlendingMode expected) {
        isNotNull();
        assertThat(actual.getBlendingMode()).isEqualTo(expected);
        return myself;
    }

    public S isVisible() {
        isNotNull();
        assertThat(actual.isVisible()).isTrue();
        return myself;
    }

    public S isNotVisible() {
        isNotNull();
        assertThat(actual.isVisible()).isFalse();
        return myself;
    }

    public S isVisible(boolean expected) {
        return expected ? isVisible() : isNotVisible();
    }

    public S uiIsVisible() {
        isNotNull();
        assertThat(actual.getUI().isEyeOpen()).isTrue();
        return myself;
    }

    public S uiIsNotVisible() {
        isNotNull();
        assertThat(actual.getUI().isEyeOpen()).isFalse();
        return myself;
    }

    public S uiIsVisible(boolean expected) {
        return expected ? uiIsVisible() : uiIsNotVisible();
    }

    public S isActive() {
        isNotNull();
        assertThat(actual.isActive()).isTrue();
        return myself;
    }

    public S isNotActive() {
        isNotNull();
        assertThat(actual.isActive()).isFalse();
        return myself;
    }

    public S isActive(boolean expected) {
        return expected ? isActive() : isNotActive();
    }

    public S hasUI() {
        isNotNull();
        assertThat(actual.hasUI()).isTrue();
        return myself;
    }

    public S hasNoUI() {
        isNotNull();
        assertThat(actual.hasUI()).isFalse();
        return myself;
    }

    public S hasUI(boolean expected) {
        return expected ? hasUI() : hasNoUI();
    }

    public S hasMask() {
        isNotNull();
        assertThat(actual.hasMask()).isTrue();
        return myself;
    }

    public S hasNoMask() {
        isNotNull();
        assertThat(actual.hasMask()).isFalse();
        return myself;
    }

    public S hasMask(boolean expected) {
        return expected ? hasMask() : hasNoMask();
    }

    public S hasMaskUI() {
        isNotNull();
        assertThat(actual.getMask().hasUI()).isTrue();
        return myself;
    }

    public S hasNoMaskUI() {
        isNotNull();
        if (actual.hasMask()) {
            assertThat(actual.getMask().hasUI()).isFalse();
        }
        return myself;
    }

    public S hasMaskUI(boolean expected) {
        return expected ? hasMaskUI() : hasNoMaskUI();
    }

    public S maskIsEnabled() {
        isNotNull();
        assertThat(actual.isMaskEnabled()).isTrue();
        return myself;
    }

    public S maskIsDisabled() {
        isNotNull();
        assertThat(actual.isMaskEnabled()).isFalse();
        return myself;
    }

    public S maskIsLinked() {
        isNotNull();
        hasMask();
        assertThat(actual.getMask().isLinked()).isTrue();
        return myself;
    }

    public S maskIsNotLinked() {
        isNotNull();
        hasMask();
        assertThat(actual.getMask().isLinked()).isFalse();
        return myself;
    }

    public S isMaskEditing() {
        isNotNull();
        assertThat(actual.isMaskEditing()).isTrue();
        return myself;
    }

    public S isNotMaskEditing() {
        isNotNull();
        assertThat(actual.isMaskEditing()).isFalse();
        return myself;
    }

    public S isTopLevel() {
        isNotNull();
        assertThat(actual.isTopLevel()).isTrue();
        return myself;
    }

    public S isNotTopLevel() {
        isNotNull();
        assertThat(actual.isTopLevel()).isFalse();
        return myself;
    }

    public S holderIs(LayerHolder expected) {
        isNotNull();
        assertThat(actual.getHolder()).isEqualTo(expected);
        return myself;
    }

    public S isRasterizable() {
        isNotNull();
        assertThat(actual.isRasterizable()).isTrue();
        return myself;
    }

    public S isNotRasterizable() {
        isNotNull();
        assertThat(actual.isRasterizable()).isFalse();
        return myself;
    }

    public S isConvertibleToSmartObject() {
        isNotNull();
        assertThat(actual.isConvertibleToSmartObject()).isTrue();
        return myself;
    }

    public S isNotConvertibleToSmartObject() {
        isNotNull();
        assertThat(actual.isConvertibleToSmartObject()).isFalse();
        return myself;
    }
}
