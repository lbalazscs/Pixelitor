/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import org.junit.Test;
import pixelitor.guides.GuideStyle;
import pixelitor.guides.GuidesRenderer;

import static org.assertj.core.api.Assertions.assertThat;

public class DIContainerTest {
    @Test
    public void testGetReturnSameObject() {
        GuideStyle gs1 = DIContainer.get(GuideStyle.class);
        GuideStyle gs2 = DIContainer.get(GuideStyle.class);
        assertThat(gs1).isSameAs(gs2);
    }

    @Test
    public void testGetReturnSameObjectForSameId() {
        GuidesRenderer gr1 = DIContainer.get(GuidesRenderer.class, DIContainer.GUIDES_RENDERER);
        GuidesRenderer gr2 = DIContainer.get(GuidesRenderer.class, DIContainer.GUIDES_RENDERER);
        assertThat(gr1).isSameAs(gr2);

        GuidesRenderer gr3 = DIContainer.get(GuidesRenderer.class, DIContainer.CROP_GUIDES_RENDERER);
        assertThat(gr3).isNotSameAs(gr2);
    }
}
