package pixelitor.particles;

import java.util.ArrayList;
import java.util.List;

public abstract class ParticleSystem<P extends Particle> {

    private final List<ParticleGroup<P>> groups;
    private final int groupCount;
    private final int groupSize;
    private final int particleCount;

    public ParticleSystem() {
        this(10, 100, -1);
    }

    public ParticleSystem(int groupCount, int groupSize) {
        this(groupCount, groupSize, -1);
    }

    public ParticleSystem(int groupCount, int groupSize, int particleCount) {
        this.groupCount = groupCount;
        this.groupSize = groupSize;
        if (particleCount == -1) particleCount = groupCount * groupSize;
        assert particleCount < groupCount * groupSize : "Cant accommodate more particles than the groups can provided.";
        this.particleCount = particleCount;

        groups = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount - 1; i++)
            groups.add(new ParticleGroup<>(this, i, groupSize));
        int lastGroupSize = particleCount - groupSize * (groupCount - 1);
        groups.add(new ParticleGroup<>(this, groupCount - 1, lastGroupSize));
    }

    public void step() {
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

    public int getGroupCount() {
        return groupCount;
    }

    public int getParticleCount() {
        return particleCount;
    }
}
