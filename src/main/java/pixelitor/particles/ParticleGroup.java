package pixelitor.particles;

import java.util.ArrayList;
import java.util.List;

public class ParticleGroup<P extends Particle> {

    private final ParticleSystem<P> particleSystem;
    private final int index;
    private final List<P> particles;

    public ParticleGroup(ParticleSystem<P> particleSystem, int index, int groupSize) {
        this.particleSystem = particleSystem;
        this.index = index;

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
            if(particleSystem.isParticleDead(particle))
                particleSystem.initializeParticle(particle);
            particleSystem.updateParticle(particle);
        }
    }

    public List<P> getParticles() {
        return particles;
    }
}
