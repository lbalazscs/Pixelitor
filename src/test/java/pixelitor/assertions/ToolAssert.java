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
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

/**
 * Custom AssertJ assertions for instances of {@link Tool} subclasses.
 *
 * Also see https://github.com/joel-costigliola/assertj-core/issues/9
 */
public class ToolAssert<S extends ToolAssert<S, T>, T extends Tool> extends AbstractAssert<S, T> {
    public ToolAssert(T actual, Class<S> selfType) {
        super(actual, selfType);
    }

    public S isActive() {
        isNotNull();

        if (!actual.isActive()) {
            throw new AssertionError(actual.getName()
                + " is not active. The active tool is "
                + Tools.getActive().getName() + ".");
        }

        return myself;
    }

    public S isNotActive() {
        isNotNull();

        if (actual.isActive()) {
            throw new AssertionError("is active");
        }

        return myself;
    }
}
