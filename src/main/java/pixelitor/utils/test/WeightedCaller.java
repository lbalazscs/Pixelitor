/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils.test;

import pixelitor.utils.Rnd;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls the registered functions with a probability
 * proportional to their weights.
 */
public class WeightedCaller {
    private final List<Runnable> tasks = new ArrayList<>();

    public void registerAction(int weight, Runnable r) {
        for (int i = 0; i < weight; i++) {
            tasks.add(r);
        }
    }

    public void runRandomAction() {
        Rnd.chooseFrom(tasks).run();
    }
}
