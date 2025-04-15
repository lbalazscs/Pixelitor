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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import pixelitor.history.PixelitorEdit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom AssertJ assertions for {@link PixelitorEdit} objects.
 */
public class EditAssert extends AbstractAssert<EditAssert, PixelitorEdit> {
    public EditAssert(PixelitorEdit actual) {
        super(actual, EditAssert.class);
    }

    public EditAssert nameIs(String expected) {
        isNotNull();
        assertThat(actual.getName()).isEqualTo(expected);
        return this;
    }
}
