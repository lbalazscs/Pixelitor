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

    private static final float QUALITY = .8f;

    private static final float SMOOTHNESS = 1224.3649f;

    private static final float LIMITING_ITERATIONS = 100;

    private static final float FORCE_MAGNITUDE = (float) pow(10f, 330 / 100f) / 100f;

    private static final int TOLERANCE = 30;

    private static final float ITERATION_TO_FORCE_RATIO = FORCE_MAGNITUDE / LIMITING_ITERATIONS;

    private static final float ITERATION_TO_TOLERANCE_RATIO = TOLERANCE / LIMITING_ITERATIONS;

    private enum ForceMode implements Modifier<FlowFieldParticle> {
        FORCE_MODE_VELOCITY("No Mass") {
            @Override
            public void modify(FlowFieldParticle particle) {
                particle.pos.setLocation(
                    particle.pos.getX() + particle.delta.x,
                    particle.pos.getY() + particle.delta.y
                );
            }
        },
        FORCE_MODE_ACCELERATION("Uniform Mass") {
            @Override
            public void modify(FlowFieldParticle particle) {
                particle.vel.add(particle.delta);
                particle.pos.setLocation(
                    particle.pos.getX() + particle.vel.x,
                    particle.pos.getY() + particle.vel.y
                );
            }
        },
        FORCE_MODE_JOLT("Jolt") {
            @Override
            public void modify(FlowFieldParticle particle) {
                particle.acc.add(particle.delta);
                particle.vel.add(particle.acc);
                particle.pos.setLocation(
                    particle.pos.getX() + particle.vel.x,
                    particle.pos.getY() + particle.vel.y
                );
            }
        },
        FORCE_MODE_VELOCITY_AND_NOISE_BASED_RANDOMNESS("Thicken") {
            @Override
            public void modify(FlowFieldParticle particle) {
                particle.delta.add(Noise.noise2(particle.delta.x, particle.delta.y) * 10);
                particle.pos.setLocation(
                    particle.pos.getX() + particle.delta.x,
                    particle.pos.getY() + particle.delta.y
                );
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

    private static final int COLOR_SOURCE_DEFAULT = colorSourceInt(0, false);

    private static final int COLOR_SOURCE_FROM_SOURCE_IMAGE = colorSourceInt(1, true);

    private static final int COLOR_SOURCE_FROM_ACCELERATION_1 = colorSourceInt(2, true);

    private static final int COLOR_SOURCE_FROM_ACCELERATION_2 = colorSourceInt(3, true);

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="GUI PARAM DEFINITIONS">

    private final RangeParam noiseParam = new RangeParam("Noise", 0, 100, 100);
    private final RangeParam sinkParam = new RangeParam("Sink", 0, 0, 100);
    private final RangeParam revolveParam = new RangeParam("Revolve", 0, 0, 100);

    private final RangeParam zoomParam = new RangeParam("Zoom (%)", 100, 4000, 10000);
    private final RangeParam varianceParam = new RangeParam("Variance", 1, 20, 100);
    private final RangeParam turbulenceParam = new RangeParam("Turbulence", 1, 1, 8);
    private final RangeParam windParam = new RangeParam("Wind", 0, 0, 200);

    private final EnumParam<ForceMode> forceModeParam = new EnumParam<>("Force Mode", ForceMode.class);
    private final RangeParam maxVelocityParam = new RangeParam("Maximum Velocity", 1, 4000, 5000);
    private final RangeParam iterationsParam = new RangeParam("Path Length (Makes simulation slow!!)", 1, 100, 5000, true, BORDER, IGNORE_RANDOMIZE);
    private final BooleanParam antialiasParam = new BooleanParam("Use Antialiasing", false);

    private final RangeParam particlesParam = new RangeParam("Particle Count", 1, 1000, 20000, true, BORDER, IGNORE_RANDOMIZE);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");
    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1.0f), FREE_TRANSPARENCY);
    private final ColorParam particleColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), FREE_TRANSPARENCY);
    private final IntChoiceParam initialColorsParam = new IntChoiceParam("Initialize colors,", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Default", COLOR_SOURCE_DEFAULT),
        new IntChoiceParam.Item("Source Image", COLOR_SOURCE_FROM_SOURCE_IMAGE),
        new IntChoiceParam.Item("Acceleration", COLOR_SOURCE_FROM_ACCELERATION_1),
        new IntChoiceParam.Item("Acceleration 2", COLOR_SOURCE_FROM_ACCELERATION_2)
    });
    private final BooleanParam useSourceImageAsStartingPositionParam = new BooleanParam("Start flow from source,", false, IGNORE_RANDOMIZE);
    private final RangeParam colorRandomnessParam = new RangeParam("Color Randomness (%)", 0, 0, 100);
    private final RangeParam radiusRandomnessParam = new RangeParam("Stroke Width Randomness (%)", 0, 0, 1000);

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="CONSTRUCTOR, SECOND STEP FOR INITIALIZATION">

    public FlowField() {
        super(false);

        GroupedRangeParam forceMixerParam = new GroupedRangeParam("Force Mixer", new RangeParam[]{
            noiseParam,
            sinkParam,
            revolveParam
        }, false).autoNormalized();

        DialogParam noiseAdjustmentParam = new DialogParam("Noise", zoomParam, varianceParam, turbulenceParam, windParam);
        noiseParam.setupEnableOtherIfNotZero(noiseAdjustmentParam);

        DialogParam advancedParam = new DialogParam("Advanced", forceModeParam, maxVelocityParam, iterationsParam, antialiasParam);

        setParams(

            forceMixerParam,
            noiseAdjustmentParam,

            particlesParam,
            strokeParam,
            backgroundColorParam,
            particleColorParam,
            initialColorsParam,
            useSourceImageAsStartingPositionParam,
            colorRandomnessParam,
            radiusRandomnessParam,

            advancedParam

        ).withAction(ReseedSupport.createSimplexAction());

        noiseParam.setToolTip("Add smooth randomness to the flow of particles.");
        sinkParam.setToolTip("Make particles flow towards the center point.");
        revolveParam.setToolTip("Make particles flow around the center point.");

        noiseAdjustmentParam.setToolTip("Some additional tweaks to manipulate the evaluation system.");
        zoomParam
            .setToolTip("Change zoom to make fields look bigger or smaller. Please note, that particle width will not take any effect.");
        varianceParam.setToolTip("Increase variance to add more turns and twists to the flow.");
        turbulenceParam.setToolTip("Increase to make the flow path rougher.");
        windParam.setToolTip("Spreads away the particles against the flow path.");

        advancedParam.setToolTip("Advanced or Rarely used parameters which make slight adjustments to the simulation.");
        forceModeParam.setToolTip("Advanced control over how forces act.");
        maxVelocityParam.setToolTip("Adjust maximum velocity to make particles look more organised.");
        iterationsParam.setToolTip("Make individual particles cover longer paths.");

        particlesParam.setToolTip("Adjust the number of particles flowing in the field.");
        strokeParam.setToolTip("Adjust how particles are drawn - their width, shape, joins...");
        backgroundColorParam
            .setToolTip("Fills the canvas with a color. Decrease transparency to show the previous image.");
        particleColorParam
            .setToolTip("Change the initial color of the particles. Play with transparency to get interesting fills.");
        initialColorsParam
            .setToolTip("Make particles use the same color from their positions on the source image.");
        useSourceImageAsStartingPositionParam.setToolTip("Prevent particles from spawning at any transparent regions.");
        colorRandomnessParam.setToolTip("Increase to impart the particle color with some randomness.");
        radiusRandomnessParam.setToolTip("Increase to draw particles with a randomised width.");

    }

    //</editor-fold>

    //<editor-fold desc="FLOW FIELD CREATION LOGIC">

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        //<editor-fold defaultstate="collapsed" desc="FINAL LOCAL VARIABLES DECLARATIONS">

        final float zoom = zoomParam.getValue() * 0.25f;
        final float variance = varianceParam.getValue() / 10.0f;
        final int turbulence = turbulenceParam.getValue();
        final float zFactor = windParam.getValueAsFloat() / 10000;

        final ForceMode forceMode = forceModeParam.getSelected();
        final float maximumVelocitySq = maxVelocityParam.getValue() * maxVelocityParam.getValue() / 10000.0f;
        final int iterationCount = iterationsParam.getValue() + 1;
        final boolean antialias = antialiasParam.isChecked();

        final int particleCount = particlesParam.getValue();
        final Stroke stroke = strokeParam.createStroke();
        final Color bgColor = backgroundColorParam.getColor();
        final Color particleColor = particleColorParam.getColor();
        final int colorSource = initialColorsParam.getValue();
        final boolean inheritSpawnPoints = useSourceImageAsStartingPositionParam.isChecked();
        final float colorRandomness = colorRandomnessParam.getPercentageValF();
        final float radiusRandomness = radiusRandomnessParam.getPercentageValF();

        final float quality = min(QUALITY, SMOOTHNESS / zoom);
        final int tolerance = min(TOLERANCE, ((int) (iterationCount * ITERATION_TO_TOLERANCE_RATIO)));
        final float force = min(FORCE_MAGNITUDE, iterationCount * ITERATION_TO_FORCE_RATIO);

        final float multiplierNoise = noiseParam.getPercentageValF() * force;
        final float multiplierSink = sinkParam.getPercentageValF() * force;
        final float multiplierRevolve = revolveParam.getPercentageValF() * force;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        final int imgWidth = dest.getWidth();
        final int imgHeight = dest.getHeight();

        final int fieldWidth = (int) (imgWidth * quality + 1);
        final float fieldDensity = fieldWidth * 1.0f / imgWidth;
        final int fieldHeight = (int) (imgHeight * fieldDensity);

        final Random r = ReseedSupport.getLastSeedRandom();
        final OpenSimplex2F noise = ReseedSupport.getLastSeedSimplex();

        final Vector2D center = new Vector2D(fieldWidth / 2f, fieldHeight / 2f);
        final Rectangle bounds = new Rectangle(-PAD, -PAD,
            fieldWidth + PAD * 2, fieldHeight + PAD * 2);
        float variantPI = (float) FastMath.PI * variance;
        float initTheta = (float) (r.nextFloat() * 2 * FastMath.PI);

        final int groupCount = ceilToInt(particleCount / (double) PARTICLES_PER_GROUP);
        final var pt = new StatusBarProgressTracker(NAME, groupCount);

        final Graphics2D g2 = dest.createGraphics();
        final boolean useColorField = colorRandomness != 0 | ((colorSource & 1) == 1);
        final boolean randomizeRadius = radiusRandomness != 0;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setStroke(stroke);
        Colors.fillWith(bgColor, g2, imgWidth, imgHeight);
        g2.setColor(particleColor);
        if (antialias) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        final Color[][] fieldColors = getIf(useColorField, () -> new Color[fieldWidth][fieldHeight]);
        final Stroke[] strokes = getIf(randomizeRadius, () -> new Stroke[100]);
        final Vector2D[][] fieldAccelerations = new Vector2D[fieldWidth][fieldHeight];
        final int[] sourcePixels = getIf(useColorField | inheritSpawnPoints, () -> ImageUtils.getPixelsAsArray(src));

        if (randomizeRadius) {
            fill(strokes, strokes.length, () -> strokeParam.createStrokeWithRandomWidth(r, radiusRandomness));
        }

        initializeAcceleration(multiplierNoise, multiplierSink, multiplierRevolve, zoom, turbulence, zFactor, fieldWidth, fieldHeight, noise, center, variantPI, initTheta, fieldAccelerations);

        if (useColorField) {

            GoldenRatio goldenRatio = new GoldenRatio(r, particleColor, colorRandomness);

            if (colorSource == COLOR_SOURCE_FROM_SOURCE_IMAGE) {
                fill(fieldColors, fieldWidth, fieldHeight, (x, y) -> goldenRatio
                    .next(colorFromSourceImage(x, y, imgWidth, sourcePixels, fieldDensity)));

            } else if (colorSource == COLOR_SOURCE_FROM_ACCELERATION_1) {
                fill(fieldColors, fieldWidth, fieldHeight, (x, y) -> goldenRatio
                    .next(colorFromAcceleration(x, y, fieldAccelerations, particleColor)));

            } else if (colorSource == COLOR_SOURCE_FROM_ACCELERATION_2) {
                fill(fieldColors, fieldWidth, fieldHeight, (x, y) -> goldenRatio
                    .next(colorFromAcceleration2(x, y, fieldAccelerations, particleColor)));

            } else {
                fill(fieldColors, fieldWidth, fieldHeight, goldenRatio::next);
            }

        }

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
            .setParticleCreator(() -> new FlowFieldParticle(g2, randomizeRadius ? strokes[r
                .nextInt(strokes.length)] : stroke, meta))
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

    private void initializeAcceleration(float multiplierNoise, float multiplierSink, float multiplierRevolve, float zoom, int turbulence, float zFactor, int fieldWidth, int fieldHeight, OpenSimplex2F noise, Vector2D center, float variantPI, float initTheta, Vector2D[][] fieldAccelerations) {
        Vector2D position = new Vector2D();
        Vector2D forceDueToNoise = new Vector2D();
        Vector2D forceDueToSink = new Vector2D();
        Vector2D forceDueToRevolution = new Vector2D();

        for (int i = 0; i < fieldWidth; i++) {
            for (int j = 0; j < fieldHeight; j++) {

                position.set(i, j);

                Forces.createSinkForce(position, center, multiplierSink, forceDueToSink);

                Forces.createRevolveForce(position, center, multiplierRevolve, forceDueToRevolution);

                if (zFactor == 0) {
                    Forces
                        .createNoiseForce(multiplierNoise, initTheta, variantPI, position.x / zoom, position.y / zoom, 0, turbulence, noise, forceDueToNoise);
                }

                fieldAccelerations[i][j] = Vector2D.add(forceDueToRevolution, forceDueToSink, forceDueToNoise);
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
        if (value) {
            return supplier.get();
        }
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

    public static int colorSourceInt(int index, boolean requiresColorField) {
        index <<= 1;
        if (requiresColorField) {
            index |= 1;
        }
        return index;
    }

    public static Color colorFromSourceImage(int x, int y, int imgWidth, int[] sourcePixels, float fieldDensity) {
        x /= fieldDensity;
        y /= fieldDensity;
        int i = toRange(0, sourcePixels.length, x + y * imgWidth);
        return new Color(sourcePixels[i], true);
    }

    public static Color colorFromAcceleration(int x, int y, Vector2D[][] fieldAccelerations, Color particleColor) {
        Vector2D d = fieldAccelerations[x][y];
        float ra = sigmoidFit(d.x) / 255, rb = sigmoidFit(d.y) / 255;
        return new Color(
            ra * particleColor.getRed(),
            rb * particleColor.getGreen(),
            ra * particleColor.getBlue(),
            particleColor.getAlpha() / 255f);
    }

    public static Color colorFromAcceleration2(int x, int y, Vector2D[][] fieldAccelerations, Color particleColor) {
        Vector2D d = fieldAccelerations[x][y];
        int hsbColor = Color.HSBtoRGB(d.x / 50.0f + d.y / 50.0f, 0.8f, 1.0f);
        int r = (hsbColor >> 16) & 0xFF;
        int g = (hsbColor >> 8) & 0xFF;
        int b = hsbColor & 0xFF;
        int a = particleColor.getAlpha();
        return new Color(
            r * particleColor.getRed() / 65025f,
            g * particleColor.getGreen() / 65025f,
            b * particleColor.getBlue() / 65025f,
            a / 255f);
    }

    public static float sigmoidFit(float v) {
        return (1 + sigmoid(v)) / 2;
    }

    public static float sigmoid(float v) {
        return (float) (1 / (1 + FastMath.exp(-v)));
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
                                           Vector2D[][] fieldAccelerations) implements Modifier<FlowFieldParticle> {

        @Override
        public void modify(FlowFieldParticle particle) {
            particle.delta.set(fieldAccelerations[particle.getFieldX()][particle.getFieldY()]);
        }

    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="FLOW FIELD PARTICLE">

    public static class FlowFieldParticle extends SmoothPathParticle {

        public final Vector2D delta = new Vector2D();
        public final Vector2D acc = new Vector2D();

        private final Stroke stroke;
        private final FlowFieldMeta meta;

        public FlowFieldParticle(Graphics2D g2, Stroke stroke, FlowFieldMeta meta) {
            super(g2);
            this.vel = new Vector2D();
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
            Vector2D oldVel = new Vector2D(vel.x, vel.y);

            if (meta.zFactor != 0) {
                double sampleX = pos.getX() / meta.zoom;
                double sampleY = pos.getY() / meta.zoom;
                double sampleZ = meta.zFactor * iterationIndex;
                Vector2D noiseDelta = new Vector2D();
                Forces
                    .createNoiseForce(meta.multiplierNoise, meta.initTheta, meta.variantPI, sampleX, sampleY, sampleZ, meta.turbulence, meta.noise, noiseDelta);
                delta.add(noiseDelta);
            }

            meta.forceMode.modify(this);

            if (vel.lengthSq() > meta.maximumVelocitySq) {
                vel.set(oldVel);
            }

            if (anyDisplacementInXOrYIsGreaterThanTolerance()) {
                addPoint(Geometry.newFrom(pos));
            }
        }

        private boolean anyDisplacementInXOrYIsGreaterThanTolerance() {
            return abs(las_pos.getX() - pos.getX()) > meta.tolerance || abs(las_pos.getY() - pos
                .getY()) > meta.tolerance;
        }

        public int getFieldX() {
            return FastMath.toRange(0, meta.fieldWidth, (int) pos.getX());
        }

        public int getFieldY() {
            return FastMath.toRange(0, meta.fieldHeight, (int) pos.getY());
        }
    }

    public record FlowFieldMeta(int fieldWidth, int fieldHeight, float fieldDensity, Rectangle bounds,
                                double tolerance, float maximumVelocitySq, double zFactor, double zoom, int turbulence,
                                OpenSimplex2F noise, float multiplierNoise, float initTheta, float variantPI,
                                ForceMode forceMode) {
    }

    //</editor-fold>

}
