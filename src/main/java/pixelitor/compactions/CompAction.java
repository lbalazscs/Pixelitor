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
package pixelitor.compactions;

import pixelitor.Composition;

import java.util.concurrent.CompletableFuture;

/**
 * An operation that transforms a {@link Composition}.
 */
public interface CompAction {
    /**
     * Processes the given {@link Composition}, possibly asynchronously.
     *
     * If a change was made, the returned {@link Composition} is a copy
     * reflecting that change, and the original instance is used as the undo backup.
     */
    CompletableFuture<Composition> process(Composition srcComp);
}
