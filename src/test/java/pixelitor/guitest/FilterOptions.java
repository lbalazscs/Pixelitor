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

package pixelitor.guitest;

// what GUI widgets should be tested on for a filter
public record FilterOptions(boolean randomize, boolean reseed, boolean showOriginal, boolean exportSvg) {
    // nothing
    public static final FilterOptions NONE = new FilterOptions(false, false, false, false);
    // show original
    public static final FilterOptions TRIVIAL = new FilterOptions(false, false, true, false);
    // randomize + show original
    public static final FilterOptions STANDARD = new FilterOptions(true, false, true, false);
    // randomize + show original + resed
    public static final FilterOptions STANDARD_RESEED = new FilterOptions(true, true, true, false);
    // randomize
    public static final FilterOptions RENDERING = new FilterOptions(true, false, false, false);
    // randomize + reseed
    public static final FilterOptions RENDERING_RESEED = new FilterOptions(true, true, false, false);
    // randomize + svg export
    public static final FilterOptions SHAPES = new FilterOptions(true, false, false, true);
}
