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

package pixelitor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pixelitor.colors.FillType;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("NewImage tests")
class NewImageTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setupMockFgBgSelector();
    }

    @DisplayName("create new comp")
    @ParameterizedTest(name="filled with {0}")
    @EnumSource(FillType.class)
    void createNewComposition(FillType fillType) {
        var comp = NewImage.createNewComposition(fillType, 20, 20, "New Image");
        comp.checkInvariant();
        assertThat(comp)
                .numLayersIs(1)
                .canvasSizeIs(20, 20);
    }
}