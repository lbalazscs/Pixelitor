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
import pixelitor.utils.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static net.jafama.FastMath.*;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.FREE_TRANSPARENCY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

public class FlowField extends ParametrizedFilter {

    public static final String NAME = "Flow Field";

    //<editor-fold defaultstate="collapsed" desc="PRIVATE STATIC FIELDS">

    private static final int PAD = 100;

    private static final int PARTICLES_PER_GROUP = 100;

    private static final float SMOOTHNESS = 1224.3649f;

    private enum ForceMode implements Modifier<FlowFieldParticle> {
        FORCE_MODE_VELOCITY("No Mass") {
            @Override
            public void modify(FlowFieldParticle particle) {
                Geometry.add(particle.pos, particle.delta);
            }
        },
        FORCE_MODE_ACCELERATION("Uniform Mass") {
            @Override
            public void modify(FlowFieldParticle particle) {
                Geometry.add(particle.vel, particle.delta);
                Geometry.add(particle.pos, particle.vel);
            }
        },
        FORCE_MODE_JOLT("Jolt") {
            @Override
            public void modify(FlowFieldParticle particle) {
                Geometry.add(particle.acc, particle.delta);
                Geometry.add(particle.vel, particle.acc);
                Geometry.add(particle.pos, particle.vel);
            }
        },
        FORCE_MODE_VELOCITY_AND_NOISE_BASED_RANDOMNESS("Thicken") {
            @Override
            public void modify(FlowFieldParticle particle) {
                Geometry.add(particle.delta, Noise.noise2((float) particle.delta.getX(), (float) particle.delta.getY()) * 10);
                Geometry.add(particle.pos, particle.delta);
            }
        };

        final String name;

