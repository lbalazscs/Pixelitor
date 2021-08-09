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

import net.jafama.FastMath;
import pixelitor.ThreadPool;
import pixelitor.utils.ProgressTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ParticleSystem<P extends Particle> {

    private final List<P> particles;
    private final Supplier<P> supplier;
    private final List<Modifier<P>> modifiers;

    public static <P extends Particle> ParticleSystemBuilder<P> createSystem(int particles) {
        return new ParticleSystemBuilder<>(particles);
    }

    public ParticleSystem(int particleCount, List<Modifier<P>> modifiers, Supplier<P> supplier) {
        this.particles = new ArrayList<>(particleCount);
        this.modifiers = modifiers;
        this.supplier = supplier;
        for (int i = 0; i < particleCount; i++) {
            P particle = newParticle();
            initializeParticle(particle);
            particles.add(particle);
        }
    }

    public void step() {
        for (P particle : particles) {
            stepParticle(particle);
        }
    }

    public void step(int start, int end) {
        for (int i = start, s = FastMath.min(particles.size(), end); i < s; i++) {
            P particle = particles.get(i);
            stepParticle(particle);
        }
    }

    public Future<?>[] takeStepsSplittingGroups(int iterations, int groupCount, ProgressTracker pt) {
        Future<?>[] futures = new Future[groupCount];

        int s = particles.size();
        int groupSize = (int) FastMath.ceil(s * 1d / groupCount);

        for (int i = 0, k = 0; k < groupCount; i += groupSize, k++) {

            int finalI = i;

            futures[k] = ThreadPool.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    step(finalI, finalI + groupSize);
                }
            });
        }

        return futures;
    }

    private void stepParticle(P particle) {
        if (particle.isDead()) {
            particle.flush();
            initializeParticle(particle);
        }
        particle.update();
    }

    public void flush() {
        particles.forEach(Particle::flush);
    }

    protected P newParticle() {
        return supplier.get();
    }

    protected void initializeParticle(P particle) {
        modifiers.forEach(modifier -> modifier.modify(particle));
        particle.reset();
    }

    public List<P> getParticles() {
        return particles;
    }

    public static class ParticleSystemBuilder<P extends Particle> {
        private final List<Modifier<P>> modifiers = new ArrayList<>();
        private Supplier<P> supplier = () -> null;
        private final int particles;

        public ParticleSystemBuilder(int particles) {
            this.particles = particles;
        }

        public ParticleSystemBuilder<P> setParticleCreator(Supplier<P> supplier) {
            this.supplier = supplier;
            return this;
        }

        public ParticleSystemBuilder<P> addModifier(Modifier<P> modifier) {
            modifiers.add(modifier);
            return this;
        }

        public ParticleSystem<P> build() {
            return new ParticleSystem<>(particles, modifiers, supplier);
        }

    }

}
