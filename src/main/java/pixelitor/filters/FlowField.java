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
import pixelitor.particles.ParticleSystem;
import pixelitor.particles.SmoothPathParticle;
import pixelitor.utils.Geometry;
import pixelitor.utils.GoldenRatio;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Supplier;

import static net.jafama.FastMath.*;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.FREE_TRANSPARENCY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class FlowField extends ParametrizedFilter {
    public static final String NAME = "Flow Field";

    private static final int PAD = 100;
    private static final int PARTICLES_PER_GROUP = 100;

    public enum ParticleUpdater implements Modifier<FlowFieldParticle> {
        FLOW_FIELD("Flow Fields") {
            @Override
            public void physicsAct(FlowFieldParticle particle) {
                ParticleFlowUpdater.FORCE_MODE_VELOCITY.modify(particle);
            }
        },
        FLOW_FIELD_2("Distorted Flow") {
            @Override
            public void physicsAct(FlowFieldParticle particle) {
                ParticleFlowUpdater.FORCE_MODE_ACCELERATION.modify(particle);
            }
        },
        FLOW_FIELD_3("Haywire") {
            @Override
            public void physicsAct(FlowFieldParticle particle) {
                ParticleFlowUpdater.FORCE_MODE_JOLT.modify(particle);
            }
        },
        FLOW_FIELD_4("Spread Fields") {
            @Override
            public void physicsAct(FlowFieldParticle particle) {
                ParticleFlowUpdater.FORCE_MODE_VELOCITY_AND_NOISE_BASED_RANDOMNESS.modify(particle);
            }
        },
        SINK("Sink") {
            @Override
            public void init(FlowFieldParticle particle) {
                Geometry.subtract(particle.center, particle.pos, particle.tempPoint);
                Geometry.setMagnitude(particle.tempPoint, particle.force);
            }

            @Override
            public void physicsAct(FlowFieldParticle particle) {
                Geometry.add(particle.pos, particle.tempPoint, particle.pos);
            }
        },
        REVOLVE("Revolve") {
            Point2D.Float none = new Point2D.Float();

            @Override
            public void init(FlowFieldParticle particle) {
                particle.tempFloat = Geometry.distance(particle.pos, particle.center);
            }

            @Override
            public void physicsAct(FlowFieldParticle particle) {
                Geometry.subtract(particle.center, particle.pos, particle.tempPoint);
                Geometry.perpendiculars(particle.tempPoint, particle.tempPoint, none);
                Geometry.setMagnitude(particle.tempPoint, particle.force);
                Geometry.add(particle.pos, particle.tempPoint, particle.pos);
                // TODO: Optional improvements
                Geometry.subtract(particle.center, particle.pos, particle.tempPoint);
                Geometry.setMagnitude(particle.tempPoint,particle.tempFloat);
                Geometry.subtract(particle.center,particle.tempPoint, particle.pos);
            }
        };

        private final String name;

        ParticleUpdater(String name) {
            this.name = name;
        }

        public void init(FlowFieldParticle particle) {
        }

        @Override
        public void modify(FlowFieldParticle particle) {
            Point2D oldVel = Geometry.newFrom(particle.vel);

            physicsAct(particle);

            if (Geometry.distanceSq(particle.vel) > particle.maximumVelocitySq) {
                particle.vel.setLocation(oldVel);
            }

            if (particle.anyDisplacementInXOrYIsGreaterThanTolerance()) {
                particle.addPoint(Geometry.newFrom(particle.pos));
                particle.las_pos.setLocation(particle.pos);
            }
        }

        public abstract void physicsAct(FlowFieldParticle particle);

        @Override
        public String toString() {
            return name;
        }
    }

    public enum ParticleFlowUpdater implements Modifier<FlowFieldParticle> {
        FORCE_MODE_VELOCITY {
            @Override
            void updateParticle(FlowFieldParticle particle, Point2D delta) {
                Geometry.add(particle.pos, delta);
            }
        }, FORCE_MODE_ACCELERATION {
            @Override
            void updateParticle(FlowFieldParticle particle, Point2D delta) {
                Geometry.add(particle.vel, delta);
                Geometry.add(particle.pos, particle.vel);
            }
        }, FORCE_MODE_JOLT {
            @Override
            void updateParticle(FlowFieldParticle particle, Point2D delta) {
                Geometry.add(particle.acc, delta);
                Geometry.add(particle.vel, particle.acc);
                Geometry.add(particle.pos, particle.vel);
            }
        }, FORCE_MODE_VELOCITY_AND_NOISE_BASED_RANDOMNESS {
            @Override
            void updateParticle(FlowFieldParticle particle, Point2D delta) {
                Geometry.add(delta, Noise.noise2((float) delta.getX(), (float) delta.getY()) * 10);
                Geometry.add(particle.pos, delta);
            }
        };

        @Override
        public void modify(FlowFieldParticle particle) {
            Point2D fieldPoint = particle.getFieldPoint();
            Point2D delta = new Point2D.Float();

            Geometry.deScale(fieldPoint, particle.zoom * particle.fieldDensity);

            double sampleZ = particle.zFactor.doubleValue();

            float value = particle.noise.get(fieldPoint.getX(), fieldPoint.getY(), sampleZ);
            delta.setLocation(particle.force * cos(value), particle.force * sin(value));
            updateParticle(particle, delta);
        }

        abstract void updateParticle(FlowFieldParticle particle, Point2D delta);
    }

    private final EnumParam<ParticleUpdater> updaterParam = new EnumParam<>("Mode", ParticleUpdater.class);
    private final RangeParam particlesParam = new RangeParam("Particle Count", 1, 1000, 10000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam zoomParam = new RangeParam("Zoom (%)", 100, 4000, 10000);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");

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

        DialogParam physicsParam = new DialogParam("Physics", maxVelocityParam, forceParam, varianceParam);
        DialogParam advancedParam = new DialogParam("Advanced", smoothnessParam, iterationsParam, turbulenceParam, windParam, drawToleranceParam);

        setParams(
                updaterParam,
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

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        ParticleUpdater updater = updaterParam.getSelected();
        int particleCount = particlesParam.getValue();
        float zoom = zoomParam.getValue() * 0.1f;
        Stroke stroke = strokeParam.createStroke();
        Color bgColor = backgroundColorParam.getColor();
        Color particleColor = particleColorParam.getColor();
        float colorRandomness = colorRandomnessParam.getPercentageValF();

        float maximumVelocitySq = maxVelocityParam.getValue() * maxVelocityParam.getValue() / 10000.0f;
        float force = (float) forceParam.getZoomRatio();
        float variance = varianceParam.getValue() / 10.0f;

        float quality = smoothnessParam.getValueAsFloat() / 99 * 400 / zoom;
        int iterationCount = iterationsParam.getValue();
        int turbulence = turbulenceParam.getValue();
        float zFactor = windParam.getValueAsFloat() / 10000;
        int tolerance = drawToleranceParam.getValue();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int imgWidth = dest.getWidth();
        int imgHeight = dest.getHeight();
        Point2D center = new Point2D.Float(imgWidth / 2f, imgHeight / 2f);

        int fieldWidth = (int) (imgWidth * quality + 1);
        float fieldDensity = fieldWidth * 1.0f / imgWidth;
        int fieldHeight = (int) (imgHeight * fieldDensity);

        Graphics2D g2 = dest.createGraphics();
        g2.setStroke(stroke);
        Colors.fillWith(bgColor, g2, imgWidth, imgHeight);

        Random r = ReseedSupport.getLastSeedRandom();
        OpenSimplex2F noise = ReseedSupport.getLastSeedSimplex();

        int groupCount = ceilToInt(particleCount / (double) PARTICLES_PER_GROUP);
        Future<?>[] futures = new Future[groupCount];
        var pt = new StatusBarProgressTracker(NAME, futures.length + 1);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Rectangle bounds = new Rectangle(-PAD, -PAD,
                imgWidth + PAD * 2, imgHeight + PAD * 2);
        boolean randomizeColor = colorRandomness != 0;
        float PI = (float) FastMath.PI * variance;
        float initTheta = (float) (r.nextFloat() * 2 * FastMath.PI);

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

        AtomicInteger particlesCreated = new AtomicInteger();
        DoubleAdder[] zFactors = new DoubleAdder[groupCount];
        for (int i = 0; i < groupCount; i++) {
            zFactors[i] = new DoubleAdder();
        }

        ParticleSystem<FlowFieldParticle> system = ParticleSystem.<FlowFieldParticle>createSystem(particleCount)
                .setParticleCreator(() -> new FlowFieldParticle(g2, bounds, fieldWidth, fieldHeight, fieldDensity, zoom, zFactors[particlesCreated.getAndIncrement() / PARTICLES_PER_GROUP], (x, y, z) -> initTheta + (float) (noise.turbulence3(x, y, z, turbulence) * PI), force, maximumVelocitySq, tolerance, center, updater))
                .addModifier(new Modifier.RandomizePosition<>(bounds.x, bounds.y, bounds.width, bounds.height, r))
                .addModifier(particle -> {
                    int fieldX = toRange(0, fieldWidth - 1, (int) (particle.pos.getX() * fieldDensity));
                    int fieldY = toRange(0, fieldHeight - 1, (int) (particle.pos.getY() * fieldDensity));

                    particle.color = randomizeColor ? fieldColors[fieldX][fieldY] : particleColor;

//                    particle.pathPoints.clear();
                    particle.addPoint(Geometry.newFrom(particle.pos));
                })
                .addModifier(particle -> {
                    for (ParticleUpdater particleUpdater : particle.updaters) {
                        particleUpdater.init(particle);
                    }
                })
                .build();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            int finalGroupIndex = groupIndex;
            futures[groupIndex] = ThreadPool.submit(() -> {
                int start = finalGroupIndex * PARTICLES_PER_GROUP;
                int end = start + PARTICLES_PER_GROUP;
                for (int iterationIndex = 0; iterationIndex < iterationCount; iterationIndex++) {
                    zFactors[finalGroupIndex].add(zFactor);
                    system.step(start, end);
                }
            });
        }

        ThreadPool.waitFor(futures, pt);
        system.flush();
        pt.finished();
        g2.dispose();
        return dest;
    }

    public static class FlowFieldParticle extends SmoothPathParticle {

        // Our Fields
        public final Point2D acc;
        public final Point2D tempPoint;
        public float tempFloat = 0;

        // Imported Fields
        private final Rectangle bounds;
        private final int fieldWidth;
        private final int fieldHeight;
        private final float fieldDensity;
        private final float zoom;
        private final DoubleAdder zFactor;
        private final NoiseSupplier noise;
        private final float force;
        private final float maximumVelocitySq;
        private final double tolerance;
        private final ParticleUpdater[] updaters;

        private final Point2D center;

        public FlowFieldParticle(Graphics2D g2, Rectangle bounds, int fieldWidth, int fieldHeight, float fieldDensity, float zoom, DoubleAdder zFactor, NoiseSupplier noise, float force, float maximumVelocitySq, double tolerance, Point2D center, ParticleUpdater... updaters) {
            super(g2);
            this.center = center;
            this.acc = new Point2D.Float();
            this.tempPoint = new Point2D.Float();
            this.vel = new Point2D.Float();
            this.pos = new Point2D.Float();
            this.las_pos = new Point2D.Float();

            this.bounds = bounds;
            this.fieldWidth = fieldWidth - 1;
            this.fieldHeight = fieldHeight - 1;
            this.fieldDensity = fieldDensity;
            this.zoom = zoom;
            this.zFactor = zFactor;
            this.noise = noise;
            this.force = force;
            this.maximumVelocitySq = maximumVelocitySq;
            this.tolerance = tolerance;

            this.updaters = updaters;
        }

        @Override
        public void reset() {
            las_pos.setLocation(pos);
        }

        @Override
        public boolean isDead() {
            return !bounds.contains(pos);
        }

        @Override
        public void update() {
            for (ParticleUpdater updater : updaters) {
                updater.modify(this);
            }
        }

        private boolean anyDisplacementInXOrYIsGreaterThanTolerance() {
            return abs(las_pos.getX() - pos.getX()) > tolerance || abs(las_pos.getY() - pos.getY()) > tolerance;
        }

        public Point2D getFieldPoint() {
            Point2D fieldPoint = new Point2D.Float();
            fieldPoint.setLocation(pos);
            Geometry.scale(fieldPoint, fieldDensity);
            Geometry.toRange(fieldPoint, 0, 0, fieldWidth, fieldHeight);
            return fieldPoint;
        }

    }


    public interface NoiseSupplier {
        float get(double x, double y, double z);
    }

}
