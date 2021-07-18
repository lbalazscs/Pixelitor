package pixelitor.particles;

import java.util.ArrayList;
import java.util.List;

public class ParticleGroup<P extends Particle> {

    private final ParticleSystem<P> particleSystem;
    private final List<P> particles;

    public ParticleGroup(ParticleSystem<P> particleSystem, int groupSize) {
        this.particleSystem = particleSystem;

        particles = new ArrayList<>(groupSize);
        for (int i = 0; i < groupSize; i++)
            particles.add(particleSystem.newParticle());
    }

    public void step() {
        for (P particle : particles) {
            if(particleSystem.isParticleDead(particle))
                particleSystem.initializeParticle(particle);
            particleSystem.updateParticle(particle);
        }
    }
}
