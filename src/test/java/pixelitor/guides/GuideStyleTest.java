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

package pixelitor.guides;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GuideStyle tests")
@TestMethodOrder(MethodOrderer.Random.class)
public class GuideStyleTest {
    private GuideStyle guideStyle;

    @BeforeEach
    void beforeEachTest() {
        guideStyle = new GuideStyle();
    }

    @Test
    void colorA() {
        guideStyle.setColorA(Color.MAGENTA);
        assertThat(guideStyle.getColorA()).isEqualTo(Color.MAGENTA);
    }

    @Test
    void colorB() {
        guideStyle.setColorB(Color.MAGENTA);
        assertThat(guideStyle.getColorB()).isEqualTo(Color.MAGENTA);
    }

    @Test
    void strokeType() {
        guideStyle.setStrokeType(GuideStrokeType.SOLID);
        assertThat(guideStyle.getStrokeType()).isEqualTo(GuideStrokeType.SOLID);
    }

    @Test
    void strokeGetters() {
        assertThat(guideStyle.getStrokeA()).isEqualTo(guideStyle.getStrokeType().getStrokeA());
        assertThat(guideStyle.getStrokeB()).isEqualTo(guideStyle.getStrokeType().getStrokeB());
    }
}