        ForceMode(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="GUI PARAM DEFINITIONS">

    private final RangeParam noiseParam = new RangeParam("Noise", 0, 100, 100);
    private final RangeParam sinkParam = new RangeParam("Sink", 0, 0, 100);
    private final RangeParam revolveParam = new RangeParam("Revolve", 0, 0, 100);

    private final RangeParam particlesParam = new RangeParam("Particle Count", 1, 1000, 20000, true, BORDER, IGNORE_RANDOMIZE);

    private final EnumParam<ForceMode> forceModeParam = new EnumParam<>("Force Mode", ForceMode.class);
    private final RangeParam maxVelocityParam = new RangeParam("Maximum Velocity", 1, 4000, 5000);
    private final LogZoomParam forceParam = new LogZoomParam("Force", 1, 320, 400);
    private final RangeParam varianceParam = new RangeParam("Variance", 1, 20, 100);

    private final RangeParam zoomParam = new RangeParam("Zoom (%)", 100, 4000, 10000);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");
    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1.0f), FREE_TRANSPARENCY);
    private final ColorParam particleColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), FREE_TRANSPARENCY);
    private final BooleanParam useSourceImageAsInitialColorsParam = new BooleanParam("Initialize colors from source,", false, IGNORE_RANDOMIZE);
    private final BooleanParam useSourceImageAsStartingPositionParam = new BooleanParam("Start flow from source,", false, IGNORE_RANDOMIZE);
    private final RangeParam colorRandomnessParam = new RangeParam("Color Randomness (%)", 0, 0, 100);
    private final RangeParam radiusRandomnessParam = new RangeParam("Radius Randomness (%)", 0, 0, 1000);

    private final RangeParam iterationsParam = new RangeParam("Iterations (Makes simulation slow!!)", 1, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final RangeParam turbulenceParam = new RangeParam("Turbulence", 1, 1, 8);
    private final RangeParam windParam = new RangeParam("Wind", 0, 0, 200);
    private final RangeParam drawToleranceParam = new RangeParam("Tolerance", 0, 30, 200);

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="CONSTRUCTOR, SECOND STEP FOR INITIALIZATION">

    public FlowField() {
        super(false);

        GroupedRangeParam forceModifierParam = new GroupedRangeParam("Noise", new RangeParam[]{
                noiseParam,
                sinkParam,
                revolveParam
        }, false).autoNormalized();


        DialogParam physicsParam = new DialogParam("Physics", forceModeParam, maxVelocityParam, forceParam, varianceParam);
        DialogParam advancedParam = new DialogParam("Advanced", iterationsParam, turbulenceParam, windParam, drawToleranceParam);

        setParams(

                forceModifierParam,

                particlesParam,
                physicsParam,

                zoomParam,
                strokeParam,
                backgroundColorParam,
                particleColorParam,
                useSourceImageAsInitialColorsParam,
                useSourceImageAsStartingPositionParam,
                colorRandomnessParam,
                radiusRandomnessParam,

                advancedParam

        ).withAction(ReseedSupport.createSimplexAction());

        noiseParam.setToolTip("Add smooth randomness to the flow of particles.");
        sinkParam.setToolTip("Make particles flow towards the center point.");
        revolveParam.setToolTip("Make particles flow around the center point.");

        particlesParam.setToolTip("Adjust the number of particles flowing in the field.");

        physicsParam.setToolTip("Some additional tweaks to manipulate the evaluation system.");
        forceModeParam.setToolTip("TODO"); // TODO
        maxVelocityParam.setToolTip("Adjust maximum velocity to make particles look more organised.");
        varianceParam.setToolTip("Increase variance to add more turns and twists to the flow.");
        forceParam.setToolTip("Determine how powerful the final force applied is.");

        zoomParam.setToolTip("Change zoom to make fields look bigger or smaller. Please note, that particle width will not take any effect.");
        strokeParam.setToolTip("Adjust how particles are drawn - their width, shape, joins...");
        backgroundColorParam.setToolTip("Fills the canvas with a color. Decrease transparency to show the previous image.");
        particleColorParam.setToolTip("Change the initial color of the particles. Play with transparency to get interesting fills.");
        colorRandomnessParam.setToolTip("Increase to impart the particle color with some randomness.");
        radiusRandomnessParam.setToolTip("Increase to draw particles with a randomised width.");

        advancedParam.setToolTip("Advanced or Rarely used parameters which make slight adjustments to the simulation.");
        iterationsParam.setToolTip("Make individual particles cover longer paths.");
        turbulenceParam.setToolTip("Increase to make the flow path rougher.");
        windParam.setToolTip("Spreads away the particles against the flow path.");
        drawToleranceParam.setToolTip("Require only longer paths to be drawn.");

    }

    //</editor-fold>

    //<editor-fold desc="FLOW FIELD CREATION LOGIC">

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        //<editor-fold defaultstate="collapsed" desc="FINAL LOCAL VARIABLES DECLARATIONS">

        final int particleCount = particlesParam.getValue();
        final ForceMode forceMode = forceModeParam.getSelected();
        final float maximumVelocitySq = maxVelocityParam.getValue() * maxVelocityParam.getValue() / 10000.0f;
        final float force = (float) forceParam.getZoomRatio();
        final float variance = varianceParam.getValue() / 10.0f;

        final float multiplierNoise = noiseParam.getPercentageValF() * force;
        final float multiplierSink = sinkParam.getPercentageValF() * force;
        final float multiplierRevolve = revolveParam.getPercentageValF() * force;

        final float zoom = zoomParam.getValue() * 0.25f;
        final Stroke stroke = strokeParam.createStroke();
        final Color bgColor = backgroundColorParam.getColor();
        final Color particleColor = particleColorParam.getColor();
        final boolean inheritColors = useSourceImageAsInitialColorsParam.isChecked();
        final boolean inheritSpawnPoints = useSourceImageAsStartingPositionParam.isChecked();
        final float colorRandomness = colorRandomnessParam.getPercentageValF();
        final float radiusRandomness = radiusRandomnessParam.getPercentageValF();

        final float quality = Math.min(1.2f, SMOOTHNESS / zoom);
        final int iterationCount = iterationsParam.getValue();
        final int turbulence = turbulenceParam.getValue();
        final float zFactor = windParam.getValueAsFloat() / 10000;
        final int tolerance = drawToleranceParam.getValue();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        final int imgWidth = dest.getWidth();
        final int imgHeight = dest.getHeight();

        final int fieldWidth = (int) (imgWidth * quality + 1);
        final float fieldDensity = fieldWidth * 1.0f / imgWidth;
        final int fieldHeight = (int) (imgHeight * fieldDensity);

        final Random r = ReseedSupport.getLastSeedRandom();
        final OpenSimplex2F noise = ReseedSupport.getLastSeedSimplex();

        final Point2D center = Geometry.deScale(new Point2D.Float(fieldWidth, fieldHeight), 2);
        final Rectangle bounds = new Rectangle(-PAD, -PAD,
                fieldWidth + PAD * 2, fieldHeight + PAD * 2);
        float variantPI = (float) FastMath.PI * variance;
        float initTheta = (float) (r.nextFloat() * 2 * FastMath.PI);

        final int groupCount = ceilToInt(particleCount / (double) PARTICLES_PER_GROUP);
        final var pt = new StatusBarProgressTracker(NAME, groupCount);

        final Graphics2D g2 = dest.createGraphics();
        final boolean useColorField = colorRandomness != 0 | inheritColors;
        final boolean randomizeRadius = radiusRandomness != 0;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setStroke(stroke);
        Colors.fillWith(bgColor, g2, imgWidth, imgHeight);
        g2.setColor(particleColor);

        final Color[][] fieldColors = getIf(useColorField, () -> new Color[fieldWidth][fieldHeight]);
        final Stroke[] strokes = getIf(randomizeRadius, () -> new Stroke[100]);
        final Point2D.Float[][] fieldAccelerations = new Point2D.Float[fieldWidth][fieldHeight];
        final int[] sourcePixels = getIf(useColorField | inheritSpawnPoints, () -> ImageUtils.getPixelsAsArray(src));

        if (useColorField) {
            GoldenRatio goldenRatio = new GoldenRatio(r, particleColor, colorRandomness);
            if (inheritColors) {
                fill(fieldColors, fieldWidth, fieldHeight, (x, y) -> goldenRatio.next(new Color(sourcePixels[toRange(0, sourcePixels.length, (x /= fieldDensity) + (y /= fieldDensity) * imgWidth)], true)));
            } else
                fill(fieldColors, fieldWidth, fieldHeight, goldenRatio::next);
        }

        if (randomizeRadius) {
            fill(strokes, strokes.length, () -> strokeParam.createStrokeWithRandomWidth(r, radiusRandomness));
        }

        initializeAccelerations(multiplierSink, multiplierRevolve, fieldWidth, fieldHeight, center, /*out*/ fieldAccelerations);

        List<Point2D> spawns = null;
        if (inheritSpawnPoints) {
            spawns = initializeSpawnPoints(imgWidth, fieldDensity, sourcePixels);
        }

        //</editor-fold>

        final FlowFieldMeta meta = new FlowFieldMeta(fieldWidth - 1, fieldHeight - 1, fieldDensity, bounds, tolerance, maximumVelocitySq, zFactor, zoom, turbulence, noise, multiplierNoise, initTheta, variantPI, forceMode);

        PositionRandomizer positionRandomizer = new PositionRandomizer(bounds.x, bounds.y, bounds.width, bounds.height, spawns, r);
        ParticleInitializer particleInitializer = new ParticleInitializer(particleColor, useColorField, fieldColors);
        ForceModeUpdater forceModeUpdater = new ForceModeUpdater(forceMode, fieldAccelerations);

        ParticleSystem<FlowFieldParticle> particleSystem = ParticleSystem.<FlowFieldParticle>createSystem(particleCount)
                .setParticleCreator(() -> new FlowFieldParticle(g2, randomizeRadius ? strokes[r.nextInt(strokes.length)] : stroke, meta))
                .addModifier(positionRandomizer)
                .addModifier(particleInitializer)
                .addUpdater(forceModeUpdater)
                .build();

        final Future<?>[] futures = particleSystem.iterate(iterationCount, groupCount);
        ThreadPool.waitFor(futures, pt);
        particleSystem.flush();
        pt.finished();
        g2.dispose();

        return dest;
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="UTILITIES TO MAKE STUFF A BIT MORE READABLE">

    private void initializeAccelerations(float multiplierSink, float multiplierRevolve, int fieldWidth, int fieldHeight, Point2D center, Point2D.Float[][] fieldAccelerations) {
        Point2D none = new Point();
        Point2D.Float position = new Point2D.Float();
        Point2D.Float relativePosition = new Point2D.Float();
        Point2D.Float forceDueToRevolution = new Point2D.Float();
        Point2D.Float forceDueToSink = new Point2D.Float();

        for (int i = 0; i < fieldWidth; i++) {
            for (int j = 0; j < fieldHeight; j++) {

                // Here the vector represents the point on canvas where it's present.
                position.setLocation(i, j);
                Geometry.subtract(center, position, /*out*/ relativePosition);

                forceDueToRevolution.setLocation(relativePosition);
                Geometry.perpendiculars(forceDueToRevolution, /*out*/ forceDueToRevolution, /*out*/ none);
                Geometry.setMagnitude( /*out*/ forceDueToRevolution, multiplierRevolve);

                Geometry.normalizeIfNonzero( /*out*/ forceDueToRevolution);
                Geometry.scale( /*out*/ forceDueToRevolution, multiplierRevolve);

                forceDueToSink.setLocation(relativePosition);
                Geometry.setMagnitude( /*out*/ forceDueToSink, multiplierSink);

                // Adding forces to get relative force.
                var fieldAcceleration = new Point2D.Float();
                Geometry.add( /*out*/ fieldAcceleration, forceDueToRevolution);
                Geometry.add( /*out*/ fieldAcceleration, forceDueToSink);
                fieldAccelerations[i][j] = fieldAcceleration;
            }
        }
    }

    private List<Point2D> initializeSpawnPoints(int imgWidth, float fieldDensity, int[] sourcePixels) {
        List<Point2D> spawns = new ArrayList<>();
        for (int i = 0; i < sourcePixels.length; i++) {
            if ((sourcePixels[i] & 0xFF000000) != 0) {
                int y = i / imgWidth;
                int x = i - y * imgWidth;
                spawns.add(new Point(x *= fieldDensity, y *= fieldDensity));
            }
        }
        return spawns;
    }

    public static <T> T getIf(boolean value, Supplier<T> supplier) {
        if (value) return supplier.get();
        return null;
    }

    public static <T> void fill(T[] array, int w, Supplier<T> value) {
        for (int i = 0; i < w; i++) {
            array[i] = value.get();
        }
    }

    public static <T> void fill(T[][] array, int w, int h, Supplier<T> value) {
        fill(array, w, h, (x, y) -> value.get());
    }

    public static <T> void fill(T[][] array, int w, int h, Coord2DFunction<T> value) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                array[i][j] = value.get(i, j);
            }
        }
    }

    public interface Coord2DFunction<T> {
        T get(int x, int y);
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="PARTICLE PROPERTY MODIFIERS">

    private static class PositionRandomizer implements Modifier<FlowFieldParticle> {

        final Modifier<FlowFieldParticle> modifier;

        public PositionRandomizer(int x, int y, int width, int height, List<Point2D> spawnPoints, Random random) {
            if (spawnPoints == null || spawnPoints.size() <= 0) {
                modifier = new RandomizePosition<>(x, y, width, height, random);
            } else {
                int size = spawnPoints.size();
                modifier = particle -> particle.pos.setLocation(spawnPoints.get(random.nextInt(size)));
            }
        }

        @Override
        public void modify(FlowFieldParticle particle) {
            modifier.modify(particle);
        }
    }

    private static record ParticleInitializer(Color particleColor,
                                              boolean randomizeColor,
                                              Color[][] fieldColors) implements Modifier<FlowFieldParticle> {

        @Override
        public void modify(FlowFieldParticle particle) {
            particle.addPoint(particle.pos);
            int fieldX = particle.getFieldX();
            int fieldY = particle.getFieldY();
            particle.color = randomizeColor ? fieldColors[fieldX][fieldY] : particleColor;
        }
    }

    private static record ForceModeUpdater(ForceMode forceMode,
                                           Point2D.Float[][] fieldAccelerations) implements Modifier<FlowFieldParticle> {

        @Override
        public void modify(FlowFieldParticle particle) {
            particle.delta.setLocation(fieldAccelerations[particle.getFieldX()][particle.getFieldY()]);
        }
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="FLOW FIELD PARTICLE">

    public static class FlowFieldParticle extends SmoothPathParticle {

        public final Point2D.Float delta = new Point2D.Float();
        public final Point2D.Float acc = new Point2D.Float();

        private final Stroke stroke;
        private final FlowFieldMeta meta;

        public FlowFieldParticle(Graphics2D g2, Stroke stroke, FlowFieldMeta meta) {
            super(g2);
            this.vel = new Point2D.Float();
            this.pos = new Point2D.Float();
            this.las_pos = new Point2D.Float();

            this.stroke = stroke;
            this.meta = meta;
        }

        @Override
        public void addPoint(Point2D point) {
            las_pos.setLocation(point);
            super.addPoint(Geometry.deScale(Geometry.newFrom(point), meta.fieldDensity));
        }

        @Override
        public void flush() {
            g2.setStroke(stroke);
            super.flush();
        }

        @Override
        public void reset() {
            las_pos.setLocation(pos);
        }

        @Override
        public boolean isDead() {
            return !meta.bounds.contains(pos);
        }

        @Override
        public void update() {
            Point2D oldVel = Geometry.newFrom(vel);

            double sampleX = pos.getX() / meta.zoom;
            double sampleY = pos.getY() / meta.zoom;
            double sampleZ = meta.zFactor * iterationIndex;
            double value = meta.initTheta + meta.noise.turbulence3(sampleX, sampleY, sampleZ, meta.turbulence) * meta.variantPI;

            Point2D noiseDelta = new Point2D.Double(cos(value), sin(value));
            Geometry.setMagnitude(noiseDelta, meta.multiplierNoise);
            Geometry.add(delta, noiseDelta);
            meta.forceMode.modify(this);

            if (Geometry.distanceSq(vel) > meta.maximumVelocitySq) {
                vel.setLocation(oldVel);
            }

            if (anyDisplacementInXOrYIsGreaterThanTolerance()) {
                addPoint(Geometry.newFrom(pos));
                las_pos.setLocation(pos);
            }
        }

        private boolean anyDisplacementInXOrYIsGreaterThanTolerance() {
            return abs(las_pos.getX() - pos.getX()) > meta.tolerance || abs(las_pos.getY() - pos.getY()) > meta.tolerance;
        }

        public int getFieldX() {
            return FastMath.toRange(0, meta.fieldWidth, (int) pos.getX());
        }

        public int getFieldY() {
            return FastMath.toRange(0, meta.fieldHeight, (int) pos.getY());
        }
    }

    public static class FlowFieldMeta {
        public final int fieldWidth;
        public final int fieldHeight;
        public final float fieldDensity;
        public final Rectangle bounds;
        public final double tolerance;
        public final float maximumVelocitySq;
        public final double zFactor;
        public final double zoom;
        public final int turbulence;
        public final OpenSimplex2F noise;
        public final float multiplierNoise;
        public final float initTheta;
        public final float variantPI;
        public final ForceMode forceMode;

        public FlowFieldMeta(int fieldWidth, int fieldHeight, float fieldDensity, Rectangle bounds, double tolerance, float maximumVelocitySq, double zFactor, double zoom, int turbulence, OpenSimplex2F noise, float multiplierNoise, float initTheta, float variantPI, ForceMode forceMode) {
            this.fieldWidth = fieldWidth;
            this.fieldHeight = fieldHeight;
            this.fieldDensity = fieldDensity;
            this.bounds = bounds;
            this.tolerance = tolerance;
            this.maximumVelocitySq = maximumVelocitySq;
            this.zFactor = zFactor;
            this.zoom = zoom;
            this.turbulence = turbulence;
            this.noise = noise;
            this.multiplierNoise = multiplierNoise;
            this.initTheta = initTheta;
            this.variantPI = variantPI;
            this.forceMode = forceMode;
        }
    }

    //</editor-fold>

}
