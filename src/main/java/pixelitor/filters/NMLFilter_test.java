package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.particles.Modifier;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;
import pixelitor.utils.Geometry;
import pixelitor.utils.ReseedSupport;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static net.jafama.FastMath.ceilToInt;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.FREE_TRANSPARENCY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class NMLFilter_test extends ParametrizedFilter {

    public static final String NAME = "NMLFilter_test";
    public static final int PARTICLES_PER_GROUP = 100;

    public enum Physics {
        SINK() {
            @Override
            public void init(NMLParticle particle) {
                Geometry.subtract(particle.center, particle.pos, particle.dcx);
                Geometry.setMagnitude(particle.dcx, 0.9f);
            }

            @Override
            public void actFor(NMLParticle particle) {
                Geometry.add(particle.pos, particle.dcx, particle.pos);
            }

        }, REVOLVE() {
            Point2D.Float none = new Point2D.Float();

            @Override
            public void init(NMLParticle particle) {
            }

            @Override
            public void actFor(NMLParticle particle) {
                Geometry.subtract(particle.center, particle.pos, particle.dcx);
                Geometry.perpendiculars(particle.dcx, particle.dcx, none);
                Geometry.setMagnitude(particle.dcx, 0.3f);
                Geometry.add(particle.pos, particle.dcx, particle.pos);
            }
        };

        public abstract void init(NMLParticle particle);

        public abstract void actFor(NMLParticle particle);
    }

    private final RangeParam particlesParam = new RangeParam("Particle Count", 1, 1000, 10000, true, BORDER, IGNORE_RANDOMIZE);
    private final EnumParam<Physics> physicsParam = new EnumParam<>("Physic Effect", Physics.class);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");
    private final RangeParam iterationsParam = new RangeParam("Iterations (Makes simulation slow!!)", 1, 100, 1000, true, BORDER, IGNORE_RANDOMIZE);
    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1.0f), FREE_TRANSPARENCY);
    private final ColorParam particleColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), FREE_TRANSPARENCY);
    private final RangeParam colorRandomnessParam = new RangeParam("Color Randomness (%)", 0, 0, 100);
    private final RangeParam radiusRandomnessParam = new RangeParam("Radius Randomness (%)", 0, 0, 1000);

    public NMLFilter_test() {
        super(true);
        setParams(
                particlesParam,
                physicsParam,
                strokeParam,
                iterationsParam,
                backgroundColorParam,
                particleColorParam,
                colorRandomnessParam,
                radiusRandomnessParam
        ).withAction(ReseedSupport.createSimplexAction());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        final int particleCount = particlesParam.getValue();
        final int imgWidth = dest.getWidth();
        final int imgHeight = dest.getHeight();
        final float radiusRandomness = radiusRandomnessParam.getPercentageValF();

        Random r = ReseedSupport.getLastSeedRandom();
        Point2D.Float center = new Point2D.Float(imgWidth / 2f, imgHeight / 2f);
        Graphics2D g = dest.createGraphics();

        Colors.fillWith(backgroundColorParam.getColor(), g, imgWidth, imgHeight);

        final Stroke[] strokes = new Stroke[radiusRandomness == 0 ? 1 : FastMath.min(particleCount, 100)];
        for (int i = 0; i < strokes.length; i++) {
            strokes[i] = strokeParam.createStrokeWithRandomWidth(r, radiusRandomness);
        }

        ParticleSystem<NMLParticle> system = ParticleSystem.<NMLParticle>createSystem(particleCount)
                .setParticleCreator(() -> new NMLParticle(center, g, physicsParam.getSelected(), strokes[r.nextInt(strokes.length)]))
                .addModifier(new Modifier.RandomizePosition<>(imgWidth, imgHeight, r))
                .addModifier(new Modifier.RandomGoldenRatioColor<>(r, particleColorParam.getColor(), colorRandomnessParam.getPercentageValF()))
                .build();

        for (int i = 0, s = iterationsParam.getValue(); i < s; i++) {
            system.step();
        }

        system.flush();
        g.dispose();
        return dest;
    }


    private static class NMLParticle extends Particle {

        final Point2D center;
        final Graphics2D g;
        final Physics physics;
        final Stroke stroke;
        Point2D dcx = new Point2D.Float();

        public NMLParticle(Point2D center, Graphics2D g, Physics physics, Stroke stroke) {
            pos = new Point2D.Float();
            las_pos = new Point2D.Float();

            this.center = center;
            this.g = g;
            this.physics = physics;
            this.stroke = stroke;
        }

        @Override
        public void flush() {
            draw();
        }

        public void reset() {
            las_pos.setLocation(pos);
            physics.init(this);
        }

        @Override
        public boolean isDead() {
            return FastMath.abs(pos.getX() - center.getX()) < 1 && FastMath.abs(pos.getY() - center.getY()) < 1;
        }

        public void update() {
            physics.actFor(this);
        }

        public void draw() {
            if (stroke != g.getStroke())
                g.setStroke(stroke);
            g.setColor(color);
            g.drawLine((int) las_pos.getX(), (int) las_pos.getY(), (int) pos.getX(), (int) pos.getY());
        }
    }
}
