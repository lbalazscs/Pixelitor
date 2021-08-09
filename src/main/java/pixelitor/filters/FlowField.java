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

package pixelitor.filters;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pd.OpenSimplex2F;
import pixelitor.ThreadPool;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.particles.Modifier;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;
import pixelitor.utils.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Supplier;

//import static net.jafama.CmnFastMath.toRange;
import static net.jafama.FastMath.*;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.FREE_TRANSPARENCY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class FlowField extends ParametrizedFilter {
    public static final String NAME = "Flow Field";

    private static final int PAD = 100;
    private static final int PARTICLES_PER_GROUP = 100;

    public enum PhysicsMode {
        FORCE_MODE_VELOCITY("No Mass") {
            @Override
            void updateParticle(FlowFieldParticle particle, Point2D delta) {
                Geometry.add(particle.pos, delta, particle.pos);
            }
        }, FORCE_MODE_ACCELERATION("Uniform Mass") {
            @Override
            void updateParticle(FlowFieldParticle particle, Point2D delta) {
                Geometry.add(particle.vel, delta, particle.vel);
                Geometry.add(particle.pos, particle.vel, particle.pos);
            }
        }, FORCE_MODE_JOLT("Jolt") {
            @Override
            void updateParticle(FlowFieldParticle particle, Point2D delta) {
                Geometry.add(particle.acc, delta, particle.acc);
                Geometry.add(particle.vel, particle.acc, particle.vel);
                Geometry.add(particle.pos, particle.vel, particle.pos);
            }
        }, FORCE_MODE_VELOCITY_AND_NOISE_BASED_RANDOMNESS("Thicken") {
            @Override
            void updateParticle(FlowFieldParticle particle, Point2D delta) {
                float nmmas = Noise.noise2((float) delta.getX(), (float) delta.getY()) * 10;
                particle.pos.setLocation(delta.getX() + nmmas, delta.getY() + nmmas);
            }
        };

        private final String name;

        PhysicsMode(String name) {
            this.name = name;
        }

        abstract void updateParticle(FlowFieldParticle particle, Point2D delta);

        @Override
        public String toString() {
            return name;
        }
    }

    private final RangeParam particlesParam = new RangeParam("Particle Count", 1, 1000, 10000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam zoomParam = new RangeParam("Zoom (%)", 100, 4000, 10000);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");

    private final EnumParam<PhysicsMode> physicsModeParam = new EnumParam<>("Field Effect", PhysicsMode.class);
    //    private final RangeParam massRandomnessParam = new RangeParam("Mass Randomness", 0, 10, 100);
    private final RangeParam maxVelocityParam = new RangeParam("Maximum Velocity", 1, 4000, 5000);
    private final LogZoomParam forceParam = new LogZoomParam("Force", 1, 320, 400);
    private final RangeParam varianceParam = new RangeParam("Variance", 1, 20, 100);

    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1.0f), FREE_TRANSPARENCY);
    private final ColorParam particleColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), FREE_TRANSPARENCY);
    private final RangeParam colorRandomnessParam = new RangeParam("Color Randomness (%)", 0, 0, 100);

    private final RangeParam smoothnessParam = new RangeParam("Smoothness (%)", 1, 75, 100);
    private final RangeParam iterationsParam = new RangeParam("Iterations (Makes simulation slow!!)", 1, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam turbulenceParam = new RangeParam("Turbulence", 1, 1, 8);
    private final RangeParam windParam = new RangeParam("Wind", 0, 0, 200);
    private final RangeParam drawToleranceParam = new RangeParam("Tolerance", 0, 30, 200);

    public FlowField() {
        super(false);

        DialogParam physicsParam = new DialogParam("Physics", physicsModeParam, maxVelocityParam, forceParam, varianceParam);
        DialogParam advancedParam = new DialogParam("Advanced", smoothnessParam, iterationsParam, turbulenceParam, windParam, drawToleranceParam);

        setParams(
                particlesParam,
                zoomParam,
                strokeParam,
                physicsParam,
                backgroundColorParam,
                particleColorParam,
                colorRandomnessParam,
                advancedParam
        ).withAction(ReseedSupport.createSimplexAction());

        iterationsParam.setToolTip("Change filament thickness");
        particlesParam.setToolTip("Number of filaments");
        zoomParam.setToolTip("Adjust the zoom");
        strokeParam.setToolTip("Adjust the stroke style");
        colorRandomnessParam.setToolTip("Randomize colors");

        smoothnessParam.setToolTip("Smoothness of filament");
        forceParam.setToolTip("Stroke Length");
        turbulenceParam.setToolTip("Adjust the variance provided by Noise.");
        windParam.setToolTip("Spreads away the flow");
        drawToleranceParam.setToolTip("Require longer fibres to be drawn.");
    }

    Rectangle bounds;
    float zoom;
    int fieldWidth;
    int fieldHeight;
    float fieldDensity;
    List<DoubleAdder> zFactors;
    float initTheta;
    OpenSimplex2F noise;
    PhysicsMode physicsMode;
    float force;
    int turbulence;
    float maximumVelocity;
    int tolerance;

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        zoom = zoomParam.getValue() * 0.1f;
        physicsMode = physicsModeParam.getSelected();
        maximumVelocity = maxVelocityParam.getValue() * maxVelocityParam.getValue() / 10000.0f;
        force = (float) forceParam.getZoomRatio();


        int particleCount = particlesParam.getValue();
        Color particleColor = particleColorParam.getColor();

        float colorRandomness = colorRandomnessParam.getPercentageValF();
        boolean randomizeColor = colorRandomness != 0;

        float variance = varianceParam.getValue() / 10.0f;
        float quality = smoothnessParam.getValueAsFloat() / 99 * 400 / zoom;
        turbulence = turbulenceParam.getValue();
        float zFactor = windParam.getValueAsFloat() / 10000;
        tolerance = drawToleranceParam.getValue();

        /////////////////////////////////////////////////////////////////////////////////////////////////////

        int imgWidth = dest.getWidth();
        int imgHeight = dest.getHeight();

        Random r = ReseedSupport.getLastSeedRandom();
        noise = ReseedSupport.getLastSeedSimplex();

        Graphics2D g2 = dest.createGraphics();
        g2.setStroke(strokeParam.createStroke());

        Colors.fillWith(backgroundColorParam.getColor(), g2, imgWidth, imgHeight);

        fieldWidth = (int) (imgWidth * quality + 1);
        fieldDensity = fieldWidth * 1.0f / imgWidth;
        fieldHeight = (int) (imgHeight * fieldDensity);

        bounds = new Rectangle(-PAD, -PAD, imgWidth + PAD * 2, imgHeight + PAD * 2);

        /////////////////////////////////////////////////////////////////////////////////////////////////////

        float PI = (float) FastMath.PI * variance;
        initTheta = r.nextFloat() * 2 * PI;

        Color[][] fieldColors = ((Supplier<Color[][]>) () -> {
            if (randomizeColor) {
                return new Color[fieldWidth][fieldHeight];
            }
            return null;
        }).get();

        if (randomizeColor) {
            GoldenRatio goldenRatio = new GoldenRatio(r, particleColor, colorRandomness);

            for (int i = 0; i < fieldWidth; i++) {
                for (int j = 0; j < fieldHeight; j++) {
                    fieldColors[i][j] = goldenRatio.next();
                }
            }
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////

        int groupCount = ceilToInt(particleCount / (double) PARTICLES_PER_GROUP);

        zFactors = new ArrayList<>(groupCount+1);
        for (int i = 0; i < groupCount; i++) {
            zFactors.add(new DoubleAdder());
        }

        ParticleSystem<FlowFieldParticle> system = ParticleSystem.<FlowFieldParticle>createSystem(particleCount)
                .setParticleCreator(() -> new FlowFieldParticle(g2))
                .addModifier(new Modifier.RandomizePosition<>(bounds.x, bounds.y, bounds.width, bounds.height, r))
                .addModifier(particle -> {
                    int fieldX = toRange(0, fieldWidth - 1, (int) (particle.pos.getX() * fieldDensity));
                    int fieldY = toRange(0, fieldHeight - 1, (int) (particle.pos.getY() * fieldDensity));

                    particle.color = randomizeColor ? fieldColors[fieldX][fieldY] : particleColor;

                    particle.pathPoints = new ArrayList<>();
                    particle.pathPoints.add(new Point2D.Double(particle.las_pos.getX(), particle.las_pos.getY()));
                })
                .build();


        var pt = new StatusBarProgressTracker(NAME, groupCount);
        int groupSize = (int) FastMath.ceil(particleCount * 1d / groupCount);
        Future<?>[] futures = new Future[groupCount];

        for (int i = 0; i < iterationsParam.getValue(); i++) {
            system.step();
        }
//        for (int i = 0, k = 0; i < groupCount && k < particleCount; i++, k += groupSize) {
//            int finalI = i;
//            futures[i] = ThreadPool.submit(() -> {
//                for (int j = 0; j < iterationsParam.getValue(); j++) {
//                    zFactors.get(finalI).add(zFactor);
//                    system.step(finalI, finalI + groupSize);
//                }
//            });
//        }

//        ThreadPool.waitFor(futures, pt);
        system.flush();
        pt.finished();
        g2.dispose();
        return dest;
    }

    private class FlowFieldParticle extends Particle {
        static int index;

        Point2D acc = new Point2D.Float();

        public List<Point2D> pathPoints;
        public final int groupIndex;
        public final Graphics2D g2;

        public FlowFieldParticle(Graphics2D g) {
            las_pos = new Point2D.Float();
            pos = new Point2D.Float();
            vel = new Point2D.Float();
            this.groupIndex = index / PARTICLES_PER_GROUP;
            this.g2 = g;
            index++;
        }

        @Override
        public void flush() {
            if (isPathReady()) g2.draw(getPath());
        }

        @Override
        public void reset() {
            flush();
            las_pos.setLocation(pos);
        }

        @Override
        public boolean isDead() {
            return !bounds.contains(pos);
        }

        public void update() {

            int fieldX = toRange(0, fieldWidth - 1, (int) (pos.getX() * fieldDensity));
            int fieldY = toRange(0, fieldHeight - 1, (int) (pos.getY() * fieldDensity));

            Point2D prevVelocity = new Point2D.Float();
            prevVelocity.setLocation(vel);

            float sampleX = fieldX / zoom / fieldDensity;
            float sampleY = fieldY / zoom / fieldDensity;
            double sampleZ = zFactors.get(groupIndex).doubleValue();
            float value = initTheta + (float) (noise.turbulence3(sampleX, sampleY, sampleZ, turbulence) * PI);
            Point2D delta = new Point2D.Double(force * cos(value), force * sin(value));
            physicsMode.updateParticle(this, delta);

            if (vel.getX() * vel.getX() + vel.getY() * vel.getY() > maximumVelocity) {
                vel.setLocation(prevVelocity);
            }

            if (anyDisplacementInXOrYIsGreaterThanOne()) {
                pathPoints.add(Geometry.newFrom(pos));
                las_pos.setLocation(pos);
            }
        }

        private boolean anyDisplacementInXOrYIsGreaterThanOne() {
            return abs(las_pos.getX() - pos.getX()) > tolerance || abs(las_pos.getY() - pos.getY()) > tolerance;
        }

        public boolean isPathReady() {
            return pathPoints != null && pathPoints.size() >= 3;
        }

        public Shape getPath() {
            return Shapes.smoothConnect(pathPoints);
        }
    }
}
