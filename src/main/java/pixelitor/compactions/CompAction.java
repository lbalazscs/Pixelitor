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
package pixelitor.compactions;

import pixelitor.Composition;

import java.util.concurrent.CompletableFuture;

/**
 * An action that acts on all layers of a {@link Composition}
 */
public interface CompAction {
    /**
     * Processes the given {@link Composition}, possibly asynchronously.
     * If there was any change, then the returned value is a different
     * instance, and the original instance is used as backup for the undo.
     */
    CompletableFuture<Composition> process(Composition oldComp);
}
