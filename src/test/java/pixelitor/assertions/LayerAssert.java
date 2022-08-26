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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerUI;

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
        assertThat(actual.getUI().getLayerName()).isEqualTo(expected);
        return myself;
    }

    public S classIs(Class<? extends Layer> expected) {
        isNotNull();
        assertThat(actual.getClass()).isEqualTo(expected);
        return myself;
    }

    public S opacityIs(float expected) {
        isNotNull();
        assertThat(actual.getOpacity())
            .isCloseTo(expected, within(0.001f));
        return myself;
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

    public S uiIsVisible() {
        isNotNull();
        LayerUI layerUI = actual.getUI();
        assertThat(layerUI.isEyeOpen()).isTrue();
        return myself;
    }

    public S uiIsNotVisible() {
        isNotNull();
        LayerUI layerUI = actual.getUI();
        assertThat(layerUI.isEyeOpen()).isFalse();
        return myself;
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
        assertThat(actual.getMask().isLinked()).isTrue();
        return myself;
    }

    public S maskIsNotLinked() {
        isNotNull();
        assertThat(actual.getMask().isLinked()).isFalse();
        return myself;
    }
}
