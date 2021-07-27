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

public class ParticleGroup<P extends Particle> {
    private final ParticleSystem<P> particleSystem;
    private final List<P> particles;

    public ParticleGroup(ParticleSystem<P> particleSystem, int index, int groupSize) {
        this.particleSystem = particleSystem;

        particles = new ArrayList<>(groupSize);
        for (int i = 0; i < groupSize; i++) {
            P particle = particleSystem.newParticle();
            particle.groupIndex = index;
            particleSystem.initializeParticle(particle);
            particles.add(particle);
        }
    }

    public void step() {
        for (P particle : particles) {
            if (particleSystem.isParticleDead(particle)) {
                particleSystem.initializeParticle(particle);
            }
            particleSystem.updateParticle(particle);
        }
    }

    public List<P> getParticles() {
        return particles;
    }
}
