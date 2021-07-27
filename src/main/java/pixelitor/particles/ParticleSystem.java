/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.particles;

import java.util.ArrayList;
import java.util.List;

public abstract class ParticleSystem<P extends Particle> {
    private final List<ParticleGroup<P>> groups;

    protected ParticleSystem(int groupCount, int groupSize) {
        this(groupCount, groupSize, -1);
    }

    protected ParticleSystem(int groupCount, int groupSize, int particleCount) {
        if (particleCount == -1) {
            particleCount = groupCount * groupSize;
        }
        assert particleCount <= groupCount * groupSize : "Cant accommodate more particles than the groups can provided.";

        groups = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount - 1; i++) {
            groups.add(new ParticleGroup<>(this, i, groupSize));
        }
        int lastGroupSize = particleCount - groupSize * (groupCount - 1);
        assert lastGroupSize != 0 : "We have an extra group!";
        groups.add(new ParticleGroup<>(this, groupCount - 1, lastGroupSize));
    }

    public void step() {
        for (ParticleGroup<P> group : groups) {
            group.step();
        }
    }

    public void step(int idx) {
        groups.get(idx).step();
    }

    protected abstract P newParticle();

    protected abstract void initializeParticle(P particle);

    protected abstract boolean isParticleDead(P particle);

    protected abstract void updateParticle(P particle);

    public ParticleGroup<P> group(int i) {
        return groups.get(i);
    }
}
