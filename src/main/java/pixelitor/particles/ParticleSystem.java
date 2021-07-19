package pixelitor.particles;

import java.util.ArrayList;
import java.util.List;

public abstract class ParticleSystem<P extends Particle> {

    private final List<ParticleGroup<P>> groups;

    public ParticleSystem() {
        this(10, 100);
    }

    public ParticleSystem(int groupCount, int groupSize) {
        groups = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount; i++)
            groups.add(new ParticleGroup<>(this, groupSize));
    }

    public final void step() {
        for (ParticleGroup<P> group : groups) group.step();
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
